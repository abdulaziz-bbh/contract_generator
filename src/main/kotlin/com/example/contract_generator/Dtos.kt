package com.example.contract_generator

import jakarta.persistence.Column

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