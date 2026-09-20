package com.pensiunsehat.finansial.data.backup

import android.content.Context
import androidx.room.withTransaction
import com.pensiunsehat.finansial.data.local.AppDatabase
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import java.io.File
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

enum class BackupImportMode {
    MERGE,
    REPLACE,
}

data class BackupImportSummary(
    val profileImported: Boolean,
    val financialUpdateCount: Int,
    val assetPurchaseCount: Int,
    val goldSnapshotCount: Int,
)

class LocalEncryptedBackupManager(
    private val context: Context,
    private val database: AppDatabase,
    private val settingsRepository: SettingsRepository,
) {
    suspend fun exportBackup(passphrase: String): File {
        require(passphrase.length >= 8) { "Passphrase backup minimal 8 karakter." }
        val payload = buildBackupPayload()
        val encrypted = BackupCipher.encrypt(payload.toByteArray(Charsets.UTF_8), passphrase)
        val backupRoot = context.getExternalFilesDir(null) ?: context.filesDir
        val backupDir = File(backupRoot, "backups").apply { mkdirs() }
        return File(backupDir, "retirement-backup-${System.currentTimeMillis()}.json.enc").also {
            it.writeBytes(encrypted)
        }
    }

    suspend fun importLatestBackup(mode: BackupImportMode, passphrase: String): BackupImportSummary {
        require(passphrase.length >= 8) { "Passphrase backup minimal 8 karakter." }
        val file = latestBackupFile() ?: throw IllegalStateException("Belum ada file backup.")
        val decrypted = BackupCipher.decrypt(file.readBytes(), passphrase).toString(Charsets.UTF_8)
        val payload = JSONObject(decrypted)

        val profile = payload.optJSONObject("profile")?.toProfileEntity()
        val financialUpdates = payload.optJSONArray("financialUpdates").toFinancialUpdateEntities()
        val assetPurchases = payload.optJSONArray("assetPurchases").toAssetPurchaseEntities()
        val goldSnapshots = payload.optJSONArray("goldPriceSnapshots").toGoldPriceSnapshotEntities()
        database.withTransaction {
            if (mode == BackupImportMode.REPLACE) {
                database.retirementProfileDao().clearAll()
                database.financialUpdateDao().clearAll()
                database.assetPurchaseDao().clearAll()
                database.goldPriceSnapshotDao().clearAll()
            }
            profile?.let { database.retirementProfileDao().upsert(it) }
            if (financialUpdates.isNotEmpty()) database.financialUpdateDao().upsertAll(financialUpdates)
            if (assetPurchases.isNotEmpty()) database.assetPurchaseDao().upsertAll(assetPurchases)
            if (goldSnapshots.isNotEmpty()) database.goldPriceSnapshotDao().upsertAll(goldSnapshots)
        }

        return BackupImportSummary(
            profileImported = profile != null,
            financialUpdateCount = financialUpdates.size,
            assetPurchaseCount = assetPurchases.size,
            goldSnapshotCount = goldSnapshots.size,
        )
    }

    fun latestBackupFile(): File? {
        val backupRoot = context.getExternalFilesDir(null) ?: context.filesDir
        val backupDir = File(backupRoot, "backups")
        return backupDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json.enc") }
            ?.maxByOrNull { it.lastModified() }
    }

    private suspend fun buildBackupPayload(): String {
        val profile = database.retirementProfileDao().getLatest()
        val financialUpdates = database.financialUpdateDao().getAll()
        val assetPurchases = database.assetPurchaseDao().getAll()
        val goldSnapshots = database.goldPriceSnapshotDao().getAll()
        return JSONObject()
            .put("backupVersion", 1)
            .put("exportedAt", Instant.now().toString())
            .put("profile", profile?.toJson())
            .put("financialUpdates", JSONArray(financialUpdates.map { it.toJson() }))
            .put("assetPurchases", JSONArray(assetPurchases.map { it.toJson() }))
            .put("goldPriceSnapshots", JSONArray(goldSnapshots.map { it.toJson() }))
            .toString()
    }

    private fun RetirementProfileEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("currentAge", currentAge)
        .put("retirementAge", retirementAge)
        .put("startingSavingsBalance", startingSavingsBalance)
        .put("liquidAssets", liquidAssets)
        .put("stockAssets", stockAssets)
        .put("otherInvestmentAssets", otherInvestmentAssets)
        .put("propertyAssets", propertyAssets)
        .put("dplkBalance", dplkBalance)
        .put("jhtBalance", jhtBalance)
        .put("goldGramsOwned", goldGramsOwned)
        .put("goldGramsPawned", goldGramsPawned)
        .put("goldPawnLiability", goldPawnLiability)
        .put("monthlyRetirementNeeds", monthlyRetirementNeeds)
        .put("postRetirementIncomeTarget", postRetirementIncomeTarget)
        .put("inflationRatePercent", inflationRatePercent)
        .put("annualGoldGrowthRatePercent", annualGoldGrowthRatePercent)
        .put("targetLegacy", targetLegacy)
        .put("notes", notes)
        .put("updatedAtEpochMs", updatedAtEpochMs)

    private fun FinancialUpdateEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("updateDate", updateDate)
        .put("sortDateEpochDay", sortDateEpochDay)
        .put("netIncome", netIncome)
        .put("mandatoryExpenses", mandatoryExpenses)
        .put("lifestyleExpenses", lifestyleExpenses)
        .put("healthExpenses", healthExpenses)
        .put("debtPayments", debtPayments)
        .put("remainingDebt", remainingDebt)
        .put("notes", notes)
        .put("updatedAtEpochMs", updatedAtEpochMs)

    private fun AssetPurchaseEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("assetType", assetType)
        .put("assetName", assetName)
        .put("quantity", quantity)
        .put("unit", unit)
        .put("purchasePriceRupiah", purchasePriceRupiah)
        .put("purchaseValueRupiah", purchaseValueRupiah)
        .put("purchaseDate", purchaseDate)
        .put("sortDateEpochDay", sortDateEpochDay)
        .put("fundingSource", fundingSource)
        .put("notes", notes)
        .put("updatedAtEpochMs", updatedAtEpochMs)

    private fun GoldPriceSnapshotEntity.toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("pricePerGram", pricePerGram)
        .put("currency", currency)
        .put("source", source)
        .put("capturedAtIso", capturedAtIso)
        .put("capturedAtEpochDay", capturedAtEpochDay)
        .put("status", status)

    private fun JSONObject.toProfileEntity(): RetirementProfileEntity = RetirementProfileEntity(
        id = optLong("id", 1L).coerceAtLeast(1L),
        currentAge = optInt("currentAge"),
        retirementAge = optInt("retirementAge"),
        startingSavingsBalance = optString("startingSavingsBalance", "0"),
        liquidAssets = optString("liquidAssets", "0"),
        stockAssets = optString("stockAssets", "0"),
        otherInvestmentAssets = optString("otherInvestmentAssets", "0"),
        propertyAssets = optString("propertyAssets", "0"),
        dplkBalance = optString("dplkBalance", "0"),
        jhtBalance = optString("jhtBalance", "0"),
        goldGramsOwned = optString("goldGramsOwned", "0"),
        goldGramsPawned = optString("goldGramsPawned", "0"),
        goldPawnLiability = optString("goldPawnLiability", "0"),
        monthlyRetirementNeeds = optString("monthlyRetirementNeeds", "0"),
        postRetirementIncomeTarget = optString("postRetirementIncomeTarget", "0"),
        inflationRatePercent = optString("inflationRatePercent", "0"),
        annualGoldGrowthRatePercent = optString("annualGoldGrowthRatePercent", "0"),
        targetLegacy = optString("targetLegacy", "0"),
        notes = optString("notes").takeIf { it.isNotBlank() },
        updatedAtEpochMs = optLong("updatedAtEpochMs", System.currentTimeMillis()),
    )

    private fun JSONArray?.toFinancialUpdateEntities(): List<FinancialUpdateEntity> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                FinancialUpdateEntity(
                    id = item.optLong("id", 0L),
                    updateDate = item.optString("updateDate", LocalDate.now(ZoneOffset.UTC).toString()),
                    sortDateEpochDay = item.optLong("sortDateEpochDay", 0L),
                    netIncome = item.optString("netIncome", "0"),
                    mandatoryExpenses = item.optString("mandatoryExpenses", "0"),
                    lifestyleExpenses = item.optString("lifestyleExpenses", "0"),
                    healthExpenses = item.optString("healthExpenses", "0"),
                    debtPayments = item.optString("debtPayments", "0"),
                    remainingDebt = item.optString("remainingDebt", "0"),
                    notes = item.optString("notes").takeIf { it.isNotBlank() },
                    updatedAtEpochMs = item.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                )
            }
        }
    }

    private fun JSONArray?.toAssetPurchaseEntities(): List<AssetPurchaseEntity> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                AssetPurchaseEntity(
                    id = item.optLong("id", 0L),
                    assetType = item.optString("assetType", "OTHER"),
                    assetName = item.optString("assetName"),
                    quantity = item.optString("quantity", "0"),
                    unit = item.optString("unit", "unit"),
                    purchasePriceRupiah = item.optString("purchasePriceRupiah", "0"),
                    purchaseValueRupiah = item.optString("purchaseValueRupiah", "0"),
                    purchaseDate = item.optString("purchaseDate", LocalDate.now(ZoneOffset.UTC).toString()),
                    sortDateEpochDay = item.optLong("sortDateEpochDay", 0L),
                    fundingSource = item.optString("fundingSource", "NON_SAVINGS"),
                    notes = item.optString("notes").takeIf { it.isNotBlank() },
                    updatedAtEpochMs = item.optLong("updatedAtEpochMs", System.currentTimeMillis()),
                )
            }
        }
    }

    private fun JSONArray?.toGoldPriceSnapshotEntities(): List<GoldPriceSnapshotEntity> {
        if (this == null) return emptyList()
        return (0 until length()).mapNotNull { index ->
            optJSONObject(index)?.let { item ->
                GoldPriceSnapshotEntity(
                    id = item.optLong("id", 0L),
                    pricePerGram = item.optString("pricePerGram").takeIf { it.isNotBlank() && it != "null" },
                    currency = item.optString("currency", "IDR"),
                    source = item.optString("source", "backup-import"),
                    capturedAtIso = item.optString("capturedAtIso").takeIf { it.isNotBlank() },
                    capturedAtEpochDay = item.takeIf { it.has("capturedAtEpochDay") }?.optLong("capturedAtEpochDay"),
                    status = item.optString("status", "UNAVAILABLE"),
                )
            }
        }
    }
}

internal object BackupCipher {
    fun encrypt(plain: ByteArray, passphrase: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plain)
        return salt + iv + ciphertext
    }

    fun decrypt(payload: ByteArray, passphrase: String): ByteArray {
        require(payload.size > 28) { "Backup file tidak valid." }
        val salt = payload.copyOfRange(0, 16)
        val iv = payload.copyOfRange(16, 28)
        val ciphertext = payload.copyOfRange(28, payload.size)
        val key = deriveKey(passphrase, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun deriveKey(passphrase: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, 120_000, 256)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }
}
