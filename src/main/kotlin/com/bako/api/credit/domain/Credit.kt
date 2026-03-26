package com.bako.api.credit.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("credits")
data class Credit(
    @Id
    val id: Long? = null,

    @Column("member_id")
    val memberId: Long,

    @Column("balance_min")
    val balanceMin: Int = 0,

    @Column("updated_at")
    val updatedAt: LocalDateTime? = null,
)

@Table("credit_logs")
data class CreditLog(
    @Id
    val id: Long? = null,

    @Column("member_id")
    val memberId: Long,

    @Column("job_id")
    val jobId: Long? = null,

    @Column("payment_id")
    val paymentId: Long? = null,

    @Column("change_amount")
    val changeAmount: Int,

    @Column("reason")
    val reason: CreditReason,

    @Column("balance_after")
    val balanceAfter: Int,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,
)

enum class CreditReason {
    PURCHASE, USAGE, REFUND, ADMIN
}
