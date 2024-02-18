package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractSatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    private val constraint: Boolean = false,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    open val amount: ValueRange<UInt64>? = null

    protected val inequalities by lazy { inequalities.map { it.normalize() } }

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
            if (!constraint) {
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
                    polyY = LinearPolynomial(y)
                    polyY.range.set(possibleRange)
                }
            } else {
                polyY = LinearPolynomial(1)
                polyY.range.set(possibleRange)
            }
        } else {
            if (!::polyY.isInitialized) {
                polyY = sum(u)
                polyY.range.set(possibleRange)
            }
        }

        return Ok(success)
    }

    override fun register(model: Model<LinearMonomialCell, Linear>): Try {
        for ((i, inequality) in inequalities.withIndex()) {
            when (val result = inequality.register(name, u[i], model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (::y.isInitialized) {
            model.addConstraint(
                sum(u) geq amount!!.lowerBound.toFlt64() - UInt64(inequalities.size) * (Flt64.one - y),
                "${name}_lb"
            )

            model.addConstraint(
                sum(u) leq amount!!.upperBound.toFlt64() + UInt64(inequalities.size) * (Flt64.one - y),
                "${name}_ub"
            )
        } else {
            model.addConstraint(
                sum(u) geq amount!!.lowerBound.toFlt64(),
                "${name}_lb"
            )

            model.addConstraint(
                sum(u) leq amount!!.upperBound.toFlt64(),
                "${name}_ub"
            )
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
            if (amount!!.contains(counter)) {
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
            if (amount!!.contains(counter)) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }
}

class AnyFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64.one, UInt64(inequalities.size), UInt64)

    override fun toRawString(unfold: Boolean): String {
        return "any(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class NotAllFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64.one, UInt64(inequalities.size - 1), UInt64)

    override fun toRawString(unfold: Boolean): String {
        return "for_all(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class AllFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName),
    LinearLogicFunctionSymbol {
    override val amount: ValueRange<UInt64> = ValueRange(UInt64(inequalities.size), UInt64(inequalities.size), UInt64)

    override fun toRawString(unfold: Boolean): String {
        return "for_all(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class SatisfiedAmountInequalityFunction(
    inequalities: List<LinearInequality>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName)

class AtLeastInequalityFunction(
    inequalities: List<LinearInequality>,
    constraint: Boolean = true,
    amount: UInt64,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, constraint, name, displayName), LinearLogicFunctionSymbol {
    init {
        assert(amount != UInt64.zero)
        assert(UInt64(inequalities.size) geq amount)
    }

    override val amount: ValueRange<UInt64> = ValueRange(amount, UInt64(inequalities.size), UInt64)

    override fun toRawString(unfold: Boolean): String {
        return "at_least_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}

class NumerableFunction(
    inequalities: List<LinearInequality>,
    override val amount: ValueRange<UInt64>,
    constraint: Boolean = true,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountInequalityFunction(inequalities, constraint, name, displayName), LinearLogicFunctionSymbol {
    override fun toRawString(unfold: Boolean): String {
        return "numerable_${amount}(${inequalities.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
