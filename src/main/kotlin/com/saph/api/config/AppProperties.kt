package com.saph.api.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppProperties(
    val storage: Storage,
) {
    data class Storage(
        val videosDir: String,
        val resultsDir: String,
    )
}
