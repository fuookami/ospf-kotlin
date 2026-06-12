/** Bunch 模式产能调度资源使用量管理（支持列生成） / Bunch-mode resource usage for Capacity Scheduling (with column generation) */
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
 * Bunch 模式的产能调度资源使用量管理（支持列生成）/ Bunch-mode resource usage for Capacity Scheduling (with column generation)
 *
 * 用于列生成场景，通过 CapacityColumn 追加资源使用量贡献
 * Used for column generation scenarios, adds resource usage contribution through CapacityColumn
 *
 * @param E 执行器类型 / Executor type
 * @param A 生产动作类型 / Production action type
 * @param R 资源类型 / Resource type
 * @param C 资源容量类型 / Resource capacity type
 * @param V 值类型 / Value type
 * @param timeWindow 时间窗口 / Time window
 * @param resources 资源列表 / List of resources
 * @param times 时间槽列表 / List of time slots
 * @param actions 生产动作列表 / List of production actions
 * @param interval 时间间隔 / Time interval
 */
class BunchCapacitySchedulingResourceUsage<
        E : Executor,
        A : ProductionAction,
        R,
        C : AbstractResourceCapacity<V>,
        V
        >(
    timeWindow: TimeWindow<V>,
    resources: List<R>,
    times: List<TimeSlot>,
    actions: List<A>,
    interval: Duration = timeWindow.interval
) : CapacitySchedulingResourceUsage<A, CapacityActionResourceTimeSlot<R, C, V>, R, C, V>(
    timeWindow, resources, actions, interval
) where R : Resource<C, V>, R : CapacityActionResource<C, V>, V : RealNumber<V>, V : NumberField<V> {
    private val capacitySlots: List<TimeSlot> = times

    override val name: String = "bunch_capacity_scheduling_resource"
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
     * 从 IterativeCapacityCompilation 添加列贡献 / Add column contribution from IterativeCapacityCompilation
     *
     * 用于列生成场景，在每次迭代中添加新列的资源使用量贡献
     * Used for column generation, adds resource usage contribution from new columns in each iteration
     *
     * @param iteration 当前迭代 / Current iteration
     * @param columns 产能列列表 / Capacity columns
     * @param compilation 迭代编译对象 / Iterative compilation object
     * @return 成功与否 / Success or failure
     */
    @Suppress("UNUSED_PARAMETER")
    suspend fun addColumns(
        iteration: UInt64,
        columns: List<CapacityColumn<E, A, V>>,
        compilation: IterativeCapacityCompilation<V, E, A>
    ): Try {
        // Rebuild from operationTime to keep consistency when iterative x variables are reshaped.
        // 基于 operationTime 重建，避免迭代扩容后 x 变量重建导致的表达式引用失配。
        for (slot in timeSlots) {
            quantity[slot].asMutable().let {
                it.clear()
                it.setConstant(Flt64.zero)
            }
            val slotIndex = resolveCapacitySlotIndex(slot)
            if (slotIndex < 0 || slotIndex >= compilation.operationTime.shape[1]) {
                continue
            }
            for ((actionIndex, action) in actions.withIndex()) {
                val unitUsage = slot.resource.usedBy(action, slot.time)
                if (unitUsage neq unitUsage.constants.zero) {
                    quantity[slot].asMutable() += LinearMonomial(unitUsage.toSolverValue(), compilation.operationTime[actionIndex, slotIndex])
                }
            }
        }
        return ok
    }

    private fun resolveCapacitySlotIndex(slot: CapacityActionResourceTimeSlot<R, C, V>): Int {
        if (capacitySlots.isNotEmpty()) {
            return capacitySlots.indexOfFirst { it.time.contains(slot.time) }
        }
        return slot.indexInRule.toInt()
    }
}
