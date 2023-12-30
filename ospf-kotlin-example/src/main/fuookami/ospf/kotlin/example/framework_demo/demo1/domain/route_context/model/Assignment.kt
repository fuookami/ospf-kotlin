package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class Assignment(
    private val nodes: List<Node>,
    private val services: List<Service>
) {
    lateinit var x: BinVariable2
    lateinit var nodeAssignment: LinearSymbols1
    lateinit var serviceAssignment: LinearSymbols1

    fun register(model: LinearMetaModel): Try {
        if (!this::x.isInitialized) {
            x = BinVariable2("x", Shape2(nodes.size, services.size))
            for (service in services) {
                for (node in nodes.asSequence().filter(normal)) {
                    x[node, service]!!.name = "${x.name}_${node}_$service"
                }
                for (node in nodes.asSequence().filter(client)) {
                    val variable = x[node, service]!!
                    variable.name = "${x.name}_${node}_$service"
                    variable.range.eq(UInt8.zero)
                }
            }
        }
        model.addVars(x)

        if (!this::nodeAssignment.isInitialized) {
            nodeAssignment = LinearSymbols1("node_assignment", Shape1(nodes.size))
            for (node in nodes.asSequence().filter(normal)) {
                val poly = LinearPolynomial()
                for (service in services) {
                    poly += x[node, service]!!
                }
                nodeAssignment[node] = LinearSymbol(poly, "${nodeAssignment.name}_$node")
            }
            for (node in nodes.asSequence().filter(client)) {
                nodeAssignment[node] = LinearSymbol(LinearPolynomial(), "${nodeAssignment.name}_$node")
            }
        }
        model.addSymbols(nodeAssignment)

        if (!this::serviceAssignment.isInitialized) {
            serviceAssignment = LinearSymbols1("service_assignment", Shape1(services.size))
            for (service in services) {
                val poly = LinearPolynomial()
                for (node in nodes.asSequence().filter(normal)) {
                    poly += x[node, service]!!
                }
                serviceAssignment[service] = LinearSymbol(poly, "${serviceAssignment.name}_$service")
            }
        }
        model.addSymbols(serviceAssignment)

        return Ok(success)
    }
}
