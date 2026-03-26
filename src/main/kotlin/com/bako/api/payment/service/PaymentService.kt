package com.bako.api.payment.service

import com.bako.api.common.ApiException
import com.bako.api.credit.service.CreditService
import com.bako.api.payment.domain.PackageType
import com.bako.api.payment.domain.Payment
import com.bako.api.payment.domain.PaymentStatus
import com.bako.api.payment.dto.ConfirmResponse
import com.bako.api.payment.dto.PackageInfo
import com.bako.api.payment.dto.PurchaseResponse
import com.bako.api.payment.repository.PaymentRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.util.UUID

@Service
class PaymentService(
    private val paymentRepository: PaymentRepository,
    private val creditService: CreditService,
) {

    fun getPackages(): List<PackageInfo> {
        return PackageType.entries.map { pkg ->
            PackageInfo(
                type = pkg.name,
                creditMin = pkg.creditMin,
                price = pkg.price,
                label = pkg.label,
            )
        }
    }

    @Transactional
    suspend fun purchase(memberId: Long, packageTypeName: String): PurchaseResponse {
        val packageType = try {
            PackageType.valueOf(packageTypeName)
        } catch (_: IllegalArgumentException) {
            throw ApiException.badRequest("INVALID_PACKAGE_TYPE", "Unknown package type: $packageTypeName")
        }

        val orderId = UUID.randomUUID().toString()

        paymentRepository.save(
            Payment(
                memberId = memberId,
                packageType = packageType.name,
                creditAmount = packageType.creditMin,
                price = packageType.price,
                status = PaymentStatus.READY,
                pgOrderId = orderId,
            )
        ).awaitSingle()

        return PurchaseResponse(
            orderId = orderId,
            packageType = packageType.name,
            price = packageType.price,
            creditMin = packageType.creditMin,
        )
    }

    @Transactional
    suspend fun confirm(orderId: String, paymentKey: String, amount: Int): ConfirmResponse {
        val payment = paymentRepository.findByPgOrderId(orderId).awaitSingleOrNull()
            ?: throw ApiException.badRequest("INVALID_ORDER", "Payment order not found: $orderId")

        if (payment.status != PaymentStatus.READY) {
            throw ApiException.conflict("ALREADY_CONFIRMED", "Payment is already in status: ${payment.status}")
        }

        val now = LocalDateTime.now()

        if (payment.price != amount) {
            paymentRepository.save(
                payment.copy(status = PaymentStatus.FAILED, failedReason = "Amount mismatch: expected ${payment.price}, got $amount", updatedAt = now)
            ).awaitSingle()

            throw ApiException.badRequest("AMOUNT_MISMATCH", "Payment amount does not match: expected ${payment.price} but got $amount")
        }

        // Update payment to DONE
        paymentRepository.save(
            payment.copy(status = PaymentStatus.DONE, pgPaymentKey = paymentKey, paidAt = now, updatedAt = now)
        ).awaitSingle()

        val newBalance = creditService.charge(payment.memberId, payment.creditAmount, payment.id!!)

        return ConfirmResponse(
            creditBalance = newBalance,
            chargedMin = payment.creditAmount,
        )
    }
}
