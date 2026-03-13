package com.saph.api.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class SignupRequest(
    @field:Email(message = "Invalid email format")
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-zA-Z])(?=.*[0-9]).+$",
        message = "Password must contain both letters and numbers"
    )
    val password: String,

    @field:NotBlank(message = "Nickname is required")
    @field:Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    val nickname: String,
)

data class LoginRequest(
    @field:NotBlank(message = "Email is required")
    val email: String,

    @field:NotBlank(message = "Password is required")
    val password: String,
)

data class RefreshRequest(
    @field:NotBlank(message = "Refresh token is required")
    val refreshToken: String,
)

data class SignupResponse(
    val memberId: Long,
    val email: String,
    val nickname: String,
)

data class TokenResponse(
    val accessToken: String,
    val refreshToken: String?,
    val expiresIn: Long,
)

data class RefreshResponse(
    val accessToken: String,
    val expiresIn: Long,
)
