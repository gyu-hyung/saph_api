package com.saph.api.auth.controller

import com.saph.api.auth.dto.LoginRequest
import com.saph.api.auth.dto.RefreshRequest
import com.saph.api.auth.dto.SignupRequest
import com.saph.api.auth.service.AuthService
import com.saph.api.common.ApiResponse
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(private val authService: AuthService) {

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun signup(@Valid @RequestBody request: SignupRequest): ApiResponse {
        val result = authService.signup(request)
        return ApiResponse.success(result)
    }

    @PostMapping("/login")
    suspend fun login(@Valid @RequestBody request: LoginRequest): ApiResponse {
        val result = authService.login(request)
        return ApiResponse.success(result)
    }

    @PostMapping("/refresh")
    suspend fun refresh(@Valid @RequestBody request: RefreshRequest): ApiResponse {
        val result = authService.refresh(request.refreshToken)
        return ApiResponse.success(result)
    }
}
