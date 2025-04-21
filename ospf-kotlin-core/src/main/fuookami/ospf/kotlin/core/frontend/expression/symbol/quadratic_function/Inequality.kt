package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

internal fun QuadraticInequality.register(
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    model: AbstractQuadraticMechanismModel
): Try {
    return register(parentName, PctVariableView1(k), flag, model)
}

internal fun QuadraticInequality.register(
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    model: AbstractQuadraticMechanismModel
): Try {
    assert(k.size == 3)

    if (lhs.range.valueRange?.contains(rhs.constant) == true) {
        when (val result = model.addConstraint(
            lhs eq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            name.ifEmpty { parentName }
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            sum(k[_a]) eq Flt64.one,
            "${name.ifEmpty { parentName }}_k"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (sign) {
            Sign.Less, Sign.LessEqual -> {
                when (val result = model.addConstraint(
                    k[0] leq flag,
                    "${name.ifEmpty { parentName }}_k_0"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    k[2] leq Flt64.one - flag,
                    "${name.ifEmpty { parentName }}_k_2"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            Sign.Greater, Sign.GreaterEqual -> {
                when (val result = model.addConstraint(
                    k[0] leq Flt64.one - flag,
                    "${name.ifEmpty { parentName }}_k_0"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    k[2] leq flag,
                    "${name.ifEmpty { parentName }}_k_2"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            Sign.Equal -> {
                when (val result = model.addConstraint(
                    k[0] leq Flt64.one - flag,
                    "${name.ifEmpty { parentName }}_k_0"
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }

                when (val result = model.addConstraint(
                    k[2] leq Flt64.one - flag,
                    "${name.ifEmpty { parentName }}_k_2"
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
    }

    return ok
}
