package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.core.frontend.expression.monomial.LinearMonomial as FrontendLinearMonomial
import fuookami.ospf.kotlin.core.frontend.expression.monomial.times
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.AbstractLinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.minus
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.plus
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.sum
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.times
import fuookami.ospf.kotlin.core.frontend.expression.symbol.IntermediateSymbol
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.frontend.model.mechanism.LinearConstraintInput
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt8
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.toFlt64
import fuookami.ospf.kotlin.multiarray._a

// ========== LinearConstraintInput register overloads ==========

internal fun LinearConstraintInput.register(
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    tokenTable: AddableTokenCollection
): Try {
    return register(
        parentName = parentName,
        k = PctVariableView1(k),
        flag = flag,
        tokenTable = tokenTable
    )
}

internal fun LinearConstraintInput.register(
    parent: IntermediateSymbol?,
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel
): Try {
    return register(
        parent = parent,
        parentName = parentName,
        k = PctVariableView1(k),
        flag = flag,
        epsilon = epsilon,
        model = model
    )
}

internal fun LinearConstraintInput.register(
    parent: IntermediateSymbol?,
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel,
    fixedValues: Map<Symbol, Flt64>
): Try {
    return register(
        parent = parent,
        parentName = parentName,
        k = PctVariableView1(k),
        flag = flag,
        epsilon = epsilon,
        model = model,
        fixedValues = fixedValues
    )
}

