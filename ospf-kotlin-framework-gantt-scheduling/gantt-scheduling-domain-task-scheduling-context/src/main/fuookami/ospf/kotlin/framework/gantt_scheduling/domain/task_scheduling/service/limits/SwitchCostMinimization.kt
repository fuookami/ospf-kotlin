package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.service.limits

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_scheduling.model.*

class SwitchCostMinimization<Args : GanttSchedulingShadowPriceArguments<E, A>, T : AbstractTask<E, A>, E : Executor, A : AssignmentPolicy<E>>(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val switch: Switch,
    private val coefficient: Extractor<Flt64?, Triple<E, T, T>> = { Flt64.one },
    override val name: String = "switch_cost_minimization"
) : GanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: LinearMetaModel): Try {
        val cost = MutableLinearPolynomial()
        for (executor in executors) {
            for (task1 in tasks) {
                for (task2 in tasks) {
                    val thisCoefficient = coefficient(Triple(executor, task1, task2)) ?: Flt64.infinity
                    cost += thisCoefficient * switch.switch[executor, task1, task2]
                }
            }
        }
        model.minimize(cost, "switch cost")

        return Ok(success)
    }
}
