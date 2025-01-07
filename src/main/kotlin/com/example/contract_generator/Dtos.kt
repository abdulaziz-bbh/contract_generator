package com.example.contract_generator


data class BaseMessage(val code: Int, val message: String?)

data class KeyCreateRequest(
    val key: String,
    val value: String,
    val language: KeyLanguage
)

data class KeyResponse(
    val id: Long?,
    val key: String?,
    val value: String?,
    val language: KeyLanguage?
)

data class KeyUpdateRequest(
    val key: String,
    val value: String,
    val language: KeyLanguage
)