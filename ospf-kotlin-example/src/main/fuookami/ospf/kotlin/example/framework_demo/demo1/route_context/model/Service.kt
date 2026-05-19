package fuookami.ospf.kotlin.example.framework_demo.demo1.route_context.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.concept.*

class Service(
    val id: UInt64,
    val capacity: UInt64,
    val cost: UInt64
) : AutoIndexed(Service::class) {
    override fun toString() = "S$id"
}
