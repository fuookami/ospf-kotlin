package fuookami.ospf.kotlin.utils.functional

import kotlinx.datetime.*

class LocalDateClosedRangeIterator(
    private var current: LocalDate,
    private val range: ClosedRange<LocalDate>,
    private val step: DatePeriod
) : Iterator<LocalDate> {
    override fun hasNext(): Boolean {
        return current <= range.endInclusive
    }

    override fun next(): LocalDate {
        val next = current
        current += step
        return next
    }
}

fun ClosedRange<LocalDate>.iterator(step: DatePeriod): Iterator<LocalDate> {
    return LocalDateClosedRangeIterator(
        this.start,
        this,
        step
    )
}

class LocalDateOpenEndRangeIterator(
    private var current: LocalDate,
    val range: OpenEndRange<LocalDate>,
    val step: DatePeriod
) : Iterator<LocalDate> {
    override fun hasNext(): Boolean {
        return current < range.endExclusive
    }

    override fun next(): LocalDate {
        val next = current
        current += step
        return next
    }
}

fun OpenEndRange<LocalDate>.iterator(step: DatePeriod): Iterator<LocalDate> {
    return LocalDateOpenEndRangeIterator(
        this.start,
        this,
        step
    )
}
