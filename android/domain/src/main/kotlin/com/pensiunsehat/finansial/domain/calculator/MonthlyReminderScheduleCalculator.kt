package com.pensiunsehat.finansial.domain.calculator

import java.time.Duration
import java.time.ZonedDateTime

object MonthlyReminderScheduleCalculator {
    fun nextRun(now: ZonedDateTime, reminderDayOfMonth: Int): ZonedDateTime {
        val safeDay = reminderDayOfMonth.coerceIn(1, 28)
        val todayTarget = now.withDayOfMonth(safeDay.coerceAtMost(now.toLocalDate().lengthOfMonth()))
            .withHour(9)
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
        return if (todayTarget.isAfter(now)) {
            todayTarget
        } else {
            val nextMonth = now.plusMonths(1)
            nextMonth.withDayOfMonth(safeDay.coerceAtMost(nextMonth.toLocalDate().lengthOfMonth()))
                .withHour(9)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        }
    }

    fun delayMillis(now: ZonedDateTime, reminderDayOfMonth: Int): Long =
        Duration.between(now, nextRun(now, reminderDayOfMonth)).toMillis().coerceAtLeast(0)
}
