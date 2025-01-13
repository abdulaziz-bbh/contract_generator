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
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.time.LocalDate
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


interface UserService{
    fun createOperator(request: CreateOperatorRequest)
    fun existsUserData(passportId: String, phoneNumber: String)
}
interface AuthService{
    fun registration(request: CreateDirectorRequest)
    fun login(request: LoginRequest) : AuthenticationDto
    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse)
}
interface OrganizationService{
    fun create(request: CreateOrganizationRequest)
    fun update(request: UpdateOrganizationRequest)
    fun existsByName(name: String)
}

interface KeyService {
    fun getAll(page: Int, size: Int): Page<KeyResponse>
    fun getAll(): List<KeyResponse>
    fun getOne(id: Long): KeyResponse
    fun create(request: KeyCreateRequest)
    fun update(id: Long, request: KeyUpdateRequest)
    fun delete(id: Long)
}

interface TemplateService {
    fun getAll(page: Int, size: Int): Page<TemplateResponse>
    fun getAll(): List<TemplateResponse>
    fun getOne(id: Long): TemplateResponse
    fun create( organizationId: Long, multipartFile: MultipartFile): TemplateDto
    fun delete(id: Long)
    fun update(templateId: Long, multipartFile: MultipartFile)
}

interface AttachmentService {
    fun upload(multipartFile: MultipartFile , subFolder : String? = null): AttachmentInfo
    fun download(id: Long): ResponseEntity<*>
    fun preview(id: Long): ResponseEntity<*>
    fun findById(id: Long): Attachment
    fun delete(id: Long)
}

interface ContractService{
    fun generateContract(list: List<ContractRequestDto>): ResponseEntity<*>
    fun updateContract(list: List<ContractUpdateDto>): ResponseEntity<*>
    fun getPdfsZip(date: LocalDate):ResponseEntity<*>
}

