@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

typealias ShadowPriceArguments = AbstractGanttSchedulingShadowPriceArguments<Aircraft, FlightTaskAssignment>
typealias TaskShadowPriceArguments = BunchGanttSchedulingShadowPriceArguments<Aircraft, FlightTaskAssignment>
typealias ShadowPriceMap = GanttSchedulingShadowPriceMap<Aircraft, FlightTaskAssignment>

operator fun ShadowPriceMap.invoke(
    aircraft: Aircraft,
): Flt64 {
    return invoke(TaskShadowPriceArguments(aircraft, null, null))
}

operator fun ShadowPriceMap.invoke(
    task: FlightTask
): Flt64 {
    return invoke(TaskShadowPriceArguments(task.aircraft!!, task, null))
}

operator fun ShadowPriceMap.invoke(
    prevTask: FlightTask?,
    task: FlightTask?
): Flt64 {
    if (prevTask == null && task == null) {
        return Flt64.zero
    }

    return invoke(TaskShadowPriceArguments(task?.aircraft ?: prevTask!!.aircraft!!, task, prevTask))
}

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

typealias ShadowPriceExtractor = AbstractGanttSchedulingShadowPriceExtractor<ShadowPriceArguments, Aircraft, FlightTaskAssignment>
typealias CGPipeline = AbstractGanttSchedulingCGPipeline<ShadowPriceArguments, Aircraft, FlightTaskAssignment>
typealias CGPipelineList = AbstractGanttSchedulingCGPipelineList<ShadowPriceArguments, Aircraft, FlightTaskAssignment>
