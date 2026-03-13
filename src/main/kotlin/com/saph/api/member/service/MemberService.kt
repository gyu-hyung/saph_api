package com.saph.api.member.service

import com.saph.api.auth.repository.MemberRepository
import com.saph.api.auth.repository.RefreshTokenRepository
import com.saph.api.common.ApiException
import com.saph.api.config.AppProperties
import com.saph.api.credit.repository.CreditRepository
import com.saph.api.job.repository.JobRepository
import com.saph.api.member.dto.MemberResponse
import com.saph.api.member.dto.WithdrawResponse
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
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
    private val refreshTokenRepository: RefreshTokenRepository,
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

        // Update member status to WITHDRAWN and null out PII
        databaseClient.sql(
            """
            UPDATE members
            SET status = 'WITHDRAWN', email = NULL, password = NULL,
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

        // Delete all refresh tokens for this member
        refreshTokenRepository.deleteByMemberId(memberId).awaitSingleOrNull()

        // Delete stored files for member's jobs
        deleteStorageFilesForMember(memberId)

        return WithdrawResponse(message = "Account successfully withdrawn")
    }

    private suspend fun deleteStorageFilesForMember(memberId: Long) {
        val jobs = jobRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
            .collectList()
            .awaitSingle()

        for (job in jobs) {
            // Delete video file
            job.videoPath?.let { path ->
                try {
                    val file = Path.of(path)
                    if (file.exists()) Files.deleteIfExists(file)
                } catch (_: Exception) {}
            }
            // Delete SRT files
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
