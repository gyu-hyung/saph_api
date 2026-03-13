package com.saph.api.auth.service

import com.saph.api.auth.domain.Member
import com.saph.api.auth.domain.MemberStatus
import com.saph.api.auth.domain.RefreshToken
import com.saph.api.auth.dto.RefreshResponse
import com.saph.api.auth.dto.SignupRequest
import com.saph.api.auth.dto.SignupResponse
import com.saph.api.auth.dto.LoginRequest
import com.saph.api.auth.dto.TokenResponse
import com.saph.api.auth.repository.MemberRepository
import com.saph.api.auth.repository.RefreshTokenRepository
import com.saph.api.auth.security.JwtProvider
import com.saph.api.common.ApiException
import com.saph.api.credit.domain.Credit
import com.saph.api.credit.repository.CreditRepository
import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class AuthService(
    private val memberRepository: MemberRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val creditRepository: CreditRepository,
    private val jwtProvider: JwtProvider,
) {
    private val passwordEncoder = BCryptPasswordEncoder()

    @Transactional
    suspend fun signup(request: SignupRequest): SignupResponse {
        val existingId = memberRepository
            .findIdByEmailAndStatusNot(request.email, MemberStatus.WITHDRAWN.name)
            .awaitSingleOrNull()

        if (existingId != null) {
            throw ApiException.conflict("DUPLICATE_EMAIL", "Email already in use")
        }

        val member = memberRepository.save(
            Member(
                email = request.email,
                password = passwordEncoder.encode(request.password),
                nickname = request.nickname,
                status = MemberStatus.ACTIVE,
            )
        ).awaitSingle()

        creditRepository.save(
            Credit(
                memberId = member.id!!,
                balanceMin = 0,
            )
        ).awaitSingle()

        return SignupResponse(
            memberId = member.id,
            email = member.email!!,
            nickname = member.nickname!!,
        )
    }

    @Transactional
    suspend fun login(request: LoginRequest): TokenResponse {
        val member = memberRepository
            .findByEmail(request.email)
            .awaitSingleOrNull()
            ?: throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password")

        when (member.status) {
            MemberStatus.BLOCKED -> throw ApiException.forbidden("ACCOUNT_BLOCKED", "Account is blocked")
            MemberStatus.WITHDRAWN -> throw ApiException.forbidden("ACCOUNT_WITHDRAWN", "Account has been withdrawn")
            else -> Unit
        }

        if (!passwordEncoder.matches(request.password, member.password)) {
            throw ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid email or password")
        }

        val accessToken = jwtProvider.generateAccessToken(member.id!!)
        val refreshToken = jwtProvider.generateRefreshToken(member.id)

        val expiresAt = LocalDateTime.now().plusSeconds(
            jwtProvider.getRefreshTokenExpiryMs() / 1000
        )

        refreshTokenRepository.save(
            RefreshToken(
                memberId = member.id,
                token = refreshToken,
                expiresAt = expiresAt,
            )
        ).awaitSingle()

        return TokenResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtProvider.getAccessTokenExpiryMs() / 1000,
        )
    }

    @Transactional
    suspend fun refresh(refreshTokenStr: String): RefreshResponse {
        val storedToken = refreshTokenRepository
            .findByToken(refreshTokenStr)
            .awaitSingleOrNull()
            ?: throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Refresh token not found")

        if (storedToken.expiresAt.isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(refreshTokenStr).awaitSingleOrNull()
            throw ApiException.unauthorized("REFRESH_TOKEN_EXPIRED", "Refresh token has expired")
        }

        val memberId = jwtProvider.validateAndGetMemberId(refreshTokenStr)
            ?: throw ApiException.unauthorized("INVALID_REFRESH_TOKEN", "Invalid refresh token")

        val accessToken = jwtProvider.generateAccessToken(memberId)

        return RefreshResponse(
            accessToken = accessToken,
            expiresIn = jwtProvider.getAccessTokenExpiryMs() / 1000,
        )
    }
}
