package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.EdgeBandwidth
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Edge
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.from
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.normal
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class BandwidthCostObjective(
    private val edges: List<Edge>,
    private val edgeBandwidth: EdgeBandwidth,
    override val name: String = "bandwidth_cost"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        model.minimize(
            sum(edges.filter(from(normal))) { it.costPerBandwidth * edgeBandwidth.bandwidth[it] },
            "bandwidth cost"
        )
        return ok
    }
}

