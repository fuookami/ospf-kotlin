package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class AbstractBinaryzationFunctionImpl(
    protected val x: AbstractQuadraticPolynomial<*>,
    protected val parent: QuadraticLogicFunctionSymbol
) : QuadraticLogicFunctionSymbol {
    protected abstract val polyY: AbstractQuadraticPolynomial<*>

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
                UInt8.zero
            } else {
                UInt8.one
            },
            if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                UInt8.zero
            } else {
                UInt8.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun toRawString(unfold: UInt64): String {
        return "bin(${x.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenList, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenTable, zeroIfNone)
            ?: return null
        return if (value neq Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class BinaryzationFunctionImpl(
    x: AbstractQuadraticPolynomial<*>,
    parent: QuadraticLogicFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, parent) {
    override val polyY: AbstractQuadraticPolynomial<*> by lazy {
        x.copy()
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val yValue = if (xValue gr Flt64.zero) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                yValue
            }
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        return ok
    }
}

class BinaryzationFunctionLinearImpl(
    x: AbstractQuadraticPolynomial<*>,
    parent: QuadraticLogicFunctionSymbol,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : AbstractBinaryzationFunctionImpl(x, parent) {
    private val linearX: LinearFunction by lazy {
        LinearFunction(x, "${name}_linear")
    }

    private val y: BinVar by lazy {
        val y = BinVar("${name}_y")
        y.range.set(possibleRange)
        y
    }

    override val polyY: AbstractQuadraticPolynomial<*> by lazy {
        val polyY = QuadraticPolynomial(y)
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        linearX.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        linearX.prepareAndCache(tokenTable)

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            linearX.evaluate(tokenTable)?.let { xValue ->
                val yValue = if (xValue gr Flt64.zero) {
                    Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting BinaryzationFunction ${name}.y initial solution: $yValue" }
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

        when (val result = linearX.register(tokenTable)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = linearX.register(model)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        model.addConstraint(
            (Flt64.one - y) * linearX leq x.upperBound!!.value.unwrap() * y,
            "${name}_ub"
        )
        model.addConstraint(
            x geq epsilon * y,
            "${name}_lb"
        )
        return ok
    }
}

class BinaryzationFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    private val epsilon: Flt64 = Flt64(1e-6),
    impl: AbstractBinaryzationFunctionImpl? = null,
    override var name: String,
    override var displayName: String? = null
) : QuadraticLogicFunctionSymbol {
    private val logger = logger()

    private val impl: AbstractBinaryzationFunctionImpl by lazy {
        impl ?: if (x.discrete && ValueRange(Flt64.zero, Flt64.one).value!! contains x.range.range!!) {
            BinaryzationFunctionImpl(x, this, name, displayName)
        } else {
            BinaryzationFunctionLinearImpl(x, this, epsilon, name, displayName)
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

    override fun register(model: AbstractQuadraticMechanismModel): Try {
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
            "bin(${x.toTidyRawString(unfold - UInt64.one)})"
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
