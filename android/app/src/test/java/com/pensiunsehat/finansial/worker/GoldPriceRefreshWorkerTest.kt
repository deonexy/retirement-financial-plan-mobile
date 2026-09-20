package com.pensiunsehat.finansial.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GoldPriceRefreshWorkerTest {
    @Test
    fun placeholderWorkerReturnsSuccess() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val worker = TestListenableWorkerBuilder<GoldPriceRefreshWorker>(context).build()

        assertEquals(ListenableWorker.Result.success(), worker.doWork())
    }
}
