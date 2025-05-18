package fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function

import org.apache.logging.log4j.kotlin.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.symbol.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
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
    private val x: AbstractQuadraticPolynomial<*>,
    private val y: AbstractQuadraticPolynomial<*>,
    val triangles: List<Triangle3>,
    override var name: String,
    override var displayName: String? = null
) : QuadraticFunctionSymbol {
    private val logger = logger()

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

    val size by triangles::size
    val indices by triangles::indices

    fun z(x: Flt64, y: Flt64): Flt64? {
        for (i in indices) {
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

    private val u: PctVariable1 by lazy {
        PctVariable1("${name}_u", Shape1(size))
    }

    private val v: PctVariable1 by lazy {
        PctVariable1("${name}_v", Shape1(size))
    }

    private val w: BinVariable1 by lazy {
        BinVariable1("${name}_w", Shape1(size))
    }

    private val polyZ: QuadraticPolynomial by lazy {
        val polyZ = calculatePolyZ()
        polyZ.name = "${name}_z"
        polyZ.range.set(
            ValueRange(
                triangles.minOf { minOf(it.p1.z, it.p2.z, it.p3.z) },
                triangles.maxOf { maxOf(it.p1.z, it.p2.z, it.p3.z) }
            ).value!!
        )
        polyZ
    }

    override val range get() = polyZ.range
    override val lowerBound get() = polyZ.lowerBound
    override val upperBound get() = polyZ.upperBound

    override val category = Linear

    override val dependencies: Set<IntermediateSymbol>
        get() {
            val dependencies = HashSet<IntermediateSymbol>()
            dependencies.addAll(x.dependencies)
            dependencies.addAll(y.dependencies)
            return dependencies
        }
    override val cells get() = polyZ.cells
    override val cached get() = polyZ.cached

    override fun flush(force: Boolean) {
        x.flush(force)
        y.flush(force)
        polyZ.flush(force)
    }

    override fun prepare(tokenTable: AbstractTokenTable): Flt64? {
        x.cells
        y.cells

        return if (tokenTable.cachedSolution && tokenTable.cached(this) == false) {
            val xValue = x.evaluate(tokenTable) ?: return null
            val yValue = y.evaluate(tokenTable) ?: return null

            var zValue: Flt64? = null
            for (i in indices) {
                val uValue = this.calculateU(i, xValue, yValue)
                val vValue = this.calculateV(i, xValue, yValue)

                if ((Flt64.zero leq uValue) && (uValue leq Flt64.one)
                    && (Flt64.zero leq vValue) && (vValue leq Flt64.one)
                    && (Flt64.zero leq (uValue + vValue)) && ((uValue + vValue) leq Flt64.one)
                ) {
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.w[$i] initial solution: true" }
                    tokenTable.find(w[i])?.let { token ->
                        token._result = Flt64.one
                    }
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.u[$i] initial solution: $uValue" }
                    tokenTable.find(u[i])?.let { token ->
                        token._result = uValue
                    }
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.v[$i] initial solution: $vValue" }
                    tokenTable.find(v[i])?.let { token ->
                        token._result = vValue
                    }
                    val triangle = triangles[i]
                    zValue = triangle.p1.z + (triangle.p2.z - triangle.p1.z) * uValue + (triangle.p3.z - triangle.p1.z) * vValue
                } else {
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.w[$i] initial solution: false" }
                    tokenTable.find(w[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.u[$i] initial solution: 0" }
                    tokenTable.find(u[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                    logger.trace { "Setting BivariateLinearPiecewiseFunction ${name}.v[$i] initial solution: 0" }
                    tokenTable.find(v[i])?.let { token ->
                        token._result = Flt64.zero
                    }
                }
            }

            zValue
        } else {
            null
        }
    }

    override fun register(tokenTable: AbstractMutableTokenTable): Try {
        when (val result = tokenTable.add(u)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(v)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        when (val result = tokenTable.add(w)) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        return ok
    }

    override fun register(model: AbstractQuadraticMechanismModel): Try {
        val m = calculateM()

        for (i in indices) {
            val rhs = polyU(i)
            when (val result = model.addConstraint(
                (u[i] - m * w[i] + m) geq rhs,
                "${name}_ul_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (u[i] + m * w[i] - m) leq rhs,
                "${name}_ur_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        for (i in indices) {
            val rhs = polyV(i)
            when (val result = model.addConstraint(
                (v[i] - m * w[i] + m) geq rhs,
                "${name}_vl_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
            when (val result = model.addConstraint(
                (v[i] + m * w[i] - m) leq rhs,
                "${name}_vr_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        when (val result = model.addConstraint(
            sum(w) eq Flt64.one,
            "${name}_w"
        )) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }
        }

        for (i in indices) {
            when (val result = model.addConstraint(
                (u[i] + v[i]) leq w[i],
                "${name}_uv_$i"
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }
            }
        }

        return ok
    }

    private fun calculatePolyZ(): QuadraticPolynomial {
        val monomials = ArrayList<QuadraticMonomial>()
        for ((i, triangle) in triangles.withIndex()) {
            monomials.add(QuadraticMonomial(triangle.p1.z * w[i]))
            monomials.add(QuadraticMonomial((triangle.p2.z - triangle.p1.z) * u[i]))
            monomials.add(QuadraticMonomial((triangle.p3.z - triangle.p1.z) * v[i]))
        }
        return QuadraticPolynomial(monomials)
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

    private fun polyU(i: Int): AbstractQuadraticPolynomial<*> {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = MutableQuadraticPolynomial()
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

    private fun polyV(i: Int): AbstractQuadraticPolynomial<*> {
        val triangle = triangles[i]
        val area = triangle.bottomArea()
        val coefficient = Flt64(.5) / area

        val poly = MutableQuadraticPolynomial()
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

    override fun toRawString(unfold: UInt64): String {
        return if (unfold eq UInt64.zero) {
            displayName ?: name
        } else {
            "${name}(${x.toTidyRawString(unfold - UInt64.one)}, ${y.toTidyRawString(unfold - UInt64.one)})"
        }
    }

    override fun evaluate(tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val thisX = x.evaluate(tokenList, zeroIfNone) ?: return null
        val thisY = y.evaluate(tokenList, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }

    override fun evaluate(results: List<Flt64>, tokenList: AbstractTokenList, zeroIfNone: Boolean): Flt64? {
        val thisX = x.evaluate(results, tokenList, zeroIfNone) ?: return null
        val thisY = y.evaluate(results, tokenList, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }

    override fun calculateValue(tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val thisX = x.evaluate(tokenTable, zeroIfNone) ?: return null
        val thisY = y.evaluate(tokenTable, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }

    override fun calculateValue(results: List<Flt64>, tokenTable: AbstractTokenTable, zeroIfNone: Boolean): Flt64? {
        val thisX = x.evaluate(results, tokenTable, zeroIfNone) ?: return null
        val thisY = y.evaluate(results, tokenTable, zeroIfNone) ?: return null
        return z(thisX, thisY)
    }
}

class BivariateLinearPiecewiseFunction(
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
    val points: List<Point3>,
    triangles: List<Triangle3>,
    name: String,
    displayName: String? = null
) : AbstractBivariateLinearPiecewiseFunction(x, y, triangles, name, displayName) {
    companion object {
        operator fun invoke(
            x: AbstractQuadraticPolynomial<*>,
            y: AbstractQuadraticPolynomial<*>,
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
    x: AbstractQuadraticPolynomial<*>,
    y: AbstractQuadraticPolynomial<*>,
    val isolines: List<Pair<Flt64, List<Point2>>>,
    triangles: List<Triangle3>,
    name: String,
    displayName: String? = null
) : AbstractBivariateLinearPiecewiseFunction(x, y, triangles, name, displayName) {
    companion object {
        operator fun invoke(
            x: AbstractQuadraticPolynomial<*>,
            y: AbstractQuadraticPolynomial<*>,
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
