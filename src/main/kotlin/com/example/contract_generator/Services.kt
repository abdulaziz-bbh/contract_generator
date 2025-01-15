package com.example.contract_generator

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Sort
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.time.LocalDate
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


interface UserService{
    fun createOperator(request: CreateOperatorRequest)
    fun updateOperator(request: UpdateOperatorRequest, id: Long)
    fun existsUserData(passportId: String, phoneNumber: String)
    fun dismissal (operatorId: Long, organizationId: Long)
    fun getAllByOrganizationId(organizationId: Long):List<UserDto>?
}
interface AuthService{
    fun registration(request: CreateDirectorRequest)
    fun login(request: LoginRequest) : AuthenticationDto
//    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse)
}
interface OrganizationService{
    fun create(request: CreateOrganizationRequest)
    fun update(request: UpdateOrganizationRequest, id: Long)
    fun existsByName(name: String)
}

interface KeyService {
    fun getAll(page: Int, size: Int): Page<KeyResponse>
    fun getAll(): List<KeyResponse>
    fun getOne(id: Long): KeyResponse
    fun create(request: KeyCreateRequest): KeyResponse
    fun update(id: Long, request: KeyUpdateRequest)
    fun delete(id: Long)
}

interface TemplateService {
    fun getAll(page: Int, size: Int): Page<TemplateResponse>
    fun getAll(): List<TemplateResponse>
    fun getOne(id: Long): TemplateResponse
    fun getTemplatesByOrganization(organizationId: Long): List<TemplateResponse>
    fun create( organizationId: Long, multipartFile: MultipartFile): TemplateDto
    fun delete(id: Long)
    fun update(templateId: Long, organizationId: Long, multipartFile: MultipartFile)
}

interface AttachmentService {
    fun upload(multipartFile: MultipartFile , subFolder : String? = null): AttachmentInfo
    fun download(id: Long): ResponseEntity<*>
    fun preview(id: Long): ResponseEntity<*>
    fun findById(id: Long): Attachment
    fun delete(id: Long)
}

interface ContractService{
    fun createContract(templateId: Long, list: List<ContractRequestDto>): List<ContractResponseDto>
    fun updateContract(list: List<ContractRequestDto>)
    fun getZip(list: List<String>): ResponseEntity<*>
    fun getPdfsZip(date: LocalDate):ResponseEntity<*>
    fun delete(contractIds: List<Long>)
    fun getAll(isGenerated:Boolean?): List<ContractResponseDto>
    fun findById(contractId: Long):ContractResponseDto
}

