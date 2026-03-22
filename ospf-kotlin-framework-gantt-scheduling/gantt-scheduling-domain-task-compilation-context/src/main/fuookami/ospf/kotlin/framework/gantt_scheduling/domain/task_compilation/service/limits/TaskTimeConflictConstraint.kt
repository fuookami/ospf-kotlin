@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.frontend.expression.polynomial.plus
import fuookami.ospf.kotlin.core.frontend.inequality.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskCompilation
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.math.Flt64

class TaskTimeConflictConstraint<
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    override val name: String = "task_time_conflict"
) : Pipeline<AbstractLinearMetaModel> {
    val tasks = tasks
        .filter { it.time != null && !it.advanceEnabled && !it.delayEnabled }
        .sortedBy { it.time!!.start }

    override operator fun invoke(model: AbstractLinearMetaModel): Try {
        val x = compilation.x

        for (executor in executors) {
            for (i in tasks.indices) {
                for (j in (i + 1) until tasks.size) {
                    if (tasks[i].time!!.withIntersection(tasks[j].time!!)) {
                        when (val result = model.addConstraint(
                            (x[tasks[i], executor] + x[tasks[j], executor]) leq Flt64.one,
                            name = "${name}_${tasks[i]}_${tasks[j]}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }

                            is Fatal -> {
                                return Fatal(result.errors)
                            }
                        }
                    } else {
                        continue
                    }
                }
            }
        }

        return ok
    }
}
