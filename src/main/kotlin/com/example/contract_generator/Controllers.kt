package com.example.contract_generator

import jakarta.validation.Valid
import org.springframework.context.support.ResourceBundleMessageSource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.multipart.MultipartFile

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
        @RequestParam("file") multipartFile: MultipartFile,
        @RequestParam @Valid request: TemplateCreateRequest) = service.create(request,multipartFile)


    @DeleteMapping("{id}")
    fun delete(@PathVariable id: Long) = service.delete(id)
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
