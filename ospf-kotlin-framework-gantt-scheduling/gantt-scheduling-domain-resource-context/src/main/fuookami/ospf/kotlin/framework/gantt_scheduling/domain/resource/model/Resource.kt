/** 资源领域核心建模：容量、使用、影子价格 / Resource domain core modeling: capacity, usage, shadow price */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 将资源 slack 变量范围收敛为 Flt64 表达式范围并设置上限 / Narrows resource slack-variable ranges to Flt64 expression ranges and sets upper bounds
 *
 * 资源变量范围由 Flt64 表达式变量构造，运行期类型不变量由资源建模路径保证。
 * Resource variable ranges are built from Flt64 expression variables; the resource modeling path owns the runtime type invariant.
 *
 * @param range 表达式范围 / Expression range
 * @param upperBound 上界 / Upper bound
 * @return 是否设置成功 / Whether the upper bound was set
*/
@Suppress("UNCHECKED_CAST")
private fun setResourceSlackUpperBoundAsFlt64(
    range: ExpressionRange<*>,
    upperBound: Flt64
): Boolean {
    return (range as ExpressionRange<Flt64>).setUb(upperBound)
}

/** 解析资源容量值类型的零值（nullable） / Resolve the zero value for the resource capacity value type (nullable) */
internal fun <V> resourceQuantityZeroOrNull(capacities: List<AbstractResourceCapacity<V>>): V?
        where V : RealNumber<V>, V : NumberField<V> {
    return capacities.asSequence()
        .mapNotNull {
            it.quantityRangeValue.value.lowerBound.value.unwrapOrNull()
                    ?: it.quantityRangeValue.value.upperBound.value.unwrapOrNull()
        }
        .firstOrNull()
        ?.constants
        ?.zero
}

/** 解析资源容量值类型的零值 / Resolve the zero value for the resource capacity value type */
internal fun <V> resourceQuantityZero(capacities: List<AbstractResourceCapacity<V>>): Ret<V>
        where V : RealNumber<V>, V : NumberField<V> {
    val zero = resourceQuantityZeroOrNull(capacities)
    return if (zero != null) {
        Ok(zero)
    } else {
        Failed(ErrorCode.IllegalArgument, "resource capacities must contain at least one finite quantity bound.")
    }
}

/**
 * 抽象资源容量接口 / Abstract resource capacity interface
 *
 * @param V 值类型 / Value type
 * @property time 时间范围 / Time range
 * @property quantityRangeValue 数量范围物理量 / Quantity range value
 * @property lessQuantityValue 不足数量物理量 / Less quantity value
 * @property overQuantityValue 超量数量物理量 / Over quantity value
 * @property interval 时间间隔 / Time interval
 * @property name 名称 / Name
 * @property lessEnabled 是否启用不足 / Whether less quantity is enabled
 * @property overEnabled 是否启用超量 / Whether over quantity is enabled
*/
interface AbstractResourceCapacity<V> where V : RealNumber<V>, V : NumberField<V> {

    /** Time range for this capacity / 此容量的时间范围 */
    val time: TimeRange

    /** Quantity range value / 数量范围物理量 */
    val quantityRangeValue: ResourceQuantityRange<V>

    /** Less quantity value / 不足数量物理量 */
    val lessQuantityValue: ResourceQuantity<V>? get() = null

    /** Over quantity value / 超量数量物理量 */
    val overQuantityValue: ResourceQuantity<V>? get() = null

    /** Time interval / 时间间隔 */
    val interval: Duration
    val name: String? get() = null

    /** Whether less quantity is enabled / 是否启用不足量 */
    val lessEnabled: Boolean get() = lessQuantityValue != null

    /** Whether over quantity is enabled / 是否启用超量 */
    val overEnabled: Boolean get() = overQuantityValue != null

    /**
     * 数量范围物理量 / Quantity range as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 数量范围物理量 / Quantity range quantity
    */
    fun quantityRange(unit: PhysicalUnit = NoneUnit): ResourceQuantityRange<V> {
        return Quantity(quantityRangeValue.value, unit)
    }

    /**
     * 不足数量物理量 / Less quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 不足数量物理量 / Less quantity
    */
    fun lessQuantity(unit: PhysicalUnit = NoneUnit): ResourceQuantity<V>? {
        return lessQuantityValue?.let { Quantity(it.value, unit) }
    }