@Service
class ContractServiceImpl(
    private val templateRepository: TemplateRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentMapper: AttachmentMapper,
    private val contractRepository: ContractRepository,
    private val contractDataRepository: ContractDataRepository,
    private val keyRepository: KeyRepository,
    private val contractMapper: ContractMapper

): ContractService{
    @Transactional
    override fun createContract(templateId: Long, list: List<ContractRequestDto>): List<ContractResponseDto> {
        val template: Template = templateRepository.findByIdAndDeletedFalse(templateId)
            ?: throw TemplateNotFoundException()
        val currentUser = getCurrentUserId()?:throw UserNotFoundException()

        var responseDtos = mutableListOf<ContractResponseDto>()
        list.map { contractRequestDto ->
            val keyIds = contractRequestDto.contractData.keys
            val keys = keyRepository.findAllByIdInAndDeletedFalse(keyIds.toList())

            val unresolvedKeyIds = keyIds - keys.map { it.id }
            if (unresolvedKeyIds.isNotEmpty()) {
                throw KeyNotFoundException()
            }
            val contract = Contract(
                template = template,
            ).apply {
                operators.add(currentUser)
            }
            contractRepository.save(contract)

            val contractDataList = keys.map { key ->
                ContractData(
                    key = key,
                    value = contractRequestDto.contractData[key.id]?:throw KeyNotFoundException(),
                    contract = contract
                )
            }
            contractDataRepository.saveAll(contractDataList)
            responseDtos.add(contractMapper.toDto(contract, contractDataList))
        }
        return responseDtos
    }



    @Transactional
    override fun updateContract(list: List<ContractRequestDto>){
        list.forEach { contractRequestDto ->
            val contractDataIds = contractRequestDto.contractData.keys
            val contractDataList = contractDataRepository.findAllByIdInAndDeletedFalse(contractDataIds.toList())

            val unresolvedDataIds = contractDataIds - contractDataList.mapNotNull { it.id }
            if (unresolvedDataIds.isNotEmpty()) {
                throw ContractDataNotFound()
            }
            contractDataList.forEach { contractData ->
                val newValue = contractRequestDto.contractData[contractData.id]
                if (newValue != null) {
                    contractData.value = newValue
                }
            }
            contractDataRepository.saveAll(contractDataList)
        }
    }


    override fun getZip(list: List<String>): ResponseEntity<*> {
        var files :MutableList<File> = mutableListOf()
        for (s in list) {
            files.add(contractRepository.findByFile_Name(s)?.let {
                File(it.file?.path)
            }
                ?: throw ContractNotFound())
        }
        val pdfPath = "${attachmentMapper.filePath}/pdf"
        val file = File(pdfPath)
        if (!file.exists()) {
            file.mkdirs()
        }
        val pdfFiles = mutableListOf<File>()
        for (f in files) {
            convertDocxToPdf(f.absolutePath, pdfPath)
            pdfFiles.add(File("${pdfPath}/${f.nameWithoutExtension}.pdf"))
        }
        return createZipFile(pdfFiles)
    }

    override fun getPdfsZip(date: LocalDate): ResponseEntity<*> {

        val year = date.year
        val month = date.monthValue
        val day = date.dayOfMonth

        val contractFolderPath = "file/$year/$month/$day/contract"
        val contractFolder = File(contractFolderPath)

        if (!contractFolder.exists() || !contractFolder.isDirectory) {
            throw ContractNotFound()
        }

        val docxFiles = contractFolder.listFiles { file -> file.extension == "docx" } ?: emptyArray()

        if (docxFiles.isEmpty()) {
            return ResponseEntity.accepted().body<Any>(null)
        }
        val pdfPath = "${attachmentMapper.filePath}/pdf"
        val file = File(pdfPath)
        if (!file.exists()) {
            file.mkdirs()
        }

        val pdfFiles = mutableListOf<File>()
        for (docxFile in docxFiles) {
            convertDocxToPdf(docxFile.absolutePath, pdfPath)
            pdfFiles.add(File("${pdfPath}/${docxFile.nameWithoutExtension}.pdf"))
        }

        return createZipFile(pdfFiles)
    }

    @Transactional
    override fun delete(contractIds: List<Long>) {
        val trashedContracts = contractRepository.trashList(contractIds)
        if (trashedContracts.any { it == null }) {
            throw ContractNotFound()
        }
        trashedContracts.filterNotNull().forEach { contract ->
            val contractDataIds = contractDataRepository.findAllByContract(contract).mapNotNull { it.id }
            contractDataRepository.trashList(contractDataIds)
        }
    }



    override fun getAll(isGenerated: Boolean?): List<ContractResponseDto> {
        val contracts = if (isGenerated != null) {
            contractRepository.findByIsGeneratedAndDeletedFalse(isGenerated)
        } else {
            contractRepository.findAllNotDeleted()
        }
        return contracts.map { contract ->
            contractMapper.toDto(contract, contractDataRepository.findAllByContract(contract))
        }
    }


    override fun findById(contractId: Long): ContractResponseDto {
        val contract = contractRepository.findByIdAndDeletedFalse(contractId)
            ?: throw ContractNotFound()

        return contractMapper.toDto(contract, contractDataRepository.findAllByContract(contract))
    }


    private fun replaceKeysWithStyles(docxFile: File, keys: Map<String, String>): File {
        val wordDoc = XWPFDocument(Files.newInputStream(docxFile.toPath()))

        wordDoc.paragraphs.forEach { paragraph ->
            replaceTextInRuns(paragraph.runs, keys)
        }

        wordDoc.tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    cell.paragraphs.forEach { paragraph ->
                        replaceTextInRuns(paragraph.runs, keys)
                    }
                }
            }
        }

        val replacedFile = File(docxFile.parentFile, "contract_${docxFile.name}")
        Files.newOutputStream(replacedFile.toPath()).use { wordDoc.write(it) }
        docxFile.delete()
        return replacedFile
    }

    fun convertDocxToPdf(inputFile: String, outputFileDir: String) {
        val processBuilder = ProcessBuilder(
            "libreoffice",
            "--headless",
            "--convert-to", "pdf",
            "--outdir", outputFileDir,
            inputFile
        )

        try {
            val process =processBuilder.start()
            process.waitFor()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun createZipFile(files: List<File>): ResponseEntity<*> {
        val zipFilePath =  "${attachmentMapper.filePath}/zip/${UUID.randomUUID()}.zip"

        val parentDir = File(zipFilePath).parentFile
        if (!parentDir.exists()) {
            parentDir.mkdirs()
        }
        val zipFile = File(zipFilePath)

        try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
                for (file in files) {
                    if (file.exists()) {
                        FileInputStream(file).use { input ->
                            val entry = ZipEntry(file.name)
                            zipOut.putNextEntry(entry)
                            input.copyTo(zipOut)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val resource = InputStreamResource(FileInputStream(zipFile))
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"${zipFile.name}\"")
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .body(resource)
    }


    private fun replaceTextInRuns(runs: List<XWPFRun>, keys: Map<String, String>) {
        runs.forEach { run ->
            keys.forEach { (key, value) ->
                if (run.text().contains(key)) {
                    val fontFamily = run.fontFamily
                    val fontSize = run.fontSize
                    val bold = run.isBold
                    val italic = run.isItalic
                    val color = run.color
                    val underline = run.underline

                    val newText = run.text().replace(key, value)
                    run.setText(newText, 0)

                    run.fontFamily = fontFamily
                    run.fontSize = fontSize
                    run.isBold = bold
                    run.isItalic = italic
                    run.color = color
                    run.underline = underline
                }
            }
        }
    }
}

@Service
class KeyServiceImpl(
    private val keyMapper: KeyMapper,
    private val entityManager: EntityManager,
    private val keyRepository: KeyRepository
) : KeyService {

    override fun getAll(page: Int, size: Int): Page<KeyResponse> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))
        val usersPage = keyRepository.findAllNotDeletedForPageable(pageable)
        return usersPage.map { keyMapper.toDto(it) }
    }

    override fun getAll(): List<KeyResponse> {
        val sortedKeys = keyRepository.findAllNotDeleted().sortedByDescending { it.createdAt }
        return sortedKeys.map {
            keyMapper.toDto(it)
        }
    }

    override fun getOne(id: Long): KeyResponse {
        keyRepository.findByIdAndDeletedFalse(id)?.let {
            return keyMapper.toDto(it)
        } ?: throw KeyNotFoundException()
    }

    override fun create(request: KeyCreateRequest): KeyResponse {
        val existingKey = keyRepository.findByKeyAndDeletedFalse(request.key)
        if (existingKey != null) throw KeyAlreadyExistsException()
        val key = keyMapper.toEntity(request)
        val savedKey = keyRepository.save(key)
        return keyMapper.toDto(savedKey)
    }

    override fun update(id: Long, request: KeyUpdateRequest) {
        if (request.key.isBlank()) {
            throw BadRequestException()
        }
        val key = keyRepository.findByIdAndDeletedFalse(id) ?: throw KeyNotFoundException()
        keyRepository.findByName(id, request.key)?.let { throw KeyAlreadyExistsException() }

        val updateKey = keyMapper.updateEntity(key, request)
        keyRepository.save(updateKey)
    }

    @Transactional
    override fun delete(id: Long) {
        keyRepository.trash(id) ?: throw KeyNotFoundException()
    }

}

