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

class XorFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    init {
        assert(polynomials.size >= 2)
    }

    private lateinit var maxmin: MaxMinFunction
    private lateinit var minmax: MinMaxFunction
    private lateinit var bins: SymbolCombination<BinaryzationFunction, LinearMonomialCell, Linear, Shape1>
    private lateinit var y: BinVar
    private lateinit var polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            Flt64.zero,
            if (polynomials.all { it.upperBound.toFlt64() eq Flt64.zero }
                || polynomials.all { it.lowerBound.toFlt64() eq Flt64.one }
            ) {
                Flt64.zero
            } else {
                Flt64.one
            },
            Flt64
        )

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

        if (polynomials.size > 2) {
            if (!::minmax.isInitialized) {
                minmax = MinMaxFunction(polynomials, "${name}_minmax")
            }
            when (val result = minmax.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::maxmin.isInitialized) {
                maxmin = MaxMinFunction(polynomials, "${name}_maxmin")
            }
            when (val result = maxmin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (!::bins.isInitialized) {
                bins = SymbolCombination("${name}_bin", Shape1(2)) { (i, _) ->
                    if (i == 0) {
                        BinaryzationFunction(LinearPolynomial(minmax), name = "${name}_bin_$i")
                    } else {
                        BinaryzationFunction(LinearPolynomial(maxmin), name = "${name}_bin_$i")
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
        } else {
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

        for ((i, bin) in bins.withIndex()) {
            model.addConstraint(
                y geq bin - sum(bins.withIndex().mapNotNull {
                    if (it.index == i) {
                        null
                    } else {
                        it.value
                    }
                }),
                "${name}_yb_$i"
            )
        }

        if (extract) {
            model.addConstraint(
                y leq sum(bins),
                "${name}_y_1"
            )

            model.addConstraint(
                y leq Flt64(bins.size) - sum(bins),
                "${name}_y_2"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "xor(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.value(tokenList, zeroIfNone)
                ?: return null
            if (result eq Flt64.zero) {
                zero = true
            }
            if (result eq Flt64.one) {
                one = true
            }
            if (zero && one) {
                return Flt64.one
            }
        }
        return Flt64.zero
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.value(results, tokenList, zeroIfNone)
                ?: return null
            if (result eq Flt64.zero) {
                zero = true
            }
            if (result eq Flt64.one) {
                one = true
            }
            if (zero && one) {
                return Flt64.one
            }
        }
        return Flt64.zero
    }
}
