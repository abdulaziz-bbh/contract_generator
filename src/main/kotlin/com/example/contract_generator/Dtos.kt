package com.example.contract_generator

import jakarta.persistence.Column

data class BaseMessage(val code: Int, val message: String?)



data class ContractRequestDto(
    val templateId: Long,
    val keys: Map<String,String>
)