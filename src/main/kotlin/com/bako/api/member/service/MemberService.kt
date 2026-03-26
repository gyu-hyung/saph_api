package com.bako.api.member.service

import com.google.firebase.auth.FirebaseAuth
import com.bako.api.auth.repository.MemberRepository
import com.bako.api.common.ApiException
import com.bako.api.config.AppProperties
import com.bako.api.credit.repository.CreditRepository
import com.bako.api.job.repository.JobRepository
import com.bako.api.member.dto.MemberResponse
import com.bako.api.member.dto.WithdrawResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import kotlinx.coroutines.withContext
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.Files
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.io.path.exists

@Service
class MemberService(
    private val memberRepository: MemberRepository,
    private val creditRepository: CreditRepository,
    private val jobRepository: JobRepository,
    private val databaseClient: DatabaseClient,
    private val appProperties: AppProperties,
) {

    suspend fun getMe(memberId: Long): MemberResponse {
        val member = memberRepository.findById(memberId).awaitSingleOrNull()
            ?: throw ApiException.notFound("MEMBER_NOT_FOUND", "Member not found")

        val credit = creditRepository.findByMemberId(memberId).awaitSingleOrNull()
        val balance = credit?.balanceMin ?: 0

        return MemberResponse(
            memberId = member.id!!,
            email = member.email,
            nickname = member.nickname,
            creditBalance = balance,
            createdAt = member.createdAt,
        )
    }

    @Transactional
    suspend fun withdraw(memberId: Long): WithdrawResponse {
        val member = memberRepository.findById(memberId).awaitSingleOrNull()
            ?: throw ApiException.notFound("MEMBER_NOT_FOUND", "Member not found")

        // Revoke Firebase refresh tokens before nulling firebase_uid
        member.firebaseUid?.let { uid ->
            withContext(Dispatchers.IO) {
                runCatching { FirebaseAuth.getInstance().revokeRefreshTokens(uid) }
            }
        }

        databaseClient.sql(
            """
            UPDATE members
            SET status = 'WITHDRAWN', email = NULL, password = NULL, firebase_uid = NULL,
                nickname = NULL, deleted_at = :deletedAt, updated_at = :updatedAt
            WHERE id = :id
            """
        )
            .bind("deletedAt", LocalDateTime.now())
            .bind("updatedAt", LocalDateTime.now())
            .bind("id", memberId)
            .fetch()
            .rowsUpdated()
            .awaitSingle()

        deleteStorageFilesForMember(memberId)

        return WithdrawResponse(message = "Account successfully withdrawn")
    }

    private suspend fun deleteStorageFilesForMember(memberId: Long) {
        val jobs = jobRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .collectList()
            .awaitSingle()

        for (job in jobs) {
            job.videoPath?.let { path ->
                try {
                    val file = Path.of(path)
                    if (file.exists()) Files.deleteIfExists(file)
                } catch (_: Exception) {}
            }
            job.originalSrt?.let { path ->
                try {
                    val file = Path.of(path)
                    if (file.exists()) Files.deleteIfExists(file)
                } catch (_: Exception) {}
            }
            job.translatedSrt?.let { path ->
                try {
                    val file = Path.of(path)
                    if (file.exists()) Files.deleteIfExists(file)
                } catch (_: Exception) {}
            }
        }
    }
}
