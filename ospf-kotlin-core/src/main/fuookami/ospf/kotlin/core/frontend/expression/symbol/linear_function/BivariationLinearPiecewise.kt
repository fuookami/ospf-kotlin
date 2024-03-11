package fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.utils.multi_array.*
import fuookami.ospf.kotlin.core.frontend.variable.*
import fuookami.ospf.kotlin.core.frontend.expression.monomial.*
import fuookami.ospf.kotlin.core.frontend.expression.polynomial.*
import fuookami.ospf.kotlin.core.frontend.expression.symbol.*
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

sealed class AbstractBivariateLinearPiecewiseFunction(
    private val x: AbstractLinearPolynomial<*>,
    private val y: AbstractLinearPolynomial<*>,
    protected val triangles: List<Triangle3>,
    override var name: String,
    override var displayName: String? = null
) : LinearFunctionSymbol {
    companion object {
        fun Triangle3.bottomArea(): Flt64 {
            return Flt64(.5) * (-p2.y * p3.x + p1.y * (-p2.x + p3.x) + p1.x * (p2.y - p3.y) + p2.x * p3.y);
        }

        infix fun <D : Dimension> Point<D>.ord(rhs: Point<D>): Order {
            for (i in indices) {
                when (val result = (this[i] ord rhs[i])) {
                    Order.Equal -> {}
                    else -> {
                        return result
                    }
                }
            }
            return Order.Equal
        }

        infix fun <P : Point<D>, D : Dimension> Triangle<P, D>.ord(rhs: Triangle<P, D>): Order {
            val lhsLestPoint =
                listOf(p1, p2, p3).sortedWithThreeWayComparator { lhsPoint, rhsPoint -> lhsPoint ord rhsPoint }.first()
            val rhsLestPoint = listOf(
                rhs.p1,
                rhs.p2,
                rhs.p3
            ).sortedWithThreeWayComparator { lhsPoint, rhsPoint -> lhsPoint ord rhsPoint }.first()
            return lhsLestPoint ord rhsLestPoint
        }
    }

    val size get() = triangles.size

    fun z(x: Flt64, y: Flt64): Flt64? {
        for (i in 0 until size) {
            val u = this.calculateU(i, x, y)
            val v = this.calculateV(i, x, y)

            if ((Flt64.zero leq u) && (u leq Flt64.one)
                && (Flt64.zero leq v) && (v leq Flt64.one)
                && (Flt64.zero leq (u + v)) && ((u + v) leq Flt64.one)
            ) {
                val triangle = triangles[i]
                return triangle.p1.z +
                        (triangle.p2.z - triangle.p1.z) * u +
                        (triangle.p3.z - triangle.p1.z) * v
            }
        }

        return null
    }

    private lateinit var u: PctVariable1
    private lateinit var v: PctVariable1
    private lateinit var w: BinVariable1

    private lateinit var polyZ: LinearPolynomial

    override val range get() = polyZ.range
    override val lowerBound
        get() = if (::polyZ.isInitialized) {
            polyZ.lowerBound
        } else {
            triangles.minOf { minOf(it.p1.z, it.p2.z, it.p3.z) }
        }
    override val upperBound
        get() = if (::polyZ.isInitialized) {
            polyZ.upperBound
        } else {
            triangles.maxOf { maxOf(it.p1.z, it.p2.z, it.p3.z) }
        }

    override val dependencies: Set<Symbol<*, *>>
        get() {
            val dependencies = HashSet<Symbol<*, *>>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyZ.cells
    override val cached
        get() = if (::polyZ.isInitialized) {
            polyZ.cached
        } else {
            false
        }

    override fun flush(force: Boolean) {
        if (::polyZ.isInitialized) {
            polyZ.flush(force)
        }
    }

    override suspend fun prepare() {
        x.cells
        y.cells
    }

    override fun register(tokenTable: LinearMutableTokenTable): Try {
        if (!::u.isInitialized) {
            u = PctVariable1("${name}_u", Shape1(size))
        }
        when (val result = tokenTable.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::v.isInitialized) {
            v = PctVariable1("${name}_v", Shape1(size))
        }
        when (val result = tokenTable.add(v)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::w.isInitialized) {
            w = BinVariable1("${name}_w", Shape1(size))
        }
        when (val result = tokenTable.add(w)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        if (!::polyZ.isInitialized) {
            polyZ = calculatePolyZ()
            polyZ.name = "${name}_z"
            polyZ.range.set(
                ValueRange(
                    triangles.minOf { minOf(it.p1.z, it.p2.z, it.p3.z) },
                    triangles.maxOf { maxOf(it.p1.z, it.p2.z, it.p3.z) }
                )
            )
        }

        return Ok(success)
    }

    override fun register(model: AbstractLinearModel): Try {
        val m = calculateM()

        for (i in 0 until size) {
            val rhs = polyU(i)
            model.addConstraint(
                (u[i] - m * w[i] + m) geq rhs,
                "${name}_ul_$i"
            )
            model.addConstraint(
                (u[i] + m * w[i] - m) leq rhs,
                "${name}_ur_$i"
            )
        }

        for (i in 0 until size) {
            val rhs = polyV(i)
            model.addConstraint(
                (v[i] - m * w[i] + m) geq rhs,
                "${name}_vl_$i"
            )
            model.addConstraint(
                (v[i] + m * w[i] - m) leq rhs,
                "${name}_vr_$i"
            )
        }

        model.addConstraint(
            sum(w) eq Flt64.one,
            "${name}_w"
        )

        for (i in 0 until size) {
            model.addConstraint(
                (u[i] + v[i]) leq w[i],
                "${name}_uv_$i"
            )
        }

        return Ok(success)
    }

    private fun calculatePolyZ(): LinearPolynomial {
        val monomials = ArrayList<LinearMonomial>()
        for ((i, triangle) in triangles.withIndex()) {
            monomials.add(triangle.p1.z * w[i])
            monomials.add((triangle.p2.z - triangle.p1.z) * u[i])
            monomials.add((triangle.p3.z - triangle.p1.z) * v[i])
        }
        return LinearPolynomial(monomials)
    }

    private fun calculateU(i: Int, x: Flt64, y: Flt64): Flt64 {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        var ret = Flt64.zero
        ret += coefficient * (triangle.p3.y - triangle.p1.y) * x
        ret += coefficient * (triangle.p1.x - triangle.p3.x) * y
        ret += coefficient * (triangle.p1.y * triangle.p3.x - triangle.p1.x * triangle.p3.y)
        return ret
    }

    private fun polyU(i: Int): AbstractLinearPolynomial<*> {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = MutableLinearPolynomial()
        poly += coefficient * (triangle.p3.y - triangle.p1.y) * x
        poly += coefficient * (triangle.p1.x - triangle.p3.x) * y
        poly += coefficient * (triangle.p1.y * triangle.p3.x - triangle.p1.x * triangle.p3.y)
        return poly
    }

    private fun calculateV(i: Int, x: Flt64, y: Flt64): Flt64 {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        var ret = Flt64.zero
        ret += coefficient * (triangle.p1.y - triangle.p2.y) * x
        ret += coefficient * (triangle.p2.x - triangle.p1.x) * y
        ret += coefficient * (triangle.p1.x * triangle.p2.y - triangle.p1.y * triangle.p2.x)
        return ret
    }

    private fun polyV(i: Int): AbstractLinearPolynomial<*> {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = MutableLinearPolynomial()
        poly += coefficient * (triangle.p1.y - triangle.p2.y) * x
        poly += coefficient * (triangle.p2.x - triangle.p1.x) * y
        poly += coefficient * (triangle.p1.x * triangle.p2.y - triangle.p1.y * triangle.p2.x)
        return poly
    }

    private fun calculateM(): Flt64 {
        var minX = Flt64.maximum
        var maxX = Flt64.minimum
        var minY = Flt64.maximum
        var maxY = Flt64.minimum

        for (triangle in triangles) {
            minX = minOf(minX, triangle.p1.x, triangle.p2.x, triangle.p3.x)
            maxX = maxOf(maxX, triangle.p1.x, triangle.p2.x, triangle.p3.x)
            minY = minOf(minY, triangle.p1.y, triangle.p2.y, triangle.p3.y)
            maxY = maxOf(maxY, triangle.p1.y, triangle.p2.y, triangle.p3.y)
        }

        var minU = Flt64.maximum
        var maxU = Flt64.minimum
        var minV = Flt64.maximum
        var maxV = Flt64.minimum

        for (i in triangles.indices) {
            val u = arrayOf(
                this.calculateU(i, minX, minY),
                this.calculateU(i, minX, maxY),
                this.calculateU(i, maxX, minY),
                this.calculateU(i, maxX, maxY)
            )
            minU = minOf(minU, u.minOf { it })
            maxU = maxOf(maxU, u.maxOf { it })

            val v = arrayOf(
                this.calculateV(i, minX, minY),
                this.calculateV(i, minX, maxY),
                this.calculateV(i, maxX, minY),
                this.calculateV(i, maxX, maxY)
            )
            minV = minOf(minV, v.minOf { it })
            maxV = maxOf(maxV, v.maxOf { it })
        }

        return maxOf(minX.abs(), maxX.abs(), minY.abs(), maxY.abs())
    }

    override fun toString(): String {
        return displayName ?: name
    }

    override fun toRawString(unfold: Boolean): String {
        return "${name}(${x.toRawString(unfold)}, ${y.toRawString(unfold)})"
    }

    override fun value(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val thisX = x.value(tokenList, zeroIfNone) ?: return null
        val thisY = y.value(tokenList, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }

    override fun value(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val thisX = x.value(results, tokenList, zeroIfNone) ?: return null
        val thisY = y.value(results, tokenList, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }
}

class BivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    private val points: List<Point3>,
    triangles: List<Triangle3>,
    name: String,
    displayName: String? = null
) : AbstractBivariateLinearPiecewiseFunction(x, y, triangles, name, displayName) {
    companion object {
        operator fun invoke(
            x: AbstractLinearPolynomial<*>,
            y: AbstractLinearPolynomial<*>,
            points: List<Point3>,
            name: String,
            displayName: String? = null
        ): BivariateLinearPiecewiseFunction {
            val sortedPoints = points.sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            val triangles = triangulate(sortedPoints)
            return BivariateLinearPiecewiseFunction(
                x = x,
                y = y,
                points = sortedPoints,
                triangles = triangles,
                name = name,
                displayName = displayName
            )
        }
    }
}

class IsolineBivariateLinearPiecewiseFunction(
    x: AbstractLinearPolynomial<*>,
    y: AbstractLinearPolynomial<*>,
    private val isolines: List<Pair<Flt64, List<Point2>>>,
    triangles: List<Triangle3>,
    name: String,
    displayName: String? = null
) : AbstractBivariateLinearPiecewiseFunction(x, y, triangles, name, displayName) {
    companion object {
        operator fun invoke(
            x: AbstractLinearPolynomial<*>,
            y: AbstractLinearPolynomial<*>,
            isolines: List<Pair<Flt64, List<Point2>>>,
            name: String,
            displayName: String? = "${name}(${x.name}, ${y.name})"
        ): IsolineBivariateLinearPiecewiseFunction {
            val sortedIsolines = isolines
                .map { Pair(it.first, it.second.sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }) }
                .sortedWithThreeWayComparator { lhs, rhs -> lhs.first ord rhs.first }
            val triangles = triangulate(sortedIsolines)
            return IsolineBivariateLinearPiecewiseFunction(
                x = x,
                y = y,
                isolines = sortedIsolines,
                triangles = triangles,
                name = name,
                displayName = displayName
            )
        }
    }
}
