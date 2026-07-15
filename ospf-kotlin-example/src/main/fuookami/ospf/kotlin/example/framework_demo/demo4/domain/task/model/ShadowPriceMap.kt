@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

/** 专门用于飞机和航班任务分配的影子价格参数的类型别名。Type alias for shadow price arguments specialized with aircraft and flight task assignment. */
typealias ShadowPriceArguments = AbstractGanttSchedulingShadowPriceArguments<Aircraft, FlightTaskAssignment>

/** 批次特定影子价格参数的类型别名。Type alias for bunch-specific shadow price arguments. */
typealias TaskShadowPriceArguments = BunchGanttSchedulingShadowPriceArguments<Aircraft, FlightTaskAssignment>

/** 专门用于航班任务类型的影子价格映射的类型别名。Type alias for the shadow price map specialized with flight task types. */
typealias ShadowPriceMap = GanttSchedulingShadowPriceMap<Aircraft, FlightTaskAssignment>

/** Looks up the shadow price for a specific aircraft.
 * 查找特定飞机的影子价格。
 *
 * @param aircraft The aircraft whose shadow price is looked up / 要查找影子价格的飞机
 * @return The shadow price value for the aircraft / 飞机的影子价格值
*/
operator fun ShadowPriceMap.invoke(
    aircraft: Aircraft,
): Flt64 {
    return invoke(TaskShadowPriceArguments(aircraft, null, null))
}

/** Looks up the shadow price for a specific flight task.
 * 查找特定航班任务的影子价格。
 *
 * @param task The flight task whose shadow price is looked up / 要查找影子价格的航班任务
 * @return The shadow price value for the flight task / 航班任务的影子价格值
*/
operator fun ShadowPriceMap.invoke(
    task: FlightTask
): Flt64 {
    return invoke(TaskShadowPriceArguments(task.aircraft!!, task, null))
}

/** Looks up the shadow price for a task pair (previous and current).
 * 查找任务对（前一个和当前）的影子价格。
 *
 * @param prevTask The previous flight task, or null / 前一个航班任务，或 null
 * @param task The current flight task, or null / 当前航班任务，或 null
 * @return The shadow price value for the task pair / 任务对的影子价格值
*/
operator fun ShadowPriceMap.invoke(
    prevTask: FlightTask?,
    task: FlightTask?
): Flt64 {
    if (prevTask == null && task == null) {
        return Flt64.zero
    }

    return invoke(TaskShadowPriceArguments(task?.aircraft ?: prevTask!!.aircraft!!, task, prevTask))
}

/** Computes the reduced cost of a bunch using shadow prices.
 * 使用影子价格计算批次的缩减成本。
 *
 * @param bunch The flight task bunch whose reduced cost is computed / 要计算缩减成本的航班任务束
 * @return The reduced cost value for the bunch / 批次的缩减成本值
*/
fun ShadowPriceMap.reducedCost(
    bunch: FlightTaskBunch
): Flt64 {
    var ret = bunch.cost.costSum!!.value.toFlt64()
    if (bunch.executor.indexed) {
        ret -= this(bunch.executor)
        for ((index, task) in bunch.tasks.withIndex()) {
            val prevTask = if (index != 0) {
                bunch.tasks[index - 1]
            } else {
                bunch.lastTask
            }
            ret -= this(prevTask, task)
        }
        if (bunch.tasks.isNotEmpty()) {
            ret -= this(bunch.tasks.last(), null)
        }
    }
    return ret
}

/** 影子价格提取器的类型别名。Type alias for the shadow price extractor. */
typealias ShadowPriceExtractor = AbstractGanttSchedulingShadowPriceExtractor<ShadowPriceArguments, Aircraft, FlightTaskAssignment>

/** 列生成管线的类型别名。Type alias for a column generation pipeline. */
typealias CGPipeline = AbstractGanttSchedulingCGPipeline<ShadowPriceArguments, Aircraft, FlightTaskAssignment>

/** 列生成管线列表的类型别名。Type alias for a column generation pipeline list. */
typealias CGPipelineList = AbstractGanttSchedulingCGPipelineList<ShadowPriceArguments, Aircraft, FlightTaskAssignment>
