package com.saph.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SaphApiApplication

fun main(args: Array<String>) {
    runApplication<SaphApiApplication>(*args)
}
