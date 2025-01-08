package com.example.contract_generator

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.time.LocalDate
import java.util.*


class AttachmentMapper {
    companion object {
        @Value("\${file.path}")
        lateinit var filePath: String

        fun createDirectoryPath(
            date: LocalDate = LocalDate.now(),
            subFolder: String? = null,
            contentType: String? = null
        ): Triple<File, UUID, String> {
            val uuid = UUID.randomUUID()
            val baseDir = "$filePath/${date.year}/${date.monthValue}/${date.dayOfMonth}"

            val fullPath = when {
                subFolder != null -> "$baseDir/$subFolder"
                contentType != null -> "$baseDir/${contentType.split("/")[0]}"
                else -> baseDir
            }

            val directory = File(fullPath)
            if (!directory.exists()) {
                directory.mkdirs()
            }

            return Triple(directory, uuid, fullPath)
        }

        fun toEntity(multipartFile: MultipartFile): Attachment {
            val contentType = multipartFile.contentType ?: throw IllegalArgumentException("Content type is required")
            val split = contentType.split("/")
            val extension = split.getOrElse(1) { "" }

            val (_, uuid, path) = createDirectoryPath(contentType = contentType)

            return Attachment(
                name = uuid.toString(),
                contentType = contentType,
                size = multipartFile.size,
                extension = extension,
                path = "$path/$uuid.$extension"
            )
        }
    }
}