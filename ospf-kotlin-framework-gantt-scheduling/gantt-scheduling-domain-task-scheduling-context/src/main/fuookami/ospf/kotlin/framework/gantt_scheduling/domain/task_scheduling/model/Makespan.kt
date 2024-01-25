package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*

interface Makespan {
    val makespan: LinearSymbol

    fun register(model: LinearMetaModel): Try
}

class TaskSchedulingMakespan<T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val tasks: List<T>,
    private val taskTime: TaskTime,
    private val extra: Boolean = false
) : Makespan {
    override lateinit var makespan: LinearSymbol

    override fun register(model: LinearMetaModel): Try {
        if (!this::makespan.isInitialized) {
            makespan = if (extra) {
                MaxFunction(tasks.map { LinearPolynomial(taskTime.estimateEndTime[it]) }, name = "makespan")
            } else {
                MinMaxFunction(tasks.map { LinearPolynomial(taskTime.estimateEndTime[it]) }, name = "makespan")
            }
        }
        model.addSymbol(makespan)

        return Ok(success)
    }
}
