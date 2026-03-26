package com.bako.api.credit.repository

import com.bako.api.credit.domain.CreditLog
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface CreditLogRepository : ReactiveCrudRepository<CreditLog, Long>
