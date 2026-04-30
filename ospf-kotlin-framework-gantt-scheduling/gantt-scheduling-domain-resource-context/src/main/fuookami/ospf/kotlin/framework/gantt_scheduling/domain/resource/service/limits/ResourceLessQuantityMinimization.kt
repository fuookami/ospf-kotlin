@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.service.limits

import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModelFlt64
import fuookami.ospf.kotlin.core.variable.UContinuous
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.AbstractResourceCapacity
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.Resource
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceTimeSlot
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.resource.model.ResourceUsage
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingCGPipeline
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AbstractGanttSchedulingShadowPriceArguments
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.AssignmentPolicy
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.task.model.Executor
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.*

class ResourceLessQuantityMinimization<
        Args : AbstractGanttSchedulingShadowPriceArguments<E, A>,
        E : Executor,
        A : AssignmentPolicy<E>,
        S : ResourceTimeSlot<R, C>,
        R : Resource<C>,
        C : AbstractResourceCapacity
        >(
    private val quantity: ResourceUsage<S, R, C>,
    private val threshold: (S) -> Flt64 = { Flt64.zero },
    private val coefficient: (S) -> Flt64 = { Flt64.one },
    override val name: String = "resource_less_capacity_minimization"
) : AbstractGanttSchedulingCGPipeline<Args, E, A> {
    private val slots = if (quantity.lessEnabled) {
        quantity.timeSlots.filter { it.resourceCapacity.lessEnabled }
    } else {
        emptyList()
    }

    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        if (slots.isNotEmpty()) {
            val cost = MutableLinearPolynomial(constant = Flt64.zero)
            for (slot in slots) {
                val thresholdValue = threshold(slot)
                val thisCoefficient = coefficient(slot)
                if (thresholdValue eq Flt64.zero) {
                    cost += LinearMonomial(thisCoefficient, quantity.lessQuantity[slot])
                } else {
                    val slack = SlackFunction(
                        x = quantity.lessQuantity[slot],
                        threshold = thresholdValue,
                        type = UContinuous,
                        name = "${quantity.name}_${slot}_${name}_over_quantity_threshold"
                    )
                    when (val result = model.add(slack)) {
                        is Ok -> {}

                        is Failed -> {
                            return Failed(result.error)
                        }

                        is Fatal -> {
                            return Fatal(result.errors)
                        }
                    }
                    cost += LinearMonomial(thisCoefficient, slack)
                }
            }
            when (val result = model.minimize(
                polynomial = cost.toLinearPolynomial(),
                name = "${quantity.name} less quantity"
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

        return ok
    }
}
