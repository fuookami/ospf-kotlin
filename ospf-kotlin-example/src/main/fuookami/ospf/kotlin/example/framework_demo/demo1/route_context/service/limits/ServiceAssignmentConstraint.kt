package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Service

/**
 * Constrains each service to be assigned to at most one node.
 * 约束每个服务最多分配一个节点。
 *
 * @property services 服务列表 / the list of services
 * @property assignment 服务到节点的分配模型 / the service-to-node assignment model
*/
class ServiceAssignmentConstraint(
    private val services: List<Service>,
    private val assignment: Assignment,
    override val name: String = "service_assignment"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        for (service in services) {
            model.addConstraint(
                assignment.serviceAssignment[service] leq 1,
                name = "${name}_$service"
            )
        }
        return ok
    }
}
