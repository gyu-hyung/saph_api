package com.bako.api.job.controller

import com.bako.api.common.ApiResponse
import com.bako.api.job.service.JobService
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/jobs")
class JobController(private val jobService: JobService) {

    @GetMapping
    suspend fun listJobs(authentication: Authentication): ApiResponse {
        val memberId = authentication.name.toLong()
        val jobs = jobService.listJobs(memberId)
        return ApiResponse.success(jobs)
    }
}
