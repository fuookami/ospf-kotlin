package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.bandwidth_context.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model.*

class EdgeBandwidth(
    private val edges: List<Edge>,
    private val services: List<Service>
) {
    lateinit var y: UIntVariable2
    lateinit var bandwidth: LinearSymbols1

    fun register(model: LinearMetaModel): Try {
        if (!this::y.isInitialized) {
            y = UIntVariable2("y", Shape2(edges.size, services.size))
            for (service in services) {
                for (edge in edges.asSequence().filter(from(normal))) {
                    y[edge, service]!!.name = "${y.name}_${edge}_$service"
                    y[edge, service]!!.range.leq(edge.maxBandwidth)
                }
                for (edge in edges.asSequence().filter(!from(normal))) {
                    y[edge, service]!!.range.eq(UInt64.zero)
                }
            }
        }
        model.addVars(y)

        if (!this::bandwidth.isInitialized) {
            bandwidth = LinearSymbols1("bandwidth", Shape1(edges.size))
            for (edge in edges.asSequence().filter(from(normal))) {
                val poly = LinearPolynomial()
                for (service in services) {
                    poly += y[edge, service]!!
                }
                bandwidth[edge] = LinearSymbol(poly, "${bandwidth.name}_$edge")
            }
        }
        model.addSymbols(bandwidth)

        return Ok(success)
    }
}
