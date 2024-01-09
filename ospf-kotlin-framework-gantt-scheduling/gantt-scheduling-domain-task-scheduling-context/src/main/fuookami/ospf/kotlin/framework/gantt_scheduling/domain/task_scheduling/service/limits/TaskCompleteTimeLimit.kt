package fuookami.ospf.kotlin.framework.gantt_scheduling.mip.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.mip.model.*

class TaskCompleteTimeLimit<E : Executor>(
    private val timeWindow: TimeWindow,
    private val tasks: List<Task<E>>,
    private val taskTime: TaskTime<E>,
    private val overExpirationTimeCostCalculator: CostCalculator<E>? = null,
    override val name: String = "task_compilation_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try {
        val ect = taskTime.ect
        val oet = taskTime.oet

        if (!taskTime.overExpirationTimeEnabled) {
            for (task in tasks.filter { it.expirationTime != null }) {
                model.addConstraint(
                    ect[task]!! leq timeWindow.dump(task.expirationTime!!),
                    "${name}_${task}"
                )
            }
        } else {
            for (task in tasks.filter { it.expirationTime != null }) {
                model.addConstraint(
                    (ect[task]!! - oet[task]!!) leq timeWindow.dump(task.expirationTime!!),
                    "${name}_${task}"
                )
            }

            if (overExpirationTimeCostCalculator != null) {
                val cost = LinearPolynomial()
                for (task in tasks.filter { it.expirationTime != null }) {
                    val penalty = overExpirationTimeCostCalculator!!(task) ?: Flt64.infinity
                    cost += penalty * oet[task]!!
                }
                model.minimize(cost, "over expiration time")
            }
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Ret<Flt64?> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
