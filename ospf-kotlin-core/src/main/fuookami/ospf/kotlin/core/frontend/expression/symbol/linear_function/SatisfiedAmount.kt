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

sealed class AbstractSatisfiedAmountFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    private val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    open val amount: UInt64? = null

    private lateinit var or: OrFunction
    private lateinit var and: AndFunction
    private lateinit var bins: SymbolCombination<BinaryzationFunction, LinearMonomialCell, Linear, Shape1>
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
            val minAmount = UInt64(polynomials.count { it.lowerBound neq Flt64.zero })
            val maxAmount = UInt64(polynomials.size - polynomials.count { it.upperBound eq Flt64.zero })
            return if (amount != null) {
                if (minAmount geq amount!!) {
                    ValueRange(Flt64.one, Flt64.one, Flt64)
                } else if (maxAmount ls amount!!) {
                    ValueRange(Flt64.zero, Flt64.zero, Flt64)
                } else {
                    ValueRange(Flt64.zero, Flt64.one, Flt64)
                }
            } else {
                ValueRange(minAmount.toFlt64(), maxAmount.toFlt64(), Flt64)
            }
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        if (amount?.let { it == UInt64.one } == true) {
            if (!::or.isInitialized) {
                or = OrFunction(polynomials, name, displayName)
            }
            when (val result = or.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (amount?.let { it == UInt64(polynomials.size) } == true) {
            if (!::and.isInitialized) {
                and = AndFunction(polynomials, name, displayName)
            }
            when (val result = and.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else {
            if (!::bins.isInitialized) {
                bins = SymbolCombination("${name}_bin", Shape1(2)) { (i, _) ->
                    if (i == 0) {
                        BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
                    } else {
                        BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
                    }
                }
            }
            for (bin in bins) {
                when (val result = bin.register(tokenTable)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
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
                    polyY = LinearPolynomial(y)
                    polyY.range.set(possibleRange)
                }
            } else {
                if (!::polyY.isInitialized) {
                    polyY = sum(bins)
                    polyY.range.set(possibleRange)
                }
            }
        }

        return Ok(success)
    }

    override fun register(model: Model<LinearMonomialCell, Linear>): Try {
        if (::or.isInitialized) {
            when (val result = or.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (::and.isInitialized) {
            when (val result = and.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        } else if (::bins.isInitialized) {
            for (bin in bins) {
                when (val result = bin.register(model)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            if (::y.isInitialized) {
                model.addConstraint(
                    y geq (sum(bins) - amount!! + UInt64.one) / UInt64(polynomials.size),
                    "${name}_ub"
                )

                if (extract) {
                    model.addConstraint(
                        y leq sum(bins) / amount!!,
                        "${name}_lb"
                    )
                }
            }
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return if (amount != null) {
            "satisfied_amount_${amount}(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
        } else {
            "satisfied_amount(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
        }
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.value(tokenList, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
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
        for (polynomial in polynomials) {
            val value = polynomial.value(results, tokenList, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
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

class SatisfiedAmountFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountFunction(polynomials, name = name, displayName = displayName)

class AtLeastFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    override val amount: UInt64,
    extract: Boolean = true,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountFunction(polynomials, extract, name, displayName), LinearLogicFunctionSymbol {
    init {
        assert(amount != UInt64.zero)
        assert(UInt64(polynomials.size) geq amount)
    }

    override fun toRawString(unfold: Boolean): String {
        return "at_least_${amount}(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }
}