    /**
     * 超量数量物理量 / Over quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 超量数量物理量 / Over quantity
    */
    fun overQuantity(unit: PhysicalUnit = NoneUnit): ResourceQuantity<V>? {
        return overQuantityValue?.let { Quantity(it.value, unit) }
    }
}

/**
 * 资源容量 / Resource capacity
 *
 * @param V 值类型 / Value type
 * @property time 时间范围 / Time range
 * @property quantityRangeValue 数量范围物理量 / Quantity range value
 * @property lessQuantityValue 不足数量物理量 / Less quantity value
 * @property overQuantityValue 超量数量物理量 / Over quantity value
 * @property interval 时间间隔 / Time interval
 * @property name 名称 / Name
*/
open class ResourceCapacity<V>(
    override val time: TimeRange,
    override val quantityRangeValue: ResourceQuantityRange<V>,
    override val lessQuantityValue: ResourceQuantity<V>? = null,
    override val overQuantityValue: ResourceQuantity<V>? = null,
    override val interval: Duration = Duration.INFINITE,
    override val name: String? = null
) : AbstractResourceCapacity<V> where V : RealNumber<V>, V : NumberField<V> {
    override fun toString() = name ?: "${quantityRangeValue.value}_${interval}"
}

/**
 * 资源抽象类 / Resource abstract class
 *
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @property id 资源ID / Resource ID
 * @property name 资源名称 / Resource name
 * @property capacities 容量列表 / List of capacities
 * @property initialQuantityValue 初始数量裸值 / Initial quantity raw value
*/
abstract class Resource<C, V> : ManualIndexed() where C : AbstractResourceCapacity<V>, V : RealNumber<V>, V : NumberField<V> {
    abstract val id: ResourceId
    abstract val name: String
    abstract val capacities: List<C>

    /**
     * 初始数量裸值 / Initial quantity raw value
     *
     * 子类应 override 此属性提供初始数量裸值 / Subclasses should override this property to provide the initial quantity raw value
    */
    abstract val initialQuantityValue: V

    /**
     * 计算使用量物理量 / Calculate used quantity as a physical quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param bunch 任务束 / Task bunch
     * @param time 时间范围 / Time range
     * @param unit 数量单位 / Quantity unit
     * @return 使用量物理量 / Used quantity
    */
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantityQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange,
        unit: PhysicalUnit = NoneUnit
    ): ResourceQuantity<V>

    /**
     * 初始数量物理量 / Initial quantity as a physical quantity
     *
     * @param unit 数量单位 / Quantity unit
     * @return 初始数量物理量 / Initial quantity
    */
    fun initialQuantity(unit: PhysicalUnit = NoneUnit): ResourceQuantity<V> {
        return Quantity(initialQuantityValue, unit)
    }
}

/**
 * 资源时间槽接口 / Resource time slot interface
 *
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @property origin 原始时间槽 / Origin time slot
 * @property resource 资源 / Resource
 * @property resourceCapacity 资源容量 / Resource capacity
 * @property indexInRule 规则内索引 / Index in rule
*/
interface ResourceTimeSlot<
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > : TimeSlot, Indexed where V : RealNumber<V>, V : NumberField<V> {

    /** Origin time slot / 原始时间槽 */
    val origin: TimeSlot

    /** Associated resource / 关联的资源 */
    val resource: R

    /** Resource capacity for this slot / 此时槽对应的资源容量 */
    val resourceCapacity: C
    override val time: TimeRange get() = origin.time

    /** Index within the rule / 规则内索引 */
    val indexInRule: UInt64

    /**
     * 判断任务是否与此时槽相关 / Check whether tasks are related to this time slot
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param prevTask 前驱任务 / Previous task
     * @param task 当前任务 / Current task
     * @return 是否相关 / Whether related
    */
    fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        val relation = relationTo(prevTask, task)
        return relation neq relation.constants.zero
    }

    /**
     * 计算任务与此时槽的关联量 / Calculate the relation quantity between tasks and this time slot
     *
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param prevTask 前驱任务 / Previous task
     * @param task 当前任务 / Current task
     * @return 关联量 / Relation quantity
    */
    fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return resource.initialQuantityValue.constants.zero
    }

    /**
     * 计算任务束在此时槽的资源使用量 / Calculate resource usage of a task bunch at this time slot
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param bunch 任务束 / Task bunch
     * @return 资源使用量裸值 / Resource usage raw value
    */
    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(
        bunch: AbstractTaskBunch<T, E, A, V>
    ): V {
        return resource.usedQuantityQuantity(bunch, time).value
    }
}

