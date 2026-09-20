package com.pensiunsehat.finansial.data.remote

import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToLong
import org.json.JSONObject

class DefaultGoldPriceApi : GoldPriceApi {
    override suspend fun fetchLatestGoldPrice(): GoldPricePayload {
        val connection = (URL(GOLD_API_URL).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 10_000
        }

        return try {
            if (connection.responseCode !in 200..299) error("Gold API error ${connection.responseCode}")
            val json = JSONObject(connection.inputStream.bufferedReader().use { reader -> reader.readText() })
            val pricePerGram = resolvePricePerGram(json)
            GoldPricePayload(
                pricePerGram = pricePerGram.toString(),
                currency = "IDR",
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

    private companion object {
        const val GOLD_API_URL = "https://api.gold-api.com/price/XAU/IDR"
        const val TROY_OUNCE_IN_GRAM = 31.1034768
    }
}
