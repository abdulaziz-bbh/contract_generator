package com.example.contract_generator

enum class Role {
    ADMIN, DIRECTOR, OPERATOR
}

enum class ContractStatus {
    STARTED, PENDING, COMPLETED
}

enum class KeyLanguage {
    LATIN,KRILL
}
enum class ErrorCode(val code: Int) {
    TOKEN_NOT_FOUND(0),
    USER_ALREADY_EXISTS(10),
    USER_NOT_FOUND(11),
    USERNAME_INVALID(12),
    ORGANIZATION_ALREADY_EXISTS(30),
    ORGANIZATION_NOT_FOUND(31),
    ATTACHMENT_NOT_FOUND(40),
    ATTACHMENT_ALREADY_EXISTS(41)
    KEY_NOT_FOUND(50),
    KEY_ALREADY_EXISTS(51),
    TEMPLATE_NOT_FOUND(60),
    TEMPLATE_ALREADY_EXISTS(61),

    USER_NOT_FOUND(100),
    USER_ALREADY_EXISTS(101),

    KEY_NOT_FOUND(200),
    KEY_ALREADY_EXISTS(201),

enum class Role(private var permissions: MutableSet<Permission>) {
    ADMIN(
        mutableSetOf(
            Permission.ADMIN_READ,
            Permission.ADMIN_CREATE,
            Permission.ADMIN_UPDATE,
            Permission.ADMIN_DELETE
        )
    ), DIRECTOR(
        mutableSetOf(
            Permission.DIRECTOR_READ,
            Permission.DIRECTOR_CREATE,
            Permission.DIRECTOR_UPDATE,
            Permission.DIRECTOR_DELETE
        )
    ), OPERATOR(
        mutableSetOf(
            Permission.OPERATOR_READ,
            Permission.OPERATOR_CREATE,
            Permission.OPERATOR_UPDATE,
            Permission.OPERATOR_DELETE
        )
    );

    fun getAuthority(): List<SimpleGrantedAuthority> {
        val authority = permissions
            .map { permission -> SimpleGrantedAuthority(permission.permission) }
            .toMutableList()
        authority.add(SimpleGrantedAuthority("ROLE_${this.name}"))
        return authority
    }
}
enum class Permission(val permission: String) {
        ADMIN_READ("admin:read"),
        ADMIN_UPDATE("admin:update"),
        ADMIN_CREATE("admin:create"),
        ADMIN_DELETE("admin:delete"),
        OPERATOR_READ("operator:read"),
        OPERATOR_UPDATE("operator:update"),
        OPERATOR_CREATE("operator:create"),
        OPERATOR_DELETE("operator:delete"),
        DIRECTOR_READ("director:read"),
        DIRECTOR_UPDATE("director:update"),
        DIRECTOR_CREATE("director:create"),
        DIRECTOR_DELETE("director:delete")
}