package com.example.contract_generator



import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/attachments")
class AttachmentController(private val attachmentService: AttachmentService) {


    @PostMapping("/upload", consumes = ["multipart/form-data"])
    fun uploadFile(@RequestParam("file") file: MultipartFile): Attachment {
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
