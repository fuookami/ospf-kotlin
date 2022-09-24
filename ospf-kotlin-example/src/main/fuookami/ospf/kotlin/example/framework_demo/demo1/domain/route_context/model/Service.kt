package fuookami.ospf.kotlin.example.framework_demo.demo1.domain.route_context.model

import fuookami.ospf.kotlin.utils.math.UInt64
import fuookami.ospf.kotlin.utils.concept.Indexed

class Service(
    override val index: Int,
    val id: UInt64,
    val capacity: UInt64,
    val cost: UInt64
) : Indexed {
    override fun toString() = "S$id"
}
