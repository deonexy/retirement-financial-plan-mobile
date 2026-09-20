package com.pensiunsehat.finansial.domain.usecase

import com.pensiunsehat.finansial.data.repository.LocalFinancialRepository
import com.pensiunsehat.finansial.domain.model.FinancialSummary
import kotlinx.coroutines.flow.Flow

class ObserveFinancialSummaryUseCase(
    private val repository: LocalFinancialRepository,
) {
    operator fun invoke(): Flow<FinancialSummary> = repository.observeFinancialSummary()
}
