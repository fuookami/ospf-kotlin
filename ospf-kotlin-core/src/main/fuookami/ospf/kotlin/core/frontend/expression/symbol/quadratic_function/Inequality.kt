package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.minus
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.plus
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.inequality.QuadraticInequality
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.inequality.eq
import fuookami.ospf.kotlin.core.frontend.inequality.leq
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractQuadraticMechanismModel
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.multiarray._a

internal fun QuadraticInequality.register(
    parent: IntermediateSymbol,
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    model: AbstractQuadraticMechanismModel
): Try {
    return register(
        parent = parent,
        parentName = parentName,
        k = PctVariableView1(k),
        flag = flag,
        model = model
    )
}

internal fun QuadraticInequality.register(
    parent: IntermediateSymbol,
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    model: AbstractQuadraticMechanismModel
): Try {
    assert(k.size == 3)

    if (lhs.range.valueRange?.contains(rhs.constant) == true) {
        when (val result = model.addConstraint(
            lhs eq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            name = name.ifEmpty { parentName },
            from = parent
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

            is Fatal -> {
                return Fatal(result.errors)
            }

            is Fatal -> {
                return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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

                    is Fatal -> {
                        return Fatal(result.errors)
                    }

                    is Fatal -> {
                        return Fatal(result.errors)
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



