package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model

import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class ServiceBandwidth(
    private val graph: Graph,
    private val services: List<Service>,
    private val edgeBandwidth: EdgeBandwidth
) {
    lateinit var inDegree: LinearSymbols2
    lateinit var outDegree: LinearSymbols2
    lateinit var outFlow: LinearSymbols2

    fun register(model: LinearMetaModel): Try<Error> {
        val y = edgeBandwidth.y
        val to: (Node) -> Predicate<Edge> =
            { fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.to(it) }

        if (!this::inDegree.isInitialized) {
            inDegree = LinearSymbols2("bandwidth_indegree_service", Shape2(graph.nodes.size, services.size))
            for (node in graph.nodes) {
                val edges = graph.edges.asSequence().filter(to(node))
                for (service in services) {
                    val poly = LinearPolynomial()
                    for (edge in edges) {
                        poly += y[edge, service]!!
                    }
                    inDegree[node, service] = LinearSymbol(poly, "${inDegree.name}_${node}_$service")
                }
            }
        }
        model.addSymbols(inDegree)

        if (!this::outDegree.isInitialized) {
            outDegree = LinearSymbols2("bandwidth_outdegree_service", Shape2(graph.nodes.size, services.size))
            for (node in graph.nodes.asSequence().filter(normal)) {
                val edges = graph.edges.asSequence().filter(from(node))
                for (service in services) {
                    val poly = LinearPolynomial()
                    for (edge in edges) {
                        poly += y[edge, service]!!
                    }
                    outDegree[node, service] = LinearSymbol(poly, "${outDegree.name}_${node}_$service")
                }
            }
            for (node in graph.nodes.asSequence().filter(client)) {
                for (service in services) {
                    outDegree[node, service] = LinearSymbol(LinearPolynomial(), "${outDegree.name}_${node}_$service")
                }
            }
        }
        model.addSymbols(outDegree)

        if (!this::outFlow.isInitialized) {
            outFlow = LinearSymbols2("bandwidth_outflow_service", Shape2(graph.nodes.size, services.size))
            for (node in graph.nodes.asSequence().filter(normal)) {
                for (service in services) {
                    outFlow[node, service] = LinearSymbol(
                        outDegree[node, service]!! - inDegree[node, service]!!,
                        "${outFlow.name}_${node}_$service"
                    )
                }
            }
            for (node in graph.nodes.asSequence().filter(client)) {
                for (service in services) {
                    outFlow[node, service] = LinearSymbol(LinearPolynomial(), "${outFlow.name}_${node}_$service)")
                }
            }
        }
        model.addSymbols(outFlow)

        return Ok(success)
    }
}