internal fun LinearConstraintInput.register(
    @Suppress("UNUSED_PARAMETER") parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>?,
    tokenTable: AddableTokenCollection
): Try {
    assert(k.size == 3)

    val lb = lowerBound
    val ub = upperBound
    if (lb != null && ub != null) {
        val zero = Flt64.zero
        val inRange = zero geq lb && zero leq ub
        if (inRange) {
            when (val result = tokenTable.add(k)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
    }

    if (flag != null) {
        when (val result = tokenTable.add(flag)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }

    return ok
}

/**
 * Convert LinearFlattenData to LinearPolynomial for internal Big-M constraint construction.
 */
private fun LinearConstraintInput.toLinearPolynomial(): LinearPolynomial {
    val monomials = flattenData.monomials.mapNotNull { m ->
        val variable = m.symbol as? AbstractVariableItem<*, *>
        if (variable != null) {
            FrontendLinearMonomial(m.coefficient, variable)
        } else {
            null
        }
    }
    return LinearPolynomial(monomials = monomials, constant = flattenData.constant)
}

internal fun LinearConstraintInput.register(
    parent: IntermediateSymbol?,
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel
): Try {
    assert(k.size == 3)

    val lb = lowerBound
    val ub = upperBound
    if (lb != null && ub != null) {
        val zero = Flt64.zero
        val inRange = zero geq lb && zero leq ub
        if (inRange) {
            val poly = toLinearPolynomial()
            when (val result = model.addConstraint(
                relation = poly leq (k[0] * lb + k[1] * zero + k[2] * ub),
                name = "${name.ifEmpty { parentName }}_ub",
                from = parent
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (val result = model.addConstraint(
                relation = poly geq (k[0] * lb + k[1] * zero + k[2] * ub),
                name = "${name.ifEmpty { parentName }}_lb",
                from = parent
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (val result = model.addConstraint(
                relation = sum(k[_a]) eq Flt64.one,
                name = "${name.ifEmpty { parentName }}_k",
                from = parent
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val constraints = when (sign) {
                Comparison.LT, Comparison.LE -> {
                    listOf(
                        Pair(k[0] leq flag, "${name.ifEmpty { parentName }}_k_0"),
                        Pair(k[2] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_2_ub"),
                        Pair(k[2] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_2_lb"),
                    )
                }

                Comparison.GT, Comparison.GE -> {
                    listOf(
                        Pair(k[0] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_0_ub"),
                        Pair(k[0] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_0_lb"),
                        Pair(k[2] leq flag, "${name.ifEmpty { parentName }}_k_2")
                    )
                }

                Comparison.EQ -> {
                    listOf(
                        Pair(k[0] + k[2] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_ub"),
                        Pair(k[0] + k[2] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_lb")
                    )
                }

                Comparison.NE -> {
                    return Failed(
                        code = ErrorCode.ApplicationFailed,
                        message = "$parentName's inequality sign unsupported: $this"
                    )
                }
            }

            for ((constraint, constraintName) in constraints) {
                when (val result = model.addConstraint(
                    relation = constraint,
                    name = constraintName
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }
    } else {
        val bin = when (sign) {
            Comparison.LT, Comparison.LE -> {
                val ubVal = ub ?: return Failed(
                    code = ErrorCode.ApplicationFailed,
                    message = "$parentName's upper bound is null"
                )
                ubVal ls Flt64.zero
            }

            Comparison.GT, Comparison.GE -> {
                val lbVal = lb ?: return Failed(
                    code = ErrorCode.ApplicationFailed,
                    message = "$parentName's lower bound is null"
                )
                lbVal gr Flt64.zero
            }

            Comparison.EQ -> false
            Comparison.NE -> {
                return Failed(
                    code = ErrorCode.ApplicationFailed,
                    message = "$parentName's inequality sign unsupported: $this"
                )
            }
        }

        when (val result = model.addConstraint(
            relation = flag eq bin,
            name = "${name.ifEmpty { parentName }}_flag",
            from = parent
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        model.tokens.find(flag)?.let { token ->
            token._result = bin.toFlt64()
        }
    }

    return ok
}

internal fun LinearConstraintInput.register(
    parent: IntermediateSymbol?,
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel,
    fixedValues: Map<Symbol, Flt64>
): Try {
    assert(k.size == 3)

    val lhsValue = isTrue(fixedValues, model.tokens)
        ?: return register(
            parent = parent,
            parentName = parentName,
            k = k,
            flag = flag,
            epsilon = epsilon,
            model = model
        )
    val bin = lhsValue

    val lb = lowerBound
    val ub = upperBound
    if (lb != null && ub != null) {
        val zero = Flt64.zero
        val inRange = zero geq lb && zero leq ub
        if (inRange) {
            val poly = toLinearPolynomial()
            when (val result = model.addConstraint(
                poly leq (k[0] * lb + k[1] * zero + k[2] * ub),
                name = "${name.ifEmpty { parentName }}_ub",
                from = parent
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            when (val result = model.addConstraint(
                poly geq (k[0] * lb + k[1] * zero + k[2] * ub),
                name = "${name.ifEmpty { parentName }}_lb",
                from = parent
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            val kValue = when (sign) {
                Comparison.LT, Comparison.LE -> {
                    if (bin) {
                        val pct = (lhsValue.toFlt64() - lb) / (Flt64.zero - lb)
                        listOf(pct, Flt64.one - pct, Flt64.zero)
                    } else {
                        val pct = (ub - lhsValue.toFlt64()) / (ub - Flt64.zero)
                        listOf(Flt64.zero, Flt64.one - pct, pct)
                    }
                }

                Comparison.GT, Comparison.GE -> {
                    if (bin) {
                        val pct = (ub - lhsValue.toFlt64()) / (ub - Flt64.zero)
                        listOf(Flt64.zero, Flt64.one - pct, pct)
                    } else {
                        val pct = (lhsValue.toFlt64() - lb) / (Flt64.zero - lb)
                        listOf(pct, Flt64.one - pct, Flt64.zero)
                    }
                }

                Comparison.EQ -> {
                    if (bin) {
                        listOf(Flt64.zero, Flt64.one, Flt64.zero)
                    } else if (lhsValue.toFlt64() ls Flt64.zero) {
                        val pct = (lhsValue.toFlt64() - lb) / (Flt64.zero - lb)
                        listOf(pct, Flt64.one - pct, Flt64.zero)
                    } else {
                        val pct = (ub - lhsValue.toFlt64()) / (ub - Flt64.zero)
                        listOf(Flt64.zero, Flt64.one - pct, pct)
                    }
                }

                Comparison.NE -> {
                    return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
                }
            }

            for ((i, value) in kValue.withIndex()) {
                when (val result = model.addConstraint(
                    k[i] eq value,
                    name = "${name.ifEmpty { parentName }}_k_$i",
                    from = parent
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }

                model.tokens.find(k[i])?.let { token ->
                    token._result = value
                }
            }
        }
    }

    when (val result = model.addConstraint(
        flag eq bin,
        name = "${name.ifEmpty { parentName }}_flag",
        from = parent
    )) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }

    model.tokens.find(flag)?.let { token ->
        token._result = bin.toFlt64()
    }

    return ok
}



