package com.saph.api.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.bind.ConstructorBinding

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val jwt: Jwt,
    val storage: Storage,
) {
    data class Jwt(
        val secret: String,
        val accessTokenExpiryMs: Long,
        val refreshTokenExpiryMs: Long,
    )

    data class Storage(
        val videosDir: String,
        val resultsDir: String,
    )
}