/**
 * 资源使用接口 / Resource usage interface
 *
 * @param S 资源时间槽类型 / Resource time slot type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @property name 名称 / Name
 * @property timeSlots 时间槽列表 / List of time slots
 * @property quantity 数量变量 / Quantity variables
 * @property overQuantity 超量变量 / Over quantity variables
 * @property lessQuantity 不足量变量 / Less quantity variables
 * @property overEnabled 是否启用超量 / Whether over quantity is enabled
 * @property lessEnabled 是否启用不足 / Whether less quantity is enabled
*/
interface ResourceUsage<
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > where V : RealNumber<V>, V : NumberField<V> {
    val name: String

    /** List of time slots / 时间槽列表 */
    val timeSlots: List<S>

    /** Quantity variables / 数量变量 */
    val quantity: LinearIntermediateSymbols1<Flt64>

    /** Over quantity variables / 超量变量 */
    val overQuantity: LinearIntermediateSymbols1<Flt64>

    /** Less quantity variables / 不足量变量 */
    val lessQuantity: LinearIntermediateSymbols1<Flt64>

    /** Whether over quantity is enabled / 是否启用超量 */
    val overEnabled: Boolean

    /** Whether less quantity is enabled / 是否启用不足量 */
    val lessEnabled: Boolean

    /**
     * 注册松弛变量到元模型 / Register slack variables to the meta model
     *
     * @param model 元模型 / Meta model
     * @return 成功与否 / Success or failure
    */
    fun register(model: MetaModel<Flt64>): Try

    /**
     * 读取已求解资源使用量物理量 / Read solved resource usage quantity
     *
     * @param W 目标数值类型 / Target numeric type
     * @param slot 资源时间槽 / Resource time slot
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 数量单位 / Quantity unit
     * @return 资源使用量物理量 / Resource usage quantity
    */
    fun <W : RealNumber<W>> solvedQuantity(
        slot: S,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<W>,
        unit: PhysicalUnit = NoneUnit
    ): ResourceQuantity<W>? {
        return quantity[slot].resourceQuantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取已求解资源超量物理量 / Read solved resource over-quantity
     *
     * @param W 目标数值类型 / Target numeric type
     * @param slot 资源时间槽 / Resource time slot
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 数量单位 / Quantity unit
     * @return 资源超量物理量 / Resource over-quantity
    */
    fun <W : RealNumber<W>> solvedOverQuantity(
        slot: S,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<W>,
        unit: PhysicalUnit = NoneUnit
    ): ResourceQuantity<W>? {
        return overQuantity[slot].resourceQuantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }

    /**
     * 读取已求解资源不足量物理量 / Read solved resource less-quantity
     *
     * @param W 目标数值类型 / Target numeric type
     * @param slot 资源时间槽 / Resource time slot
     * @param model 元模型 / Meta model
     * @param adapter solver 数值适配器 / Solver value adapter
     * @param unit 数量单位 / Quantity unit
     * @return 资源不足量物理量 / Resource less-quantity
    */
    fun <W : RealNumber<W>> solvedLessQuantity(
        slot: S,
        model: MetaModel<Flt64>,
        adapter: SchedulingSolverValueAdapter<W>,
        unit: PhysicalUnit = NoneUnit
    ): ResourceQuantity<W>? {
        return lessQuantity[slot].resourceQuantityOf(
            model = model,
            adapter = adapter,
            unit = unit
        )
    }
}

/**
 * 从线性中间符号求解资源数量物理量 / Solve resource quantity from a linear intermediate symbol
 *
 * @param V 目标数值类型 / Target numeric type
 * @param model 元模型 / Meta model
 * @param adapter solver 数值适配器 / Solver value adapter
 * @param unit 数量单位 / Quantity unit
 * @return 资源数量物理量，求解失败时返回 null / Resource quantity, or null if evaluation fails
*/
private fun <V : RealNumber<V>> LinearIntermediateSymbol<Flt64>.resourceQuantityOf(
    model: MetaModel<Flt64>,
    adapter: SchedulingSolverValueAdapter<V>,
    unit: PhysicalUnit
): ResourceQuantity<V>? {
    val value = (this as IntermediateSymbol<Flt64>).evaluate(
        tokenTable = model.tokens,
        converter = schedulingSolverValueAdapter,
        zeroIfNone = true
    ) ?: toLinearPolynomial().constant
    return Quantity(adapter.intoValue(value), unit)
}

/**
 * 抽象资源使用 / Abstract resource usage
 *
 * @param S 资源时间槽类型 / Resource time slot type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
*/
abstract class AbstractResourceUsage<
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > : ResourceUsage<S, R, C, V> where V : RealNumber<V>, V : NumberField<V> {
    override lateinit var overQuantity: LinearIntermediateSymbols1<Flt64>
    override lateinit var lessQuantity: LinearIntermediateSymbols1<Flt64>

    override fun register(model: MetaModel<Flt64>): Try {
        if (timeSlots.isNotEmpty()) {
            if (overEnabled) {
                if (!::overQuantity.isInitialized) {
                    overQuantity = LinearIntermediateSymbols1<Flt64>(
                        name = "${name}_over_quantity",
                        shape = Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.overEnabled) {
                            val slack = resourceSlack(
                                x = quantity[slot],
                                threshold = slot.resourceCapacity.solverUpperBound(),
                                type = UContinuous,
                                withNegative = false,
                                withPositive = true,
                                constraint = false,
                                name = "${name}_over_quantity_$slot"
                            )
                            slot.resourceCapacity.overQuantityValue?.let {
                                setResourceSlackUpperBoundAsFlt64(
                                    range = slack.helperVariables.last().range,
                                    upperBound = slot.resourceCapacity.solverOverQuantity()
                                )
                            }
                            slack
                        } else {
                            LinearIntermediateSymbol.empty(
                                Flt64,
                                name = "${name}_over_quantity_$slot"
                            )
                        }
                    }
                }
                when (val result = model.add(overQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }

            if (lessEnabled) {
                if (!::lessQuantity.isInitialized) {
                    lessQuantity = LinearIntermediateSymbols1<Flt64>(
                        name = "${name}_less_quantity",
                        shape = Shape1(timeSlots.size)
                    ) { i, _ ->
                        val slot = timeSlots[i]
                        if (slot.resourceCapacity.lessEnabled) {
                            val slack = resourceSlack(
                                x = quantity[slot],
                                threshold = slot.resourceCapacity.solverLowerBound(),
                                type = UContinuous,
                                withNegative = true,
                                withPositive = false,
                                constraint = false,
                                name = "${name}_less_quantity_$slot"
                            )
                            slot.resourceCapacity.lessQuantityValue?.let {
                                setResourceSlackUpperBoundAsFlt64(
                                    range = slack.helperVariables.first().range,
                                    upperBound = slot.resourceCapacity.solverLessQuantity()
                                )
                            }
                            slack
                        } else {
                            LinearIntermediateSymbol.empty(
                                Flt64,
                                name = "${name}_less_quantity_$slot"
                            )
                        }
                    }
                }
                when (val result = model.add(lessQuantity)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }

    /**
     * 提取影子价格 / Extract shadow prices from slack variables
     *
     * @param Map 影子价格表类型 / Shadow price map type
     * @param shadowPriceMap 影子价格表 / Shadow price map
     * @param shadowPrices 原始影子价格（对偶变量的解）/ Raw shadow prices (dual solution)
     * @return 成功与否 / Success or failure
    */
    fun <Map : AbstractShadowPriceMap<*, Map>> refresh(
        shadowPriceMap: Map,
        shadowPrices: MetaDualSolution
    ): Try {
        if (::overQuantity.isInitialized) {
            for (overQuantity in this.overQuantity) {
                when (val result = overQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        if (::lessQuantity.isInitialized) {
            for (lessQuantity in this.lessQuantity) {
                when (val result = lessQuantity.refresh(
                    shadowPriceMap = shadowPriceMap,
                    shadowPrices = shadowPrices
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
                    }
                }
            }
        }

        return ok
    }
}
