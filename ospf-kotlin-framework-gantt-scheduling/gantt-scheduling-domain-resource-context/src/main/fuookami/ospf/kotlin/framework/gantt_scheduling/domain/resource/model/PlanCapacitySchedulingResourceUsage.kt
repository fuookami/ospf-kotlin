/** Plan 模式产能调度资源使用量管理（非列生成场景） / Plan-mode resource usage for Capacity Scheduling (non-column generation scenarios) */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.Duration
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * Plan 模式的产能调度资源使用量管理 / Plan-mode resource usage for Capacity Scheduling
 *
 * 用于非列生成场景，在构造时绑定 Capacity 编译对象
 * Used for non-column generation scenarios, binds to Capacity compilation object at construction
 *
 * @param A 生产动作类型 / Production action type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param resources 资源列表 / Resource list
 * @param compilation Capacity 编译对象 / Capacity compilation object
 * @param times 时隙列表 / Time slot list
 * @param actions 生产动作列表 / Production action list
 * @param timeWindow 时间窗口 / Time window
 * @param interval 时间间隔 / Time interval
 */
class PlanCapacitySchedulingResourceUsage<
        A : ProductionAction,
        R,
        C : AbstractResourceCapacity<V>,
        V
        >(
    resources: List<R>,
    private val compilation: Capacity<A>,
    times: List<TimeSlot>,
    actions: List<A>,
    timeWindow: TimeWindow<V>,
    interval: Duration = timeWindow.interval
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C, V>, R, C, V>(
    timeWindow, resources, actions, interval
) where R : Resource<C, V>, R : CapacityActionResource<C, V>, V : RealNumber<V>, V : NumberField<V> {
    private val capacitySlots: List<TimeSlot> = times

    override val name: String = "plan_capacity_scheduling_resource"
    override lateinit var quantity: LinearExpressionSymbols1<Flt64>

    final override val timeSlots: List<CapacityActionResourceTimeSlot<R, C, V>>

    init {
        AutoIndexed.flush<CapacityActionResourceTimeSlot<R, C, V>>()

        val timeSlots = ArrayList<CapacityActionResourceTimeSlot<R, C, V>>()
        for (resource in resources) {
            for (capacity in resource.capacities) {
                var index = UInt64.zero
                if (capacitySlots.isNotEmpty()) {
                    val thisTimes = capacitySlots.filter { it.time.withIntersection(capacity.time) }
                    for (time in thisTimes) {
                        val thisTime = TimeRange(
                            maxOf(time.start, capacity.time.start),
                            minOf(time.end, capacity.time.end)
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
                if (unitUsage neq unitUsage.constants.zero) {
                    val actionIndex = actions.indexOf(action)
                    val slotIndex = resolveCapacitySlotIndex(slot)
                    if (actionIndex >= 0 && slotIndex >= 0 && slotIndex < compilation.operationTime.shape[1]) {
                        quantity[slot].asMutable() += LinearMonomial(unitUsage.toSolverValue(), compilation.operationTime[actionIndex, slotIndex])
                    }
                }
            }
        }
    }

    /**
     * 注册变量到线性元模型 / Register variables to the linear meta model
     *
     * @param model 线性元模型 / Linear meta model
     * @return 成功与否 / Success or failure
     */
    override fun register(model: LinearMetaModel<Flt64>): Try {
        return addQuantityToModel(model, timeSlots)
    }

    /**
     * 解析产能时间槽索引 / Resolve the capacity slot index for a given time slot
     *
     * @param slot 资源时间槽 / Resource time slot
     * @return 产能时间槽索引 / Capacity slot index
     */
    private fun resolveCapacitySlotIndex(slot: CapacityActionResourceTimeSlot<R, C, V>): Int {
        if (capacitySlots.isNotEmpty()) {
            return capacitySlots.indexOfFirst { it.time.contains(slot.time) }
        }
        return slot.indexInRule.toInt()
    }
}
