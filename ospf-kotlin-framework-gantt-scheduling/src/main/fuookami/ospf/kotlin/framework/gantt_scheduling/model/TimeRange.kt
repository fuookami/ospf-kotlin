package fuookami.ospf.kotlin.framework.gantt_scheduling.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.ordinary.*

fun max(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        rhs
    } else {
        lhs
    }
}

fun min(lhs: Instant, rhs: Instant): Instant {
    return if (lhs <= rhs) {
        lhs
    } else {
        rhs
    }
}

// [b, e)
data class TimeRange(
    val start: Instant = Instant.DISTANT_PAST,
    val end: Instant = Instant.DISTANT_FUTURE
) {
    val empty: Boolean get() = start >= end
    val duration: Duration get() = end - start

    fun withIntersection(ano: TimeRange): Boolean {
        return start <= ano.end && ano.start < end
    }

    fun intersectionWith(ano: TimeRange): TimeRange {
        val maxBegin = max(start, ano.start)
        val minEnd = min(end, ano.end)
        return if (minEnd > maxBegin) {
            TimeRange(maxBegin, minEnd)
        } else {
            TimeRange(maxBegin, maxBegin)
        }
    }

    fun contains(time: Instant): Boolean {
        return start <= time && time < end;
    }

    fun contains(time: TimeRange): Boolean {
        return start <= time.start && time.end <= end;
    }

    operator fun plus(rhs: Duration): TimeRange {
        return TimeRange(start + rhs, end + rhs)
    }

    operator fun minus(rhs: Duration): TimeRange {
        return TimeRange(start - rhs, end - rhs)
    }
}
