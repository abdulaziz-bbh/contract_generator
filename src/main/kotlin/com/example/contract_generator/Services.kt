package com.example.contract_generator

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

    private fun toEntity(multipartFile: MultipartFile): Attachment {
        val contentType = multipartFile.contentType ?: throw IllegalArgumentException("Content type is required")
        val split = contentType.split("/")
        val date = LocalDate.now()
        val uuid = UUID.randomUUID()
//        val filename = multipartFile.originalFilename
        val extension = split.getOrElse(1) { "" }
        val path = "$filePath/${date.year}/${date.monthValue}/${date.dayOfMonth}/${split[0]}/$uuid.$extension"
        return Attachment(
            name = uuid.toString(),
            contentType = contentType,
            size = multipartFile.size,
            extension = extension,
            path = path
        )
    }
    private fun toInfo(attachment: Attachment): AttachmentInfo {
        return attachment.run { AttachmentInfo(
            id = id!!,
            name = name,
            contentType = contentType,
            size = size,
            extension = extension,
            path = path
        )
        }
    }


}
