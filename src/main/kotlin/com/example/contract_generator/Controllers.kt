package com.example.contract_generator

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

@ControllerAdvice
class ExceptionHandler(private val errorMessageSource: ResourceBundleMessageSource) {

    @ExceptionHandler(GenericException::class)
    fun handleAccountException(exception: GenericException): ResponseEntity<BaseMessage> {
        return ResponseEntity.badRequest().body(exception.getErrorMessage(errorMessageSource))
    }
}


@RestController
@RequestMapping("/api/keys")
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
    fun signup(@RequestBody @Valid request: ContractRequestDto): Contract {
        return service.generateContract(request)
    }
}

@RestController
@RequestMapping("/api/templates")
class TemplateController(val service: TemplateService) {

    @GetMapping
    fun getAll() = service.getAll()


    @GetMapping("/page")
    fun getAll(
        @RequestParam(value = "page", defaultValue = "0") page: Int,
        @RequestParam(value = "size", defaultValue = "10") size: Int) =
        service.getAll(page, size)


    @GetMapping("{id}")
    fun getOne(@PathVariable id: Long) = service.getOne(id)


    @PostMapping(consumes = ["multipart/form-data"])
    fun create(
        @RequestParam("file") multipartFile: MultipartFile) = service.create(multipartFile)


    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
}



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

