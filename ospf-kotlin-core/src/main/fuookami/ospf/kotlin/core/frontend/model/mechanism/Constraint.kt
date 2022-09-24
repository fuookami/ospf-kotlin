package fuookami.ospf.kotlin.core.frontend.model.mechanism

import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.Inequality
import fuookami.ospf.kotlin.utils.functional.Either
import fuookami.ospf.kotlin.utils.math.Flt64

sealed class Constraint<C: Category>(
    val lhs: List<Cell<C>>,
    val sign: Sign,
    val rhs: Flt64,
    val name: String = ""
) {
}

class LinearConstraint(
    lhs: List<Cell<Linear>>,
    sign: Sign,
    rhs: Flt64,
    name: String = ""
) : Constraint<Linear>(lhs, sign, rhs, name) {

    companion object {
        operator fun invoke(inequality: Inequality<Linear>, tokens: TokenTable<Linear>): LinearConstraint {
            val lhs = ArrayList<LinearCell>()
            var rhs = Flt64.zero
            for (cell in inequality.cells) {
                when(val temp = (cell as LinearMonomialCell).cell) {
                    is Either.Left -> {
                        val token = tokens.token(temp.value.variable)
                        if (token != null) {
                            lhs.add(LinearCell(temp.value.coefficient, token))
                        }
                    }
                    is Either.Right -> {
                        rhs += temp.value
                    }
                }
            }
            return LinearConstraint(lhs, Sign(inequality.sign), -rhs, inequality.name)
        }
    }
}
