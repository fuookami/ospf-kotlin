/**
 * 生产动作接口
 * Production Action Interface
 *
 * 生产动作表示产能的生产方式。
 * A production action represents a way to produce capacity.
 *
 * 分为：
 * Can be:
 * - **离散型**：有固定批次时长，决策变量表示批次数
 * - **Discrete**: Has fixed batch duration, decision variable represents batch count
 * - **连续型**：无固定批次时长，决策变量表示时长单位数
 * - **Continuous**: No fixed batch duration, decision variable represents duration units
*/
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/**
 * 生产动作接口
 * Production Action Interface
 *
 * A production action represents a way to produce capacity.
 * 生产动作表示产能的生产方式。
 *
 * It can be:
 * - **Discrete**: Has fixed batch duration, decision variable represents batch count
 * - **Continuous**: No fixed batch duration, decision variable represents duration units
 *
 * 分为：
 * - **离散型**：有固定批次时长，决策变量表示批次数
 * - **连续型**：无固定批次时长，决策变量表示时长单位数
*/
interface ProductionAction {

    /**
     * 动作唯一标识
     * Unique identifier for the action
    */
    val id: ProductionActionId

    /**
     * 动作名称
     * Name of the action
    */
    val name: String
    val displayName: String get() = name

    /**
     * 执行者
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
     * - true: 离散型，x 表示批次数
     * - false: 连续型，x 表示时长单位数
    */
    val discrete: Boolean

    /**
     * 批次时长（仅离散型有效）
     * Batch duration (only for discrete actions)
    */
    val batchDuration: Duration? get() = null

    /**
     * 每个单位 x 对应的产能
     * Unit capacity per x value
     *
     * @param timeWindow Time window / 时间窗口
     * @return Unit capacity / 单位产能
    */
    fun <V : RealNumber<V>> unitCapacity(timeWindow: TimeWindow<V>): V

    /**
     * 单位成本
     * Unit cost
     *
     * @param time Time instant / 时间点
     * @param fromDouble Double 到 V 的转换函数 / Double to V converter
     * @return Unit cost / 单位成本
    */
    fun <V : RealNumber<V>> unitCost(time: Instant, fromDouble: (Double) -> V): V

    /**
     * x 变量的上界
     * Upper bound for x variable
     *
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗口
     * @return Upper bound value / 上界值
    */
    fun <V : RealNumber<V>> upperBound(slot: TimeSlot, timeWindow: TimeWindow<V>): UInt64

    /**
     * 泛型单位产能物理量 / Generic unit capacity quantity
     *
     * @param V 数值类型 / Numeric type
     * @param timeWindow Time window / 时间窗口
     * @param unit 产能单位 / Capacity unit
     * @return Unit capacity as Quantity<V> / 单位产能物理量
    */
    fun <V : RealNumber<V>> unitCapacityQuantity(
        timeWindow: TimeWindow<V>,
        unit: PhysicalUnit = NoneUnit
    ): CapacityQuantity<V> {
        return Quantity(unitCapacity(timeWindow), unit)
    }

    /**
     * 泛型单位成本物理量 / Generic unit cost quantity
     *
     * @param V 数值类型 / Numeric type
     * @param time Time instant / 时间点
     * @param fromDouble Double 到 V 的转换函数 / Double to V converter
     * @param unit 成本单位 / Cost unit
     * @return Unit cost as Quantity<V> / 单位成本物理量
    */
    fun <V : RealNumber<V>> unitCostQuantity(
        time: Instant,
        fromDouble: (Double) -> V,
        unit: PhysicalUnit = NoneUnit
    ): CapacityCostQuantity<V> {
        return Quantity(unitCost(time, fromDouble), unit)
    }

    /**
     * 求解器单位成本值 / Solver unit cost value
     *
     * @param time Time instant / 时间点
     * @return Unit cost for solver / 求解器单位成本
    */
    fun unitCostSolverValue(time: Instant): Flt64 {
        return unitCost(time) { Flt64(it) }
    }
}
