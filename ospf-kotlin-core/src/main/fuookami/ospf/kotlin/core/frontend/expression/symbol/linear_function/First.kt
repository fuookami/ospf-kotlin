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
    override val parent: IntermediateSymbol? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    companion object {
        operator fun invoke(
            polynomials: List<ToLinearPolynomial<*>>,
            parent: IntermediateSymbol? = null,
            name: String,
            displayName: String? = null
        ): FirstFunction {
            return FirstFunction(
                polynomials = polynomials.map { it.toLinearPolynomial() },
                parent = parent,
                name = name,
                displayName = displayName
            )
        }
    }

    private val bins: SymbolCombination<BinaryzationFunction, Shape1> by lazy {
        SymbolCombination("${name}_bin", Shape1(polynomials.size)) { i, _ ->
            BinaryzationFunction(polynomials[i], parent = parent ?: this, name = "${name}_bin_$i")
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

    override fun prepare(values: Map<Symbol, Flt64>?, tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        tokenTable.cache(
            bins.mapNotNull {
                val value = if (values.isNullOrEmpty()) {
                    it.prepare(null, tokenTable)
                } else {
                    it.prepare(values, tokenTable)
                }
                if (value != null) {
                    (it as IntermediateSymbol) to value
                } else {
                    null
                }
            }.toMap()
        )

        return if ((!values.isNullOrEmpty() || tokenTable.cachedSolution) && if (values.isNullOrEmpty()) {
            tokenTable.cached(this)
        } else {
            tokenTable.cached(this, values)
        } == false) {
            var first: Int? = null
            polynomials.withIndex().forEach { (i, polynomial) ->
                if (first == null) {
                    val value = if (values.isNullOrEmpty()) {
                        polynomial.evaluate(tokenTable)
                    } else {
                        polynomial.evaluate(values, tokenTable)
                    } ?: return@forEach

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
                } else {
                    logger.trace { "Setting FirstFunction ${name}.y[$i] initial solution: false" }
                    tokenTable.find(y[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                }
            }

            Flt64(first ?: polynomials.size)
        } else {
            null
        }
    }

    override fun register(tokenTable: AddableTokenCollection): Try {
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
            when (val result = model.addConstraint(
                y[i] leq bins[i],
                name = "${name}_ub1_$i",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (i == 0) {
                when (val result = model.addConstraint(
                    y[i] geq bins[i],
                    name = "${name}_lb_0",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else {
                when (val result = model.addConstraint(
                    y[i] geq bins[i] - sum((0 until i).map { y[it] }),
                    name = "${name}_lb_$i",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
                when (val result = model.addConstraint(
                    y[i] leq y[i - 1],
                    name = "${name}_y_$i",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }
        }

        return ok
    }

    override fun register(
        tokenTable: AddableTokenCollection,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        for (bin in bins) {
            when (val result = bin.register(tokenTable, fixedValues)) {
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

    override fun register(
        model: AbstractLinearMechanismModel,
        fixedValues: Map<Symbol, Flt64>
    ): Try {
        val first = bins.indexOfFirst {
            val bin = it.evaluate(fixedValues, model.tokens) ?: return register(model)
            bin gr Flt64.zero
        }

        for (i in polynomials.indices) {
            when (val result = model.addConstraint(
                y[i] leq bins[i],
                "${name}_ub1_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (i == 0) {
                when (val result = model.addConstraint(
                    y[i] geq bins[i],
                    name = "${name}_lb_0",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            } else if (i < first) {
                when (val result = model.addConstraint(
                    y[i] geq bins[i] - sum((0 until i).map { y[it] }),
                    name = "${name}_lb_$i",
                    from = parent ?: this
                )) {
                    is Ok -> {}

                    is Failed -> {
                        return Failed(result.error)
                    }
                }
            }

            when (val result = model.addConstraint(
                y[i] eq (i == first),
                name = "${name}_y",
                from = parent ?: this
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            model.tokens.find(y[i])?.let { token ->
                token._result = (i == first).toFlt64()
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

    override fun evaluate(
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun evaluate(
        results: List<Flt64>,
        tokenList: AbstractTokenList,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(results, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun evaluate(
        values: Map<Symbol, Flt64>,
        tokenList: AbstractTokenList?,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(values, tokenList, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun calculateValue(
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun calculateValue(
        results: List<Flt64>,
        tokenTable: AbstractTokenTable,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(results, tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }

    override fun calculateValue(
        values: Map<Symbol, Flt64>,
        tokenTable: AbstractTokenTable?,
        zeroIfNone: Boolean
    ): Flt64? {
        for ((i, polynomial) in polynomials.withIndex()) {
            val value = polynomial.evaluate(values, tokenTable, zeroIfNone) ?: return null
            if (value neq Flt64.zero) {
                return Flt64(i)
            }
        }
        return -Flt64.one
    }
}
