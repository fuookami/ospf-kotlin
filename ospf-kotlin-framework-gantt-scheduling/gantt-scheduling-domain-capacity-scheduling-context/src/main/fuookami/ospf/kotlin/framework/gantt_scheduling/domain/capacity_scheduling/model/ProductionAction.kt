@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import kotlinx.datetime.Instant
import kotlin.time.Duration

/**
 * 生产动作接口
 * Production Action Interface
 *
 * A production action represents a way to produce capacity.
 * 生产动作表示产能的生产方式�?
 *
 * It can be:
 * - **Discrete**: Has fixed batch duration, decision variable represents batch count
 * - **Continuous**: No fixed batch duration, decision variable represents duration units
 *
 * 分为�?
 * - **离散�?*：有固定批次时长，决策变量表示批次数
 * - **连续�?*：无固定批次时长，决策变量表示时长单位数
 */
interface ProductionAction {
    /**
     * 动作唯一标识
     * Unique identifier for the action
     */
    val id: String

    /**
     * 动作名称
     * Name of the action
     */
    val name: String
    val displayName: String get() = name

    /**
     * 执行�?
     * The executor that performs this action
     */
    val executor: Executor

    /**
     * 是否为离散型动作
     * Whether the action is discrete
     *
     * - true: discrete, x represents batch count
     * - false: continuous, x represents duration units
     *
     * - true: 离散型，x 表示批次�?
     * - false: 连续型，x 表示时长单位�?
     */
    val discrete: Boolean

    /**
     * 批次时长（仅离散型有效）
     * Batch duration (only for discrete actions)
     */
    val batchDuration: Duration? get() = null

    /**
     * 每个单位 x 对应的产�?
     * Unit capacity per x value
     *
     * @param timeWindow Time window / 时间窗口
     * @return Unit capacity as Flt64 / 单位产能
     */
    fun unitCapacity(timeWindow: TimeWindow): Flt64

    /**
     * 单位成本
     * Unit cost
     *
     * @param time Time instant / 时间�?
     * @return Unit cost / 单位成本
     */
    fun unitCost(time: Instant): Flt64

    /**
     * x 变量的上�?
     * Upper bound for x variable
     *
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗口
     * @return Upper bound value / 上界�?
     */
    fun upperBound(slot: TimeSlot, timeWindow: TimeWindow): UInt64
}


