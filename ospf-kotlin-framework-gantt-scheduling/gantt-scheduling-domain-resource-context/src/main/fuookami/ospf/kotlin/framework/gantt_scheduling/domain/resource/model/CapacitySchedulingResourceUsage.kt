@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbols1
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeWindow
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.multiarray.Shape1
import kotlin.time.Duration
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel

/**
 * 产能调度场景的资源使用量管理抽象基类
 * Abstract base class for resource usage in Capacity Scheduling
 *
 * 提供产能调度场景下资源使用量计算的通用框架
 * Provides a common framework for resource usage calculation in capacity scheduling scenarios
 */
abstract class CapacitySchedulingResourceUsage<
        A : ProductionAction,
        S : ResourceTimeSlot<R, C, V>,
        R : Resource<C, V>,
        C : AbstractResourceCapacity<V>,
        V
        >(
    protected val timeWindow: TimeWindow<V>,
    resources: List<R>,
    protected val actions: List<A>,
    interval: Duration = timeWindow.interval
) : AbstractResourceUsage<S, R, C, V>() where V : RealNumber<V>, V : NumberField<V> {

    override val overEnabled: Boolean = true
    override val lessEnabled: Boolean = true

    abstract override val name: String
    abstract override var quantity: LinearExpressionSymbols1<Flt64>

    /**
     * 注册变量到模型
     * Register variables to model
     */
    abstract fun register(model: LinearMetaModel<Flt64>): Try

    /**
     * 初始化 quantity 变量
     * Initialize quantity variables
     *
     * 子类应在 init 块中先计算 timeSlots，然后调用此方法初始化 quantity
     * Subclasses should calculate timeSlots first in init block, then call this method
     */
    protected fun initQuantity(timeSlots: List<S>) {
        quantity = LinearExpressionSymbols1<Flt64>(
            name = "${name}_quantity",
            shape = Shape1(timeSlots.size)
        ) { s, _ ->
            val slot = timeSlots[s]
            LinearExpressionSymbol(
                Flt64,
                name = "${name}_quantity_${slot}"
            )
        }
        for (slot in timeSlots) {
            quantity[slot].range.set(
                ValueRange(
                    slot.resourceCapacity.quantity.lowerBound.value.unwrap().toFlt64() - (slot.resourceCapacity.lessQuantity?.toFlt64() ?: Flt64.zero),
                    slot.resourceCapacity.quantity.upperBound.value.unwrap().toFlt64() + (slot.resourceCapacity.overQuantity?.toFlt64() ?: Flt64.zero)
                ).value!!
            )
        }
    }

    /**
     * 将 quantity 变量添加到模型
     * Add quantity variables to model
     */
    protected fun addQuantityToModel(model: LinearMetaModel<Flt64>, timeSlots: List<S>): Try {
        if (timeSlots.isNotEmpty()) {
            when (val result = model.add(quantity)) {
                is Ok -> {}
                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}
