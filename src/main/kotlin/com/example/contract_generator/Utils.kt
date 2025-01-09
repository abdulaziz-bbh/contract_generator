package com.example.contract_generator

import org.springframework.security.core.context.SecurityContextHolder

fun getCurrentUserId(): Long? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication != null && authentication.principal is User) {
        return (authentication.principal as User).id
    }
    return null
}