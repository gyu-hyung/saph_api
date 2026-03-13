package com.saph.api.auth.security

import com.saph.api.config.AppProperties
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date

@Component
class JwtProvider(private val appProperties: AppProperties) {

    private val key by lazy {
        Keys.hmacShaKeyFor(
            appProperties.jwt.secret.toByteArray(StandardCharsets.UTF_8)
        )
    }

    fun generateAccessToken(memberId: Long): String {
        val now = Date()
        val expiry = Date(now.time + appProperties.jwt.accessTokenExpiryMs)
        return Jwts.builder()
            .subject(memberId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .claim("type", "access")
            .signWith(key)
            .compact()
    }

    fun generateRefreshToken(memberId: Long): String {
        val now = Date()
        val expiry = Date(now.time + appProperties.jwt.refreshTokenExpiryMs)
        return Jwts.builder()
            .subject(memberId.toString())
            .issuedAt(now)
            .expiration(expiry)
            .claim("type", "refresh")
            .signWith(key)
            .compact()
    }

    fun validateAndGetMemberId(token: String): Long? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .payload
            claims.subject.toLong()
        } catch (e: JwtException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun getAccessTokenExpiryMs(): Long = appProperties.jwt.accessTokenExpiryMs
    fun getRefreshTokenExpiryMs(): Long = appProperties.jwt.refreshTokenExpiryMs
}
