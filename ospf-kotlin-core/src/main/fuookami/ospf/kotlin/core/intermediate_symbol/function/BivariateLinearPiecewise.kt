@file:Suppress("unused")

package fuookami.ospf.kotlin.core.intermediate_symbol.function

import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.variable.BinVariable1
import fuookami.ospf.kotlin.core.variable.PctVariable1
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Triangle3
import fuookami.ospf.kotlin.math.geometry.x
import fuookami.ospf.kotlin.math.geometry.y
import fuookami.ospf.kotlin.math.geometry.z
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality

/**
 * BivariateLinearPiecewiseFunction - Piecewise linear function of two variables using triangle interpolation.
 *
 * Given input variables x and y, and a set of triangles with vertices (x_i, y_i, z_i):
 * - The function value z at point (x, y) is interpolated from the containing triangle
 * - Uses barycentric coordinates within each triangle
 *
 * Constraints:
 * - For each triangle i, create lambda variables lambda_i_j (PctVariable1) for each vertex j=0,1,2
 * - For each triangle i, create binary z_i (BinVar) for triangle selection
 * - x = weighted sum of x-coords, y = weighted sum of y-coords
 * - output = weighted sum of z-coords
 * - sum(all lambda) = 1
 * - SOS2: only one triangle active (lambda_i_sum <= 3 * z_i, sum(z_i) = 1)
 */
