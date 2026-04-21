@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.service.limits

import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.intermediate_model.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.*
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task_compilation.model.Switch
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class SwitchCostMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        T : AbstractTask<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>
        >(
    private val executors: List<E>,
    private val tasks: List<T>,
    private val switch: Switch,
    private val coefficient: Extractor<Flt64?, Triple<E, T, T>> = { Flt64.one },
    override val name: String = "switch_cost_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        val cost = MutableLinearPolynomial<Flt64>(constant = Flt64.zero)
        for (executor in executors) {
            for (task1 in tasks) {
                for (task2 in tasks) {
                    val thisCoefficient = coefficient(Triple(executor, task1, task2)) ?: Flt64.infinity
                    cost += thisCoefficient * switch.switch[executor, task1, task2].toMathLinearPolynomial()
                }
            }
        }
        when (val result = model.minimize(
            polynomial = cost.toLinearPolynomial(),
            name = "switch cost"
        )) {
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



