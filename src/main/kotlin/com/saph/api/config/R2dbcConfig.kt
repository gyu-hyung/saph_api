package com.saph.api.config

import com.saph.api.auth.domain.MemberStatus
import com.saph.api.credit.domain.CreditReason
import com.saph.api.job.domain.JobStatus
import com.saph.api.job.domain.JobStep
import com.saph.api.payment.domain.PackageType
import com.saph.api.payment.domain.PaymentStatus
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration
import io.r2dbc.postgresql.PostgresqlConnectionFactory
import io.r2dbc.postgresql.codec.EnumCodec
import io.r2dbc.spi.ConnectionFactory
import org.springframework.boot.autoconfigure.r2dbc.R2dbcProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories

@Configuration
@EnableR2dbcRepositories(basePackages = ["com.saph.api"])
class R2dbcConfig(
    private val r2dbcProperties: R2dbcProperties,
) : AbstractR2dbcConfiguration() {

    @Bean
    @Primary
    override fun connectionFactory(): ConnectionFactory {
        val url = r2dbcProperties.url
            ?: "r2dbc:postgresql://postgres:5432/saph"

        val withoutScheme = url
            .removePrefix("r2dbc:postgresql://")
            .removePrefix("r2dbc:pool:postgresql://")
        val hostPortDb = withoutScheme.split("/")
        val hostPort = hostPortDb[0].split(":")
        val host = hostPort[0]
        val port = if (hostPort.size > 1) hostPort[1].toInt() else 5432
        val database = if (hostPortDb.size > 1) hostPortDb[1].split("?")[0] else "saph"

        val username = r2dbcProperties.username ?: "saph"
        val password = r2dbcProperties.password ?: "saph"

        return PostgresqlConnectionFactory(
            PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .codecRegistrar(
                    EnumCodec.builder()
                        .withEnum("member_status", MemberStatus::class.java)
                        .withEnum("job_status", JobStatus::class.java)
                        .withEnum("job_step", JobStep::class.java)
                        .withEnum("package_type", PackageType::class.java)
                        .withEnum("payment_status", PaymentStatus::class.java)
                        .withEnum("credit_reason", CreditReason::class.java)
                        .build()
                )
                .build()
        )
    }
}
