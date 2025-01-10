package com.example.contract_generator

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.support.JpaEntityInformation
import org.springframework.data.jpa.repository.support.SimpleJpaRepository
import org.springframework.data.repository.NoRepositoryBean
import org.springframework.data.repository.findByIdOrNull
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository


@NoRepositoryBean
interface BaseRepository<T : BaseEntity> : JpaRepository<T, Long>, JpaSpecificationExecutor<T> {
    fun findByIdAndDeletedFalse(id: Long): T?
    fun trash(id: Long): T?
    fun trashList(ids: List<Long>): List<T?>
    fun findAllNotDeleted(): List<T>
    fun findAllNotDeleted(pageable: Pageable): List<T>
    fun findAllNotDeletedForPageable(pageable: Pageable): Page<T>
    fun saveAndRefresh(t: T): T
}

class BaseRepositoryImpl<T : BaseEntity>(
    entityInformation: JpaEntityInformation<T, Long>,
    private val entityManager: EntityManager
) : SimpleJpaRepository<T, Long>(entityInformation, entityManager), BaseRepository<T> {

    val isNotDeletedSpecification = Specification<T> { root, _, cb -> cb.equal(root.get<Boolean>("deleted"), false) }

    override fun findByIdAndDeletedFalse(id: Long) = findByIdOrNull(id)?.run { if (deleted) null else this }

    @Transactional
    override fun trash(id: Long): T? = findByIdOrNull(id)?.run {
        deleted = true
        save(this)
    }

    override fun findAllNotDeleted(): List<T> = findAll(isNotDeletedSpecification)
    override fun findAllNotDeleted(pageable: Pageable): List<T> = findAll(isNotDeletedSpecification, pageable).content
    override fun findAllNotDeletedForPageable(pageable: Pageable): Page<T> =
        findAll(isNotDeletedSpecification, pageable)

    @Transactional
    override fun trashList(ids: List<Long>): List<T?> = ids.map { trash(it) }


    @Transactional
    override fun saveAndRefresh(t: T): T {
        return save(t).apply { entityManager.refresh(this) }
    }
}

@Repository
interface KeyRepository : BaseRepository<Key> {

    fun findByKeyAndDeletedFalse(key: String): Key?

    @Query("""
        select k from Key k
        where k.id != :id
        and k.key = :key
        and k.deleted = false 
    """)
    fun findByName(id: Long, name: String): Key?

    fun findAllByKeyInAndDeletedFalse(keys: Collection<String>): List<Key>


}


interface ContractRepository : BaseRepository<Contract> {
    fun findByFile_Name(fileName: String): Contract?
}

@Repository
interface TemplateRepository : BaseRepository<Template> {


}

interface UserRepository : BaseRepository<User>{

    fun existsByPassportId(passportId:String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean

    fun findByPhoneNumber(phoneNumber: String): User?

}
interface TokenRepository : BaseRepository<Token>{

    @Query("select t from Token t inner join users u " +
            "on t.user.id = u.id where u.id = :id and (t.expired = false or t.revoked = false )")
    fun findAllValidTokenByUser(@Param("id") id: Long): List<Token>

    fun findByToken(token: String): Token?
}

interface OrganizationRepository : BaseRepository<Organization>{
    fun existsByName(name: String): Boolean

    @Query(value = "select * from Organization where id = :id", nativeQuery = true)
    fun findByIdNative(@Param("id") id: Long): Organization?
}

interface AttachmentRepository : BaseRepository<Attachment> {
}

interface ContractDataRepository : BaseRepository<ContractData>{
    fun findAllByContract(contract: Contract): List<ContractData>
}