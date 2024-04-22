package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.*

class TaskConflictConstraint<
    T : AbstractTask<E, A>,
    E : Executor,
    A : AssignmentPolicy<E>
>(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    private val conflict: (T, T) -> Boolean,
    override val name: String = "task_conflict"
) : Pipeline<AbstractLinearMetaModel> {
    override operator fun invoke(model: AbstractLinearMetaModel): Try {
        val x = compilation.x

        for (executor in executors) {
            for (i in 0 until (tasks.size - 1)) {
                for (j in (i + 1) until tasks.size) {
                    if (conflict(tasks[i], tasks[j])) {
                        when (val result = model.addConstraint(
                            (x[tasks[i], executor] + x[tasks[j], executor]) leq Flt64.one,
                            "${name}_${tasks[i]}_${tasks[j]}"
                        )) {
                            is Ok -> {}

                            is Failed -> {
                                return Failed(result.error)
                            }
                        }
                    }
                }
            }
        }

        return ok
    }
}
