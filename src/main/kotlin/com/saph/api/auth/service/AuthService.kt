package com.saph.api.auth.service

import com.saph.api.auth.domain.Member
import com.saph.api.auth.domain.MemberStatus
import com.saph.api.auth.dto.RegisterRequest
import com.saph.api.auth.dto.RegisterResponse
import com.saph.api.auth.repository.MemberRepository
import com.saph.api.common.ApiException
import com.saph.api.credit.domain.Credit
import com.saph.api.credit.repository.CreditRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val creditRepository: CreditRepository,
) {

    @Transactional
    suspend fun register(
        firebaseUid: String,
        email: String?,
        displayName: String?,
        request: RegisterRequest,
    ): RegisterResponse {
        val existing = memberRepository.findByFirebaseUid(firebaseUid).awaitSingleOrNull()

        if (existing != null) {
            return when (existing.status) {
                MemberStatus.BLOCKED -> throw ApiException.forbidden("ACCOUNT_BLOCKED", "Account is blocked")
                MemberStatus.WITHDRAWN -> throw ApiException.forbidden("ACCOUNT_WITHDRAWN", "Account has been withdrawn")
                else -> RegisterResponse(
                    memberId = existing.id!!,
                    email = existing.email,
                    nickname = existing.nickname,
                )
            }
        }

        val effectiveNickname = request.nickname?.takeIf { it.isNotBlank() }
            ?: displayName?.takeIf { it.isNotBlank() }
            ?: email?.substringBefore('@')
            ?: "User"

        val member = memberRepository.save(
            Member(
                firebaseUid = firebaseUid,
                email = email,
                nickname = effectiveNickname,
                status = MemberStatus.ACTIVE,
            )
        ).awaitSingle()

        creditRepository.save(
            Credit(
                memberId = member.id!!,
                balanceMin = 0,
            )
        ).awaitSingle()

        return RegisterResponse(
            memberId = member.id,
            email = member.email,
            nickname = member.nickname,
        )
    }
}
