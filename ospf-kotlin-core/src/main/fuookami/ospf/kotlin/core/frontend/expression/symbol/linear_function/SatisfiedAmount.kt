package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class AbstractSatisfiedAmountPolynomialFunctionImpl(
    protected val amount: UInt64? = null,
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    protected val parent: LinearFunctionSymbol
) : LinearFunctionSymbol {
    protected abstract val polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category get() = Linear

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

    protected val possibleRange: ValueRange<Flt64>
        get() {
            val minAmount = UInt64(polynomials.count { it.lowerBound!!.value.unwrap() neq Flt64.zero })
            val maxAmount = UInt64(polynomials.size - polynomials.count { it.upperBound!!.value.unwrap() eq Flt64.zero })
            return if (amount != null) {
                if (minAmount geq amount) {
                    ValueRange(Flt64.one, Flt64.one).value!!
                } else if (maxAmount ls amount) {
                    ValueRange(Flt64.zero, Flt64.zero).value!!
                } else {
                    ValueRange(Flt64.zero, Flt64.one).value!!
                }
            } else {
                ValueRange(minAmount.toFlt64(), maxAmount.toFlt64()).value!!
            }
        }

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }

        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        return null
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            if (amount != null) {
                "satisfied_amount_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            } else {
                "satisfied_amount(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(tokenList, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(results, tokenList, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(tokenTable, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        var counter = UInt64.zero
        for (polynomial in polynomials) {
            val value = polynomial.evaluate(results, tokenTable, zeroIfNone)
                ?: return null
            if (value neq Flt64.zero) {
                counter += UInt64.one
            }
        }
        return if (amount != null) {
            if (counter geq amount) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            counter.toFlt64()
        }
    }
}

