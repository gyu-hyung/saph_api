package com.bako.api

import com.bako.api.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class BakoApiApplication

fun main(args: Array<String>) {
    runApplication<BakoApiApplication>(*args)
}
