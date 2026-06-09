@file:OptIn(kotlin.time.ExperimentalTime::class)

/** Solver 时间窗口边界 / Solver time-window boundary */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/**
 * Solver 时间窗口边界，用于集中把业务时间转为 solver 数值 /
 * Solver time-window boundary that centralizes business-time to solver-value conversion
 *
 * @property source Flt64 时间窗口 / Flt64 time window
 */
class SolverTimeWindowBoundary(
    val source: TimeWindow<Flt64>
) {
    /** 是否连续 / Whether the solver time window is continuous */
    val continues: Boolean get() = source.continues

    /** 窗口持续时间数值 / Solver value of the window duration */
    val durationValue get() = with(source) { duration.value }

    /** 窗口结束时间数值 / Solver value of the window end */
    val endValue get() = with(source) { end.value }

    /**
     * 读取持续时间数值 / Read a duration as a solver value
     *
     * @param duration 持续时间 / Duration
     * @return solver 数值 / Solver value
     */
    fun valueOf(duration: Duration) = with(source) { duration.value }

    /**
     * 读取时间点数值 / Read an instant as a solver value
     *
     * @param instant 时间点 / Instant
     * @return solver 数值 / Solver value
     */
    fun valueOf(instant: Instant) = with(source) { instant.value }

    /**
     * 从 solver 数值读取时间点 / Read an instant from a solver value
     *
     * @param value solver 数值 / Solver value
     * @return 时间点 / Instant
     */
    fun instantOf(value: Flt64) = source.instantOf(value)

    /**
     * 读取向下取整后的时间点数值 / Read a floored instant solver value
     *
     * @param instant 时间点 / Instant
     * @return 向下取整后的 solver 数值 / Floored solver value
     */
    fun flooredValueOf(instant: Instant) = valueOf(instant).floor()

    /**
     * 读取向下取整后的持续时间有符号整数数值 / Read a floored duration as a signed integer solver value
     *
     * @param duration 持续时间 / Duration
     * @return 有符号整数 solver 数值 / Signed integer solver value
     */
    fun signedFlooredValueOf(duration: Duration) = valueOf(duration).floor().toInt64()

    /**
     * 读取向下取整后的时间点无符号整数数值 / Read a floored instant as an unsigned integer solver value
     *
     * @param instant 时间点 / Instant
     * @return 无符号整数 solver 数值 / Unsigned integer solver value
     */
    fun unsignedFlooredValueOf(instant: Instant) = flooredValueOf(instant).toUInt64()

    /**
     * 读取窗口结束到指定时间点的剩余数值 / Read remaining solver value from an instant to the window end
     *
     * @param instant 时间点 / Instant
     * @return 剩余 solver 数值 / Remaining solver value
     */
    fun remainingValueAfter(instant: Instant) = with(source) { (source.end - instant).value }

    /**
     * 读取窗口开始到指定时间点的已过数值 / Read elapsed solver value from the window start to an instant
     *
     * @param instant 时间点 / Instant
     * @return 已过 solver 数值 / Elapsed solver value
     */
    fun elapsedValueBefore(instant: Instant) = with(source) { (instant - source.start).value }

    /**
     * 读取两个时间点之间的距离数值 / Read solver value between two instants
     *
     * @param from 开始时间点 / Start instant
     * @param to 结束时间点 / End instant
     * @return 距离 solver 数值 / Distance solver value
     */
    fun distanceValue(
        from: Instant,
        to: Instant
    ) = with(source) { (to - from).value }

    /**
     * 读取指定时间点加一个窗口持续时间后的数值 / Read solver value after adding one window duration to an instant
     *
     * @param instant 时间点 / Instant
     * @return solver 数值 / Solver value
     */
    fun afterWindowDurationValue(instant: Instant) = with(source) { (instant + source.duration).value }

    /**
     * 读取指定时间点减一个窗口持续时间后的数值 / Read solver value after subtracting one window duration from an instant
     *
     * @param instant 时间点 / Instant
     * @return solver 数值 / Solver value
     */
    fun beforeWindowDurationValue(instant: Instant) = with(source) { (instant - source.duration).value }
}
