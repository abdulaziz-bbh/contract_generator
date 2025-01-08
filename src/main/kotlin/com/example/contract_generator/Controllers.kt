package com.example.contract_generator

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
) {

    @PostMapping("/sign-up")
    fun signup(@RequestBody @Valid request: CreateDirectorRequest){
        return authService.registration(request)
    }

    @PostMapping("/sign-in")
    fun signIn(@RequestBody @Valid request: LoginRequest): AuthenticationDto {
        return authService.login(request)
    }

    @PostMapping("/refresh-token")
    fun refreshToken(request: HttpServletRequest, response: HttpServletResponse){
        return authService.refreshToken(request, response)
    }
}

@RestController
@RequestMapping("/api/v1/user")
class UserController(
    private val userService: UserService
){

    @PostMapping
    fun create(@RequestBody @Valid request: CreateOperatorRequest){
        return userService.createOperator(request)
    }
}

@RestController
@RequestMapping("/api/v1/organizations")
class OrganizationController(
    private val organizationService: OrganizationService,
){

    @PostMapping
    fun create(@RequestBody @Valid request: CreateOrganizationRequest){
        organizationService.create(request)
    }
}


@RestController
@RequestMapping("/attachments")
class AttachmentController(private val attachmentService: AttachmentService) {


    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadFile(@RequestParam("file") file: MultipartFile): AttachmentInfo {
        return attachmentService.upload(file)
    }


    @GetMapping("/download/{id}")
    fun downloadFile(@PathVariable id: Long): ResponseEntity<*> {
        return attachmentService.download(id)
    }


    @GetMapping("/preview/{id}")
    fun previewFile(@PathVariable id: Long): ResponseEntity<*> {
        return attachmentService.preview(id)
    }
}
