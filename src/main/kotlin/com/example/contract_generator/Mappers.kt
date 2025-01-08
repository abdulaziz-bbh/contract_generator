package com.example.contract_generator

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component

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
            pnfl = request.pnfl,
            passportId = request.passportId,
            role = Role.OPERATOR
        )
    }
    fun toEntity(request: CreateOperatorRequest): User {
        return User(
            fullName = request.fullName,
            phoneNumber = request.phoneNumber,
            passWord = passwordEncoder.encode(request.password),
            pnfl = request.pnfl,
            passportId = request.passportId,
            role = Role.DIRECTOR,
            organization = mutableListOf(
                organizationRepository.findByIdAndDeletedFalse(request.organizationId)
                    ?: throw OrganizationNotFoundException())
        )
    }
}