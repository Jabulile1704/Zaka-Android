package com.jabulile.zaka.core

import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId

/**
 * One calendar month as a half-open range of instants: `start` is included,
 * `endExclusive` is not.
 *
 * Half-open rather than inclusive because the end of a month is not a round
 * number — an expense logged at 23:59:59.999 on the 31st belongs to that
 * month, and `date < endExclusive` says so without anyone having to reason
 * about milliseconds.
 *
 * The zone matters: a month begins at local midnight, not at UTC midnight.
 */
data class MonthWindow(
    val start: Instant,
    val endExclusive: Instant,
) {
    operator fun contains(moment: Instant): Boolean =
        moment >= start && moment < endExclusive

    companion object {
        fun of(month: YearMonth, zone: ZoneId = ZoneId.systemDefault()): MonthWindow =
            MonthWindow(
                start = month.atDay(1).atStartOfDay(zone).toInstant(),
                endExclusive = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant(),
            )

        fun containing(moment: Instant, zone: ZoneId = ZoneId.systemDefault()): MonthWindow =
            of(YearMonth.from(moment.atZone(zone)), zone)
    }
}
