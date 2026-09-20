package com.pensiunsehat.finansial.domain.calculator

import com.pensiunsehat.finansial.domain.model.GoldPriceSnapshot
import com.pensiunsehat.finansial.domain.model.GoldPriceStatus
import com.pensiunsehat.finansial.domain.model.RetirementProfile
import java.math.BigDecimal

data class GoldValuation(
    val totalSaleValue: BigDecimal?,
    val netValue: BigDecimal?,
    val statusLabel: String,
)

object GoldCalculator {
    fun calculate(profile: RetirementProfile, snapshot: GoldPriceSnapshot?): GoldValuation {
        val pricePerGram = snapshot?.pricePerGram
        if (pricePerGram == null) {
            return GoldValuation(
                totalSaleValue = null,
                netValue = null,
                statusLabel = formatGoldPriceStatus(snapshot),
            )
        }

        val totalGrams = profile.goldGramsOwned + profile.goldGramsPawned
        val totalSaleValue = totalGrams.multiply(pricePerGram)
        val netValue = totalSaleValue.subtract(profile.goldPawnLiability)
            .takeIf { it >= BigDecimal.ZERO }
            ?: BigDecimal.ZERO

        return GoldValuation(
            totalSaleValue = totalSaleValue,
            netValue = netValue,
            statusLabel = formatGoldPriceStatus(snapshot),
        )
    }
}

fun formatGoldPriceStatus(snapshot: GoldPriceSnapshot?): String = when {
    snapshot == null || snapshot.pricePerGram == null || snapshot.status == GoldPriceStatus.UNAVAILABLE -> "Harga emas belum tersedia"
    snapshot.status == GoldPriceStatus.LIVE -> "Harga emas live ${snapshot.capturedAtIso.orEmpty()}"
    snapshot.status == GoldPriceStatus.LAST_KNOWN -> "Harga terakhir diperbarui pada ${snapshot.capturedAtIso.orEmpty()}"
    snapshot.status == GoldPriceStatus.STALE -> "Harga terakhir diperbarui pada ${snapshot.capturedAtIso.orEmpty()} (stale)"
    else -> "Harga emas belum tersedia"
}
