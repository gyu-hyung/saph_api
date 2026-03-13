package com.saph.api.member.controller

import com.saph.api.common.ApiResponse
import com.saph.api.member.service.MemberService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/members")
class MemberController(private val memberService: MemberService) {

    @GetMapping("/me")
    suspend fun getMe(authentication: Authentication): ApiResponse {
        val memberId = authentication.name.toLong()
        val result = memberService.getMe(memberId)
        return ApiResponse.success(result)
    }

    @DeleteMapping("/me")
    suspend fun withdraw(authentication: Authentication): ApiResponse {
        val memberId = authentication.name.toLong()
        val result = memberService.withdraw(memberId)
        return ApiResponse.success(result)
    }
}
