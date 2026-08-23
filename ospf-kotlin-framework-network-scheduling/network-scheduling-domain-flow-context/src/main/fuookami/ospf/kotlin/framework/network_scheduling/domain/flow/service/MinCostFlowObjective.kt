package fuookami.ospf.kotlin.framework.network_scheduling.domain.flow.service

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
import fuookami.ospf.kotlin.quantities.unit.*

/**
 * 最小费用流目标：最小化 sum(flow * cost)。 / Min-cost-flow objective: minimize sum(flow * cost).
 */
class MinCostFlowObjective<V : RealNumber<V>>(
    private val graph: FlowGraph<V>,
    private val valueAdapter: NetworkSchedulingSolverValueAdapter<V>,
    private val costUnit: PhysicalUnit = NoneUnit,
    override val name: String = "min_cost_flow"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = graph.register(model)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        var polynomial = LinearPolynomial()
        for (commodity in graph.commodities) {
            for (arc in graph.arcs) {
                val variable = when (val result = graph.flowVariable(commodity.id, arc)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                val cost = when (val result = valueAdapter.normalize(arc.cost, costUnit)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                polynomial += cost * variable
            }
        }
        return when (val result = model.minimize(
            polynomial = polynomial,
            name = name
        )) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}
