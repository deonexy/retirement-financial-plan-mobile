package com.pensiunsehat.finansial.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DefaultGoldPriceApi : GoldPriceApi {
    override suspend fun fetchLatestGoldPrice(): GoldPricePayload = withContext(Dispatchers.IO) {
        val connection = (URL(GOLD_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (connection.responseCode !in 200..299) error("Gold API error ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { reader -> reader.readText() })
            val pricePerGram = resolvePricePerGram(json)
            val currency = resolveCurrency(json)
            GoldPricePayload(
                pricePerGram = pricePerGram.toString(),
                currency = currency,
                source = "gold-api",
                capturedAtIso = LocalDate.now(ZoneOffset.UTC).toString(),
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun resolvePricePerGram(json: JSONObject): Long {
        val directPerGram = json.optDouble("price_gram_24k")
        if (!directPerGram.isNaN()) return directPerGram.roundToLong()
        val directPrice = json.optDouble("price")
        if (!directPrice.isNaN()) {
            val perGram = directPrice / TROY_OUNCE_IN_GRAM
            return perGram.roundToLong()
        }
        error("Gold API payload missing price field")
    }

    private fun resolveCurrency(json: JSONObject): String {
        val detected = sequenceOf(
            json.optString("currency"),
            json.optString("currency_code"),
            json.optString("curr"),
        ).firstOrNull { it.isNotBlank() } ?: "IDR"
        if (detected != "IDR") error("Unsupported currency $detected")
        return detected
    }

    private companion object {
        const val GOLD_API_URL = "https://api.gold-api.com/price/XAU/IDR"
        const val TROY_OUNCE_IN_GRAM = 31.1034768
    }
}
