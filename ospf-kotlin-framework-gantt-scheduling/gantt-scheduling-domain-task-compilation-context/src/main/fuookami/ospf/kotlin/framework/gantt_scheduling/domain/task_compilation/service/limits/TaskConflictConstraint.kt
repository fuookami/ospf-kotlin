@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.core.model.mechanism.leq
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.TaskCompilation
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.*

class TaskConflictConstraint<
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val executors: List<E>,
    private val compilation: TaskCompilation<T, E, A>,
    private val conflict: (E, T, T) -> Boolean,
    override val name: String = "task_conflict"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override operator fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val x = compilation.x

        for (executor in executors) {
            for (i in tasks.indices) {
                for (j in (i + 1) until tasks.size) {
                    if (conflict(executor, tasks[i], tasks[j])) {
                        when (val result = model.addConstraint(
                            (x[tasks[i], executor].toMathLinearPolynomial() + x[tasks[j], executor].toMathLinearPolynomial()) leq Flt64.one,
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
                    }
                }
            }
        }

        return ok
    }
}



