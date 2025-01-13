package com.example.contract_generator

import jakarta.validation.constraints.NotNull


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
    val organizationName: String?,
    val keys : List<KeyResponse>?
)

data class TemplateDto(
    val id: Long?,
    val templateName: String?,
    val organizationId: Long?,
    val keys: List<String>?,
    val attachmentId: Long?
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
    @field:NotNull val fullName: String,
    @field:NotNull val password: String,
    @field:NotNull val phoneNumber: String,
    @field:NotNull val passportId: String
)

data class CreateOperatorRequest(
    @field:NotNull val fullName: String,
    @field:NotNull val password: String,
    @field:NotNull val phoneNumber: String,
    @field:NotNull val passportId: String,
    @field:NotNull val organizationId: Long
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
    @field:NotNull val username: String,
    @field:NotNull val password: String,
)

data class AuthenticationDto(
    val accessToken: String,
    val refreshToken: String
)

data class CreateOrganizationRequest(
    @field:NotNull val name: String,
    @field:NotNull val address: String,
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
    val templateId: Long,
//    val attachment: AttachmentInfo?,
    val operators: List<UserDto>,
    val contractData: List<ContractDataDto>,
    val isGenerated: Boolean
)

data class ContractDataDto(
    val id: Long,
    val key: KeyResponse,
    val value: String
)

data class ContractUpdateDto(
    val fileName: String,
    val keys: Map<String,String>
)
data class  GenerateContractRequest(
    val isDocsOrPdf: Boolean,
    val contractData: List<Long>
)

data class OrganizationDto(
    val id: Long,
    val name: String,
    val address: String
)
    