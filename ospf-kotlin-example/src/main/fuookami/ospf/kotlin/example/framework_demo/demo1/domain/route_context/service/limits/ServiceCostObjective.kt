package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class ServiceCostObjective(
    private val services: List<Service>,
    private val assignment: Assignment,
    override val name: String = "service_cost"
) : Pipeline<LinearMetaModel> {
    override fun invoke(model: LinearMetaModel): Try<Error> {
        val poly = LinearPolynomial()
        for (service in services) {
            poly += service.cost * assignment.serviceAssignment[service]!!
        }
        model.minimize(poly, "service cost")
        return Ok(success)
    }
}