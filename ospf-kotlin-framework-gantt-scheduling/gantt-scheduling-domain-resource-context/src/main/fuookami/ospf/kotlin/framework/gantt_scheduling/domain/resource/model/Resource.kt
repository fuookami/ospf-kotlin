@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.SchedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.schedulingSolverValueAdapter
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.framework.model.AbstractShadowPriceMap
import fuookami.ospf.kotlin.framework.model.refresh
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.NoneUnit
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel

/**
 * 将资源 slack 变量范围收敛为 Flt64 表达式范围并设置上限。
 * 资源变量范围由 Flt64 表达式变量构造，运行期类型不变量由资源建模路径保证。
 *
 * Narrows resource slack-variable ranges to Flt64 expression ranges and sets upper bounds.
 * Resource variable ranges are built from Flt64 expression variables; the resource modeling path owns the runtime type invariant.
 */
@Suppress("UNCHECKED_CAST")
private fun setResourceSlackUpperBoundAsFlt64(
    range: ExpressionRange<*>,
    upperBound: Flt64
): Boolean {
    return (range as ExpressionRange<Flt64>).setUb(upperBound)
}

/** 解析资源容量值类型的零值 / Resolve the zero value for the resource capacity value type */
internal fun <V> resourceQuantityZero(capacities: List<AbstractResourceCapacity<V>>): V
        where V : RealNumber<V>, V : NumberField<V> {
    return capacities.asSequence()
        .mapNotNull {
            it.quantityRangeValue.value.lowerBound.value.unwrapOrNull()
                    ?: it.quantityRangeValue.value.upperBound.value.unwrapOrNull()
        }
        .firstOrNull()
        ?.constants
        ?.zero
        ?: throw IllegalArgumentException("resource capacities must contain at least one finite quantity bound.")
}

/**
 * 抽象资源容量接口 / Abstract resource capacity interface
 *
 * @property time 时间范围 / Time range
 * @property quantityRangeValue 数量范围物理量 / Quantity range value
 * @property lessQuantityValue 不足数量物理量 / Less quantity value
 * @property overQuantityValue 超量数量物理量 / Over quantity value
 * @property interval 时间间隔 / Time interval
 * @property name 名称 / Name
 */
interface AbstractResourceCapacity<V> where V : RealNumber<V>, V : NumberField<V> {
    val time: TimeRange
    val quantityRangeValue: ResourceQuantityRange<V>
    val lessQuantityValue: ResourceQuantity<V>? get() = null
    val overQuantityValue: ResourceQuantity<V>? get() = null
    val interval: Duration
    val name: String? get() = null
    val lessEnabled: Boolean get() = lessQuantityValue != null
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
 * @property id 资源ID / Resource ID
 * @property name 资源名称 / Resource name
 * @property capacities 容量列表 / List of capacities
 * @property initialQuantityValue 初始数量裸值 / Initial quantity raw value
 */
abstract class Resource<C, V> : ManualIndexed() where C : AbstractResourceCapacity<V>, V : RealNumber<V>, V : NumberField<V> {
    abstract val id: String
    abstract val name: String
    abstract val capacities: List<C>

    /**
     * 初始数量裸值 / Initial quantity raw value
     *
     * 子类应 override 此属性提供初始数量裸值 / Subclasses should override this property to provide the initial quantity raw value
     */
    abstract val initialQuantityValue: V

    @Deprecated(
        message = "Use initialQuantity(unit) returning Quantity instead",
        replaceWith = ReplaceWith("initialQuantity(NoneUnit).value")
    )
    val initialQuantity: V get() = initialQuantityValue

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
     * 计算使用量 / Calculate used quantity
     *
     * @param T 任务类型 / Task type
     * @param E 执行器类型 / Executor type
     * @param A 分配策略类型 / Assignment policy type
     * @param bunch 任务束 / Task bunch
     * @param time 时间范围 / Time range
     * @return 使用量 / Used quantity
     */
    @Deprecated(
        message = "Use usedQuantityQuantity returning Quantity instead",
        replaceWith = ReplaceWith("usedQuantityQuantity(bunch, time).value")
    )
    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange
    ): V = usedQuantityQuantity(bunch, time).value

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
 */
interface ResourceTimeSlot<
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > : TimeSlot, Indexed where V : RealNumber<V>, V : NumberField<V> {
    val origin: TimeSlot
    val resource: R
    val resourceCapacity: C
    override val time: TimeRange get() = origin.time
    val indexInRule: UInt64

    fun <E : Executor, A : AssignmentPolicy<E>> relatedTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): Boolean {
        val relation = relationTo(prevTask, task)
        return relation neq relation.constants.zero
    }

    fun <E : Executor, A : AssignmentPolicy<E>> relationTo(
        prevTask: AbstractTask<E, A>?,
        task: AbstractTask<E, A>?
    ): V {
        return resource.initialQuantityValue.constants.zero
    }

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
 */
interface ResourceUsage<
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        > where V : RealNumber<V>, V : NumberField<V> {
    val name: String

    val timeSlots: List<S>
    val quantity: LinearIntermediateSymbols1<Flt64>
    val overQuantity: LinearIntermediateSymbols1<Flt64>
    val lessQuantity: LinearIntermediateSymbols1<Flt64>

    val overEnabled: Boolean
    val lessEnabled: Boolean

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
     * 提取影子价格
     * Extract shadow prices from slack variables
     *
     * @param Map              影子价格表类�?
     * @param shadowPriceMap   影子价格�?/ Shadow price map
     * @param shadowPrices     原始影子价格（对偶变量的解）/ Raw shadow prices (dual solution)
     * @return                 成功与否 / Success or failure
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
