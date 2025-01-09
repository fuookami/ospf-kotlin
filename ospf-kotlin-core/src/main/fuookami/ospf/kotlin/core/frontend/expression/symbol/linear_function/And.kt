package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class AbstractAndFunctionImpl(
    protected val polynomials: List<AbstractLinearPolynomial<*>>,
    protected val parent: LinearLogicFunctionSymbol
) : LinearLogicFunctionSymbol {
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

    protected val possibleRange
        get() = ValueRange(
            if (polynomials.any { it.lowerBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            },
            if (polynomials.any { it.upperBound!!.value.unwrap() eq Flt64.zero }) {
                Flt64.zero
            } else {
                Flt64.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        for (polynomial in polynomials) {
            polynomial.flush(force)
        }

        polyY.flush(force)
        polyY.range.set(possibleRange)
    }

    override fun toRawString(unfold: Boolean): String {
        return "and(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.evaluate(tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.evaluate(results, tokenList, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.evaluate(tokenTable, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return if (polynomials.all {
                val thisValue = it.evaluate(results, tokenTable, zeroIfNone) ?: return null
                thisValue neq Flt64.zero
            }) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

private class AndFunctionOnePolynomialImpl(
    val polynomial: AbstractLinearPolynomial<*>,
    parent: LinearLogicFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(listOf(polynomial), parent) {
    private val bin: BinaryzationFunction by lazy {
        BinaryzationFunction(polynomial, name = "${name}_bin")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(bin)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        bin.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        polynomial.cells
        bin.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            bin.evaluate(tokenTable)?.let { binValue ->
                tokenTable.cache(parent, null, binValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = bin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = bin.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class AndFunctionMultiPolynomialImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: LinearLogicFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(polynomials, parent) {
    private val maxmin: MaxMinFunction by lazy {
        MaxMinFunction(polynomials, "${name}_maxmin")
    }

    private val bin: BinaryzationFunction by lazy {
        BinaryzationFunction(LinearPolynomial(maxmin), name = "${name}_bin")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(bin)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        maxmin.flush(force)
        bin.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }
        maxmin.prepare(tokenTable)
        bin.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            bin.evaluate(tokenTable)?.let { binValue ->
                tokenTable.cache(parent, null, binValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = maxmin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = maxmin.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = bin.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

private class AndFunctionMultiPolynomialBinaryImpl(
    polynomials: List<AbstractLinearPolynomial<*>>,
    parent: LinearLogicFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractAndFunctionImpl(polynomials, parent) {
    private val y: BinVar by lazy {
        BinVar("${name}_y")
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange)
        polyY
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        for (polynomial in polynomials) {
            polynomial.cells
        }

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            val yValue = polynomials.all { polynomial ->
                val thisValue = polynomial.evaluate(tokenTable) ?: return
                thisValue eq Flt64.zero
            }
            logger.trace { "Setting AndFunction ${name}.y to $yValue" }
            tokenTable.find(y)?.let { token ->
                token._result = if (yValue) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            }

            tokenTable.cache(
                parent, null, if (yValue) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            )
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(y)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        // if any polynomial is zero, y will be zero
        for ((i, polynomial) in polynomials.withIndex()) {
            when (val result = model.addConstraint(
                y leq polynomial,
                "${name}_ub_${polynomial.name.ifEmpty { "$i" }}"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }
        // if all polynomial are not zero, y will be not zero
        when (val result = model.addConstraint(
            y geq (sum(polynomials) - Flt64(polynomials.size - 1)),
            "${name}_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

class AndFunction(
    private val polynomials: List<AbstractLinearPolynomial<*>>,
    override var name: String,
    override var displayName: String? = null,
    impl: AbstractAndFunctionImpl? = null
) : LinearLogicFunctionSymbol {
    private val impl: AbstractAndFunctionImpl by lazy {
        impl ?: if (polynomials.size == 1) {
            AndFunctionOnePolynomialImpl(polynomials[0], this, name, displayName)
        } else if (polynomials.all { it.discrete && it.upperBound!!.value.unwrap() leq Flt64.one }) {
            AndFunctionMultiPolynomialBinaryImpl(polynomials, this, name, displayName)
        } else {
            AndFunctionMultiPolynomialImpl(polynomials, this, name, displayName)
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

    override fun prepare(tokenTable: AbstractTokenTable) {
        impl.prepare(tokenTable)
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        // all polys must be ∈ (R - R-)
        for (polynomial in polynomials) {
            if (polynomial.lowerBound!!.value.unwrap() ls Flt64.zero) {
                return Failed(Err(ErrorCode.ApplicationFailed, "$name's domain of definition unsatisfied: $polynomial"))
            }
        }

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

    override fun toRawString(unfold: Boolean): String {
        return "and(${polynomials.joinToString(", ") { it.toRawString(unfold) }})"
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
