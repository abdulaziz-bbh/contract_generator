package com.example.contract_generator

import com.fasterxml.jackson.annotation.JsonFormat
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(GenericException::class)
    fun handleAccountException(exception: GenericException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
}


@RestController
@RequestMapping("/api/v1/keys")
class KeyController(val service: KeyService) {

    @GetMapping
    fun getAll() = service.getAll()


    @GetMapping("/page")
    fun getAll(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "10") size: Int) =
        service.getAll(page, size)


    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)


    @PostMapping
    fun create(@RequestBody @Valid request: KeyCreateRequest) = service.create(request)


    @PutMapping("{id}")
    fun update(@PathVariable id: Long, @RequestBody @Valid request: KeyUpdateRequest) = service.update(id, request)


    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}
@RestController
@RequestMapping("/api/contracts")
class ContractController(val service:ContractService) {
    @PostMapping()
    fun generate(@RequestBody @Valid request: List<ContractRequestDto>): ResponseEntity<*> {
        return service.generateContract(request)
    }
    @PutMapping("/update")
    fun update(@RequestBody @Valid request: List<ContractUpdateDto>): ResponseEntity<*> {
        return service.updateContract(request)
    }
    @GetMapping
    fun getAll( @RequestParam("date") @JsonFormat(pattern = "yyyy-MM-dd") date: LocalDate) = service.getPdfsZip(date)

    @GetMapping("/get")
    fun getZip( @RequestParam list: List<String>) = service.getZip(list)

}
    @RestController
    @RequestMapping("/api/v1/templates")
    class TemplateController(val service: TemplateService) {

        @GetMapping
        fun getAll() = service.getAll()


        @GetMapping("/page")
        fun getAll(
            @RequestParam(value = "page", defaultValue = "0") page: Int,
            @RequestParam(value = "size", defaultValue = "10") size: Int
        ) =
            service.getAll(page, size)



    @PostMapping("{organization-id}",consumes = ["multipart/form-data"])
    fun create(
        @PathVariable("organization-id") organizationId: Long,
        @RequestParam("file") multipartFile: MultipartFile) = service.create(organizationId,multipartFile)

        @PutMapping("{template-id}",consumes = ["multipart/form-data"])
        fun update(
            @PathVariable("template-id") templateId: Long,
            @RequestParam("file") multipartFile: MultipartFile
        ) = service.update(templateId, multipartFile)


        @DeleteMapping("{id}")
        fun delete(@PathVariable id: Long) = service.delete(id)
    }

@RestController
@RequestMapping("/api/v1/attachments")
class AttachmentController(private val service: AttachmentService) {

    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadFile(@RequestParam("file") file: MultipartFile): AttachmentInfo {
        return service.upload(file)
    }


    @GetMapping("/download/{id}")
    fun downloadFile(@PathVariable id: Long): ResponseEntity<*> {
        return service.download(id)
    }


    @GetMapping("/preview/{id}")
    fun previewFile(@PathVariable id: Long): ResponseEntity<*> {
        return service.preview(id)
    }

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.findById(id)

    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)

}



    @RestController
    @RequestMapping("/api/v1/auth")
    class AuthController(
        private val authService: AuthService,
    ) {

        @PostMapping("/sign-up")
        fun signup(@RequestBody @Valid request: CreateDirectorRequest) {
            return authService.registration(request)
        }

        @PostMapping("/sign-in")
        fun signIn(@RequestBody @Valid request: LoginRequest): AuthenticationDto {
            return authService.login(request)
        }

        @PostMapping("/refresh-token")
        fun refreshToken(request: HttpServletRequest, response: HttpServletResponse) {
            return authService.refreshToken(request, response)
        }
    }

    @RestController
    @RequestMapping("/api/v1/user")
    class UserController(
        private val userService: UserService
    ) {

        @PostMapping
        fun create(@RequestBody @Valid request: CreateOperatorRequest) {
            return userService.createOperator(request)
        }
    }

    @RestController
    @RequestMapping("/api/v1/organizations")
    class OrganizationController(
        private val organizationService: OrganizationService,
    ) {

        @PostMapping
        fun create(@RequestBody @Valid request: CreateOrganizationRequest) {
            organizationService.create(request)
        }
    }