@Service
class TemplateServiceImpl(
    private val keyService: KeyService,
    private val keyRepository: KeyRepository,
    private val templateMapper: TemplateMapper,
    private val attachmentService: AttachmentService,
    private val templateRepository: TemplateRepository,
    private val organizationRepository: OrganizationRepository
    //private val attachment: AttachmentMapper
) : TemplateService {

    override fun getAll(page: Int, size: Int): Page<TemplateResponse> {
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(Sort.Order.desc("createdAt")))
        val usersPage = templateRepository.findAllNotDeletedForPageable(pageable)
        return usersPage.map { templateMapper.toDto(it) }
    }

    override fun getAll(): List<TemplateResponse> {
        val sortedTemplates = templateRepository.findAllNotDeleted().sortedByDescending { it.createdAt }
        return sortedTemplates.map {
            templateMapper.toDto(it)
        }
    }

    override fun getOne(id: Long): TemplateResponse {
        templateRepository.findByIdAndDeletedFalse(id)?.let {
            return templateMapper.toDto(it)
        } ?: throw KeyNotFoundException()
    }

    override fun getTemplatesByOrganization(organizationId: Long): List<TemplateResponse> {
        val templates = templateRepository.findByOrganizationIdAndDeletedFalse(organizationId)
        val sortedTemplates = templates.sortedByDescending { it.createdAt }
        return sortedTemplates.map { templateMapper.toDto(it) }
    }

    override fun create(organizationId: Long, multipartFile: MultipartFile): TemplateDto {
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { OrganizationNotFoundException() }

        val allowedExtensions = listOf("doc", "docx")
        val originalFilename = multipartFile.originalFilename
            ?: throw InvalidFileFormatException()
        val fileExtension = originalFilename.substringAfterLast(".", "").lowercase()
        if (fileExtension !in allowedExtensions) {
            throw InvalidFileFormatException()
        }
        val templateName = multipartFile.originalFilename?.substringBeforeLast(".")
            ?: throw InvalidTemplateNameException()

        if (templateRepository.existsByTemplateNameAndOrganizationId(templateName, organizationId)) {
            throw TemplateAlreadyExistsException()
        }
        val attachmentInfo = attachmentService.upload(multipartFile)
        val attachment = attachmentService.findById(attachmentInfo.id)

        val extractedKeys = extractKeysFromFile(attachmentInfo)

        val keyEntities = extractedKeys.map { keyString ->
            val existingKey = keyRepository.findByKeyAndDeletedFalse(keyString)
            if (existingKey != null) {
                existingKey
            } else {
                val keyCreateRequest = KeyCreateRequest(key = keyString)
                keyService.create(keyCreateRequest)
                keyRepository.findByKeyAndDeletedFalse(keyString)
                    ?: throw KeyAlreadyExistsException()
            }
        }.toSet()

        val template = templateMapper.toEntity(templateName, attachment, keyEntities.toMutableList(), organization)
        val savedTemplate = templateRepository.save(template)
        return templateMapper.toTDto(savedTemplate)
    }

    private fun extractKeysFromFile(attachmentInfo: AttachmentInfo): List<String> {
        val regex = Regex("\\$([a-zA-Z0-9]+)\\$")
        val keys = mutableListOf<String>()

        FileInputStream(File(attachmentInfo.path)).use { fis ->
            XWPFDocument(fis).use { document ->
                (document.paragraphs.map { it.text } +
                        document.tables.flatMap { table ->
                            table.rows.flatMap { row -> row.tableCells.map { it.text } }
                        }).forEach { line ->
                    regex.findAll(line).forEach { match -> keys.add(match.value)
                    }
                }
            }
        }
        return keys
    }

    override fun update(templateId: Long, organizationId: Long, multipartFile: MultipartFile) {
        val existingTemplate = templateRepository.findByIdAndDeletedFalse(templateId)
            ?: throw TemplateNotFoundException()

        val updatedTemplateName = multipartFile.originalFilename?.substringBeforeLast(".")
            ?: existingTemplate.templateName

        val existingTemplateWithSameName = templateRepository.findByTemplateNameWithOrganizationIdAndDeletedFalse(
            updatedTemplateName, organizationId)
        if (existingTemplateWithSameName != null && existingTemplateWithSameName.id != templateId) {
            throw TemplateAlreadyExistsException()
        }
        val updatedAttachmentInfo = attachmentService.upload(multipartFile)
        val updatedAttachment = attachmentService.findById(updatedAttachmentInfo.id)

        if (existingTemplate.file.id != null) {
            attachmentService.delete(existingTemplate.file.id!!)
        }

        val extractedKeys = extractKeysFromFile(updatedAttachmentInfo)
        val existingKeys = keyRepository.findAllByKeyInAndDeletedFalse(extractedKeys)
        val existingKeyStrings = existingKeys.map { it.key }.toSet()

        val newKeys = extractedKeys.filter {  it !in existingKeyStrings }.map { keyString ->
            val keyCreateRequest = KeyCreateRequest(key = keyString)
            keyService.create(keyCreateRequest)
            keyRepository.findByKeyAndDeletedFalse(keyString)
                ?: throw KeyAlreadyExistsException()
        }
        val allKeys = existingKeys + newKeys

        val updatedTemplate = existingTemplate.apply {
            this.templateName = updatedTemplateName
            this.file = updatedAttachment
            this.keys = allKeys.toMutableList()
        }
        templateRepository.save(updatedTemplate)
    }


    @Transactional
    override fun delete(id: Long) {
        templateRepository.trash(id) ?: throw TemplateNotFoundException()
    }
} 


