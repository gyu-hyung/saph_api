package com.bako.api.payment.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive

data class PackageInfo(
    val type: String,
    val creditMin: Int,
    val price: Int,
    val label: String,
)

data class PurchaseRequest(
    @field:NotBlank(message = "packageType is required")
    val packageType: String,
)

data class PurchaseResponse(
    val orderId: String,
    val packageType: String,
    val price: Int,
    val creditMin: Int,
)

data class ConfirmRequest(
    @field:NotBlank(message = "orderId is required")
    val orderId: String,

    @field:NotBlank(message = "paymentKey is required")
    val paymentKey: String,

    @field:NotNull(message = "amount is required")
    @field:Positive(message = "amount must be positive")
    val amount: Int,
)

data class ConfirmResponse(
    val creditBalance: Int,
    val chargedMin: Int,
)
