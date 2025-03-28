package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class XorFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    private val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private val logger = logger()

    init {
        assert(polynomials.size >= 2)
    }

    private val maxmin: MaxMinFunction by lazy {
        MaxMinFunction(polynomials, "${name}_maxmin")
    }

    private val minmax: MinMaxFunction by lazy {
        MinMaxFunction(polynomials, "${name}_minmax")
    }

    private val bins: SymbolCombination<BinaryzationFunction, Shape1> by lazy {
        if (polynomials.size > 2) {
            SymbolCombination("${name}_bin", Shape1(2)) { i, _ ->
                if (i == 0) {
                    BinaryzationFunction(LinearPolynomial(minmax), name = "${name}_bin_$i")
                } else {
                    BinaryzationFunction(LinearPolynomial(maxmin), name = "${name}_bin_$i")
                }
            }
        } else {
            SymbolCombination("${name}_bin", Shape1(polynomials.size)) { i, _ ->
                BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
            }
        }
    }

    private val y: BinVar by lazy {
        BinVar(name = "${name}_y")
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y, "${name}_y")
        polyY.range.set(possibleRange)
        polyY
    }

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            for (polynomial in polynomials) {
                dependencies.addAll(polynomial.dependencies)
            }
            return dependencies
        }
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    private val possibleRange
        get() = ValueRange(
            Flt64.zero,
            if (polynomials.all { it.upperBound!!.value.unwrap() eq Flt64.zero }
                || polynomials.all { it.lowerBound!!.value.unwrap() eq Flt64.one }
            ) {
                Flt64.zero
            } else {
                Flt64.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        if (polynomials.size > 2) {
            maxmin.flush(force)
            minmax.flush(force)
        }
        for (bin in bins) {
            bin.flush(force)
        }
        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        if (polynomials.size > 2) {
            maxmin.prepare(tokenTable)
            minmax.prepare(tokenTable)
        }
        for (bin in bins) {
            bin.prepare(tokenTable)
        }

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            var zero = false
            var one = false
            for (polynomial in polynomials) {
                val result = polynomial.evaluate(tokenTable) ?: return
                if (result eq Flt64.zero) {
                    zero = true
                }
                if (result eq Flt64.one) {
                    one = true
                }
                if (zero && one) {
                    break
                }
            }
            val bin = zero && one
            val yValue = if (bin) {
                Flt64.one
            } else {
                Flt64.zero
            }

            logger.trace { "Setting XorFunction ${name}.y initial solution: $bin" }
            tokenTable.find(y)?.let { token ->
                token._result = yValue
            }

            tokenTable.cache(this, null, yValue)
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound!!.value.unwrap() ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

        if (polynomials.size > 2) {
            when (val result = minmax.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = maxmin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
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
            for (bin in bins) {
                when (val result = bin.register(tokenTable)) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        for (bin in bins) {
            when (val result = bin.register(model)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        for ((i, bin) in bins.withIndex()) {
            when (val result = model.addConstraint(
                y geq bin - sum(bins.withIndex().mapNotNull {
                    if (it.index == i) {
                        null
                    } else {
                        it.value
                    }
                }),
                "${name}_yb_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                y leq sum(bins),
                "${name}_y_1"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                y leq Flt64(bins.size) - sum(bins),
                "${name}_y_2"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "xor(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.evaluate(tokenList, zeroIfNone)
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

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.evaluate(results, tokenList, zeroIfNone)
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

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.evaluate(tokenTable, zeroIfNone)
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

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var zero = false
        var one = false
        for (polynomial in polynomials) {
            val result = polynomial.evaluate(results, tokenTable, zeroIfNone)
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
