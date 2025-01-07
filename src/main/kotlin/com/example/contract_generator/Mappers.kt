package com.example.contract_generator

import org.springframework.stereotype.Component

@Component
class KeyMapper {

    fun toDto(key: Key): KeyResponse {
        return key.run {
            KeyResponse(
                id = this.id,
                key = this.key,
                value = this.value,
                language = this.language
            )
        }
    }

    fun toEntity(createRequest: KeyCreateRequest): Key {
        return createRequest.run {
            Key(
                key = this.key,
                value = this.value,
                language = this.language
            )
        }
    }


    fun updateEntity(key: Key, updateRequest: KeyUpdateRequest): Key {
        return updateRequest.run {
            key.apply {
                updateRequest.key.let { this.key = it }
                updateRequest.value.let { this.value = it }
                updateRequest.language.let { this.language = it }
            }
        }
    }
}
