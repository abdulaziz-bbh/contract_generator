package com.example.contract_generator

import org.springframework.security.core.context.SecurityContextHolder

fun getCurrentUserId(): User? {
    val authentication = SecurityContextHolder.getContext().authentication
    if (authentication != null && authentication.principal is User) {
        return (authentication.principal as User)
    }
    return null
}

fun getCurrentOrganization(repository: UsersOrganizationRepository): Organization {
    val operatorId = getCurrentUserId()?.id
    val userOrganization = repository.findByUserIdAndDeletedFalse(operatorId!!)
    return userOrganization!!.organization
}

