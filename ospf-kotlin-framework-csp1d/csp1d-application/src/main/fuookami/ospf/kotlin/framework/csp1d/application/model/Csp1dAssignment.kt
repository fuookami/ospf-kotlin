package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.variable.UIntVariable1
import fuookami.ospf.kotlin.utils.functional.Try

class Csp1dAssignment(
    val x: UIntVariable1,
    val planCount: Int
) {
    companion object {
        fun create(planCount: Int): Csp1dAssignment {
            return Csp1dAssignment(
                x = UIntVariable1("x", Shape1(planCount)),
                planCount = planCount
            )
        }
    }

    operator fun get(index: Int) = x[index]

    fun register(model: LinearMetaModel<Flt64>): Try {
        return model.add(x)
    }
}
