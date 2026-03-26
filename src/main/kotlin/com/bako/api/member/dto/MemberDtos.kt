package com.bako.api.member.dto

import java.time.LocalDateTime

data class MemberResponse(
    val memberId: Long,
    val email: String?,
    val nickname: String?,
    val creditBalance: Int,
    val createdAt: LocalDateTime?,
)

data class WithdrawResponse(
    val message: String,
)