@Service
class ContractServiceImpl(
    private val templateRepository: TemplateRepository,
    private val attachmentRepository: AttachmentRepository,
    private val attachmentMapper: AttachmentMapper,
    private val contractRepository: ContractRepository,
    private val contractDataRepository: ContractDataRepository

): ContractService{
    @Transactional
    override fun generateContract(list: List<ContractRequestDto>): ResponseEntity<*> {
        val files: MutableList<File> = mutableListOf()
        list.forEach { contractRequestDto ->
            val template: Template = templateRepository.findByIdAndDeletedFalse(contractRequestDto.id)
                ?: throw TemplateNotFoundException()

            val (_, uuid, path) = attachmentMapper.createDirectoryPath(subFolder = "contract")
            val tempFile = File("$path/$uuid.docx")

            Files.copy(Paths.get(template.file.path), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)


            val replacedFile = replaceKeysWithStyles(tempFile, contractRequestDto.keys)

            val attachment2 = Attachment(
                name = replacedFile.name,
                contentType = "application/docx",
                size = replacedFile.length(),
                extension = "docx",
                path = replacedFile.absolutePath
            )

            attachmentRepository.save(attachment2)

            val contract = Contract(
                file = attachment2,
                template = template,
            )

            getCurrentUserId()?.let { contract.operators.add(it) }?: throw UserNotFoundException()

            contractRequestDto.keys.forEach { (key, value) ->
                val contractData = ContractData(
                    contract = contract,
                    key = key,
                    value = value
                )
                contractDataRepository.save(contractData)
            }
            contract.status = ContractStatus.PENDING

            contractRepository.save(contract)
            files.add(replacedFile)
        }

        return createZipFile(files)
    }


    @Transactional
    override fun updateContract(list: List<ContractUpdateDto>): ResponseEntity<*>{

        val files: MutableList<File> = mutableListOf()
        list.forEach { contractUpdateDto ->
            val contract = contractRepository.findByFile_Name(contractUpdateDto.fileName)
                ?: throw ContractNotFound()

            val contractDatas = contractDataRepository
                .findAllByContract(contract)
            val keys = contractUpdateDto.keys

            for (contractData in contractDatas) {
                keys[contractData.key]?.let { newValue ->
                    contractData.value = newValue
                    contractDataRepository.save(contractData)
                }
            }

            val existingContractData: Map<String, String> = contractDatas.map { it.key to it.value }.toMap()

            val tempFile = File(contract.file.path)

            Files.copy(Paths.get(contract.template.file.path), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)

            val replacedFile = replaceKeysWithStyles(tempFile, existingContractData)

            contract.file.run {
                size = replacedFile.length()
            }

            attachmentRepository.save(contract.file)

            getCurrentUserId()?.let { contract.operators.add(it) }?:throw UserNotFoundException()
            contractRepository.save(contract)
            files.add(replacedFile)
        }

        return createZipFile(files)
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
        val pageable: Pageable = PageRequest.of(page, size)
        val usersPage = keyRepository.findAllNotDeletedForPageable(pageable)
        return usersPage.map { keyMapper.toDto(it) }
    }

    override fun getAll(): List<KeyResponse> {
        return keyRepository.findAllNotDeleted().map {
            keyMapper.toDto(it)
        }
    }

    override fun getOne(id: Long): KeyResponse {
        keyRepository.findByIdAndDeletedFalse(id)?.let {
            return keyMapper.toDto(it)
        } ?: throw KeyNotFoundException()
    }

    override fun create(request: KeyCreateRequest) {
        val existingKey = keyRepository.findByKeyAndDeletedFalse(request.key)
        if (existingKey != null) throw KeyAlreadyExistsException()
        keyRepository.save(keyMapper.toEntity(request))
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
        val pageable: Pageable = PageRequest.of(page, size)
        val usersPage = templateRepository.findAllNotDeletedForPageable(pageable)
        return usersPage.map { templateMapper.toDto(it) }
    }

    override fun getAll(): List<TemplateResponse> {
        return templateRepository.findAllNotDeleted().map {
            templateMapper.toDto(it)
        }
    }

    override fun getOne(id: Long): TemplateResponse {
        templateRepository.findByIdAndDeletedFalse(id)?.let {
            return templateMapper.toDto(it)
        } ?: throw KeyNotFoundException()
    }


    override fun create(organizationId: Long, multipartFile: MultipartFile): TemplateDto {
        val organization = organizationRepository.findById(organizationId)
            .orElseThrow { OrganizationNotFoundException() }

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

    override fun update(templateId: Long, multipartFile: MultipartFile) {
        val existingTemplate = templateRepository.findByIdAndDeletedFalse(templateId)
            ?: throw TemplateNotFoundException()

        val updatedAttachmentInfo = attachmentService.upload(multipartFile)
        val updatedAttachment = attachmentService.findById(updatedAttachmentInfo.id)

        val updatedTemplateName = multipartFile.originalFilename?.substringBeforeLast(".")
            ?: existingTemplate.templateName

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

        val oldFileName = existingTemplate.templateName
        val newFileName = multipartFile.originalFilename?.substringBeforeLast(".")
        if (oldFileName != null && newFileName != null &&  oldFileName == newFileName) {
            attachmentService.delete(existingTemplate.file.id!!)
        }

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
    private val userMapper: UserMapper
): UserService {

    override fun createOperator(request: CreateOperatorRequest) {
        existsUserData(request.passportId, request.phoneNumber)
        userRepository.save(userMapper.toEntity(request))
    }

    override fun existsUserData(passportId: String, phoneNumber: String) {
        if (userRepository.existsByPassportId(passportId))
            throw UserAlreadyExistsException()
        if(userRepository.existsByPhoneNumber(phoneNumber))
            throw UserAlreadyExistsException()
    }

}

@Service
class OrganizationServiceImpl(
    private val organizationRepository: OrganizationRepository
): OrganizationService {
    override fun create(request: CreateOrganizationRequest) {
        existsByName(request.name)
        val organization = Organization(
            name = request.name,
            address = request.address
        )
        organizationRepository.save(organization)
    }

    override fun update(request: UpdateOrganizationRequest) {
        TODO("Not yet implemented")
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
    override fun loadUserByUsername(username: String?): UserDetails {
        if(username.isNullOrBlank() || username.isBlank())
            throw UsernameInvalidException()
        return userRepository.findByPhoneNumber(username) ?: throw UserNotFoundException()
    }
}
@Service
class AuthServiceImpl(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val tokenRepository: TokenRepository,
    private val jwtProvider: JwtProvider,
    private val userMapper: UserMapper
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
        val user = userRepository.findByPhoneNumber(request.username) ?: throw UserNotFoundException()
        val accessToken = jwtProvider.generateAccessToken(user)
        val refreshToken = jwtProvider.generateRefreshToken(user)
        saveToken(user, accessToken)
        return AuthenticationDto(
            accessToken = accessToken,
            refreshToken = refreshToken
        )
    }

    override fun refreshToken(request: HttpServletRequest, response: HttpServletResponse) {
        val authHeader = request.getHeader(HttpHeaders.AUTHORIZATION)
        if (authHeader == null || authHeader.startsWith("Bearer ")) return
        val refreshToken  = authHeader.substring(7)
        val username = jwtProvider.extractUsername(refreshToken)
        username.let {
            val user = userRepository.findByPhoneNumber(username) ?: throw UserNotFoundException()
            if (jwtProvider.isTokenValid(refreshToken, user)){
                val accessToken = jwtProvider.generateAccessToken(user)
                revokeUserAllTokens(user)
                saveToken(user, accessToken)
                val dto = AuthenticationDto(
                    accessToken = accessToken,
                    refreshToken = refreshToken
                )
                ObjectMapper().writeValue(response.outputStream, dto)
            }
        }
    }

    private fun revokeUserAllTokens(user: User) {
        val tokens = user.id?.let { tokenRepository.findAllValidTokenByUser(it) }
        if (tokens.isNullOrEmpty()) return

        tokens.forEach { token -> run {
                token.revoked = true
                token.expired = true
            }
        }
        tokenRepository.saveAll(tokens)
    }
    private fun saveToken(user: User, jwt: String) {
        val token = Token(
            token = jwt,
            user = user,
            expired = false,
            revoked = false,
            tokenType = "Bearer"
        )
        tokenRepository.save(token)
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

