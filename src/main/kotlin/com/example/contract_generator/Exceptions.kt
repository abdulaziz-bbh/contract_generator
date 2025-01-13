package com.example.contract_generator

import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.context.support.ResourceBundleMessageSource


sealed class GenericException() : RuntimeException() {

    abstract fun errorCode(): ErrorCode
    open fun getArguments(): Array<Any?>? = null

    fun getErrorMessage(resourceBundleMessageSource: ResourceBundleMessageSource): BaseMessage {
        val message = try {
            resourceBundleMessageSource.getMessage(
                errorCode().name, getArguments(), LocaleContextHolder.getLocale()
            )
        } catch (e: Exception) {
            e.message ?: "error"
        }
        return BaseMessage(errorCode().code, message)
    }
}

class KeyAlreadyExistsException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.KEY_ALREADY_EXISTS
    }
}

class BadRequestException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.BAD_REQUEST
    }
}

class KeyNotFoundException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.KEY_NOT_FOUND
    }
}

class TemplateAlreadyExistsException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.TEMPLATE_ALREADY_EXISTS
    }
}

class TemplateNotFoundException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.TEMPLATE_NOT_FOUND
    }
}

class InvalidTemplateNameException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.INVALID_TEMPLATE_NAME
    }
}

class TokenNotFoundException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.TOKEN_NOT_FOUND
}

class OrganizationAlreadyExistsException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.ORGANIZATION_ALREADY_EXISTS
}

class OrganizationNotFoundException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.ORGANIZATION_NOT_FOUND
}
class UserAlreadyExistsException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.USER_ALREADY_EXISTS
}
class UsernameInvalidException : GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.USERNAME_INVALID
}
class UserNotFoundException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.USER_NOT_FOUND
}
class PassportIdAlreadyUsedException: GenericException() {
    override fun errorCode(): ErrorCode = ErrorCode.PASSPORT_ALREADY_USED
}

class AttachmentNotFound():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_NOT_FOUND
}

class AttachmentAlreadyExists():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_ALREADY_EXISTS
}

class ContractNotFound(): GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.CONTRACT_NOT_FOUND
}
