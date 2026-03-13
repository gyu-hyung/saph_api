package com.saph.api.credit.repository

import com.saph.api.credit.domain.CreditLog
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface CreditLogRepository : ReactiveCrudRepository<CreditLog, Long>
