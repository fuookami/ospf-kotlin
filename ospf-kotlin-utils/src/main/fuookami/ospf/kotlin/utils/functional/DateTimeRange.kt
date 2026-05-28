/**
 * 日期范围迭代器
 *
 * Iterator utilities for LocalDate ranges with configurable step periods.
 * LocalDate 范围的迭代器工具，支持可配置的步进周期。
 */
package fuookami.ospf.kotlin.utils.functional

import kotlinx.datetime.*

/**
 * 闭区间日期迭代器
 *
 * Iterator for closed ranges of LocalDate with a specified step period.
 * LocalDate 闭区间的迭代器，支持指定的步进周期。
 *
 * @param current 当前日期位置 / The current date position
 * @param range 日期范围 / The date range
 * @param step 步进周期 / The step period
 */
class LocalDateClosedRangeIterator(
    private var current: LocalDate,
    private val range: ClosedRange<LocalDate>,
    private val step: DatePeriod
) : Iterator<LocalDate> {
    /**
     * 检查是否有下一个日期
     *
     * Checks if there is another date within the range.
     * 检查范围内是否有下一个日期。
     *
     * @return 如果当前日期小于等于结束日期则返回 true / Returns true if current date is less than or equal to end date
     */
    override fun hasNext(): Boolean {
        return current <= range.endInclusive
    }

    /**
     * 获取下一个日期并推进迭代器
     *
     * Returns the next date and advances the iterator by the step period.
     * 返回下一个日期并按步进周期推进迭代器。
     *
     * @return 当前日期（推进前的位置） / The current date (before advancing)
     */
    override fun next(): LocalDate {
        val next = current
        current += step
        return next
    }
}

/**
 * 创建闭区间日期范围的迭代器
 *
 * Creates an iterator for a closed range of LocalDate with the specified step period.
 * 为 LocalDate 闭区间创建指定步进周期的迭代器。
 *
 * @param range 日期闭区间 / The date closed range
 * @param step 步进周期 / The step period
 * @return LocalDate 闭区间迭代器 / A LocalDate closed range iterator
 */
fun ClosedRange<LocalDate>.iterator(step: DatePeriod): Iterator<LocalDate> {
    return LocalDateClosedRangeIterator(
        this.start,
        this,
        step
    )
}

/**
 * 开区间日期迭代器
 *
 * Iterator for open-ended ranges of LocalDate with a specified step period.
 * LocalDate 开区间的迭代器，支持指定的步进周期。
 *
 * @param current 当前日期位置 / The current date position
 * @param range 日期范围 / The date range
 * @param step 步进周期 / The step period
 */
class LocalDateOpenEndRangeIterator(
    private var current: LocalDate,
    val range: OpenEndRange<LocalDate>,
    val step: DatePeriod
) : Iterator<LocalDate> {
    /**
     * 检查是否有下一个日期
     *
     * Checks if there is another date within the range (exclusive end).
     * 检查范围内是否有下一个日期（不包含结束日期）。
     *
     * @return 如果当前日期小于结束日期则返回 true / Returns true if current date is less than the end date
     */
    override fun hasNext(): Boolean {
        return current < range.endExclusive
    }

    /**
     * 获取下一个日期并推进迭代器
     *
     * Returns the next date and advances the iterator by the step period.
     * 返回下一个日期并按步进周期推进迭代器。
     *
     * @return 当前日期（推进前的位置） / The current date (before advancing)
     */
    override fun next(): LocalDate {
        val next = current
        current += step
        return next
    }
}

/**
 * 创建开区间日期范围的迭代器
 *
 * Creates an iterator for an open-ended range of LocalDate with the specified step period.
 * 为 LocalDate 开区间创建指定步进周期的迭代器。
 *
 * @param range 日期开区间 / The date open-ended range
 * @param step 步进周期 / The step period
 * @return LocalDate 开区间迭代器 / A LocalDate open-ended range iterator
 */
fun OpenEndRange<LocalDate>.iterator(step: DatePeriod): Iterator<LocalDate> {
    return LocalDateOpenEndRangeIterator(
        this.start,
        this,
        step
    )
}
