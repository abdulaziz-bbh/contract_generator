package com.example.contract_generator

import com.fasterxml.jackson.databind.ObjectMapper
import com.itextpdf.text.Document
import com.itextpdf.text.Paragraph
import com.itextpdf.text.pdf.PdfWriter
import jakarta.persistence.EntityManager
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.transaction.Transactional
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.*
import java.nio.file.Files
import java.nio.file.StandardCopyOption


interface UserService{
    fun createOperator(request: CreateOperatorRequest)
    fun existsUserData(pnfl: String, passportId: String, phoneNumber: String)
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
    fun create(request: TemplateCreateRequest, multipartFile: MultipartFile)
    fun delete(id: Long)
}

interface AttachmentService {
    fun upload(multipartFile: MultipartFile): AttachmentInfo
    fun download(id: Long): ResponseEntity<*>
    fun preview(id: Long): ResponseEntity<*>
    fun findById(id: Long): Attachment
    fun convertDocxToPdf(docxFile: File): Attachment
}
interface ContractService {
    fun generateContract(contractRequestDto: ContractRequestDto): Contract
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

    override fun create(request: TemplateCreateRequest, multipartFile: MultipartFile) {
        // 1. Faylni yuklash
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
                keyRepository.findByKeyAndDeletedFalse(keyString) ?: throw KeyAlreadyExistsException()
            }
        }
        val template = templateMapper.toEntity(request,attachment,keyEntities)
        templateRepository.save(template)
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
        existsUserData(request.pnfl, request.passportId, request.phoneNumber)
        userRepository.save(userMapper.toEntity(request))
    }

    override fun existsUserData(pnfl: String, passportId: String, phoneNumber: String) {
        if (userRepository.existsByPnflOrPassportIdOrPhoneNumber(pnfl, passportId, phoneNumber))
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
            director = SecurityContextHolder.getContext().authentication.principal as User

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
        existsUserData(request.pnfl, request.passportId, request.phoneNumber)
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
    private fun existsUserData(pnfl: String, passportId: String, phoneNumber: String) {
        if (userRepository.existsByPnflOrPassportIdOrPhoneNumber(pnfl, passportId, phoneNumber))
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
    @Transactional
    override fun upload(multipartFile: MultipartFile): AttachmentInfo {
        val entity = attachmentMapper.toEntity(multipartFile)
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
    override fun convertDocxToPdf(docxFile: File): Attachment {

        val directory = docxFile.absolutePath.substringBeforeLast("/")
        val uuid = docxFile.nameWithoutExtension
        val pdfFile = File(directory, "$uuid.pdf")
        pdfFile.createNewFile()

        val docxInputStream: InputStream = FileInputStream(docxFile)
        XWPFDocument(docxInputStream).use { document ->
            FileOutputStream(pdfFile).use { pdfOutputStream ->
                val pdfDocument = Document()
                PdfWriter.getInstance(pdfDocument, pdfOutputStream)
                pdfDocument.open()

                val paragraphs: List<XWPFParagraph> = document.getParagraphs()
                for (paragraph in paragraphs) {
                    pdfDocument.add(Paragraph(paragraph.text))
                }
                pdfDocument.close()
            }
        }
        val attachment = Attachment(
            name = uuid,
            contentType = "application/pdf",
            size = pdfFile.length(),
            extension = "pdf",
            path = directory + ".pdf"
        )
        return repository.save(attachment)
    }
}

@Service
class ContractServiceImpl(private val repository: AttachmentRepository,
                          private val templateService: TemplateService,
                          private val contractRepository: ContractRepository,
                          private val clientRepository: ClientRepository,
                          private val attachmentMapper: AttachmentMapper,
                          private val attachmentService: AttachmentService) : ContractService {
    @Transactional
    override fun generateContract(contractRequestDto: ContractRequestDto): Contract {
        val template = templateService.getOne(contractRequestDto.templateId)
        val attachment = attachmentService.download(template.file?.id!!)

        val inputStreamResource = attachment.body as? InputStreamResource
            ?: throw IllegalArgumentException("Attachment body is not an InputStreamResource")

        val (_, uuid, path) = attachmentMapper.createDirectoryPath(subFolder = "contract")
        val tempFile = File("$path/$uuid.docx")

        inputStreamResource.inputStream.use { inputStream ->
            Files.copy(inputStream, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        }

        val replacedFile = replaceKeysWithStyles(tempFile, contractRequestDto.keys)

        val pdfFile = attachmentService.convertDocxToPdf(replacedFile)

        val contract = Contract(
            file = pdfFile,
//            operators = mutableListOf()
        )
        val savedContract = contractRepository.save(contract)

//        contractRequestDto.keys.forEach { (key, value) ->
//            val contractData = ContractData(
//                contract = savedContract,
//                key = key,
//                value = value
//            )
//            contractRepository.save(contractData)
//        }

        // Add the operator to the contract
//        val operator = SecurityContextHolder.getContext().authentication.principal as User
//        contract.operators.add(operator)

        return contractRepository.save(contract)
    }

    private fun replaceKeysWithStyles(docxFile: File, keys: Map<String, String>): File {
        val wordDoc = XWPFDocument(Files.newInputStream(docxFile.toPath()))

        // Replace keys in paragraphs
        wordDoc.paragraphs.forEach { paragraph ->
            replaceTextInRuns(paragraph.runs, keys)
        }

        // Replace keys in tables
        wordDoc.tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    cell.paragraphs.forEach { paragraph ->
                        replaceTextInRuns(paragraph.runs, keys)
                    }
                }
            }
        }

        // Save the updated document
        val replacedFile = File(docxFile.parentFile, "replaced_${docxFile.name}")
        Files.newOutputStream(replacedFile.toPath()).use { wordDoc.write(it) }
        return replacedFile
    }

    private fun replaceTextInRuns(runs: List<XWPFRun>, keys: Map<String, String>) {
        runs.forEach { run ->
            keys.forEach { (key, value) ->
                if (run.text().contains(key)) {
                    // Preserve existing style attributes
                    val fontFamily = run.fontFamily
                    val fontSize = run.fontSize
                    val bold = run.isBold
                    val italic = run.isItalic
                    val color = run.color

                    // Replace text while keeping the style
                    val newText = run.text().replace(key, value)
                    run.setText(newText, 0)

                    run.fontFamily = fontFamily
                    run.fontSize = fontSize
                    run.isBold = bold
                    run.isItalic = italic
                    run.color = color
                }
            }
        }
    }






}

