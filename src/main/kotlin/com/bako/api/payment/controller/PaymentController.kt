package com.bako.api.payment.controller

import com.bako.api.common.ApiResponse
import com.bako.api.credit.service.CreditService
import com.bako.api.payment.dto.ConfirmRequest
import com.bako.api.payment.dto.PurchaseRequest
import com.bako.api.payment.service.PaymentService
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/credits")
class PaymentController(
    private val paymentService: PaymentService,
    private val creditService: CreditService,
) {

    @GetMapping
    suspend fun getBalance(authentication: Authentication): ApiResponse {
        val memberId = authentication.name.toLong()
        val balance = creditService.getBalance(memberId)
        return ApiResponse.success(mapOf("balanceMin" to balance))
    }

    @GetMapping("/packages")
    fun getPackages(): ApiResponse {
        val packages = paymentService.getPackages()
        return ApiResponse.success(packages)
    }

    @PostMapping("/purchase")
    suspend fun purchase(
        @Valid @RequestBody request: PurchaseRequest,
        authentication: Authentication,
    ): ApiResponse {
        val memberId = authentication.name.toLong()
        val result = paymentService.purchase(memberId, request.packageType)
        return ApiResponse.success(result)
    }

    @PostMapping("/purchase/confirm")
    suspend fun confirm(@Valid @RequestBody request: ConfirmRequest): ApiResponse {
        val result = paymentService.confirm(request.orderId, request.paymentKey, request.amount)
        return ApiResponse.success(result)
    }
}
