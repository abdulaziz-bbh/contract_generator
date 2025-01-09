package com.example.contract_generator

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

@Component
class KeyMapper {

    fun toDto(key: Key): KeyResponse {
        return key.run {
            KeyResponse(
                id = this.id,
                key = this.key
            )
        }
    }

    fun toEntity(createRequest: KeyCreateRequest): Key {
        return createRequest.run {
            Key(
                key = this.key
            )
        }
    }

    fun updateEntity(key: Key, updateRequest: KeyUpdateRequest): Key {
        return updateRequest.run {
            key.apply {
                updateRequest.key.let { this.key = it }
            }
        }
    }
}

@Component
class TemplateMapper {

    fun toDto(template: Template): TemplateResponse {
        return template.run {
            TemplateResponse(
                id = template.id,
                templateName = template.templateName,
                file = toAttachmentResponse(template.file),
                keys = template.keys.map { toKeyResponse(it) },
                organizationName = this.organization.name
            )
        }
    }

    private fun toKeyResponse(key: Key): KeyResponse {
        return KeyResponse(
            id = key.id,
            key = key.key
        )
    }

    private fun toAttachmentResponse(attachment: Attachment): AttachmentResponse {
        return AttachmentResponse(
            id = attachment.id,
            name = attachment.name,
            contentType = attachment.contentType,
            size = attachment.size,
            extension = attachment.extension,
            path = attachment.path
        )
    }

    fun toEntity(templateName: String, file: Attachment, keys: List<Key>, organization: Organization): Template {
        return  Template(
            templateName = templateName,
            file = file,
            keys = keys.toMutableList(),
            organization = organization
        )
    }

//    fun updateEntity(template: Template, updateRequest: TemplateUpdateRequest): Template {
//        return updateRequest.run {
//            template.apply {
//                updateRequest.templateName.let { this.templateName = it }
//                updateRequest.keys.let { this.keys = it }
//            }
//        }
//    }
}

@Component
class AttachmentMapper {
    @Value("\${file.path}")
    lateinit var filePath: String

    fun toEntity(multipartFile: MultipartFile): Attachment {
        val contentType = multipartFile.contentType ?: throw IllegalArgumentException("Content type is required")
        val split = contentType.split("/")
        val date = LocalDate.now()
        val uuid = UUID.randomUUID()
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

    fun toInfo(attachment: Attachment): AttachmentInfo {
        return attachment.run {
            AttachmentInfo(
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

@Component
class UserMapper(
    private val organizationRepository: OrganizationRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    fun toEntity(request: CreateDirectorRequest): User {
        return User(
            fullName = request.fullName,
            phoneNumber = request.phoneNumber,
            passWord = passwordEncoder.encode(request.password),
            passportId = request.passportId,
            role = Role.DIRECTOR
        )
    }

    fun toEntity(request: CreateOperatorRequest): User {
        return User(
            fullName = request.fullName,
            phoneNumber = request.phoneNumber,
            passWord = passwordEncoder.encode(request.password),
            passportId = request.passportId,
            role = Role.OPERATOR,
            organization = mutableListOf(
                organizationRepository.findByIdAndDeletedFalse(request.organizationId)
                    ?: throw OrganizationNotFoundException()
            )
        )
    }
}