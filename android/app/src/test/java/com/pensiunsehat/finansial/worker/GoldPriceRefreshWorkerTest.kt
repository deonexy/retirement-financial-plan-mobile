package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.pensiunsehat.finansial.data.local.AppDatabase
import com.pensiunsehat.finansial.data.repository.SettingsRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

class GoldPriceRefreshWorkerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase("retirement_finance.db")
        context.filesDir.parentFile?.resolve("datastore")?.listFiles()?.forEach(File::delete)
    }

    @After
    fun tearDown() {
        GoldPriceRefreshWorker.apiFactory = { com.pensiunsehat.finansial.data.remote.DefaultGoldPriceApi() }
    }

    @Test
    fun workerReturnsSuccessWhenAutoRefreshDisabled() = runTest {
        val worker = TestListenableWorkerBuilder<GoldPriceRefreshWorker>(context).build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }

    @Test
    fun workerStoresLiveSnapshotWhenAutoRefreshEnabled() = runTest {
        SettingsRepository(context).setGoldPriceAutoRefresh(true)
        GoldPriceRefreshWorker.apiFactory = {
            object : com.pensiunsehat.finansial.data.remote.GoldPriceApi {
                override suspend fun fetchLatestGoldPrice() = com.pensiunsehat.finansial.data.remote.GoldPricePayload(
                    pricePerGram = "1800000",
                    currency = "IDR",
                    source = "test",
                    capturedAtIso = "2026-09-20",
                )
            }
        }

        val worker = TestListenableWorkerBuilder<GoldPriceRefreshWorker>(context).build()
        val result = worker.doWork()

        val db = androidx.room.Room.databaseBuilder(context, AppDatabase::class.java, "retirement_finance.db")
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()
        val snapshot = db.goldPriceSnapshotDao().getLatest()
        db.close()

        assertEquals(ListenableWorker.Result.success(), result)
        assertNotNull(snapshot)
        assertEquals("1800000", snapshot?.pricePerGram)
        assertEquals("LIVE", snapshot?.status)
    }
}
