package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

abstract class AbstractBalanceTernaryzationFunctionImpl(
    protected val x: AbstractLinearPolynomial<*>,
    protected val parent: LinearFunctionSymbol
) : LinearFunctionSymbol {
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
            if (x.lowerBound!!.value.unwrap() ls Flt64.zero) {
                -Int8.one
            } else if (x.lowerBound!!.value.unwrap() eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
            },
            if (x.upperBound!!.value.unwrap() ls Flt64.zero) {
                -Int8.one
            } else if (x.upperBound!!.value.unwrap() eq Flt64.zero) {
                Int8.zero
            } else {
                Int8.one
            }
        ).value!!

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
        polyY.range.set(possibleRange.toFlt64())
    }

    override fun toRawString(unfold: Boolean): String {
        return "bter(${x.toRawString(unfold)})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenList, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(tokenTable, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val value = x.evaluate(results, tokenTable, zeroIfNone)
            ?: return null
        return if (value ls Flt64.zero) {
            -Flt64.one
        } else if (value gr Flt64.zero) {
            Flt64.one
        } else {
            Flt64.zero
        }
    }
}

class BalanceTernaryzationFunctionImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearFunctionSymbol,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, parent) {
    override val polyY: AbstractLinearPolynomial<*> by lazy {
        x.copy()
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val pos = xValue gr Flt64.zero
                val neg = xValue ls Flt64.zero
                val yValue = if (pos) {
                    Flt64.one
                } else if (neg) {
                    -Flt64.one
                } else {
                    Flt64.zero
                }

                tokenTable.cache(parent, null, yValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        return ok
    }

    override fun register(model: AbstractLinearMechanismModel): Try {
        return ok
    }
}

class BalanceTernaryzationFunctionPiecewiseImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearFunctionSymbol,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, parent) {
    private val piecewiseFunction: UnivariateLinearPiecewiseFunction by lazy {
        UnivariateLinearPiecewiseFunction(
            x,
            listOf(
                Point2(x.lowerBound!!.value.unwrap(), -Flt64.one),
                Point2(-epsilon, -Flt64.one),
                Point2(-epsilon + Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                Point2(epsilon - Flt32.decimalPrecision.toFlt64(), Flt64.zero),
                Point2(epsilon, Flt64.one),
                Point2(x.upperBound!!.value.unwrap(), Flt64.one)
            ),
            "${name}_piecewise"
        )
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = piecewiseFunction.b.last() - piecewiseFunction.b.first()
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun flush(force: Boolean) {
        super.flush(force)
        piecewiseFunction.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells
        piecewiseFunction.prepare(tokenTable)

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            piecewiseFunction.evaluate(tokenTable)?.let { yValue ->
                tokenTable.cache(parent, null, yValue)
            }
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

class BalanceTernaryzationFunctionDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearFunctionSymbol,
    private val extract: Boolean = true,
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, parent) {
    private val logger = logger()

    private val y: BinVariable1 by lazy {
        val y = BinVariable1("${name}_y", Shape1(2))
        y[0].range.leq(x.lowerBound!!.value.unwrap() ls Flt64.zero)
        y[1].range.leq(x.upperBound!!.value.unwrap() gr Flt64.zero)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = y[1] - y[0]
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val pos = xValue gr Flt64.zero
                val neg = xValue ls Flt64.zero

                logger.trace { "Setting BalanceTernaryzationFunction ${name}.y initial solution: $pos, $neg" }
                tokenTable.find(y[0])?.let { token ->
                    token._result = if (pos) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }
                tokenTable.find(y[1])?.let { token ->
                    token._result = if (neg) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }

                val yValue = if (pos) {
                    Flt64.one
                } else if (neg) {
                    -Flt64.one
                } else {
                    Flt64.zero
                }
                tokenTable.cache(parent, null, yValue)
            }
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
            x.upperBound!!.value.unwrap() * y[1] geq x,
            "${name}_plb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            x.lowerBound!!.value.unwrap() * y[0] leq x,
            "${name}_nlb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (extract) {
            when (val result = model.addConstraint(
                x geq (x.lowerBound!!.value.unwrap() - Flt64.one) * (Flt64.one - y[1]) + Flt64.one,
                "${name}_pub"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                x leq (x.upperBound!!.value.unwrap() + Flt64.one) * (Flt64.one - y[0]) - Flt64.one,
                "${name}_nlb"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }

            when (val result = model.addConstraint(
                sum(y) leq Flt64.one,
                "${name}_y"
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

class BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(
    x: AbstractLinearPolynomial<*>,
    parent: LinearFunctionSymbol,
    private val epsilon: Flt64 = Flt64(1e-6),
    override var name: String,
    override var displayName: String? = null
) : AbstractBalanceTernaryzationFunctionImpl(x, parent) {
    private val logger = logger()

    private val b: PctVariable1 by lazy {
        PctVariable1(name = "${name}_b", Shape1(2))
    }

    private val y: BinVariable1 by lazy {
        val y = BinVariable1("${name}_y", Shape1(2))
        y[0].range.leq(x.lowerBound!!.value.unwrap() ls Flt64.zero)
        y[1].range.leq(x.upperBound!!.value.unwrap() gr Flt64.zero)
        y
    }

    override val polyY: AbstractLinearPolynomial<*> by lazy {
        val polyY = y[1] - y[0]
        polyY.range.set(possibleRange.toFlt64())
        polyY
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.cachedSolution && tokenTable.cached(parent) == false) {
            x.evaluate(tokenTable)?.let { xValue ->
                val pos = xValue gr Flt64.zero
                val pocPct = if (pos) {
                    xValue / x.upperBound!!.value.unwrap()
                } else {
                    Flt64.zero
                }
                val neg = xValue ls Flt64.zero
                val negPct = if (neg) {
                    xValue / x.lowerBound!!.value.unwrap()
                } else {
                    Flt64.zero
                }
                val yValue = if (pos) {
                    Flt64.one
                } else if (neg) {
                    -Flt64.one
                } else {
                    Flt64.zero
                }

                logger.trace { "Setting BalanceTernaryzationFunction ${name}.b initial solution: $pocPct, $negPct" }
                tokenTable.find(b[0])?.let { token ->
                    token._result = pocPct
                }
                tokenTable.find(b[1])?.let { token ->
                    token._result = negPct
                }
                logger.trace { "Setting BalanceTernaryzationFunction ${name}.y initial solution: $pos, $neg" }
                tokenTable.find(y[0])?.let { token ->
                    token._result = if (pos) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }
                tokenTable.find(y[1])?.let { token ->
                    token._result = if (neg) {
                        Flt64.one
                    } else {
                        Flt64.zero
                    }
                }

                tokenTable.cache(parent, null, yValue)
            }
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
            x eq x.lowerBound!!.value.unwrap() * b[0] + x.upperBound!!.value.unwrap() * b[1],
            "${name}_xb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[1] geq b[1],
            "${name}_yb_pos_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[1] leq (Flt64.one / epsilon) * b[1],
            "${name}_yb_pos_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[0] geq b[0],
            "${name}_yb_neg_lb"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            y[0] leq (Flt64.one / epsilon) * b[0],
            "${name}_yb_neg_ub"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            b[0] + y[1] leq Flt64.one,
            "${name}_yb_pos"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            b[1] + y[0] leq Flt64.one,
            "${name}_yb_neg"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }
}

class BalanceTernaryzationFunction(
    val x: AbstractLinearPolynomial<*>,
    private val extract: Boolean = true,
    private val epsilon: Flt64 = Flt64(1e-6),
    private val piecewise: Boolean = false,
    impl: AbstractBalanceTernaryzationFunctionImpl? = null,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    companion object {
        val piecewiseThreshold: Flt64 = Flt64(1e-5)
    }

    private val impl: AbstractBalanceTernaryzationFunctionImpl by lazy {
        impl ?: if (x.discrete && ValueRange(-Flt64.one, Flt64.one).value!! contains x.range.range!!) {
            BalanceTernaryzationFunctionImpl(x, this, name, displayName)
        } else if (x.discrete) {
            BalanceTernaryzationFunctionDiscreteImpl(x, this, extract, name, displayName)
        } else if (extract && !x.discrete && (piecewise || epsilon geq piecewiseThreshold)) {
            BalanceTernaryzationFunctionPiecewiseImpl(x, this, epsilon, name, displayName)
        } else {
            BalanceTernaryzationFunctionExtractAndNotDiscreteImpl(x, this, epsilon, name, displayName)
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
        return "bter(${x.toRawString(unfold)})"
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
