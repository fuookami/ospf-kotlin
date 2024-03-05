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
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class FirstFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private lateinit var bins: SymbolCombination<BinaryzationFunction, LinearMonomialCell, Linear, Shape1>
    private val y: BinVariable1 = BinVariable1("${name}_first", Shape1(polynomials.size))
    private lateinit var polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    operator fun get(i: Int): BinVariable {
        return y[i]
    }

    private val possibleRange : ValueRange<Flt64>
        get() {
            val firstIndex = polynomials.indexOfFirst { it.lowerBound.toFlt64() eq Flt64.one }
            val lastIndex = polynomials.indexOfLast { it.upperBound.toFlt64() eq Flt64.one }
            return ValueRange(
                if (firstIndex != -1) {
                    Flt64(firstIndex)
                } else {
                    Flt64.zero
                },
                if (lastIndex != -1) {
                    Flt64(lastIndex)
                } else {
                    Flt64(polynomials.size)
                },
                Flt64
            )
        }

    override fun flush(force: Boolean) {
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun register(tokenTable: MutableTokenTable<LinearMonomialCell, Linear>): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

        if (!::bins.isInitialized) {
            bins = SymbolCombination("${name}_bin", Shape1(polynomials.size)) { (i, _) ->
                BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
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

        y[0].range.eq(true)
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return Ok(success)
    }

    override fun register(model: Model<LinearMonomialCell, Linear>): Try {
        for (bin in bins) {
            when (val result = bin.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        for (i in polynomials.indices) {
            if (i == 0) {
                continue
            }

            model.addConstraint(
                y[i] geq y[i - 1] - bins[i],
                "${name}_lb_$i"
            )
            model.addConstraint(
                y[i] leq Flt64.one - bins[i],
                "${name}_ub_$i"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "first(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.value(tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.value(results, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }
}
