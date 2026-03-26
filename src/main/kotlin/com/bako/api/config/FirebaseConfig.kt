package com.bako.api.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.Resource

@Configuration
class FirebaseConfig(
    @Value("\${firebase.credentials.path:}") private val credentialsPath: Resource?
) {

    @Bean
    fun firebaseApp(): FirebaseApp {
        if (FirebaseApp.getApps().isNotEmpty()) {
            return FirebaseApp.getInstance()
        }

        val credentials = if (credentialsPath != null && credentialsPath.exists()) {
            credentialsPath.inputStream.use { GoogleCredentials.fromStream(it) }
        } else {
            GoogleCredentials.getApplicationDefault()
        }

        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        return FirebaseApp.initializeApp(options)
    }
}
