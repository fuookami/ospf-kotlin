@file:OptIn(kotlin.time.ExperimentalTime::class)

/**
 * 时间槽接口，表示具有时间范围的可切片对象 / Time slot interface representing a sliceable object with a time range
 */
package fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure

import kotlin.time.*

/**
 * 时间槽接口，表示具有时间范围的可切片对象 / Time slot interface representing a sliceable object with a time range
 */
interface TimeSlot {
    /** 时间范围 / The time range */
    val time: TimeRange
    /** 开始时间 / Start time */
    val start: Instant get() = time.start
    /** 结束时间 / End time */
    val end: Instant get() = time.end
    /** 持续时间 / Duration */
    val duration: Duration get() = time.duration

    /**
     * 获取在给定时间范围内的子槽 / Get a sub-slot within the given time range
     *
     * @param subTime 子时间范围 / The sub time range
     * @return 子槽，若不相交则为null / The sub-slot, or null if no intersection
     */
    fun subOf(subTime: TimeRange): TimeSlot?
}