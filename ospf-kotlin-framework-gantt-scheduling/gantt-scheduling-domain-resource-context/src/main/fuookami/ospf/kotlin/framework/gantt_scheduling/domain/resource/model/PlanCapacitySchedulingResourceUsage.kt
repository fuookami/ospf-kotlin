package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * Plan 模式的产能调度资源使用量管理
 * Plan-mode resource usage for Capacity Scheduling
 *
 * 用于非列生成场景，在构造时绑定 Capacity 编译对象
 * Used for non-column generation scenarios, binds to Capacity compilation object at construction
 *
 * @param resources 资源列表 / Resource list
 * @param compilation Capacity 编译对象 / Capacity compilation object
 * @param times 时隙列表 / Time slot list
 * @param actions 生产动作列表 / Production action list
 * @param timeWindow 时间窗口 / Time window
 * @param interval 时间间隔 / Time interval
 */
class PlanCapacitySchedulingResourceUsage<
    A : ProductionAction,
    R : CapacityActionResource<C>,
    C : AbstractResourceCapacity
>(
    resources: List<R>,
    private val compilation: Capacity<A>,
    times: List<TimeSlot>,
    actions: List<A>,
    timeWindow: TimeWindow,
    interval: Duration = timeWindow.interval
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C>, R, C>(
    timeWindow, resources, actions, interval
) {

    override val name: String = "plan_capacity_scheduling_resource"
    override lateinit var quantity: LinearExpressionSymbols1

    final override val timeSlots: List<CapacityActionResourceTimeSlot<R, C>>

    init {
        AutoIndexed.flush<CapacityActionResourceTimeSlot<R, C>>()

        val timeSlots = ArrayList<CapacityActionResourceTimeSlot<R, C>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                if (times.isNotEmpty()) {
                    val thisTimes = times.filter { it.time.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            max(time.start, capacity.time.start),
                            min(time.end, capacity.time.end)
                        )
                        time.subOf(thisTime)?.let {
                            timeSlots.add(
                                CapacityActionResourceTimeSlot(
                                    origin = it,
                                    resource = resource,
                                    resourceCapacity = capacity,
                                    indexInRule = index
                                )
                            )
                            ++index
                        }
                    }
                } else {
                    var beginTime = maxOf(capacity.time.start, timeWindow.window.start)
                    val endTime = minOf(capacity.time.end, timeWindow.window.end)
                    while (beginTime < endTime) {
                        val thisInterval = minOf(endTime - beginTime, capacity.interval, interval)
                        val time = TimeRange(beginTime, beginTime + thisInterval)
                        timeSlots.add(
                            CapacityActionResourceTimeSlot(
                                origin = time,
                                resource = resource,
                                resourceCapacity = capacity,
                                indexInRule = index
                            )
                        )
                        beginTime += thisInterval
                        ++index
                    }
                }
            }
        }
        this.timeSlots = timeSlots

        // 初始化 quantity 变量
        // Initialize quantity variables
        initQuantity(this.timeSlots)

        // 在构造时绑定编译对象
        // Bind compilation object at construction
        for (slot in this.timeSlots) {
            for (action in actions) {
                val unitUsage = slot.resource.usedBy(action, slot.time)
                if (unitUsage neq Flt64.zero) {
                    quantity[slot].asMutable() += unitUsage * compilation.operationTime[action, slot]
                }
            }
        }
    }

    override fun register(model: LinearMetaModel): Try {
        return addQuantityToModel(model, timeSlots)
    }
}