package com.bako.api.common

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ApiResponse(
    val code: String,
    val data: Any? = null,
    val message: String? = null,
) {
    companion object {
        fun success(data: Any? = null): ApiResponse =
            ApiResponse(code = "SUCCESS", data = data)

        fun error(code: String, message: String): ApiResponse =
            ApiResponse(code = code, message = message)
    }
}
