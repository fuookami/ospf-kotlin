package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class ServiceCostObjective(
    private val services: List<Service>,
    private val assignment: Assignment,
    override val name: String = "service_cost"
) : Pipeline<LinearMetaModel> {
    override fun invoke(model: LinearMetaModel): Try {
        model.minimize(
            sum(services) { it.cost * assignment.serviceAssignment[it] },
            "service cost"
        )
        return Ok(success)
    }
}
