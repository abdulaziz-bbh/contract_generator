package com.example.contract_generator

import jakarta.validation.constraints.NotNull
import jakarta.persistence.Column

data class BaseMessage(val code: Int, val message: String?)

data class CreateDirectorRequest(
    @field:NotNull val fullName: String,
    @field:NotNull val password: String,
    @field:NotNull val phoneNumber: String,
    @field:NotNull val pnfl: String,
    @field:NotNull val passportId: String
)

data class CreateOperatorRequest(
    @field:NotNull val fullName: String,
    @field:NotNull val password: String,
    @field:NotNull val phoneNumber: String,
    @field:NotNull val pnfl: String,
    @field:NotNull val passportId: String,
    @field:NotNull val organizationId: Long
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
)

data class UpdateOrganizationRequest(
    val name: String,
)

data class AttachmentInfo(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val extension: String,
    val path: String
){
    companion object {

    }
}
