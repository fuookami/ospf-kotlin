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

class TaskAdvanceLimit<E : Executor>(
    private val timeWindow: TimeWindow,
    private val tasks: List<Task<E>>,
    private val taskTime: TaskTime<E>,
    private val taskAdvanceTimeCostCalculator: CostCalculator<E>? = null,
    override val name: String = "task_advance_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try<Error> {
        val est = taskTime.est
        val advance = taskTime.advance

        if (!taskTime.advanceEnabled) {
            for (task in tasks.filter { it.plan.time != null }) {
                model.addConstraint(
                    est[task]!! geq timeWindow.dump(task.plan.time!!.start),
                    "${name}_${task}"
                )
            }
        } else {
            for (task in tasks.filter { it.plan.time != null }) {
                if (task.advanceEnabled) {
                    model.addConstraint(
                        (est[task]!! + advance[task]!!) geq timeWindow.dump(task.plan.time!!.start),
                        "${name}_${task}"
                    )
                } else {
                    model.addConstraint(
                        est[task]!! geq timeWindow.dump(task.plan.time!!.start),
                        "${name}_${task}"
                    )
                }
            }

            if (taskAdvanceTimeCostCalculator != null) {
                val cost = LinearPolynomial()
                for (task in tasks.filter { it.plan.time != null }) {
                    val penalty = taskAdvanceTimeCostCalculator!!(task) ?: Flt64.infinity
                    cost += penalty * advance[task]!!
                }
                model.minimize(cost, "delay")
            }
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Result<Flt64?, Error> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
