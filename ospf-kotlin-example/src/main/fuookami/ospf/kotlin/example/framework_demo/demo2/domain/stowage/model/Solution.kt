package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.dto.*

class Solution(
    val stowage: Map<Position, List<Item>>,
    val predicateLoadWeight: Map<Position, Quantity<Flt64>>,
    val recommendedLoadWeight: Map<Position, Quantity<Flt64>>
)

fun Solution.render(): RenderDTO {
    TODO("not implemented yet")
}
