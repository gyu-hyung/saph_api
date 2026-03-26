package com.bako.api.auth.dto

import jakarta.validation.constraints.Size

data class RegisterRequest(
    val nickname: String? = null,
)

data class RegisterResponse(
    val memberId: Long,
    val email: String?,
    val nickname: String?,
)
