package com.example.contract_generator

enum class Role {
    ADMIN, DIRECTOR, OPERATOR
}

enum class ContractStatus {
    STARTED, PENDING, COMPLETED
}

enum class KeyLanguage {
    LATIN,KRILL
}
enum class ErrorCode(val code: Int) {

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101),

    KEY_NOT_FOUND(200),
    KEY_ALREADY_EXISTS(201),

    TEMPLATE_NOT_FOUND(300),
    TEMPLATE_ALREADY_EXISTS(301),

    ATTACHMENT_NOT_FOUND(0),
    ATTACHMENT_ALREADY_EXISTS(1),

}