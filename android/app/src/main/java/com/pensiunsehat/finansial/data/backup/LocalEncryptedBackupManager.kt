package com.pensiunsehat.finansial.data.backup

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.room.withTransaction
import com.pensiunsehat.finansial.data.local.AppDatabase
import com.pensiunsehat.finansial.data.local.entity.AssetPurchaseEntity
import com.pensiunsehat.finansial.data.local.entity.FinancialUpdateEntity
import com.pensiunsehat.finansial.data.local.entity.GoldPriceSnapshotEntity
import com.pensiunsehat.finansial.data.local.entity.RetirementProfileEntity
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import com.pensiunsehat.finansial.data.repository.ThemePreference
import java.io.File
import java.security.KeyStore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
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
    suspend fun exportBackup(): File {
        val payload = buildBackupPayload()
        val encrypted = encrypt(payload.toByteArray(Charsets.UTF_8))
        val backupDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val file = File(backupDir, "retirement-backup-${System.currentTimeMillis()}.json.enc")
        file.writeBytes(encrypted)
        return file
    }

    suspend fun importLatestBackup(mode: BackupImportMode): BackupImportSummary {
        val file = latestBackupFile() ?: throw IllegalStateException("Belum ada file backup.")
        val decrypted = decrypt(file.readBytes()).toString(Charsets.UTF_8)
        val payload = JSONObject(decrypted)

        val profile = payload.optJSONObject("profile")?.toProfileEntity()
        val financialUpdates = payload.optJSONArray("financialUpdates").toFinancialUpdateEntities()
        val assetPurchases = payload.optJSONArray("assetPurchases").toAssetPurchaseEntities()
        val goldSnapshots = payload.optJSONArray("goldPriceSnapshots").toGoldPriceSnapshotEntities()
        val settings = payload.optJSONObject("settings")

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

        settings?.let { applySettings(it) }

        return BackupImportSummary(
            profileImported = profile != null,
            financialUpdateCount = financialUpdates.size,
            assetPurchaseCount = assetPurchases.size,
            goldSnapshotCount = goldSnapshots.size,
        )
    }

    fun latestBackupFile(): File? {
        val backupDir = File(context.getExternalFilesDir(null), "backups")
        return backupDir.listFiles()
            ?.filter { it.isFile && it.name.endsWith(".json.enc") }
            ?.maxByOrNull { it.lastModified() }
    }

    private suspend fun buildBackupPayload(): String {
        val profile = database.retirementProfileDao().getLatest()
        val financialUpdates = database.financialUpdateDao().getAll()
        val assetPurchases = database.assetPurchaseDao().getAll()
        val goldSnapshots = database.goldPriceSnapshotDao().getAll()
        val settings = runCatching { settingsRepository.settingsFlow.first() }.getOrDefault(settingsRepository.defaultSettings)

        return JSONObject()
            .put("backupVersion", 1)
            .put("exportedAt", Instant.now().toString())
            .put("profile", profile?.toJson())
            .put("financialUpdates", JSONArray(financialUpdates.map { it.toJson() }))
            .put("assetPurchases", JSONArray(assetPurchases.map { it.toJson() }))
            .put("goldPriceSnapshots", JSONArray(goldSnapshots.map { it.toJson() }))
            .put(
                "settings",
                JSONObject()
                    .put("themePreference", settings.themePreference.name)
                    .put("monthlyReminderEnabled", settings.monthlyReminderEnabled)
                    .put("reminderDayOfMonth", settings.reminderDayOfMonth)
                    .put("goldPriceAutoRefresh", settings.goldPriceAutoRefresh),
            )
            .toString()
    }

    private suspend fun applySettings(settings: JSONObject) {
        settings.optString("themePreference").takeIf { it.isNotBlank() }?.let {
            runCatching { settingsRepository.setThemePreference(ThemePreference.valueOf(it)) }
        }
        if (settings.has("monthlyReminderEnabled")) {
            settingsRepository.setMonthlyReminderEnabled(settings.optBoolean("monthlyReminderEnabled"))
        }
        if (settings.has("reminderDayOfMonth")) {
            settingsRepository.setReminderDayOfMonth(settings.optInt("reminderDayOfMonth", 1))
        }
        if (settings.has("goldPriceAutoRefresh")) {
            settingsRepository.setGoldPriceAutoRefresh(settings.optBoolean("goldPriceAutoRefresh"))
        }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plain)
        return iv + ciphertext
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size > 12) { "Backup file tidak valid." }
        val iv = payload.copyOfRange(0, 12)
        val ciphertext = payload.copyOfRange(12, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(false)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
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

    private companion object {
        const val KEY_ALIAS = "retirement_backup_key"
    }
}
