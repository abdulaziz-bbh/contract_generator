package com.example.contract_generator

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.InputStreamResource
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.time.LocalDate
import java.util.*


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


interface AttachmentService {
    fun upload(multipartFile: MultipartFile): AttachmentInfo
    fun download(id: Long): ResponseEntity<*>
    fun preview(id: Long): ResponseEntity<*>
}

@Service
class AttachmentServiceImpl(private  val repository: AttachmentRepository) : AttachmentService {
    @Value("\${file.path}")
    lateinit var filePath: String

    override fun upload(multipartFile: MultipartFile): AttachmentInfo {
        val entity = toEntity(multipartFile)
        val file = File(entity.path).apply {
            parentFile.mkdirs()
        }.absoluteFile

        try {
            multipartFile.transferTo(file)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

        return toInfo(repository.save(entity))
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
}

