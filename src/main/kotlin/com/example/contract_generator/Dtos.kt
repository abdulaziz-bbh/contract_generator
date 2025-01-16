package com.example.contract_generator

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.LocalDate


data class BaseMessage(val code: Int, val message: String?)

data class KeyCreateRequest(
    val key: String,
)

data class KeyResponse(
    val id: Long?,
    val key: String?,
)

data class KeyUpdateRequest(
    val key: String,
)

data class TemplateCreateRequest(
    val templateName: String,
    val organizationId: Long
)

data class TemplateResponse(
    val id: Long?,
    val templateName: String?,
    val file : AttachmentResponse?,
    val organizationId: Long?,
    val organizationName: String?,
    val keys : List<KeyResponse>?
)

data class TemplateDto(
    val id: Long?,
    val templateName: String?,
    val organizationId: Long?,
    val keys: List<KeyDto>?,
    val attachmentId: Long?
)

data class KeyDto(
    val id: Long?,
    val key: String?
)

data class TemplateUpdateRequest(
    val templateName: String,
    val keys : List<KeyResponse>
)

data class AttachmentResponse(
    val id: Long?,
    val name: String?,
    val contentType: String?,
    val size: Long?,
    val extension: String?,
    val path: String?
)


data class CreateDirectorRequest(
    val fullName: String,
    val password: String,
    val phoneNumber: String,
    val passportId: String
)

data class CreateOperatorRequest(
    val fullName: String,
    val password: String,
    val phoneNumber: String,
    val passportId: String,
    val organizationId: Long
)
data class UpdateOperatorRequest(
    val fullName: String?,
    val passportId: String?,
    val phoneNumber: String?
)

data class UserDto(
    val id: Long?,
    val fullName: String,
    val role: Role,
    val phoneNumber: String,
    val passportId: String
)

data class LoginRequest(
    val username: String,
    val password: String,
)

data class AuthenticationDto(
    val accessToken: String,
    val refreshToken: String
)

data class CreateOrganizationRequest(
    val name: String,
    val address: String,
)
data class UpdateOrganizationRequest(
    val name: String?,
    val address: String?
)

data class AttachmentInfo(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val extension: String,
    val path: String
)

data class ContractRequestDto(
    val contractData: List<CreateContractDataDto>
)
data class CreateContractDataDto(
    @field:NotNull val keyId: Long,
    @field:NotNull val value: String
)

data class ContractResponseDto(
    val id: Long,
    val templateName: String,
    val operators: List<UserDto>,
    val contractData: List<ContractDataDto>,
    val isGenerated: Boolean
)

data class ContractDataDto(
    val id: Long,
    val key: String,
    val value: String
)

data class ContractDataUpdateDto(
    @field:NotNull @field:Positive val contractDataId: Long,
    @field:NotNull @field: NotBlank val value: String
)
data class ContractIdsDto(
    @field:NotNull @field: NotEmpty val contractIds: List<Long>?
)
data class JobIdsDto(
    @field:NotNull @field:NotEmpty val jobIds: List<Long>?
)

data class GenerateContractDto(
    @field:NotNull val extension:JobType,
    @field:NotNull @field:NotBlank val list:ContractIdsDto
)
data class JobDto(
    val id: Long,
    val extension: JobType,
    val status: JobStatus,
    val hashId: String?,
)

data class OrganizationDto(
    val id: Long,
    val name: String,
    val address: String
)

data class ContractCountRequest(
    val organizationId: Long,
    val operatorId: Long?,
    val date: LocalDate?
)

data class ContractCountResponse(
    val organizationId: Long,
    val countContract: Int
)