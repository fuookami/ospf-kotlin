package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractUnivariateLinearPiecewiseFunction(
    private val x: AbstractLinearPolynomial<*>,
    protected val points: List<Point2>,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
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

        for (i in 0 until (size - 1)) {
            if (points[i + 1].x geq x) {
                val dy = points[i + 1].y - points[i].y
                val dx = points[i + 1].x - points[i].x
                return dy / dx * (x - points[i].x)
            }
        }
        return null
    }

    private lateinit var k: PctVariable1
    internal lateinit var b: BinVariable1
    private lateinit var polyY: LinearPolynomial

    override val range get() = polyY.range
    override val lowerBound get() = polyY.lowerBound
    override val upperBound get() = polyY.upperBound

    override val cells get() = polyY.cells
    override val cached get() = polyY.cached

    override fun flush(force: Boolean) {
        polyY.flush(force)
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        if (!::k.isInitialized) {
            k = PctVariable1("${name}_k", Shape1(points.size))
        }
        when (val result = tokenTable.add(k)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::b.isInitialized) {
            b = BinVariable1("${name}_b", Shape1(points.size - 1))
        }
        when (val result = tokenTable.add(b)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyY.isInitialized) {
            polyY = sum(points.mapIndexed { i, p -> p.y * k[i] })
            polyY.name = "${name}_y"
            polyY.range.set(ValueRange(points.minOf { it.y }, points.maxOf { it.y }, Flt64))
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        model.addConstraint(
            x eq sum(points.mapIndexed { i, p -> p.x * k[i] }),
            "${name}_x"
        )

        model.addConstraint(
            sum(k) eq Flt64.one,
            "${name}_k"
        )
        model.addConstraint(
            sum(b) eq Flt64.one,
            "${name}_b"
        )

        for (i in 0 until size) {
            val poly = MutableLinearPolynomial()
            if (i != 0) {
                poly += b[i - 1]
            }
            if (i != (size - 1)) {
                poly += b[i]
            }
            model.addConstraint(
                k[i] leq poly,
                "${name}_kb_i"
            )
        }

        return Ok(success)
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "$name(${x.toRawString(unfold)}})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(tokenList, zeroIfNone)?.let { y(it) }
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        return x.value(results, tokenList, zeroIfNone)?.let { y(it) }
    }
}

open class UnivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    points: List<Point2>,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(x, points.sortedBy { it.x }, name, displayName)

open class MonotoneUnivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    points: List<Point2>,
    name: String,
    displayName: String? = null
) : AbstractUnivariateLinearPiecewiseFunction(x, points.sortedBy { it.x }, name, displayName) {
    init {
        assert(points.foldIndexed(true) { index, acc, point ->
            if (!acc) {
                acc
            } else {
                if (index == 0) {
                    acc
                } else {
                    point.y geq points[index - 1].y
                }
            }
        })
    }

    fun x(y: Flt64): Flt64? {
        TODO("not implement yet")
    }
}
