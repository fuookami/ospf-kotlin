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

/** 查找特定飞机的影子价格。Looks up the shadow price for a specific aircraft. */
operator fun ShadowPriceMap.invoke(
    aircraft: Aircraft,
): Flt64 {
    return invoke(TaskShadowPriceArguments(aircraft, null, null))
}

/** 查找特定航班任务的影子价格。Looks up the shadow price for a specific flight task. */
operator fun ShadowPriceMap.invoke(
    task: FlightTask
): Flt64 {
    return invoke(TaskShadowPriceArguments(task.aircraft!!, task, null))
}

/** 查找任务对（前一个和当前）的影子价格。Looks up the shadow price for a task pair (previous and current). */
operator fun ShadowPriceMap.invoke(
    prevTask: FlightTask?,
    task: FlightTask?
): Flt64 {
    if (prevTask == null && task == null) {
        return Flt64.zero
    }

    return invoke(TaskShadowPriceArguments(task?.aircraft ?: prevTask!!.aircraft!!, task, prevTask))
}

/** 使用影子价格计算批次的缩减成本。Computes the reduced cost of a bunch using shadow prices. */
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
