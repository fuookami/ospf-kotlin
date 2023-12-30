package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class NodeBandwidth(
    private val nodes: List<Node>,
    private val services: List<Service>,
    private val serviceBandwidth: ServiceBandwidth
) {
    lateinit var inDegree: LinearSymbols1
    lateinit var outDegree: LinearSymbols1
    lateinit var outFlow: LinearSymbols1

    fun register(model: LinearMetaModel): Try {
        if (!this::inDegree.isInitialized) {
            inDegree = LinearSymbols1("bandwidth_indegree_node", Shape1(nodes.size))
            for (node in nodes) {
                val poly = LinearPolynomial()
                for (service in services) {
                    poly += serviceBandwidth.inDegree[node, service]!!
                }
                inDegree[node] = LinearSymbol(poly, "${inDegree.name}_$node")
            }
        }
        model.addSymbols(inDegree)

        if (!this::outDegree.isInitialized) {
            outDegree = LinearSymbols1("bandwidth_outdegree_node", Shape1(nodes.size))
            for (node in nodes.asSequence().filter(normal)) {
                val poly = LinearPolynomial()
                for (service in services) {
                    poly += serviceBandwidth.outDegree[node, service]!!
                }
                outDegree[node] = LinearSymbol(poly, "${outDegree.name}_$node")
            }
            for (node in nodes.asSequence().filter(client)) {
                outDegree[node] = LinearSymbol(LinearPolynomial(), "${outDegree.name}_$node")
            }
        }
        model.addSymbols(outDegree)

        if (!this::outFlow.isInitialized) {
            outFlow = LinearSymbols1("bandwidth_outflow_node", Shape1(nodes.size))
            for (node in nodes.asSequence().filter(normal)) {
                val poly = LinearPolynomial()
                for (service in services) {
                    poly += serviceBandwidth.outFlow[node, service]!!
                }
                outFlow[node] = LinearSymbol(poly, "${outFlow.name}_$node")
            }
            for (node in nodes.asSequence().filter(client)) {
                outFlow[node] = LinearSymbol(LinearPolynomial(), "${outFlow.name}_$node")
            }
        }
        model.addSymbols(outFlow)

        return Ok(success)
    }
}
