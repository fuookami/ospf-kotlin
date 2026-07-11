/** 产能调度资源使用量抽象基类 / Abstract base class for resource usage in Capacity Scheduling */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 产能调度场景的资源使用量管理抽象基类 / Abstract base class for resource usage in Capacity Scheduling
 *
 * 提供产能调度场景下资源使用量计算的通用框架
 * Provides a common framework for resource usage calculation in capacity scheduling scenarios
 *
 * @param A 生产动作类型 / Production action type
 * @param S 资源时间槽类型 / Resource time slot type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param actions 生产动作列表 / List of production actions
 * @param interval 时间间隔 / Time interval
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
     * 注册变量到线性元模型 / Register variables to the linear meta model
     *
     * @param model 线性元模型 / Linear meta model
     * @return 成功与否 / Success or failure
    */
    abstract fun register(model: LinearMetaModel<Flt64>): Try

    /**
     * 初始化 quantity 变量 / Initialize quantity variables
     *
     * 子类应在 init 块中先计算 timeSlots，然后调用此方法初始化 quantity
     * Subclasses should calculate timeSlots first in init block, then call this method
     *
     * @param timeSlots 时间槽列表 / List of time slots
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
            quantity[slot].range.set(slot.resourceCapacity.solverValueRange())
        }
    }

    /**
     * 将 quantity 变量添加到模型 / Add quantity variables to the model
     *
     * @param model 线性元模型 / Linear meta model
     * @param timeSlots 时间槽列表 / List of time slots
     * @return 成功与否 / Success or failure
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
