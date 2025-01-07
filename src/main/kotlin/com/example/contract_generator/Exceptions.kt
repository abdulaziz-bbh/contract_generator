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
class AttachmentNotFound():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_NOT_FOUND
}

class AttachmentAlreadyExists():GenericException(){
    override fun errorCode(): ErrorCode = ErrorCode.ATTACHMENT_ALREADY_EXISTS
}
