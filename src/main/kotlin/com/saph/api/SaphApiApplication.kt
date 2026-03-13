package com.saph.api

import com.saph.api.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class SaphApiApplication

fun main(args: Array<String>) {
    runApplication<SaphApiApplication>(*args)
}