@Service
class UserServiceImpl(
    private val userRepository: UserRepository,
    private val userMapper: UserMapper,
    private val organizationRepository: OrganizationRepository,
    private val usersOrganizationRepository: UsersOrganizationRepository
): UserService {

    @Transactional
    override fun createOperator(request: CreateOperatorRequest) {
        existsUserData(request.passportId, request.phoneNumber)
        val operator = userRepository.save(userMapper.toEntity(request))
        val usersOrganization = UsersOrganization(
            user = operator,
            organization = organizationRepository.findByIdAndDeletedFalse(request.organizationId)
                ?: throw OrganizationNotFoundException(),
            isCurrentUser = true
        )
        usersOrganizationRepository.save(usersOrganization)
    }

    override fun updateOperator(request: UpdateOperatorRequest, id: Long) {
     val user =  id.let {
            userRepository.findByIdAndDeletedFalse(it)?: throw  UserNotFoundException()
        }
        request.passportId.let {
            if (it != null) {
                if (userRepository.findByPassportId(it, id) != null) {
                    throw PassportIdAlreadyUsedException()
                }
            }
        }
        request.phoneNumber.let {
            if (it != null) {
                if (userRepository.findByPhoneNumber(it, id) != null) {
                    throw UserAlreadyExistsException()
                }
            }
        }
        userRepository.save(userMapper.fromUpdateDto(request, user ))
    }

    override fun existsUserData(passportId: String, phoneNumber: String) {
        if (userRepository.existsByPassportId(passportId))
            throw UserAlreadyExistsException()
        if(userRepository.existsByPhoneNumber(phoneNumber))
            throw UserAlreadyExistsException()
    }

    override fun dismissal(operatorId: Long, organizationId: Long) {
        operatorId.let {
            userRepository.findByIdAndDeletedFalse(it)?: throw  UserNotFoundException()
        }
        organizationId.let {
            organizationRepository.findByIdAndDeletedFalse(it)?: throw OrganizationNotFoundException()
        }
        val usersOrganization = usersOrganizationRepository.findByOrganizationIdAndUserId(organizationId, operatorId)?: throw UserNotFoundException()
        usersOrganization.isCurrentUser = false
        usersOrganization.leftDate = Date(System.currentTimeMillis())
        usersOrganizationRepository.save(usersOrganization)
    }

    override fun getAllByOrganizationId(organizationId: Long): List<UserDto>? {
        return usersOrganizationRepository.findUsersByOrganizationId(organizationId)?.map { userMapper.toDto(it) }
    }
}

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository,
    private val userRepository: UserRepository,
    private val usersOrganizationRepository: UsersOrganizationRepository
): OrganizationService {

    @Transactional
    override fun create(request: CreateOrganizationRequest) {
        existsByName(request.name)
        var organization = Organization(
            name = request.name,
            address = request.address
        )
        organization = organizationRepository.save(organization)
        val usersOrganization = UsersOrganization(
            user = userRepository.findByIdAndDeletedFalse(getCurrentUserId()!!.id!!)
                ?: throw UserNotFoundException(),
            organization = organization,
            isCurrentUser = true
        )
        usersOrganizationRepository.save(usersOrganization)
    }

    override fun update(request: UpdateOrganizationRequest, id: Long) {
    }

    override fun existsByName(name: String) {
        name.let {
            if(organizationRepository.existsByName(name))
                throw OrganizationAlreadyExistsException()
        }
    }
}

