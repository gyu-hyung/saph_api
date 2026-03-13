package com.saph.api.payment.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("payments")
data class Payment(
    @Id
    val id: Long? = null,

    @Column("member_id")
    val memberId: Long,

    @Column("package_type")
    val packageType: PackageType,

    @Column("credit_amount")
    val creditAmount: Int,

    @Column("price")
    val price: Int,

    @Column("status")
    val status: PaymentStatus = PaymentStatus.READY,

    @Column("pg_payment_key")
    val pgPaymentKey: String? = null,

    @Column("pg_order_id")
    val pgOrderId: String,

    @Column("failed_reason")
    val failedReason: String? = null,

    @Column("paid_at")
    val paidAt: LocalDateTime? = null,

    @Column("created_at")
    val createdAt: LocalDateTime? = null,

    @Column("updated_at")
    val updatedAt: LocalDateTime? = null,
)

enum class PackageType(val creditMin: Int, val price: Int, val label: String) {
    MIN_10(10, 1900, "10분"),
    MIN_30(30, 4900, "30분"),
    HOUR_1(60, 8900, "1시간"),
    HOUR_3(180, 22900, "3시간"),
    HOUR_10(600, 59900, "10시간"),
}

enum class PaymentStatus {
    READY, DONE, FAILED, CANCELED
}
