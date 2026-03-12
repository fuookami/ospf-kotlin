package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.*
import kotlinx.datetime.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*

/**
 * 生产动作接口
 * Production Action Interface
 *
 * A production action represents a way to produce capacity.
 * It can be discrete (fixed batch duration) or continuous (variable duration).
 *
 * 生产动作表示产能的生产方式，分为离散型和连续型。
 */
interface ProductionAction : Indexed {
    /**
     * 动作唯一标识
     * Unique identifier of the action
     */
    val id: String

    /**
     * 动作名称
     * Name of the action
     */
    val name: String

    /**
     * 执行该动作的设备
     * Executor that performs this action
     */
    val executor: Executor

    /**
     * 是否为离散型动作
     * Whether the action is discrete
     *
     * - true: discrete, x represents batch count
     * - false: continuous, x represents duration units
     *
     * - true: 离散型，x 表示批次数
     * - false: 连续型，x 表示时长单位数
     */
    val discrete: Boolean

    /**
     * 批次时长（仅离散型有效）
     * Batch duration (only for discrete actions)
     */
    val batchDuration: Duration?
        get() = null

    /**
     * 每个单位 x 对应的产能（时长）
     * Unit capacity (duration) per x value
     *
     * @param timeWindow Time window for calculation / 计算所用的时间窗
     * @return Duration per unit x / 每单位 x 对应的时长
     */
    fun unitCapacity(timeWindow: TimeWindow): Duration

    /**
     * 单位负荷/成本
     * Unit load/cost
     *
     * @param timeRange Time range for calculation / 计算所用的时间范围
     * @return Cost per unit / 每单位的成本
     */
    fun unitCost(timeRange: TimeRange): Flt64

    /**
     * x 变量的上界
     * Upper bound for x variable
     *
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗
     * @return Maximum value for x / x 的最大值
     */
    fun upperBound(slot: TimeSlot, timeWindow: TimeWindow): UInt64

    /**
     * 判断是否可用于指定时隙
     * Check if the action is available for the given slot
     *
     * @param slot Time slot / 时隙
     * @return Whether the action is available / 是否可用
     */
    fun isAvailable(slot: TimeSlot): Boolean = true
}