class BivariateLinearPiecewiseFunction<V>(
    val x: LinearPolynomial<V>,
    val y: LinearPolynomial<V>,
    val triangles: List<Triangle3>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V> where V : RealNumber<V>, V : NumberField<V> {

    private val n: Int get() = triangles.size

    init {
        require(triangles.isNotEmpty()) { "At least one triangle is required" }
    }

    // Lambda variables: lambda_i_j for triangle i, vertex j (0, 1, 2)
    // Each triangle has 3 lambda variables
    val lambdaVars: List<PctVariable1> by lazy {
        triangles.mapIndexed { i, _ ->
            PctVariable1("${name}_lambda_$i", Shape1(3))
        }
    }

    // Binary variable per triangle for selection
    val zVars: BinVariable1 by lazy {
        BinVariable1("${name}_tri", Shape1(n))
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = lambdaVars.flatMap { it.items } + zVars.items

    /**
     * Result polynomial: sum of z-coordinates weighted by lambdas.
     * For each triangle i, vertices p1, p2, p3:
     * result = sum over all i,j of (triangle_i.vertex_j.z * lambda_i_j)
     */
    val result: LinearPolynomial<V> by lazy {
        val monos = mutableListOf<LinearMonomial<V>>()
        for (i in triangles.indices) {
            val tri = triangles[i]
            val lambdas = lambdaVars[i]
            // p1, p2, p3 correspond to j=0, 1, 2
            monos += LinearMonomial(converter.intoValue(tri.p1.z), lambdas[0])
            monos += LinearMonomial(converter.intoValue(tri.p2.z), lambdas[1])
            monos += LinearMonomial(converter.intoValue(tri.p3.z), lambdas[2])
        }
        LinearPolynomial(monos, converter.zero)
    }

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluate(values)?.let { converter.fromValue(it) } ?: return null
        val yVal = y.evaluate(values)?.let { converter.fromValue(it) } ?: return null

        for (i in triangles.indices) {
            val tri = triangles[i]
            val (u, v) = calculateBarycentric(tri, xVal, yVal)
            if (u in 0.0..1.0 && v in 0.0..1.0 && u + v <= 1.0) {
                val zVal = tri.p1.z.toDouble() +
                    (tri.p2.z - tri.p1.z).toDouble() * u +
                    (tri.p3.z - tri.p1.z).toDouble() * v
                return converter.intoValue(Flt64(zVal))
            }
        }
        return null
    }

    private fun calculateBarycentric(tri: Triangle3, px: Flt64, py: Flt64): Pair<Double, Double> {
        val x1 = tri.p1.x.toDouble()
        val y1 = tri.p1.y.toDouble()
        val x2 = tri.p2.x.toDouble()
        val y2 = tri.p2.y.toDouble()
        val x3 = tri.p3.x.toDouble()
        val y3 = tri.p3.y.toDouble()
        val pxVal = px.toDouble()
        val pyVal = py.toDouble()

        val det = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        if (kotlin.math.abs(det) < 1e-12) {
            return -1.0 to -1.0 // Degenerate triangle
        }
        val u = ((y2 - y3) * (pxVal - x3) + (x3 - x2) * (pyVal - y3)) / det
        val v = ((y3 - y1) * (pxVal - x3) + (x1 - x3) * (pyVal - y3)) / det
        return u to v
    }

    override fun registerAuxiliaryTokens(tokens: fuookami.ospf.kotlin.core.token.AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val allConstraints = mutableListOf<LinearInequality<Flt64>>()

        // x constraint: x = sum over all triangles and vertices of (x_coord * lambda)
        val xMonos = mutableListOf<LinearMonomial<Flt64>>()
        for (i in triangles.indices) {
            val tri = triangles[i]
            val lambdas = lambdaVars[i]
            xMonos += LinearMonomial(-tri.p1.x, lambdas[0])
            xMonos += LinearMonomial(-tri.p2.x, lambdas[1])
            xMonos += LinearMonomial(-tri.p3.x, lambdas[2])
        }
        val xPoly = x.asFlt64Poly(converter)
        xMonos += xPoly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(xMonos, xPoly.constant),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_x_eq"
        )

        // y constraint: y = sum over all triangles and vertices of (y_coord * lambda)
        val yMonos = mutableListOf<LinearMonomial<Flt64>>()
        for (i in triangles.indices) {
            val tri = triangles[i]
            val lambdas = lambdaVars[i]
            yMonos += LinearMonomial(-tri.p1.y, lambdas[0])
            yMonos += LinearMonomial(-tri.p2.y, lambdas[1])
            yMonos += LinearMonomial(-tri.p3.y, lambdas[2])
        }
        val yPoly = y.asFlt64Poly(converter)
        yMonos += yPoly.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(yMonos, yPoly.constant),
            LinearPolynomial(emptyList(), Flt64.zero),
            Comparison.EQ, "${name}_y_eq"
        )

        // sum(all lambda) = 1
        val sumLambdaMonos = lambdaVars.flatMap { it.items.map { l -> LinearMonomial(Flt64.one, l) } }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(sumLambdaMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one),
            Comparison.EQ, "${name}_sum_lambda"
        )

        // SOS2: for each triangle, sum(lambda_i_j) <= 3 * z_i
        for (i in triangles.indices) {
            val lambdas = lambdaVars[i]
            val zi = zVars[i]
            val triLambdaMonos = lambdas.items.map { LinearMonomial(Flt64.one, it) } +
                LinearMonomial(Flt64(-3.0), zi)
            allConstraints += LinearInequality<Flt64>(
                LinearPolynomial(triLambdaMonos, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.zero),
                Comparison.LE, "${name}_tri_lambda_$i"
            )
        }

        // sum(z_i) = 1 (exactly one triangle active)
        val sumZMonos = zVars.items.map { LinearMonomial(Flt64.one, it) }
        allConstraints += LinearInequality<Flt64>(
            LinearPolynomial(sumZMonos, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one),
            Comparison.EQ, "${name}_sum_z"
        )

        // lambda_i_j >= 0 (PctVariable already enforces this, but let's be explicit)
        // lambda_i_j <= 1 (PctVariable already enforces this)
        // These are handled by the variable type (Percentage = [0, 1])

        return addConstraints(model, allConstraints, converter) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            triangles: List<Triangle3>,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): BivariateLinearPiecewiseFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BivariateLinearPiecewiseFunction(x, y, triangles, converter, name, displayName)
    }
}
