package com.example.contract_generator

enum class Role {
    ADMIN, DIRECTOR, OPERATOR
}

enum class ContractStatus {
    STARTED, PENDING, COMPLETED
}
enum class ErrorCode(val code: Int) {
    ATTACHMENT_NOT_FOUND(0),
    ATTACHMENT_ALREADY_EXISTS(1),
}