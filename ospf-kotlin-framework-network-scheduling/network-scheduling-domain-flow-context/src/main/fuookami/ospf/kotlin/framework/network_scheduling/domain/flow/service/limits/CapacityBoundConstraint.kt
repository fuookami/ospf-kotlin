package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 弧流量容量上下界约束。 / Arc-flow capacity lower and upper-bound constraints.
 */
class CapacityBoundConstraint<V : RealNumber<V>>(
    private val graph: FlowGraph<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    override val name: String = "flow_capacity"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = graph.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        for (arc in graph.arcs) {
            val variables = when (val result = graph.flowVariablesOf(arc)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            var polynomial = LinearPolynomial()
            variables.forEach { variable ->
                polynomial += variable
            }
            val lower = when (val result = graph.normalizeFlow(arc.capacity.lower, valueAdapter)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            val upper = when (val result = graph.normalizeFlow(arc.capacity.upper, valueAdapter)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = model.addConstraint(
                polynomial geq lower,
                name = "${name}_${arc.from.value}_${arc.to.value}_lower"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            when (val result = model.addConstraint(
                polynomial leq upper,
                name = "${name}_${arc.from.value}_${arc.to.value}_upper"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }
}
