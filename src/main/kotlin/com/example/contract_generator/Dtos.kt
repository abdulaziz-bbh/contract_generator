package com.example.contract_generator

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.ManyToMany
import jakarta.persistence.OneToOne


data class BaseMessage(val code: Int, val message: String?)


data class AttachmentInfo(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val extension: String,
    val path: String
){
    companion object {

    }
}
data class KeyCreateRequest(
    val key: String,
)

data class KeyResponse(
    val id: Long?,
    val key: String?,
)

data class KeyUpdateRequest(
    val key: String,
)

data class TemplateCreateRequest(
    val templateName: String,
)

data class TemplateResponse(
    val id: Long?,
    val templateName: String?,
    val file : AttachmentResponse?,
    val keys : List<KeyResponse>?,
)

data class AttachmentResponse(
    val id: Long?,
    val name: String?,
    val contentType: String?,
    val size: Long?,
    val extension: String?,
    val path: String?,
)

data class AttachmentInfo(
    val id: Long,
    val name: String,
    val contentType: String,
    val size: Long,
    val extension: String,
    val path: String
){
    companion object {

    }
}