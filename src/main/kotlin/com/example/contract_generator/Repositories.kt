package com.example.contract_generator

import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.springframework.data.annotation.CreatedBy
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
import java.time.LocalDate
import java.util.Date


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
    fun findAllByIdInAndDeletedFalse(ids: Collection<Long>): List<Key>

}


interface ContractRepository : BaseRepository<Contract> {
    fun findAllByIdInAndDeletedFalse(ids: Collection<Long>): List<Contract>
    @Query("SELECT c FROM Contract c JOIN c.operators o WHERE o = :operator AND c.isGenerated = :isGenerated AND c.deleted = false")
    fun findByOperatorAndIsGeneratedAndDeletedFalse(
        @Param("operator") operator: User,
        @Param("isGenerated") isGenerated: Boolean
    ): List<Contract>

    @Query("SELECT c FROM Contract c JOIN c.operators o WHERE o = :operator AND c.deleted = false")
    fun getAllByOperatorAndDeletedFalse(
        @Param("operator") operator: User
    ): List<Contract>
    @Query(
        """
    SELECT COUNT(c) = :contractIdsCount
    FROM Contract c 
    JOIN c.operators o 
    WHERE c.id IN :contractIds 
      AND o = :operator 
      AND c.deleted = false
    """
    )
    fun existsAllByOperatorsAndDeletedFalse(
        @Param("contractIds") contractIds: List<Long>,
        @Param("operator") operator: User,
        @Param("contractIdsCount") contractIdsCount: Long
    ): Boolean


    @Query("""
        select count(*) from Contract c where c.template.organization.id = :organizationId 
        and c.createdBy = :operatorId 
        and extract(date from c.createdAt )= :date 
    """)
    fun getCountContracts(organizationId: Long, operatorId: Long, date: LocalDate): Int

    @Query("""
        select count(*) from Contract c where c.template.organization.id = :organizationId
        and extract(date from c.createdAt )= :date 
    """)
    fun getCountContracts(organizationId: Long, date: LocalDate): Int

    @Query("""
        select count(*) from Contract c where c.template.organization.id = :organizationId 
        and c.createdBy = :operatorId 
    """)
    fun getCountContracts(organizationId: Long, operatorId: Long): Int


    @Query("""
        select count(*) from Contract c where c.template.organization.id = :organizationId 
    """)
    fun getCountContracts(organizationId: Long): Int


}

@Repository
interface TemplateRepository : BaseRepository<Template> {

    fun existsByTemplateNameAndOrganizationId(templateName: String, organizationId: Long): Boolean

    @Query("select t from Template t where t.templateName = :templateName and t.deleted = false and t.organization.id = :organizationId")
    fun findByTemplateNameWithOrganizationIdAndDeletedFalse(
        @Param("templateName") templateName: String,
        @Param("organizationId") organizationId: Long
    ): Template?

    fun findByOrganizationIdAndDeletedFalse(organizationId: Long): List<Template>
}

interface UserRepository : BaseRepository<User>{

    fun existsByPassportId(passportId:String): Boolean
    fun existsByPhoneNumber(phoneNumber: String): Boolean
    fun findByPhoneNumber(phoneNumber: String): User?

    @Query("""
        select u from users u where u.id != :id and u.phoneNumber = :phoneNumber
    """)
    fun findByPhoneNumber(phoneNumber: String, id: Long): User?


    fun findByPassportId(passportId: String): User?

    @Query("""
        select u from users u where u.id != :id and u.passportId = :passportId
    """)
    fun existsUserIdAndPassportId(passportId: String, id: Long): User?

}

interface OrganizationRepository : BaseRepository<Organization>{
    fun existsByName(name: String): Boolean
}

interface UsersOrganizationRepository : BaseRepository<UsersOrganization>{

    @Query("""
        select o from Organization o join UsersOrganization uo on o.id = uo.organization.id
        join users  u on uo.user.id = u.id where  uo.user.id = :userId order by o.createdAt desc 
    """)
    fun findAllOrganizationByUserId(userId: Long): List<Organization>

    @Query("""
        select u from users u 
            join UsersOrganization uo on u.id = uo.user.id
            join Organization o on o.id = uo.organization.id
            where uo.organization.id = :organizationId and uo.isCurrentUser = true order by u.createdAt desc 

    """)
    fun findUsersByOrganizationId(organizationId: Long): List<User>?

    @Query("""
        select uo from UsersOrganization uo where uo.user.id = :userId 
            and uo.organization.id = :organizationId 
            and uo.isCurrentUser != false 
    """)
    fun findByOrganizationIdAndUserId(organizationId: Long, userId: Long): UsersOrganization?

    @Query("""
        select u from UsersOrganization u
            where u.user.id = :userId 
                and u.deleted = false
                and u.isCurrentUser = true order by u.createdAt desc
    """)
    fun findByUserIdAndDeletedFalse(userId: Long): UsersOrganization?

    @Query("""
        select exists(select u from UsersOrganization u where u.user.passportId = :passportId and u.isCurrentUser = true order by u.createdAt desc)
    """)
    fun existsByUserIdIsCurrent(passportId: String): Boolean
}

interface AttachmentRepository : BaseRepository<Attachment> {
    fun findByHashIdAndDeletedFalse(hashId: String): Attachment?
    fun findByName(name: String): Attachment?
}

interface ContractDataRepository : BaseRepository<ContractData>{
    fun findAllByContract(contract: Contract): List<ContractData>
    fun findAllByIdInAndDeletedFalse(ids: Collection<Long>): List<ContractData>
}
@Repository
interface JobRepository : BaseRepository<Job>{
    fun findAllByAttachmentAndDeletedFalse(attachment: Attachment): Job?
    fun findAllByIdInAndCreatedByAndDeletedFalse(ids: Collection<Long>,createdBy: Long): List<Job>
  }