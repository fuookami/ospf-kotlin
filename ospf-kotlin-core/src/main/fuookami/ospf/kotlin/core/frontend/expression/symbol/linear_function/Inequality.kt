package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.ordinary.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    tokenTable: AbstractMutableTokenTable
): Try {
    return register(parentName, PctVariableView1(k), flag, tokenTable)
}

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel
): Try {
    return register(parentName, PctVariableView1(k), flag, epsilon, model)
}

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariable1,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel,
    fixedValues: Map<Symbol, Flt64>
): Try {
    return register(parentName, PctVariableView1(k), flag, epsilon, model, fixedValues)
}

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>?,
    tokenTable: AbstractMutableTokenTable
): Try {
    assert(k.size == 3)

    if (rhs.constant in lhs.range.valueRange!!) {
        when (val result = tokenTable.add(k)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
    }

    if (flag != null) {
        when (val result = tokenTable.add(flag)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
    }

    return ok
}

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel
): Try {
    assert(k.size == 3)

    if (rhs.constant in lhs.range.valueRange!!) {
        when (val result = model.addConstraint(
            lhs leq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            "${name.ifEmpty { parentName }}_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            lhs geq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            "${name.ifEmpty { parentName }}_ub"
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

        val constraints = when (sign) {
            Sign.Less, Sign.LessEqual -> {
                listOf(
                    Pair(k[0] leq flag, "${name.ifEmpty { parentName }}_k_0"),
                    Pair(k[2] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_2_ub"),
                    Pair(k[2] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_2_lb"),
                )
            }

            Sign.Greater, Sign.GreaterEqual -> {
                listOf(
                    Pair(k[0] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_0_ub"),
                    Pair(k[0] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_0_lb"),
                    Pair(k[2] leq flag, "${name.ifEmpty { parentName }}_k_2")
                )
            }

            Sign.Equal -> {
                listOf(
                    Pair(k[0] + k[2] leq Flt64.one - flag, "${name.ifEmpty { parentName }}_k_ub"),
                    Pair(k[0] + k[2] geq epsilon * (Flt64.one - flag), "${name.ifEmpty { parentName }}_k_lb")
                )
            }

            Sign.Unequal -> {
                return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
            }
        }

        for ((constraint, name) in constraints) {
            when (val result = model.addConstraint(constraint, name)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
    } else {
        val bin = when (sign) {
            Sign.Less, Sign.LessEqual -> {
                sign(lhs.upperBound!!.value.unwrap(), rhs.constant)

            }

            Sign.Greater, Sign.GreaterEqual -> {
                sign(lhs.lowerBound!!.value.unwrap(), rhs.constant)
            }

            Sign.Equal -> {
                false
            }

            Sign.Unequal -> {
                return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
            }
        }

        when (val result = model.addConstraint(
            flag eq bin,
            "${name.ifEmpty { parentName }}_flag"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        model.tokens.find(flag)?.let { token ->
            token._result = bin.toFlt64()
        }
    }

    return ok
}

internal fun LinearInequality.register(
    parentName: String,
    k: PctVariableView,
    flag: AbstractVariableItem<*, Binary>,
    epsilon: Flt64,
    model: AbstractLinearMechanismModel,
    fixedValues: Map<Symbol, Flt64>
): Try {
    assert(k.size == 3)

    val lhsValue = lhs.evaluate(fixedValues, model.tokens) ?: return register(parentName, k, flag, epsilon, model)
    val bin = sign(lhsValue, rhs.constant)

    if (rhs.constant in lhs.range.valueRange!!) {
        when (val result = model.addConstraint(
            lhs leq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            "${name.ifEmpty { parentName }}_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            lhs geq (k[0] * lhs.lowerBound!!.value.unwrap() + k[1] * rhs.constant + k[2] * lhs.upperBound!!.value.unwrap()),
            "${name.ifEmpty { parentName }}_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        val kValue = when (sign) {
            Sign.Less, Sign.LessEqual -> {
                if (bin) {
                    val pct = (lhsValue - lhs.lowerBound!!.value.unwrap()) / (rhs.constant - lhs.lowerBound!!.value.unwrap())
                    listOf(pct, Flt64.one - pct, Flt64.zero)
                } else {
                    val pct = (lhs.upperBound!!.value.unwrap() - lhsValue) / (lhs.upperBound!!.value.unwrap() - rhs.constant)
                    listOf(Flt64.zero, Flt64.one - pct, pct)
                }
            }

            Sign.Greater, Sign.GreaterEqual -> {
                if (bin) {
                    val pct = (lhs.upperBound!!.value.unwrap() - lhsValue) / (lhs.upperBound!!.value.unwrap() - rhs.constant)
                    listOf(Flt64.zero, Flt64.one - pct, pct)
                } else {
                    val pct = (lhsValue - lhs.lowerBound!!.value.unwrap()) / (rhs.constant - lhs.lowerBound!!.value.unwrap())
                    listOf(pct, Flt64.one - pct, Flt64.zero)
                }
            }

            Sign.Equal -> {
                if (bin) {
                    listOf(Flt64.zero, Flt64.one, Flt64.zero)
                } else if (lhsValue ls rhs.constant) {
                    val pct = (lhsValue - lhs.lowerBound!!.value.unwrap()) / (rhs.constant - lhs.lowerBound!!.value.unwrap())
                    listOf(pct, Flt64.one - pct, Flt64.zero)
                } else {
                    val pct = (lhs.upperBound!!.value.unwrap() - lhsValue) / (lhs.upperBound!!.value.unwrap() - rhs.constant)
                    listOf(Flt64.zero, Flt64.one - pct, pct)
                }
            }

            Sign.Unequal -> {
                return Failed(Err(ErrorCode.ApplicationFailed, "$parentName's inequality sign unsupported: $this"))
            }
        }

        for ((i, value) in kValue.withIndex()) {
            when (val result = model.addConstraint(
                k[i] eq value,
                "${name.ifEmpty { parentName }}_k_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(k[i])?.let { token ->
                token._result = value
            }
        }
    }

    when (val result = model.addConstraint(
        flag eq bin,
        "${name.ifEmpty { parentName }}_flag"
    )) {
        is Ok -> {}

        is Failed -> {
            return Failed(result.error)
        }
    }

    model.tokens.find(flag)?.let { token ->
        token._result = bin.toFlt64()
    }

    return ok
}
