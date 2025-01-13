package com.example.contract_generator

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.io.File

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

    fun toTDto(template: Template): TemplateDto {
        return TemplateDto(
            id = template.id,
            templateName = template.templateName,
            organizationId = template.organization.id,
            keys = template.keys.map { it.key },
            attachmentId = template.file.id
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

        fun toEntity(multipartFile: MultipartFile , subFolder: String?=null): Attachment {
            val contentType = multipartFile.contentType ?: throw IllegalArgumentException("Content type is required")
            val split = contentType.split("/")
            val extension = split.getOrElse(1) { "" }

            val (_, uuid, path) = if (subFolder != null) {
                createDirectoryPath(subFolder = subFolder, contentType = contentType)
            } else {
                createDirectoryPath(contentType = contentType)
            }
            return Attachment(
                name = uuid.toString(),
                contentType = contentType,
                size = multipartFile.size,
                extension = extension,
                path = "$path/$uuid.$extension"
            )
        }
        fun toInfo(attachment: Attachment): AttachmentInfo {
            return attachment.run { AttachmentInfo(
                id = id!!,
                name = name,
                contentType = contentType,
                size = size,
                extension = extension,
                path = path)
            }
        }

}

@Component
class UserMapper(
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
            role = Role.OPERATOR
            )

    }
    fun toDto(entity: User): UserDto {
        return UserDto(
            fullName = entity.fullName,
            phoneNumber = entity.phoneNumber,
            passportId = entity.passportId,
            role = entity.role,
            id = entity.id
        )
    }
    fun fromUpdateDto(request: UpdateOperatorRequest, user: User):User{
        request.run {
            if (fullName != null) user.fullName = fullName
            if (phoneNumber != null) user.phoneNumber = phoneNumber
            if (passportId != null) user.passportId = passportId
        }
        return user
    }
}

@Component
class ContractMapper(private val userMapper: UserMapper,
    private val attachmentMapper: AttachmentMapper,
    private val templateMapper: TemplateMapper,
    private val keyMapper: KeyMapper) {
    fun toDto(contract: Contract, contractDataList: List<ContractData>): ContractResponseDto {
        return ContractResponseDto(
            id = contract.id!!,
            template = templateMapper.toDto(contract.template),
            attachment = contract.file?.let { attachmentMapper.toInfo(it) },
            operators = contract.operators.map { userMapper.toDto(it) },
            contractData = contractDataList.map {
                ContractDataDto(
                    id = it.id ?: throw IllegalStateException("ContractData ID cannot be null."),
                    key = keyMapper.toDto(it.key),
                    value = it.value
                )
            },
            isGenerated = contract.isGenerated
        )
    }
}
