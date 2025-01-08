package com.example.contract_generator

import ch.qos.logback.core.spi.ErrorCodes
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
class AttachmentNotFound():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_NOT_FOUND
}

class AttachmentAlreadyExists():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_ALREADY_EXISTS
}

class UserAlreadyExistsException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.USER_ALREADY_EXISTS
    }
}

class UserNotFoundException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.USER_NOT_FOUND
    }
}

class KeyAlreadyExistsException : GenericException() {
    override fun errorCode(): ErrorCode {
        return ErrorCode.KEY_ALREADY_EXISTS
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

class AttachmentNotFound():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_NOT_FOUND
}

class AttachmentAlreadyExists():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_ALREADY_EXISTS
}
