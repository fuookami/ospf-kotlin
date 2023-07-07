package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

class TaskTimeConflictLimit<E : Executor>(
    originTasks: List<Task<E>>,
    private val executors: List<E>,
    private val compilation: Compilation<E>,
    override val name: String = "task_time_conflict_limit"
) : HAPipeline<LinearMetaModel> {
    val tasks = originTasks.filter { it.plan.time != null && !it.advanceEnabled && !it.delayEnabled }

    override operator fun invoke(model: LinearMetaModel): Try<Error> {
        val x = compilation.x

        for (executor in executors) {
            for (i in 0 until (tasks.size - 1)) {
                for (j in (i + 1) until tasks.size) {
                    if (tasks[i].plan.time!!.withIntersection(tasks[j].plan.time!!)) {
                        model.addConstraint(
                            (x[tasks[i], executor]!! + x[tasks[j], executor]!!) leq Flt64.one,
                            "${name}_${tasks[i]}_${tasks[j]}"
                        )
                    }
                }
            }
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Result<Flt64?, Error> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
