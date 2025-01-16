package com.example.contract_generator

import org.springframework.beans.factory.annotation.Value
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate
import java.util.*
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import java.io.File

@Component
class OrganizationMapper{

    fun toDto(organization: Organization): OrganizationDto {
        return OrganizationDto(
            id = organization.id!!,
            name = organization.name,
            address = organization.address,
        )
    }
    fun fromUpdateDto(request: UpdateOrganizationRequest, organization: Organization): Organization {
        request.run {
            name?.let { organization.name = it }
            address?.let { organization.address = it }
            return organization
        }
    }
}
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
                keys = template.keys.map { KeyResponse(it.id, it.key.removeSurrounding("$"))},
                organizationId = this.organization.id,
                organizationName = this.organization.name
            )
        }
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
            keys = template.keys.map { KeyDto(it.id, it.key.removeSurrounding("$")) },
            attachmentId = template.file.id
        )
    }
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
            fullName?.let { user.fullName = it }
            phoneNumber?.let { user.phoneNumber = it }
            passportId?.let { user.passportId = it }
        }
        return user
    }
}

@Component
class ContractMapper(private val userMapper: UserMapper,
    private val keyMapper: KeyMapper) {
    fun toDto(contract: Contract, contractDataList: List<ContractData>): ContractResponseDto {
        return ContractResponseDto(
            id = contract.id!!,
            templateName = contract.template.templateName,
            operators = contract.operators.map { userMapper.toDto(it) },
            contractData = contractDataList.map {
                ContractDataDto(
                    id = it.id !!,
                    key = it.key.key.removeSurrounding("$"),
                    value = it.value
                )
            },
            isGenerated = contract.isGenerated
        )
    }
}
@Component
class JobMapper {
    fun toDto(job: Job,hashId:String? = null): JobDto {
        return JobDto(
            id = job.id!!,
            extension = job.extension,
            status = job.status,
            hashId = hashId
        )
    }
}