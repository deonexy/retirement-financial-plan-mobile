package com.pensiunsehat.finansial.domain.model

import java.math.BigDecimal

data class GoldPriceSnapshot(
    val id: Long = 0,
    val pricePerGram: BigDecimal? = null,
    val currency: String = "IDR",
    val source: String = "local",
    val capturedAtIso: String? = null,
    val status: GoldPriceStatus = GoldPriceStatus.UNAVAILABLE,
)
