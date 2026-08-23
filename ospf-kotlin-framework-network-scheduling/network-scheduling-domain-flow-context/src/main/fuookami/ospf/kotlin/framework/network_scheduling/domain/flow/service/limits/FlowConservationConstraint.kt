package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*

/**
 * 网络流守恒约束：sum(out) - sum(in) = balance。 / Flow conservation: sum(out) - sum(in) = balance.
 */
class FlowConservationConstraint<V : RealNumber<V>>(
    private val graph: FlowGraph<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    override val name: String = "flow_conservation"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = graph.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        for (commodity in graph.commodities) {
            for (node in graph.nodes) {
                var polynomial = LinearPolynomial()
                for (arc in graph.arcs) {
                    if (arc.from == node.id || arc.to == node.id) {
                        val variable = when (val result = graph.flowVariable(commodity.id, arc)) {
                            is Ok -> result.value
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                        val coefficient = if (arc.from == node.id) Flt64.one else -Flt64.one
                        polynomial += coefficient * variable
                    }
                }
                val balance = graph.supplyDemandOf(commodity.id, node.id)?.net?.let {
                    when (val result = graph.normalizeFlow(it, valueAdapter)) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                } ?: Flt64.zero
                when (val result = model.addConstraint(
                    polynomial eq balance,
                    name = "${name}_${commodity.id}_${node.id.value}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
        return ok
    }
}
