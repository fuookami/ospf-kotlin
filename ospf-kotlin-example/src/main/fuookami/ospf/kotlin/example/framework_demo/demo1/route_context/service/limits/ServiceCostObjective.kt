package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Assignment
import fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model.Service

class ServiceCostObjective(
    private val services: List<Service>,
    private val assignment: Assignment,
    override val name: String = "service_cost"
) : Pipeline<LinearMetaModelF64> {
    override fun invoke(model: LinearMetaModelF64): Try {
        model.minimize(
            sum(services) { it.cost * assignment.serviceAssignment[it] },
            "service cost"
        )
        return ok
    }
}

