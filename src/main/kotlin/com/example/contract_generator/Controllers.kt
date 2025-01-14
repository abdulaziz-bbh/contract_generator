package com.example.contract_generator

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile
import jakarta.validation.Valid
import jakarta.websocket.server.PathParam
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(GenericException::class)
    fun handlingException(exception: GenericException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(
        ex: AccessDeniedException,
        request: HttpServletRequest,
        response: HttpServletResponse
    ): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(BaseMessage(HttpStatus.FORBIDDEN.value(), ex.message))
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
        @RequestParam(value = "size", defaultValue = "10") size: Int
    ) =
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
@RequestMapping("api/job")
class JobController(private val jobService: JobService) {

    @GetMapping("/{isDoc}")
    fun generateContract(@RequestParam contractIds: List<Long>,@PathVariable isDoc: Boolean): JobDto {
        return jobService.generateContract(contractIds, isDoc)
    }
    @GetMapping("/status")
    fun generateContract(@RequestParam jobIds: List<Long>): Map<JobDto,String> {
        return jobService.getStatus(jobIds)
    }
}

@RestController
@RequestMapping("api/contracts")
class ContractController(
    private val contractService: ContractService
) {

    @PostMapping("/{templateId}")
    fun createContract(
        @PathVariable templateId: Long,
        @RequestBody list: List<ContractRequestDto>
    ): ResponseEntity<List<ContractResponseDto>> {
        val response = contractService.createContract(templateId, list)
        return ResponseEntity.ok(response)
    }

    @PutMapping
    fun updateContract(
        @RequestBody list: List<CreateContractDataDto>
    ): ResponseEntity<Void> {
        contractService.updateContract(list)
        return ResponseEntity.ok().build()
    }

    @DeleteMapping
    fun deleteContracts(
        @RequestParam contractIds: List<Long>
    ): ResponseEntity<Void> {
        contractService.delete(contractIds)
        return ResponseEntity.ok().build()
    }

    @GetMapping
    fun getAllContracts(
        @RequestParam(required = false) isGenerated: Boolean?
    ): ResponseEntity<List<ContractResponseDto>> {
        val response = contractService.getAll(isGenerated)
        return ResponseEntity.ok(response)
    }
    @GetMapping("/{contractId}")
    fun findContractById(
        @PathVariable contractId: Long
    ): ResponseEntity<ContractResponseDto> {
        val response = contractService.findById(contractId)
        return ResponseEntity.ok(response)
    }

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

    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)


    @GetMapping("/orgs/{organizationId}")
    fun getTemplatesByOrganizationId(@PathVariable organizationId: Long) =
        service.getTemplatesByOrganization(organizationId)


    @PostMapping("/{organization-id}", consumes = ["multipart/form-data"])
    fun create(
        @PathVariable("organization-id") organizationId: Long,
        @RequestParam("file") multipartFile: MultipartFile
    ) = service.create(organizationId, multipartFile)

    @PutMapping("{template-id}", consumes = ["multipart/form-data"])
    fun update(
        @PathVariable("template-id") templateId: Long,
        @RequestParam organizationId: Long,
        @RequestParam("file") multipartFile: MultipartFile
    ) = service.update(templateId,organizationId, multipartFile)


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


    @GetMapping("/download/{hashId}")
    fun downloadFile(@PathVariable("hashId") hashId: String): ResponseEntity<*> {
        return service.download(hashId)
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


//        @PostMapping("/refresh-token")
//        fun refreshToken(request: HttpServletRequest, response: HttpServletResponse) {
//            return authService.refreshToken(request, response)
//        }
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
        @PutMapping("/{id}")
        fun update(@RequestBody @Valid request: UpdateOperatorRequest, @PathVariable("id") id: Long) {
            return userService.updateOperator(request, id)
        }

        @PostMapping("dismissal")
        fun dismissal(
            @PathParam("operatorId") operatorId: Long,
            @PathParam("organizationId") organizationId: Long) {
            return userService.dismissal(operatorId, organizationId)
        }
        
        @PostMapping("recruitment")
        fun recruitment(
            @PathParam("organizationId") organizationId: Long,
            @PathParam("passportId") passportId: String) {
            return userService.recruitment(organizationId, passportId)
        }
        @GetMapping("get-all/{organization-id}")
        fun getOrganizations(@PathVariable("organization-id") organizationId: Long): List<UserDto>? {
            return userService.getAllByOrganizationId(organizationId)
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

    @GetMapping("/{director-id}")
    fun getAll(@PathVariable("director-id") id: Long): List<OrganizationDto> {
        return organizationService.getAll(id)
    }
}

