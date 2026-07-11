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
import fuookami.ospf.kotlin.example.framework_demo.demo1.bandwidth_context.model.ServiceBandwidth
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.*

/**
 * Constrains the out-flow at each normal node to the service capacity when the service is assigned.
 * 约束每个普通节点的流出到服务容量（当服务被分配时）。
 *
 * @property nodes the list of network nodes / 网络节点列表
 * @property services the list of services / 服务列表
 * @property assignment the service-to-node assignment model / 服务到节点的分配模型
 * @property serviceBandwidth the service bandwidth model / 服务带宽模型
*/
class ServiceCapacityConstraint(
    private val nodes: List<Node>,
    private val services: List<Service>,
    private val assignment: Assignment,
    private val serviceBandwidth: ServiceBandwidth,
    override val name: String = "service_capacity_constraint"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        val x = assignment.x
        val outFlow = serviceBandwidth.outFlow
        for (node in nodes.filter(normal)) {
            for (service in services) {
                model.addConstraint(
                    service.capacity.toFlt64() * (UInt64.one - x[node, service]) + LinearPolynomial(outFlow[node, service]) leq service.capacity.toFlt64(),
                    name = "${name}_($node,$service)"
                )
            }
        }
        return ok
    }
}
