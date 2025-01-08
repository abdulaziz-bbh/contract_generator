package com.example.contract_generator

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import java.util.*

@MappedSuperclass
abstract class BaseEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @CreatedDate @Temporal(TemporalType.TIMESTAMP) var createdAt: Date? = null,
    @LastModifiedDate @Temporal(TemporalType.TIMESTAMP) var updatedAt: Date? = null,
    @CreatedBy var createdBy: Long? = null,
    @LastModifiedBy var updatedBy: Long? = null,
    @Column(nullable = false) @ColumnDefault(value = "false") var deleted: Boolean = false
)

@Entity(name = "users")
class User(
    @Column(nullable = false) var fullName: String,
    @Column(nullable = false) var phoneNumber: String,
    @Column(nullable = false) val pnfl: String,
    @Column(nullable = false) val passportId: String,
    @ManyToOne val organization: Organization,
//    @Enumerated(EnumType.STRING) @Column(nullable = false) val role: Role
) : BaseEntity()

@Entity
class Organization(
    @Column(nullable = false) var name: String,
) : BaseEntity()

@Entity
class Attachment(
    @Column(nullable = false) val name: String,
    @Column(nullable = false) val contentType: String,
    @Column(nullable = false) val size: Long,
    @Column(nullable = false) val extension: String,
    @Column(nullable = false) val path: String
) : BaseEntity()

@Entity
class Key(
    @Column(nullable = false) val key: String,
    @Column(nullable = false) val value: String
) : BaseEntity()

@Entity
class Template(
    @Column(nullable = false) val templateName: String,
    @OneToOne val file : Attachment,
    @ManyToMany val keys : List<Key>,
) : BaseEntity()

@Entity
class Contract(
    @OneToOne val file: Attachment,
    @ManyToOne val client: Client,
    @ManyToMany val operators : MutableList<User> = mutableListOf(),
    @Enumerated(EnumType.STRING) @Column(nullable = false) val status: ContractStatus=ContractStatus.STARTED

) : BaseEntity()

@Entity
class Client(
    @Column(nullable = false) val fullName: String,
    @Column(nullable = false) val pnfl: String,
    @Column(nullable = false) val passportId: String,
) : BaseEntity()