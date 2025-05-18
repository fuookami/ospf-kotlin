package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class AbstractNotFunctionImpl(
    protected val x: AbstractLinearPolynomial<*>,
    protected val parent: LinearFunctionSymbol
) : LinearLogicFunctionSymbol {
    protected abstract val polyY: AbstractLinearPolynomial<*>

    override val discrete = true

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies get() = x.dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    protected val possibleRange
        get() = ValueRange(
            if (x.lowerBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            },
            if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.one
            } else {
                UInt8.zero
            }
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "not(${x.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenList, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenTable, zeroIfNone)
            ?: return null
        return if (value eq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class NotFunctionImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearLogicFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, parent) {
    override val polyY: AbstractLinearPolynomial<*> by lazy {
        Flt64.one - x
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                if (xValue eq Flt64.zero) {
                    Flt64.one
                } else {
                    Flt64.zero
                }
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        return ok
    }
}

class NotFunctionPiecewiseImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearLogicFunctionSymbol,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, parent) {
    private val piecewiseFunction: UnivariateLinearPiecewiseFunction by lazy {
        UnivariateLinearPiecewiseFunction(
            x,
            listOf(
                Point2(Flt64.zero, Flt64.one),
                Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.one),
                Point2(epsilon, Flt64.one),
                Point2(Flt64.one, Flt64.one)
            ),
            "${name}_piecewise"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(piecewiseFunction.b.last())
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        piecewiseFunction.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        piecewiseFunction.prepareAndCache(tokenTable)

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            piecewiseFunction.evaluate(tokenTable)
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = piecewiseFunction.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        when (val result = piecewiseFunction.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

class NotFunctionDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearLogicFunctionSymbol,
    private val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, parent) {
    private val logger = logger()

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val bin = xValue eq Flt64.zero
                val yValue = if (bin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $bin" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }

                yValue
            }
        } else {
            null
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
        when (val result = model.addConstraint(
            x.upperBound!!.value.unwrap() * (Flt64.one - y) geq x,
            "${name}_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                (Flt64.one - y) leq x,
                "${name}_ub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }
}

class NotFunctionExtractAndNotDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearLogicFunctionSymbol,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : AbstractNotFunctionImpl(x, parent) {
    private val logger = logger()

    private val b: PctVar by lazy {
        PctVar("${name}_b")
    }

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = LinearPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val pct = xValue / x.upperBound!!.value.unwrap()
                val bin = xValue eq Flt64.zero
                val yValue = if (bin) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting BinaryzationFunction ${name}.b initial solution: $pct" }
                tokenTable.find(b)?.let { token ->
                    token._result = pct
                }
                logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $bin" }
                tokenTable.find(y)?.let { token ->
                    token._result = yValue
                }

                yValue
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(b)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
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
        when (val result = model.addConstraint(
            x eq x.upperBound!!.value.unwrap() * b,
            "${name}_xb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            (Flt64.one - y) geq b,
            "${name}_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            (Flt64.one - y) leq (Flt64.one / epsilon) * b,
            "${name}_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

class NotFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    private val epsilon: Flt64 = Flt64(1e-6),
    private val piecewise: Boolean = false,
    impl: AbstractNotFunctionImpl? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearLogicFunctionSymbol {
    companion object {
        val piecewiseThreshold: Flt64 = Flt64(1e-5)
    }

    private val impl: AbstractNotFunctionImpl by lazy {
        impl ?: if (x.discrete && ValueRange(Flt64.zero, Flt64.one).value!! contains x.range.range!!) {
            NotFunctionImpl(x, this, name, displayName)
        } else if (x.discrete) {
            NotFunctionDiscreteImpl(x, this, extract, name, displayName)
        } else if (extract && !x.discrete && (piecewise || epsilon geq piecewiseThreshold)) {
            NotFunctionPiecewiseImpl(x, this, epsilon, name, displayName)
        } else {
            NotFunctionExtractAndNotDiscreteImpl(x, this, epsilon, name, displayName)
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
            "not(${x.toTidyRawString(unfold - UInt64.one)})"
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
