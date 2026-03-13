package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model

import kotlin.time.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.capacity_scheduling.model.*

/**
 * Bunch 模式的产能调度资源使用量管理（支持列生成）
 * Bunch-mode resource usage for Capacity Scheduling (with column generation)
 *
 * 用于列生成场景，通过 CapacityColumn 追加资源使用量贡献
 * Used for column generation scenarios, adds resource usage contribution through CapacityColumn
 */
class BunchCapacitySchedulingResourceUsage<
    E : Executor,
    A : ProductionAction,
    R,
    C : AbstractResourceCapacity
>(
    timeWindow: TimeWindow,
    resources: List<R>,
    times: List<TimeSlot>,
    actions: List<A>,
    interval: Duration = timeWindow.interval
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C>, R, C>(
    timeWindow, resources, actions, interval
) where R : Resource<C>, R : CapacityActionResource<C> {

    override val name: String = "bunch_capacity_scheduling_resource"
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
    }

    override fun register(model: LinearMetaModel): Try {
        return addQuantityToModel(model, timeSlots)
    }

    /**
     * 从 IterativeCapacityCompilation 添加列贡献
     * Add column contribution from IterativeCapacityCompilation
     *
     * 用于列生成场景，在每次迭代中添加新列的资源使用量贡献
     * Used for column generation, adds resource usage contribution from new columns in each iteration
     *
     * @param iteration 当前迭代 / Current iteration
     * @param columns 产能列列表 / Capacity columns
     * @param compilation 迭代编译对象 / Iterative compilation object
     * @return 成功与否 / Success or failure
     */
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A>>,
        compilation: IterativeCapacityCompilation<A>
    ): Try {
        for (slot in timeSlots) {
            for (column in columns) {
                for ((action, amount) in column.allocations) {
                    val unitUsage = slot.resource.usedBy(action, slot.time)
                    if (unitUsage neq Flt64.zero) {
                        val actionIndex = actions.indexOf(action)
                        if (actionIndex >= 0) {
                            val columnUsage = unitUsage * amount.toFlt64()
                            quantity[slot].asMutable() += columnUsage * compilation.x[actionIndex, column.slotIndex]
                        }
                    }
                }
            }
        }
        return ok
    }
}
