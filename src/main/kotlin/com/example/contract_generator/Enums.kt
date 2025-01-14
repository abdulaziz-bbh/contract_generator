package com.example.contract_generator

import org.springframework.security.core.authority.SimpleGrantedAuthority

enum class JobStatus {
    PENDING, COMPLETED,FAILED
}

enum class ErrorCode(val code: Int) {
    USER_ALREADY_EXISTS(10),
    USER_NOT_FOUND(11),
    USERNAME_INVALID(12),
    PASSPORT_ALREADY_USED(13),
    ORGANIZATION_ALREADY_EXISTS(30),
    ORGANIZATION_NOT_FOUND(31),
    NOT_FOUND_OPERATOR_IN_ORGANIZATION(32),
    OPERATION_ALREADY_CURRENT(33),
    ATTACHMENT_NOT_FOUND(40),
    ATTACHMENT_ALREADY_EXISTS(41),
    KEY_NOT_FOUND(50),
    KEY_ALREADY_EXISTS(51),
    DUPLICATE_KEY(53),
    TEMPLATE_NOT_FOUND(60),
    TEMPLATE_ALREADY_EXISTS(61),
    MISSING_TEMPLATE_KEY(62),
    INVALID_TEMPLATE_NAME(77),
    CONTRACT_NOT_FOUND(80),
    DUPLICATE_CONTRACT(82),
    BAD_REQUEST(81),
    CONTRACT_DATA_NOT_FOUND(90),
    INVALID_FILE_FORMAT(91)
    }

enum class Role{
    ADMIN, DIRECTOR, OPERATOR;
}
