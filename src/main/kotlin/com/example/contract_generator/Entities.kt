package com.example.contract_generator

import jakarta.persistence.*
import org.hibernate.annotations.ColumnDefault
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import java.util.*

@MappedSuperclass
@EntityListeners(AuditingEntityListener::class)
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
    @Column(nullable = false) var passWord: String,
    @ManyToMany val organization: MutableList<Organization>? = null,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val role: Role
) : BaseEntity(), UserDetails {

    override fun getAuthorities(): List<SimpleGrantedAuthority> = role.getAuthority()
    override fun getPassword(): String = passWord
    override fun getUsername(): String = phoneNumber
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = true

}
@Entity
class Token(
    @Column(nullable = false) var token: String,
    @Column(nullable = false) var tokenType: String,
    @Column(nullable = false) var revoked: Boolean,
    @Column(nullable = false) var expired: Boolean,
    @ManyToOne(fetch = FetchType.LAZY) var user: User
): BaseEntity()

@Entity
class Organization(
    @Column(nullable = false) var name: String,
    @ManyToOne var director: User? = null
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
    @Column(nullable = false) var key: String,
) : BaseEntity()

@Entity
class Template(
    @Column(nullable = false) var templateName: String,
    @OneToOne var file : Attachment,
    @ManyToMany var keys : MutableList<Key> = mutableListOf(),
) : BaseEntity()

@Entity
class Contract(
    @OneToOne val file: Attachment,
    @ManyToMany val operators : List<User>,
    @ManyToOne val client: Client,
    @Enumerated(EnumType.STRING) @Column(nullable = false) val status: ContractStatus

) : BaseEntity()

@Entity
class Client(
    @Column(nullable = false) val fullName: String,
    @Column(nullable = false) val pnfl: String,
    @Column(nullable = false) val passportId: String,
) : BaseEntity()