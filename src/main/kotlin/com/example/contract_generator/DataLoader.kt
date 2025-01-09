package com.example.contract_generator

import org.springframework.boot.CommandLineRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
class DataLoader(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
): CommandLineRunner {

    override fun run(vararg args: String?) {

        val admin = User(
            fullName = "Administrator",
            passWord = passwordEncoder.encode("admin"),
            phoneNumber = "1234567890",
            passportId = "1234567890",
            role = Role.ADMIN
        )
        if (!userRepository.existsByPassportIdOrPhoneNumber(admin.passportId, admin.phoneNumber)){
            userRepository.save(admin)
        }
    }


}