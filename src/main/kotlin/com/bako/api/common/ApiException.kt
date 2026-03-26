package com.bako.api.common

import org.springframework.http.HttpStatus

class ApiException(
    val httpStatus: HttpStatus,
    val code: String,
    override val message: String,
) : RuntimeException(message) {

    companion object {
        fun badRequest(code: String, message: String) =
            ApiException(HttpStatus.BAD_REQUEST, code, message)

        fun unauthorized(code: String = "UNAUTHORIZED", message: String = "Unauthorized") =
            ApiException(HttpStatus.UNAUTHORIZED, code, message)

        fun forbidden(code: String = "FORBIDDEN", message: String = "Forbidden") =
            ApiException(HttpStatus.FORBIDDEN, code, message)

        fun notFound(code: String, message: String) =
            ApiException(HttpStatus.NOT_FOUND, code, message)

        fun conflict(code: String, message: String) =
            ApiException(HttpStatus.CONFLICT, code, message)

        fun internalError(code: String = "INTERNAL_ERROR", message: String = "Internal server error") =
            ApiException(HttpStatus.INTERNAL_SERVER_ERROR, code, message)
    }
}
