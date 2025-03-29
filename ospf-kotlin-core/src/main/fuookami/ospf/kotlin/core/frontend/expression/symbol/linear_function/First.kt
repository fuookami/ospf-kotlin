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

class FirstFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    private val bins: SymbolCombination<BinaryzationFunction, Shape1> by lazy {
        SymbolCombination("${name}_bin", Shape1(polynomials.size)) { i, _ ->
            BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
        }
    }

    private val y: BinVariable1 by lazy {
        BinVariable1("${name}_first", Shape1(polynomials.size))
    }

    private val polyY: AbstractLinearPolynomial<*> by lazy {
        sum(y)
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

    private val possibleRange: ValueRange<Flt64>
        get() {
            val firstIndex = polynomials.indexOfFirst { it.lowerBound!!.value.unwrap() eq Flt64.one }
            val lastIndex = polynomials.indexOfLast { it.upperBound!!.value.unwrap() eq Flt64.one }
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
                }
            ).value!!
        }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
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
        for (bin in bins) {
            bin.prepare(tokenTable)
        }

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            var first: Int? = null
            polynomials.withIndex().forEach { (i, polynomial) ->
                if (first == null) {
                    polynomial.evaluate(tokenTable)?.let { value ->
                        val bin = value gr Flt64.zero

                        if (bin) {
                            first = i
                        }
                        logger.trace { "Setting FirstFunction ${name}.y[$i] initial solution: $bin" }
                        tokenTable.find(y[i])?.let { token ->
                            token._result = if (bin) {
                                Flt64.zero
                            } else {
                                Flt64.one
                            }
                        }
                    }
                } else {
                    logger.trace { "Setting FirstFunction ${name}.y[$i] initial solution: false" }
                    tokenTable.find(y[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                }
            }

            tokenTable.cache(this, null, Flt64(first ?: polynomials.size))
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound!!.value.unwrap() ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
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

        for (i in polynomials.indices) {
            if (i == 0) {
                continue
            }

            when (val result = model.addConstraint(
                y[i] geq y[i - 1] - bins[i],
                "${name}_lb_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y[i] leq y[i - 1],
                "${name}_ub0_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                y[i] leq Flt64.one - bins[i],
                "${name}_ub1_$i"
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
            "first(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(results, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(results, tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }
}
