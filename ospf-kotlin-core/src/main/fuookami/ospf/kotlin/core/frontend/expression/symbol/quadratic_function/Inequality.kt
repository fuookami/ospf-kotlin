package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

internal fun QuadraticInequality.register(
    parentName: String,
    flag: AbstractVariableItem<*, Binary>,
    model: AbstractQuadraticMechanismModel
): Try {
    when (sign) {
        Sign.Less, Sign.LessEqual -> {
            val m = lhs.upperBound!!.value.unwrap() - rhs.constant
            when (val result = model.addConstraint(
                lhs leq rhs + m * (Flt64.one - flag),
                name.ifEmpty { parentName }
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        Sign.Greater, Sign.GreaterEqual -> {
            val m = rhs.constant - lhs.lowerBound!!.value.unwrap()
            when (val result = model.addConstraint(
                lhs geq rhs - m * (Flt64.one - flag),
                name.ifEmpty { parentName }
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        Sign.Equal -> {
            val m1 = lhs.upperBound!!.value.unwrap() - rhs.constant
            val m2 = rhs.constant - lhs.lowerBound!!.value.unwrap()
            when (val result = model.addConstraint(
                lhs leq rhs + m1 * (Flt64.one - flag),
                name.ifEmpty { "${parentName}_ub" }
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                lhs geq rhs + m2 * (Flt64.one - flag),
                name.ifEmpty { "${parentName}_lb" }
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        Sign.Unequal -> {
            return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
        }
    }

    return ok
}
