@file:Suppress("DEPRECATION")

@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.ProductionAction
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeRange
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.TimeSlot
import fuookami.ospf.kotlin.utils.concept.AutoIndexed
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64

/**
 * 支持 ProductionAction 的资源接�?
 * Resource interface that supports ProductionAction
 *
 * 此接口定义了生产动作与资源消耗之间的关系
 * This interface defines the relationship between production actions and resource consumption
 */
interface CapacityActionResource<out C : AbstractResourceCapacity> {

    /**
     * 计算动作在指定时间范围内的资源消耗（单位操作时间的消耗）
     * Calculate resource consumption per unit operation time
     *
     * @param action 生产动作 / Production action
     * @param time 时间范围 / Time range
     * @return 单位操作时间的资源消�?/ Resource consumption per unit operation time
     */
    fun <A : ProductionAction> usedBy(action: A, time: TimeRange): Flt64
}

/**
 * CapacityActionResource 的时�?
 * Time slot for CapacityActionResource
 */
data class CapacityActionResourceTimeSlot<
        out R,
        out C : AbstractResourceCapacity
        >(
    override val origin: TimeSlot,
    override val resource: R,
    override val resourceCapacity: C,
    override val indexInRule: UInt64
) : ResourceTimeSlot<R, C>, AutoIndexed(CapacityActionResourceTimeSlot::class)
        where R : Resource<C>, R : CapacityActionResource<C> {

    /**
     * 计算动作在此时隙的资源消�?
     * Calculate resource consumption by action in this time slot
     */
    fun <A : ProductionAction> usedBy(action: A): Flt64 = resource.usedBy(action, time)

    override fun subOf(subTime: TimeRange): CapacityActionResourceTimeSlot<R, C>? {
        return origin.subOf(subTime)?.let {
            CapacityActionResourceTimeSlot(
                origin = it,
                resource = resource,
                resourceCapacity = resourceCapacity,
                indexInRule = indexInRule
            )
        }
    }

    override fun toString(): String {
        return "${resource}_${resourceCapacity}_${indexInRule}"
    }
}



