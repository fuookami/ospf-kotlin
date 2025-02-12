package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

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

sealed class AbstractUnivariateLinearPiecewiseFunction(
    private val x: AbstractQuadraticPolynomial<*>,
    protected val points: List<Point2>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

    init {
        assert(points.foldIndexed(true) { index, acc, point ->
            if (!acc) {
                acc
            } else {
                if (index == 0) {
                    acc
                } else {
                    point.x geq points[index - 1].x
                }
            }
        })
    }

    val size by points::size
    val indices by points::indices

    val empty: Boolean get() = size == 0
    val fixed: Boolean get() = size == 1
    val piecewise: Boolean get() = size >= 2

    fun y(x: Flt64): Flt64? {
        if (empty
            || x ls points.first().x
            || x gr points.last().x
        ) {
            return null
        }

        for (i in indices) {
            if (i == (size - 1)) {
                continue
            }

            if (points[i + 1].x geq x) {
                val dy = points[i + 1].y - points[i].y
                val dx = points[i + 1].x - points[i].x
                return dy / dx * (x - points[i].x)
            }
        }
        return null
    }

    private val k: PctVariable1 by lazy {
        PctVariable1("${name}_k", Shape1(points.size))
    }

    internal val b: BinVariable1 by lazy {
        BinVariable1("${name}_b", Shape1(points.size - 1))
    }

    private val polyY: QuadraticPolynomial by lazy {
        val polyY = QuadraticPolynomial(
            sum(points.mapIndexed { i, p -> p.y * k[i] }),
            "${name}_y"
        )
        polyY.range.set(ValueRange(points.minOf { it.y }, points.maxOf { it.y }).value!!)
        polyY
    }

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol> by x::dependencies
    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    override fun flush(force: Boolean) {
        x.flush(force)
        polyY.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable) {
        x.cells

        if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.evaluate(tokenTable) ?: return
            var yValue: Flt64? = null
            for (i in indices) {
                if (i == (size - 1)) {
                    continue
                }

                if (points[i].x leq xValue && xValue leq points[i + 1].x) {
                    val dx = points[i + 1].x - points[i].x
                    val lhs = (xValue - points[i].x) / dx
                    val rhs = (points[i + 1].x - xValue) / dx
                    yValue = points[i].y * lhs + points[i + 1].y * rhs

                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.b[$i] initial solution: true" }
                    tokenTable.find(b[i])?.let { token ->
                        token._result = Flt64.one
                    }
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[$i] initial solution: $lhs" }
                    tokenTable.find(k[i])?.let { token ->
                        token._result = lhs
                    }
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[${i + 1}] initial solution: $rhs" }
                    tokenTable.find(k[i + 1])?.let { token ->
                        token._result = rhs
                    }
                } else {
                    logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.b[$i] initial solution: false" }
                    tokenTable.find(b[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                    tokenTable.find(k[i])?.let { token ->
                        if (token._result == null) {
                            logger.trace { "Setting UnivariateLinearPiecewiseFunction ${name}.k[$i] initial solution: 0" }
                            token._result = Flt64.zero
                        }
                    }
                }
            }
            if (yValue != null) {
                tokenTable.cache(this, null, yValue)
            }
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(k)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(b)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        when (val result = model.addConstraint(
            x eq sum(points.mapIndexed { i, p -> p.x * k[i] }),
            "${name}_x"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = model.addConstraint(
            sum(k) eq Flt64.one,
            "${name}_k"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }
        when (val result = model.addConstraint(
            sum(b) eq Flt64.one,
            "${name}_b"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (i in indices) {
            val poly = MutableLinearPolynomial()
            if (i != 0) {
                poly += b[i - 1]
            }
            if (i != (size - 1)) {
                poly += b[i]
            }
            when (val result = model.addConstraint(
                k[i] leq poly,
                "${name}_kb_i"
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

    override fun toRawString(unfold: Boolean): String {
        return "$name(${x.toRawString(unfold)}})"
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenList, zeroIfNone)?.let { y(it) }
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenList, zeroIfNone)?.let { y(it) }
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(tokenTable, zeroIfNone)?.let { y(it) }
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        return x.evaluate(results, tokenTable, zeroIfNone)?.let { y(it) }
    }
}

open class UnivariateLinearPiecewiseFunction(
    x: AbstractQuadraticPolynomial<*>,
    points: List<Point2>,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(x, points.sortedBy { it.x }, name, displayName)

open class MonotoneUnivariateLinearPiecewiseFunction(
    x: AbstractQuadraticPolynomial<*>,
    points: List<Point2>,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(x, points.sortedBy { it.x }, name, displayName) {
    init {
        assert(points.foldIndexed(true) { index, acc, point ->
            acc && (index == 0 || point.y geq points[index - 1].y)
        })
    }

    fun x(y: Flt64): Flt64? {
        TODO("not implement yet")
    }
}
