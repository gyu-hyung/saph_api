package com.saph.api.auth.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("members")
data class Member(
    @Id
    val id: Long? = null,

    @Column("email")
    val email: String? = null,

    @Column("password")
    val password: String? = null,

    @Column("nickname")
    val nickname: String? = null,

    @Column("status")
    val status: MemberStatus = MemberStatus.ACTIVE,

    @Column("deleted_at")
    val deletedAt: LocalDateTime? = null,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,

    @Column("updated_at")
    val updatedAt: LocalDateTime? = null,
)

enum class MemberStatus {
    ACTIVE, BLOCKED, WITHDRAWN
}
