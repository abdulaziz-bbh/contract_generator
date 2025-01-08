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
            e.message ?: "Unknown error"
        }
        return BaseMessage(errorCode().code, message)
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
class AttachmentNotFound():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_NOT_FOUND
}

class AttachmentAlreadyExists():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_ALREADY_EXISTS
}
