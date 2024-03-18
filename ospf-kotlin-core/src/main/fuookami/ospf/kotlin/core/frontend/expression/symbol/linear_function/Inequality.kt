package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

internal fun LinearInequality.register(
    parentName: String,
    flag: AbstractVariableItem<*, Binary>,
    model: Model<LinearMonomialCell, Linear>
): Try {
    when (sign) {
        Sign.Less, Sign.LessEqual -> {
            val m = lhs.upperBound - rhs.constant
            model.addConstraint(
                lhs leq rhs + m * (Flt64.one - flag),
                name.ifEmpty { parentName }
            )
        }

        Sign.Greater, Sign.GreaterEqual -> {
            val m = rhs.constant - lhs.lowerBound
            model.addConstraint(
                lhs geq rhs - m * (Flt64.one - flag),
                name.ifEmpty { parentName }
            )
        }

        Sign.Equal -> {
            val m1 = lhs.upperBound - rhs.constant
            val m2 = rhs.constant - lhs.lowerBound
            model.addConstraint(
                lhs leq rhs + m1 * (Flt64.one - flag),
                name.ifEmpty { "${parentName}_ub" }
            )
            model.addConstraint(
                lhs geq rhs + m2 * (Flt64.one - flag),
                name.ifEmpty { "${parentName}_lb" }
            )
        }

        Sign.Unequal -> {
            return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
        }
    }

    return ok
}
