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

class TaskDelayLimit<E : Executor>(
    private val timeWindow: TimeWindow,
    private val tasks: List<Task<E>>,
    private val taskTime: TaskTime<E>,
    private val taskDelayTimeCostCalculator: CostCalculator<E>? = null,
    override val name: String = "task_delay_limit"
) : HAPipeline<LinearMetaModel> {
    override operator fun invoke(model: LinearMetaModel): Try {
        val est = taskTime.est
        val delay = taskTime.delay

        if (!taskTime.delayEnabled) {
            for (task in tasks.filter { it.plan.time != null }) {
                model.addConstraint(
                    est[task]!! leq timeWindow.dump(task.plan.time!!.start),
                    "${name}_${task}"
                )
            }
        } else {
            for (task in tasks.filter { it.plan.time != null }) {
                if (task.delayEnabled) {
                    model.addConstraint(
                        (est[task]!! - delay[task]!!) leq timeWindow.dump(task.plan.time!!.start),
                        "${name}_${task}"
                    )
                } else {
                    model.addConstraint(
                        est[task]!! leq timeWindow.dump(task.plan.time!!.start),
                        "${name}_${task}"
                    )
                }
            }

            if (taskDelayTimeCostCalculator != null) {
                val cost = LinearPolynomial()
                for (task in tasks.filter { it.plan.time != null }) {
                    val penalty = taskDelayTimeCostCalculator!!(task) ?: Flt64.infinity
                    cost += penalty * delay[task]!!
                }
                model.minimize(cost, "delay")
            }
        }

        return Ok(success)
    }

    override fun calculate(model: LinearMetaModel, solution: List<Flt64>): Ret<Flt64?> {
        // todo: impl
        return Ok(Flt64.zero)
    }
}
