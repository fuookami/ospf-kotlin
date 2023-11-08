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
import fuookami.ospf.kotlin.core.frontend.expression.symbol.Function
import fuookami.ospf.kotlin.core.frontend.inequality.*
import fuookami.ospf.kotlin.core.frontend.model.mechanism.*

class BivariateSymbols {
    lateinit var u: PctVariable1
    lateinit var v: PctVariable1
    lateinit var w: BinVariable1

    lateinit var z: LinearSymbol
}

abstract class BivariateLinearPiecewiseFunction(
    val x: LinearPolynomial,
    val y: LinearPolynomial,
    override var name: String,
    override var displayName: String? = "${name}(${x.name}, ${y.name})"
) : Function<Linear> {
    companion object {
        fun Triangle3.bottomArea(): Flt64 {
            return Flt64(.5) * (-p2.y * p3.x + p1.y * (-p2.x + p3.x) + p1.x * (p2.y - p3.y) + p2.x * p3.y);
        }

        infix fun <D: Dimension> Point<D>.ord(rhs: Point<D>): Order {
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

        infix fun <P: Point<D>, D: Dimension> Triangle<P, D>.ord(rhs: Triangle<P, D>): Order {
            val lhsLestPoint = listOf(p1, p2, p3).sortedWithThreeWayComparator { lhsPoint, rhsPoint -> lhsPoint ord rhsPoint }.first()
            val rhsLestPoint = listOf(rhs.p1, rhs.p2, rhs.p3).sortedWithThreeWayComparator { lhsPoint, rhsPoint -> lhsPoint ord rhsPoint }.first()
            return lhsLestPoint ord rhsLestPoint
        }
    }

    abstract val triangles: List<Triangle3>
    private lateinit var symbols: BivariateSymbols

    val size get() = triangles.size

    override val possibleRange: ValueRange<Flt64> get() = symbols.z.possibleRange
    override var range: ValueRange<Flt64>
        get() = symbols.z.range
        set(value) {
            symbols.z.range = value
        }

    override val cells: List<MonomialCell<Linear>> get() = symbols.z.cells

    override val lowerBound: Flt64 get() = symbols.z.lowerBound
    override val upperBound: Flt64 get() = symbols.z.upperBound

    override fun intersectRange(range: ValueRange<Flt64>) = symbols.z.intersectRange(range)
    override fun rangeLess(value: Flt64) = symbols.z.rangeLess(value)
    override fun rangeLessEqual(value: Flt64) = symbols.z.rangeLessEqual(value)
    override fun rangeGreater(value: Flt64) = symbols.z.rangeGreater(value)
    override fun rangeGreaterEqual(value: Flt64) = symbols.z.rangeGreaterEqual(value)

    override fun toRawString() = displayName ?: name

    override fun register(tokenTable: TokenTable<Linear>) {
        symbols = BivariateSymbols()

        symbols.u = PctVariable1("${name}_u", Shape1(size))
        tokenTable.add(symbols.u)

        symbols.v = PctVariable1("${name}_v", Shape1(size))
        tokenTable.add(symbols.v)

        symbols.w = BinVariable1("${name}_w", Shape1(size))
        tokenTable.add(symbols.w)

        symbols.z = LinearSymbol(polyZ(), "${name}_z")
        tokenTable.add(symbols.z)
    }

    override fun register(model: Model<Linear>) {
        val m = calculateM()

        for (i in 0 until size) {
            val fu = polyU(i)
            model.addConstraint(
                (symbols.u[i]!! - m * symbols.w[i]!! + m) geq fu,
                "${name}_ul_$i"
            )
            model.addConstraint(
                (symbols.u[i]!! + m * symbols.w[i]!! - m) leq fu,
                "${name}_ur_$i"
            )
        }

        for (i in 0 until size) {
            val fv = polyV(i)
            model.addConstraint(
                (symbols.v[i]!! - m * symbols.w[i]!! + m) geq fv,
                "${name}_vl_$i"
            )
            model.addConstraint(
                (symbols.v[i]!! + m * symbols.w[i]!! - m) leq fv,
                "${name}_vr_$i"
            )
        }

        model.addConstraint(
            sum(symbols.w) eq Flt64.one,
            "${name}_w"
        )

        for (i in 0 until size) {
            model.addConstraint(
                (symbols.u[i]!! + symbols.v[i]!!) leq symbols.w[i]!!,
                "${name}_uv_$i"
            )
        }
    }

    @Throws(IllegalArgumentException::class)
    open fun z(x: Flt64, y: Flt64): Flt64 {
        for (i in 0 until size) {
            val u = this.u(i, x, y)
            val v = this.v(i, x, y)

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
        throw IllegalArgumentException("Out of domain!")
    }

    private fun polyZ(): LinearPolynomial {
        val poly = LinearPolynomial()
        for (i in 0 until size) {
            val triangle = triangles[i]

            poly += triangle.p1.z * symbols.w[i]!!
            poly += (triangle.p2.z - triangle.p1.z) * symbols.u[i]!!
            poly += (triangle.p3.z - triangle.p1.z) * symbols.v[i]!!
        }
        return poly
    }

    private fun u(i: Int, x: Flt64, y: Flt64): Flt64 {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        var ret = Flt64.zero
        ret += coefficient * (triangle.p3.y - triangle.p1.y) * x
        ret += coefficient * (triangle.p1.x - triangle.p3.x) * y
        ret += coefficient * (triangle.p1.y * triangle.p3.x - triangle.p1.x * triangle.p3.y)
        return ret
    }

    private fun polyU(i: Int): LinearPolynomial {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = LinearPolynomial()
        poly += coefficient * (triangle.p3.y - triangle.p1.y) * x
        poly += coefficient * (triangle.p1.x - triangle.p3.x) * y
        poly += coefficient * (triangle.p1.y * triangle.p3.x - triangle.p1.x * triangle.p3.y)
        return poly
    }

    private fun v(i: Int, x: Flt64, y: Flt64): Flt64 {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        var ret = Flt64.zero
        ret += coefficient * (triangle.p1.y - triangle.p2.y) * x
        ret += coefficient * (triangle.p2.x - triangle.p1.x) * y
        ret += coefficient * (triangle.p1.x * triangle.p2.y - triangle.p1.y * triangle.p2.x)
        return ret
    }

    private fun polyV(i: Int): LinearPolynomial {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = LinearPolynomial()
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
            val u = arrayOf(this.u(i, minX, minY), this.u(i, minX, maxY), this.u(i, maxX, minY), this.u(i, maxX, maxY))
            minU = minOf(minU, u.minOf { it })
            maxU = maxOf(maxU, u.maxOf { it })

            val v = arrayOf(this.v(i, minX, minY), this.v(i, minX, maxY), this.v(i, maxX, minY), this.v(i, maxX, maxY))
            minV = minOf(minV, v.minOf { it })
            maxV = maxOf(maxV, v.maxOf { it })
        }

        return maxOf(minX.abs(), maxX.abs(), minY.abs(), maxY.abs())
    }
}

class CommonBivariateLinearPiecewiseFunction(
    x: LinearPolynomial,
    y: LinearPolynomial,
    val points: List<Point3>,
    override val triangles: List<Triangle3>,
    name: String,
    displayName: String? = "${name}(${x.name}, ${y.name})"
) : BivariateLinearPiecewiseFunction(x, y, name, displayName) {
    companion object {
        operator fun invoke(
            x: LinearPolynomial,
            y: LinearPolynomial,
            points: List<Point3>,
            name: String,
            displayName: String? = "${name}(${x.name}, ${y.name})"
        ): CommonBivariateLinearPiecewiseFunction {
            val sortedPoints = points.sortedWithThreeWayComparator { lhs, rhs -> lhs ord rhs }
            val triangles = triangulate(sortedPoints)
            return CommonBivariateLinearPiecewiseFunction(
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
    x: LinearPolynomial,
    y: LinearPolynomial,
    val isolines: List<Pair<Flt64, List<Point2>>>,
    override val triangles: List<Triangle3>,
    name: String,
    displayName: String? = "${name}(${x.name}, ${y.name})"
) : BivariateLinearPiecewiseFunction(x, y, name, displayName) {
    companion object {
        operator fun invoke(
            x: LinearPolynomial,
            y: LinearPolynomial,
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