private class SatisfiedAmountPolynomialFunctionAnyImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: LinearFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(UInt64.one, polynomials, parent) {
    private val or: OrFunction by lazy {
        OrFunction(polynomials, name, displayName)
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(or)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        or.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        super.prepareAndCache(tokenTable)
        or.prepareAndCache(tokenTable)

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            val bin = or.evaluate(tokenTable) ?: return null
            if (bin eq Flt64.one) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = or.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = or.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class SatisfiedAmountPolynomialFunctionAllImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: LinearFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(UInt64(polynomials.size), polynomials, parent) {
    private val and: AndFunction by lazy {
        AndFunction(polynomials, name, displayName)
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(and)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        and.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        super.prepareAndCache(tokenTable)
        and.prepareAndCache(tokenTable)

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            val bin = and.evaluate(tokenTable) ?: return null
            if (bin eq Flt64.one) {
                Flt64.one
            } else {
                Flt64.zero
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = and.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = and.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class SatisfiedAmountPolynomialFunctionSomeImpl(
    amount: UInt64?,
    polynomials: List<AbstractLinearPolynomial<*>>,
    private val extract: Boolean = true,
    parent: LinearFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunctionImpl(amount, polynomials, parent) {
    private val logger = logger()

    private val bins: SymbolCombination<BinaryzationFunction, Shape1> by lazy {
        SymbolCombination("${name}_bin", Shape1(polynomials.size)) { i, _ ->
            BinaryzationFunction(polynomials[i], name = "${name}_bin_$i")
        }
    }

    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        if (amount != null) {
            val polyY = LinearPolynomial(y)
            polyY.range.set(possibleRange)
            polyY
        } else {
            val polyY = sum(bins)
            polyY.range.set(possibleRange)
            polyY
        }
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        for (bin in bins) {
            bin.flush(force)
        }
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        super.prepareAndCache(tokenTable)
        tokenTable.cache(
            bins.mapNotNull {
                val value = it.prepare(tokenTable)
                if (value != null) {
                    (it as IntermediateSymbol) to value
                } else {
                    null
                }
            }.toMap()
        )

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            val count = bins.count {
                val value = it.evaluate(tokenTable) ?: return null
                value eq Flt64.one
            }

            val yValue = if (amount != null) {
                val bin = UInt64(count) >= amount
                val yValue = if (bin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting SatisfiedAmountPolynomialFunction ${name}.y to $bin" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }
                yValue
            } else {
                Flt64(count)
            }

            yValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        for (bin in bins) {
            when (val result = bin.register(tokenTable)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        if (amount != null) {
            when (val result = tokenTable.add(y)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
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

        if (amount != null) {
            when (val result = model.addConstraint(
                y geq (sum(bins) - amount + UInt64.one) / UInt64(polynomials.size),
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            if (extract) {
                when (val result = model.addConstraint(
                    y leq sum(bins) / amount,
                    "${name}_lb"
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
}

sealed class AbstractSatisfiedAmountPolynomialFunction(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    private val extract: Boolean = true,
    impl: AbstractSatisfiedAmountPolynomialFunctionImpl? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    open val amount: UInt64? = null

    private val impl: AbstractSatisfiedAmountPolynomialFunctionImpl by lazy {
        impl ?: when (amount) {
            UInt64.one -> {
                SatisfiedAmountPolynomialFunctionAnyImpl(polynomials, this, name, displayName)
            }

            UInt64(polynomials.size) -> {
                SatisfiedAmountPolynomialFunctionAllImpl(polynomials, this, name, displayName)
            }

            else -> {
                SatisfiedAmountPolynomialFunctionSomeImpl(amount, polynomials, extract, this, name, displayName)
            }
        }
    }

    override val discrete = true

    override val range get() = impl.range
    override val lowerBound get() = impl.lowerBound
    override val upperBound get() = impl.upperBound

    override val category get() = Linear

    override val dependencies: Set<IntermediateSymbol> get() = impl.dependencies
    override val cells get() = impl.cells
    override val cached get() = impl.cached

    override fun flush(force: Boolean) {
        impl.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        return impl.prepare(tokenTable)
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = impl.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = impl.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
            if (amount != null) {
                "satisfied_amount_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            } else {
                "satisfied_amount(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
            }
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return impl.evaluate(tokenList, zeroIfNone)
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return impl.evaluate(results, tokenList, zeroIfNone)
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return impl.calculateValue(tokenTable, zeroIfNone)
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return impl.calculateValue(results, tokenTable, zeroIfNone)
    }
}

class SatisfiedAmountPolynomialFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    impl: AbstractSatisfiedAmountPolynomialFunctionImpl? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunction(polynomials, impl = impl, name = name, displayName = displayName)

class AtLeastPolynomialFunction(
    polynomials: List<AbstractLinearPolynomial<*>>,
    override val amount: UInt64,
    extract: Boolean = true,
    impl: AbstractSatisfiedAmountPolynomialFunctionImpl? = null,
    name: String,
    displayName: String? = null
) : AbstractSatisfiedAmountPolynomialFunction(polynomials, extract, impl = impl, name = name, displayName = displayName), LinearLogicFunctionSymbol {
    init {
        assert(amount != UInt64.zero)
        assert(UInt64(polynomials.size) geq amount)
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "at_least_${amount}(${polynomials.joinToString(", ") { it.toTidyRawString(unfold - UInt64.one) }})"
        }
    }
}

data object SatisfiedAmountFunction {
    operator fun invoke(
        polynomials: List<AbstractLinearPolynomial<*>>,
        impl: AbstractSatisfiedAmountPolynomialFunctionImpl? = null,
        name: String,
        displayName: String? = null
    ): SatisfiedAmountPolynomialFunction {
        return SatisfiedAmountPolynomialFunction(polynomials, impl = impl, name = name, displayName = displayName)
    }

    operator fun invoke(
        inequalities: List<LinearInequality>,
        name: String,
        displayName: String? = null
    ): SatisfiedAmountInequalityFunction {
        return SatisfiedAmountInequalityFunction(inequalities, name = name, displayName = displayName)
    }
}

data object AtLeastFunction {
    operator fun invoke(
        polynomials: List<AbstractLinearPolynomial<*>>,
        amount: UInt64,
        extract: Boolean = true,
        impl: AbstractSatisfiedAmountPolynomialFunctionImpl? = null,
        name: String,
        displayName: String? = null
    ): AtLeastPolynomialFunction {
        return AtLeastPolynomialFunction(polynomials, amount, extract, impl = impl, name = name, displayName = displayName)
    }

    operator fun invoke(
        inequalities: List<LinearInequality>,
        constraint: Boolean = true,
        amount: UInt64,
        name: String,
        displayName: String? = null
    ): AtLeastInequalityFunction {
        return AtLeastInequalityFunction(inequalities, constraint, amount, name = name, displayName = displayName)
    }
}
