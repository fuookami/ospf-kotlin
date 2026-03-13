package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * 支持 ProductionAction 的资源接口
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
     * @return 单位操作时间的资源消耗 / Resource consumption per unit operation time
     */
    fun <A : ProductionAction> usedBy(action: A, time: TimeRange): Flt64
}

/**
 * CapacityActionResource 的时隙
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
     * 计算动作在此时隙的资源消耗
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
