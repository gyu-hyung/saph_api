package com.bako.api.credit.service

import com.bako.api.common.ApiException
import com.bako.api.credit.domain.CreditLog
import com.bako.api.credit.domain.CreditReason
import com.bako.api.credit.repository.CreditLogRepository
import com.bako.api.credit.repository.CreditRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class CreditService(
    private val creditRepository: CreditRepository,
    private val creditLogRepository: CreditLogRepository,
    private val databaseClient: DatabaseClient,
) {

    suspend fun getBalance(memberId: Long): Int {
        val credit = creditRepository.findByMemberId(memberId).awaitSingleOrNull()
            ?: return 0
        return credit.balanceMin
    }

    @Transactional
    suspend fun deduct(memberId: Long, amount: Int, jobId: Long): Int {
        val row = databaseClient.sql(
            "SELECT id, balance_min FROM credits WHERE member_id = :memberId FOR UPDATE"
        )
            .bind("memberId", memberId)
            .fetch()
            .one()
            .awaitSingleOrNull()
            ?: throw ApiException.notFound("CREDIT_NOT_FOUND", "Credit record not found")

        val creditId = row["id"] as Long
        val currentBalance = (row["balance_min"] as Number).toInt()

        if (currentBalance < amount) {
            throw ApiException.badRequest(
                "INSUFFICIENT_CREDITS",
                "크레딧이 부족합니다. ${amount}분이 필요하지만 ${currentBalance}분 남아있습니다."
            )
        }

        val newBalance = currentBalance - amount

        databaseClient.sql(
            "UPDATE credits SET balance_min = :balance, updated_at = :updatedAt WHERE id = :id"
        )
            .bind("balance", newBalance)
            .bind("updatedAt", LocalDateTime.now())
            .bind("id", creditId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        creditLogRepository.save(
            CreditLog(
                memberId = memberId,
                jobId = jobId,
                changeAmount = -amount,
                reason = CreditReason.USAGE,
                balanceAfter = newBalance,
            )
        ).awaitSingle()

        return newBalance
    }

    @Transactional
    suspend fun refund(memberId: Long, amount: Int, jobId: Long): Int {
        val row = databaseClient.sql(
            "SELECT id, balance_min FROM credits WHERE member_id = :memberId FOR UPDATE"
        )
            .bind("memberId", memberId)
            .fetch()
            .one()
            .awaitSingleOrNull()
            ?: throw ApiException.notFound("CREDIT_NOT_FOUND", "Credit record not found")

        val creditId = row["id"] as Long
        val currentBalance = (row["balance_min"] as Number).toInt()
        val newBalance = currentBalance + amount

        databaseClient.sql(
            "UPDATE credits SET balance_min = :balance, updated_at = :updatedAt WHERE id = :id"
        )
            .bind("balance", newBalance)
            .bind("updatedAt", LocalDateTime.now())
            .bind("id", creditId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        creditLogRepository.save(
            CreditLog(
                memberId = memberId,
                jobId = jobId,
                changeAmount = amount,
                reason = CreditReason.REFUND,
                balanceAfter = newBalance,
            )
        ).awaitSingle()

        return newBalance
    }

    @Transactional
    suspend fun charge(memberId: Long, amount: Int, paymentId: Long): Int {
        val row = databaseClient.sql(
            "SELECT id, balance_min FROM credits WHERE member_id = :memberId FOR UPDATE"
        )
            .bind("memberId", memberId)
            .fetch()
            .one()
            .awaitSingleOrNull()
            ?: throw ApiException.notFound("CREDIT_NOT_FOUND", "Credit record not found")

        val creditId = row["id"] as Long
        val currentBalance = (row["balance_min"] as Number).toInt()
        val newBalance = currentBalance + amount

        databaseClient.sql(
            "UPDATE credits SET balance_min = :balance, updated_at = :updatedAt WHERE id = :id"
        )
            .bind("balance", newBalance)
            .bind("updatedAt", LocalDateTime.now())
            .bind("id", creditId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        creditLogRepository.save(
            CreditLog(
                memberId = memberId,
                paymentId = paymentId,
                changeAmount = amount,
                reason = CreditReason.PURCHASE,
                balanceAfter = newBalance,
            )
        ).awaitSingle()

        return newBalance
    }
}
