package com.saph.api.auth.security

import com.google.firebase.auth.FirebaseAuth
import com.saph.api.auth.domain.MemberStatus
import com.saph.api.auth.repository.MemberRepository
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import reactor.core.scheduler.Schedulers

@Component
class FirebaseAuthFilter(private val memberRepository: MemberRepository) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = extractToken(exchange) ?: return chain.filter(exchange)

        // chain.filter() returns Mono<Void> which always completes empty.
        // Using switchIfEmpty(chain.filter()) would fire on every request (even after
        // successful auth) because Mono<Void> never emits an item.
        // Fix: wrap each branch in thenReturn(Unit) so switchIfEmpty only fires when
        // auth is absent, then convert back to Mono<Void> with then().
        return Mono.fromCallable { FirebaseAuth.getInstance().verifyIdToken(token) }
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap { decodedToken ->
                memberRepository.findByFirebaseUid(decodedToken.uid)
                    .filter { it.status != MemberStatus.BLOCKED && it.status != MemberStatus.WITHDRAWN }
                    .map { member ->
                        UsernamePasswordAuthenticationToken(
                            member.id!!.toString(),
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_USER")),
                        )
                    }
            }
            .onErrorResume { Mono.empty() }
            .flatMap { auth ->
                chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth))
                    .thenReturn(Unit)
            }
            .switchIfEmpty(chain.filter(exchange).thenReturn(Unit))
            .then()
    }

    private fun extractToken(exchange: ServerWebExchange): String? {
        val authHeader = exchange.request.headers.getFirst("Authorization")
        return when {
            authHeader != null && authHeader.startsWith("Bearer ") ->
                authHeader.removePrefix("Bearer ").trim()
            else ->
                exchange.request.queryParams.getFirst("token")?.trim()
        }
    }
}
