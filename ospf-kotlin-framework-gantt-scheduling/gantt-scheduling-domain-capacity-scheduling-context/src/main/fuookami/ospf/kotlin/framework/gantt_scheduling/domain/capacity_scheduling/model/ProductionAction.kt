@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model

import kotlin.time.Duration
import kotlin.time.Instant
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow

/** 转换为旧 ProductionAction API 使用的 Flt64 时间窗口 / Convert to the Flt64 time window used by legacy ProductionAction APIs */
internal fun <V : RealNumber<V>> TimeWindow<V>.asFlt64TimeWindow(): TimeWindow<Flt64> {
    return TimeWindow(
        window = window,
        continues = continues,
        durationUnit = durationUnit,
        dateOffset = dateOffset,
        interval = interval,
        fromDouble = { Flt64(it) },
        toDouble = { it.toDouble() }
    )
}

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
    val id: String

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
     * @return Unit capacity as Flt64 / 单位产能
     */
    fun unitCapacity(timeWindow: TimeWindow<Flt64>): Flt64

    /**
     * 单位成本
     * Unit cost
     *
     * @param time Time instant / 时间点
     * @return Unit cost / 单位成本
     */
    fun unitCost(time: Instant): Flt64

    /**
     * x 变量的上界
     * Upper bound for x variable
     *
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗口
     * @return Upper bound value / 上界值
     */
    fun upperBound(slot: TimeSlot, timeWindow: TimeWindow<Flt64>): UInt64

    // ── 泛型 V 补充入口 / Generic V supplementary entries ──
    // 默认实现委托到 Flt64 版本，通过 asFlt64TimeWindow() 隔离。
    // Default implementations delegate to Flt64 versions via asFlt64TimeWindow().
    // 新实现可直接覆盖泛型版本以避免 Flt64 转换。
    // New implementations may override generic versions to avoid Flt64 conversion.

    private fun <V : RealNumber<V>> unitCapacityValue(timeWindow: TimeWindow<V>): V {
        val result = unitCapacity(timeWindow.asFlt64TimeWindow())
        return timeWindow.fromDouble(result.toDouble())
    }

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
        return Quantity(unitCapacityValue(timeWindow), unit)
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
        return Quantity(fromDouble(unitCost(time).toDouble()), unit)
    }

    /**
     * 泛型上界 / Generic upper bound
     *
     * 默认实现：委托到 Flt64 版本（UInt64 不依赖 V）。
     * Default: delegates to Flt64 version (UInt64 is V-independent).
     *
     * @param V 数值类型 / Numeric type
     * @param slot Time slot / 时隙
     * @param timeWindow Time window / 时间窗口
     * @return Upper bound (UInt64, same as Flt64 version) / 上界
     */
    fun <V : RealNumber<V>> upperBoundV(slot: TimeSlot, timeWindow: TimeWindow<V>): UInt64 {
        return upperBound(slot, timeWindow.asFlt64TimeWindow())
    }

    // 注意: unitCost 的 Flt64 版本继续保留作为 solver 边界入口。
    // 调用方可通过 unitCostQuantity + fromDouble 获取 V 类型成本，或通过 SchedulingSolverValueAdapter 转换。
    // Note: unitCost Flt64 version is retained as solver boundary entry.
    // Callers can use unitCostQuantity + fromDouble for V-typed cost, or SchedulingSolverValueAdapter for conversion.
}
