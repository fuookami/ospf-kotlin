package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.express_effectiveness.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class AbsoluteOrder(
    private val coefficient: HashMap<CargoPriority, (Position) -> Flt64>
) {
    companion object {
        operator fun invoke(
            items: List<Item>,
            positions: List<Position>
        ): AbsoluteOrder {
            TODO("not implemented yet")
        }
    }

    operator fun invoke(priority: CargoPriority, position: Position): Flt64 {
        return coefficient[priority]?.let { it(position) } ?: Flt64.one
    }
}

