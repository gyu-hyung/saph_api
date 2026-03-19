package com.saph.api.auth.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthFilter(private val jwtProvider: JwtProvider) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authHeader = exchange.request.headers.getFirst("Authorization")

        val token = when {
            authHeader != null && authHeader.startsWith("Bearer ") ->
                authHeader.removePrefix("Bearer ").trim()
            else ->
                exchange.request.queryParams.getFirst("token")?.trim()
        } ?: return chain.filter(exchange)
        val memberId = jwtProvider.validateAndGetMemberId(token)
            ?: return chain.filter(exchange)

        val authentication = UsernamePasswordAuthenticationToken(
            memberId.toString(),
            null,
            listOf(SimpleGrantedAuthority("ROLE_USER"))
        )

        return chain.filter(exchange)
            .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
    }
}