@Service
class CustomUserDetailsService(private val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String?): User {
        if(username.isNullOrBlank() || username.isBlank())
            throw UsernameInvalidException()
        return userRepository.findByPhoneNumber(username) ?: throw UserNotFoundException()
    }
}
@Service
class AuthServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val jwtProvider: JwtProvider,
    private val userMapper: UserMapper,
    private val userDetailsService: CustomUserDetailsService
) : AuthService {

    override fun registration(request: CreateDirectorRequest) {
        existsUserData(request.passportId, request.phoneNumber)
        userRepository.save(userMapper.toEntity(request))
    }

    override fun login(request: LoginRequest): AuthenticationDto {
        if (request.username.isBlank())
            throw UsernameInvalidException()
        authenticationManager.authenticate(
            UsernamePasswordAuthenticationToken(request.username, request.password))
        val user = userDetailsService.loadUserByUsername(request.username)
        val accessToken = jwtProvider.generateAccessToken(user)
        val refreshToken = jwtProvider.generateRefreshToken(user)
        return AuthenticationDto(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    private fun existsUserData(passportId: String, phoneNumber: String) {
        if (userRepository.existsByPassportId(passportId))
            throw UserAlreadyExistsException()
        if(userRepository.existsByPhoneNumber(phoneNumber))
            throw UserAlreadyExistsException()
    }
}


@Service
class AttachmentServiceImpl(
    private  val repository: AttachmentRepository,
    private val attachmentMapper: AttachmentMapper,
    ) : AttachmentService {
    @Value("\${file.path}")
    lateinit var filePath: String
  
    override fun upload(multipartFile: MultipartFile , subFolder : String?): AttachmentInfo {
        val entity = attachmentMapper.toEntity(multipartFile, subFolder)
        val file = File(entity.path).apply {
            parentFile.mkdirs()
        }.absoluteFile
        try {
            multipartFile.transferTo(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return attachmentMapper.toInfo(repository.save(entity))
    }


    @Throws(IOException::class)
    override fun download(id: Long): ResponseEntity<*> {
        val fileEntity = repository.findByIdAndDeletedFalse(id)?:throw AttachmentNotFound()

        return fileEntity.run{
            val inputStream = FileInputStream(path)
            val resource = InputStreamResource(inputStream)

            ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"${name}\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource)
        }
    }
    @Throws(IOException::class)
    override fun preview(id: Long): ResponseEntity<*> {
        val fileEntity = repository.findByIdAndDeletedFalse(id)?:throw AttachmentNotFound()

        return fileEntity.run {
            val inputStream = FileInputStream(path)
            val resource = InputStreamResource(inputStream)

            ResponseEntity.ok()
                .header("Content-Disposition", "inline; filename=\"${name}\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource)
        }
    }

    override fun findById(id: Long): Attachment {
        val attachment = repository.findById(id)
        if (attachment.isPresent) {
            return attachment.get()
        }else throw AttachmentNotFound()
    }

    @Transactional
    override fun delete(id: Long) {
        val file = findById(id)
        File(file.path).delete()
        repository.trash(id) ?: throw AttachmentNotFound()
    }
}

