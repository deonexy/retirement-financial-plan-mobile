package com.pensiunsehat.finansial.data.remote

data class GoldPricePayload(
    val pricePerGram: String,
    val currency: String,
    val source: String,
    val capturedAtIso: String,
)

interface GoldPriceApi {
    suspend fun fetchLatestGoldPrice(): GoldPricePayload
}
