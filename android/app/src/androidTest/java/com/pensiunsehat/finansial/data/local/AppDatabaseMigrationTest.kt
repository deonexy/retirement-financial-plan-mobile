package com.pensiunsehat.finansial.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java.canonicalName,
        FrameworkSQLiteOpenHelperFactory(),
    )

    @Test
    fun migrateLatestSchemaWithoutDataLoss() {
        val dbName = "migration-test"
        helper.createDatabase(dbName, 1).apply {
            execSQL(
                """
                INSERT INTO retirement_profiles (
                  id,currentAge,retirementAge,startingSavingsBalance,liquidAssets,stockAssets,otherInvestmentAssets,
                  propertyAssets,dplkBalance,jhtBalance,goldGramsOwned,goldGramsPawned,goldPawnLiability,
                  monthlyRetirementNeeds,postRetirementIncomeTarget,inflationRatePercent,annualGoldGrowthRatePercent,
                  targetLegacy,notes,updatedAtEpochMs
                ) VALUES (
                  1,35,60,'10000000','0','0','0','0','0','0','0','0','0','5000000','0','4','5','0',NULL,0
                )
                """.trimIndent(),
            )
            close()
        }

        helper.runMigrationsAndValidate(dbName, 1, true)
    }
}
