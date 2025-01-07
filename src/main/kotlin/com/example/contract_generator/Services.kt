package com.example.contract_generator

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service

interface KeyService {
    fun getAll(page: Int, size: Int): Page<KeyResponse>
    fun getAll(): List<KeyResponse>
    fun getOne(id: Long): KeyResponse
    fun create(request: KeyCreateRequest)
    fun update(id: Long, request: KeyUpdateRequest)
    fun delete(id: Long)
}

@Service
class KeyServiceImpl(
    private val keyMapper: KeyMapper,
    private val entityManager: EntityManager,
    private val keyRepository: KeyRepository
) : KeyService {
    override fun getAll(page: Int, size: Int): Page<KeyResponse> {
        val pageable: Pageable = PageRequest.of(page, size)
        val usersPage = keyRepository.findAllNotDeletedForPageable(pageable)
        return usersPage.map { keyMapper.toDto(it) }
    }

    override fun getAll(): List<KeyResponse> {
        return keyRepository.findAllNotDeleted().map {
            keyMapper.toDto(it)
        }
    }

    override fun getOne(id: Long): KeyResponse {
        keyRepository.findByIdAndDeletedFalse(id)?.let {
            return keyMapper.toDto(it)
        } ?: throw KeyNotFoundException()
    }

    override fun create(request: KeyCreateRequest) {
        val existingKey = keyRepository.findByKeyAndDeletedFalse(request.key)
        if (existingKey != null) throw KeyAlreadyExistsException()
        keyRepository.save(keyMapper.toEntity(request))
    }

    override fun update(id: Long, request: KeyUpdateRequest) {
        val key = keyRepository.findByIdAndDeletedFalse(id) ?: throw KeyNotFoundException()
        keyRepository.findByName(id, request.key)?.let { throw KeyAlreadyExistsException() }

        val updateKey = keyMapper.updateEntity(key, request)
        keyRepository.save(updateKey)
    }

    @Transactional
    override fun delete(id: Long) {
        keyRepository.trash(id) ?: throw KeyNotFoundException()
    }

}
