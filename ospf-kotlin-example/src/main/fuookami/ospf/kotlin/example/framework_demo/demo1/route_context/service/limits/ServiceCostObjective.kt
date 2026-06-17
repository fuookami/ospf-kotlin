package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Service

/**
 * 通过求和按分配变量加权的每服务成本来最小化总服务成本。Minimizes the total service cost by summing per-service cost weighted by assignment variables.
 *
 * @property private val services 参数。
 * @property private val assignment 参数。
 * @property override val name 参数。
 */
class ServiceCostObjective(
    private val services: List<Service>,
    private val assignment: Assignment,
    override val name: String = "service_cost"
) : Pipeline<LinearMetaModel<Flt64>> {
    override fun invoke(model: LinearMetaModel<Flt64>): Try {
        model.minimize(
            sum(services) { it.cost * assignment.serviceAssignment[it] },
            "service cost"
        )
        return ok
    }
}
