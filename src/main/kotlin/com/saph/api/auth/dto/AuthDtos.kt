package com.saph.api.auth.dto

import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:Size(min = 2, max = 50, message = "Nickname must be between 2 and 50 characters")
    val nickname: String? = null,
)

data class RegisterResponse(
    val memberId: Long,
    val email: String?,
    val nickname: String?,
)
