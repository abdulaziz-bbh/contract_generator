package com.example.contract_generator

import org.springframework.security.core.context.SecurityContextHolder

fun getCurrentUser(): User {
    return SecurityContextHolder.getContext().authentication.principal as User
}