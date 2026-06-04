@file:OptIn(kotlin.time.ExperimentalTime::class)

/** 资源模型 / Resource model */
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.symbol.LinearIntermediateSymbols1
import fuookami.ospf.kotlin.core.model.mechanism.MetaDualSolution
import fuookami.ospf.kotlin.core.model.basic.ExpressionRange
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTaskBunch
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
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
        .mapNotNull { it.quantity.lowerBound.value.unwrapOrNull() ?: it.quantity.upperBound.value.unwrapOrNull() }
        .firstOrNull()
        ?.constants
        ?.zero
        ?: throw IllegalArgumentException("resource capacities must contain at least one finite quantity bound.")
}

/**
 * 抽象资源容量接口 / Abstract resource capacity interface
 *
 * @property time 时间范围 / Time range
 * @property quantity 数量范围 / Quantity range
 * @property lessQuantity 不足数量 / Less quantity
 * @property overQuantity 超量数量 / Over quantity
 * @property interval 时间间隔 / Time interval
 * @property name 名称 / Name
 */
interface AbstractResourceCapacity<V> where V : RealNumber<V>, V : NumberField<V> {
    val time: TimeRange
    val quantity: ValueRange<V>
    val lessQuantity: V? get() = null
    val overQuantity: V? get() = null
    val interval: Duration
    val name: String? get() = null
    val lessEnabled: Boolean get() = lessQuantity != null
    val overEnabled: Boolean get() = overQuantity != null
}

/**
 * 资源容量 / Resource capacity
 *
 * @property time 时间范围 / Time range
 * @property quantity 数量范围 / Quantity range
 * @property lessQuantity 不足数量 / Less quantity
 * @property overQuantity 超量数量 / Over quantity
 * @property interval 时间间隔 / Time interval
 * @property name 名称 / Name
 */
open class ResourceCapacity<V>(
    override val time: TimeRange,
    override val quantity: ValueRange<V>,
    override val lessQuantity: V? = null,
    override val overQuantity: V? = null,
    override val interval: Duration = Duration.INFINITE,
    override val name: String? = null
) : AbstractResourceCapacity<V> where V : RealNumber<V>, V : NumberField<V> {
    override fun toString() = name ?: "${quantity}_${interval}"
}

/**
 * 资源抽象类 / Resource abstract class
 *
 * @param C 资源容量类型 / Resource capacity type
 * @property id 资源ID / Resource ID
 * @property name 资源名称 / Resource name
 * @property capacities 容量列表 / List of capacities
 * @property initialQuantity 初始数量 / Initial quantity
 */
abstract class Resource<C, V> : ManualIndexed() where C : AbstractResourceCapacity<V>, V : RealNumber<V>, V : NumberField<V> {
    abstract val id: String
    abstract val name: String
    abstract val capacities: List<C>
    abstract val initialQuantity: V

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
    abstract fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> usedQuantity(
        bunch: AbstractTaskBunch<T, E, A, V>,
        time: TimeRange
    ): V
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
        return resource.initialQuantity.constants.zero
    }

    fun <T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>> invoke(
        bunch: AbstractTaskBunch<T, E, A, V>
    ): V {
        return resource.usedQuantity(bunch, time)
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
                                threshold = slot.resourceCapacity.quantity.upperBound.value.unwrap().toFlt64(),
                                type = UContinuous,
                                withNegative = false,
                                withPositive = true,
                                constraint = false,
                                name = "${name}_over_quantity_$slot"
                            )
                            slot.resourceCapacity.overQuantity?.let {
                                setResourceSlackUpperBoundAsFlt64(slack.helperVariables.last().range, it.toFlt64())
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
                                threshold = slot.resourceCapacity.quantity.lowerBound.value.unwrap().toFlt64(),
                                type = UContinuous,
                                withNegative = true,
                                withPositive = false,
                                constraint = false,
                                name = "${name}_less_quantity_$slot"
                            )
                            slot.resourceCapacity.lessQuantity?.let {
                                setResourceSlackUpperBoundAsFlt64(slack.helperVariables.first().range, it.toFlt64())
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

