package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.inequality.Sign
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    private val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    open val amount: UInt64? = null

    protected val inequalities = inequalities.map { inequality ->
        LinearInequality(
            lhs = LinearPolynomial(inequality.lhs.monomials.map { it.copy() } + inequality.rhs.monomials.map { -it }),
            rhs = LinearPolynomial(-inequality.lhs.constant + inequality.rhs.constant),
            sign = inequality.sign,
            name = inequality.name,
            displayName = inequality.displayName
        )
    }

    private lateinit var u: BinVariable1
    private lateinit var y: BinVar
    private lateinit var polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange: ValueRange<Flt64>
        get() {
            // todo: impl by Inequality.judge()
            return if (amount != null) {
                ValueRange(Flt64.zero, Flt64.one, Flt64)
            } else {
                ValueRange(Flt64.zero, Flt64(inequalities.size), Flt64)
            }
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (!::u.isInitialized) {
            u = BinVariable1("${name}_u", Shape1(inequalities.size))
        }
        when (val result = tokenTable.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (amount != null) {
            if (!::y.isInitialized) {
                y = BinVar("${name}_y")
            }
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::polyY.isInitialized) {
                polyY = sum(u)
                polyY.range.set(possibleRange)
            }
        } else {
            if (!::polyY.isInitialized) {
                polyY = LinearPolynomial(y)
                polyY.range.set(possibleRange)
            }
        }

        return Ok(success)
    }

    override fun register(model: Model<LinearMonomialCell, Linear>): Try {
        for ((i, inequality) in inequalities.withIndex()) {
            when (inequality.sign) {
                Sign.Less, Sign.LessEqual -> {
                    val m = inequality.lhs.upperBound - inequality.rhs.constant
                    model.addConstraint(
                        inequality.lhs leq inequality.rhs + m * (Flt64.one - u[i]),
                        inequality.name.ifEmpty { "${name}_$i" }
                    )
                }

                Sign.Greater, Sign.GreaterEqual -> {
                    val m = inequality.rhs.constant - inequality.lhs.lowerBound
                    model.addConstraint(
                        inequality.lhs geq inequality.rhs - m * (Flt64.one - u[i]),
                        inequality.name.ifEmpty { "${name}_$i" }
                    )
                }

                Sign.Equal -> {
                    val m1 = inequality.lhs.upperBound - inequality.rhs.constant
                    val m2 = inequality.rhs.constant - inequality.lhs.lowerBound
                    model.addConstraint(
                        inequality.lhs leq inequality.rhs + m1 * (Flt64.one - u[i]),
                        inequality.name.ifEmpty { "${name}_${i}_ub" }
                    )
                    model.addConstraint(
                        inequality.lhs geq inequality.rhs + m2 * (Flt64.one - u[i]),
                        inequality.name.ifEmpty { "${name}_${i}_lb" }
                    )
                }

                Sign.Unequal -> {
                    return Failed(Err(ErrorCode.ApplicationFailed, "$name's inequality sign unsupported: $inequality"))
                }
            }
        }

        if (::y.isInitialized) {
            model.addConstraint(
                y geq (sum(u) - amount!! + UInt64.one) / UInt64(inequalities.size),
                "${name}_ub"
            )

            if (extract) {
                model.addConstraint(
                    y leq sum(u) / amount!!,
                    "${name}_lb"
                )
            }
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return if (amount != null) {
            "satisfied_amount_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
        } else {
            "satisfied_amount(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(tokenList, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount!!) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (inequality in inequalities) {
            if (inequality.isTrue(results, tokenList, zeroIfNone) ?: return null) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount!!) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }
}

class OrInequalityFunction(
    inequalities: List<LinearInequality>,
    extract: Boolean = true,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, extract, name, displayName), LinearLogicFunctionSymbol {
    override val amount: UInt64 = UInt64.one

    override fun toRawString(unfold: Boolean): String {
        return "or(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName)

class AtLeastInequalityFunction(
    inequalities: List<LinearInequality>,
    override val amount: UInt64,
    extract: Boolean = true,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, extract, name, displayName), LinearLogicFunctionSymbol {
    init {
        assert(amount != UInt64.zero)
        assert(UInt64(inequalities.size) geq amount)
    }

    override fun toRawString(unfold: Boolean): String {
        return "at_least_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
