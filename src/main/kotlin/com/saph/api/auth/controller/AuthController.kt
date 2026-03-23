package com.saph.api.auth.controller

import com.google.firebase.auth.FirebaseAuth
import com.saph.api.auth.dto.RegisterRequest
import com.saph.api.auth.service.AuthService
import com.saph.api.common.ApiException
import com.saph.api.common.ApiResponse
import jakarta.validation.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/register")
    suspend fun register(
        @Valid @RequestBody request: RegisterRequest,
        @RequestHeader("Authorization", required = false) authHeader: String?,
    ): ApiResponse {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw ApiException.unauthorized()
        }
        val token = authHeader.removePrefix("Bearer ").trim()
        val decodedToken = withContext(Dispatchers.IO) {
            runCatching { FirebaseAuth.getInstance().verifyIdToken(token) }
                .getOrNull()
        } ?: throw ApiException.unauthorized("INVALID_TOKEN", "Invalid Firebase token")

        val result = authService.register(
            firebaseUid = decodedToken.uid,
            email = decodedToken.email,
            displayName = decodedToken.name,
            request = request,
        )
        return ApiResponse.success(result)
    }
}
