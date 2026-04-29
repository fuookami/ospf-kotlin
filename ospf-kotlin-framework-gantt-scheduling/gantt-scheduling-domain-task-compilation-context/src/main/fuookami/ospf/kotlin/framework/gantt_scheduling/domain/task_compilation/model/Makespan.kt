package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearIntermediateSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MaxFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.MinMaxFunction
import fuookami.ospf.kotlin.core.model.mechanism.MetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractTask
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.functional.*

class Makespan<
        out T : AbstractTask<E, A>,
        out E : Executor,
        out A : AssignmentPolicy<E>
        >(
    private val tasks: List<T>,
    private val taskTime: TaskTime,
    private val extra: Boolean = false
) {
    lateinit var makespan: LinearIntermediateSymbol<Flt64>

    fun register(model: MetaModel<Flt64>): Try {
        if (!::makespan.isInitialized) {
            makespan = if (extra) {
                MinMaxFunction(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    name = "makespan"
                )
            } else {
                MaxFunction(
                    tasks.map {
                        taskTime.estimateEndTime[it]
                    },
                    name = "makespan"
                )
            }
        }
        when (val result = model.add(makespan)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }
}
