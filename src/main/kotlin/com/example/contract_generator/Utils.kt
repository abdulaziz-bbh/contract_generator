package com.example.contract_generator

import org.springframework.security.core.context.SecurityContextHolder

fun getCurrentUser(): User? {
    val principal = SecurityContextHolder.getContext().authentication.details
    return if (principal is User) {
        principal
    } else {
        null
    }
}
