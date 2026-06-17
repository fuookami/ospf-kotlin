package fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.EdgeBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * 当关联服务未分配到该边时将边带宽限制为零。Limits edge bandwidth to zero when the associated service is not assigned to that edge.
 *
 * @property private val edges 参数。
 * @property private val services 参数。
 * @property private val assignment 参数。
 * @property private val edgeBandwidth 参数。
 * @property override val name 参数。
 */
class EdgeBandwidthConstraint(
    private val edges: List<Edge>,
    private val services: List<Service>,
    private val assignment: Assignment,
    private val edgeBandwidth: EdgeBandwidth,
    override val name: String = "edge_bandwidth_constraint"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val y = edgeBandwidth.y
        val assignment = assignment.serviceAssignment

        for (edge in edges.filter(from(normal))) {
            for (service in services) {
                model.addConstraint(
                    (UInt64.one - assignment[service]) * edge.maxBandwidth.toFlt64() + LinearPolynomial(y[edge, service]) leq edge.maxBandwidth.toFlt64(),
                    name = "${name}_($edge,$service)"
                )
            }
        }
        return ok
    }
}
