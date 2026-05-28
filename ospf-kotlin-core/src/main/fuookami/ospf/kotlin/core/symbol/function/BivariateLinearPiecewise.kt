/**
 * 双变量线性分段函数符号 / Bivariate linear piecewise function symbol
 *
 * 提供 [BivariateLinearPiecewiseFunction]，使用三角剖分实现双变量分段线性近似。
 *
 * Provides [BivariateLinearPiecewiseFunction] for bivariate piecewise linear approximation using triangulation.
 */
@file:Suppress("unused")
package fuookami.ospf.kotlin.core.symbol.function

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.Shape1
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.token.AddableTokenCollection

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
    val triangles: List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>,
    private val converter: IntoValue<V>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<V>, HasResultPolynomial<V> where V : RealNumber<V>, V : NumberField<V> {

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
    override val resultPolynomial: LinearPolynomial<V> get() = result

    override fun evaluate(values: Map<Symbol, V>): V? {
        val xVal = x.evaluateWith(values)?.let { converter.fromValue(it) } ?: return null
        val yVal = y.evaluateWith(values)?.let { converter.fromValue(it) } ?: return null
        val zero = Flt64.zero
        val one = Flt64.one

        for (i in triangles.indices) {
            val tri = triangles[i]
            val (u, v) = calculateBarycentric(tri, xVal, yVal)
            if (u != null && v != null &&
                (u geq zero) && (u leq one) &&
                (v geq zero) && (v leq one) &&
                ((u + v) leq one)
            ) {
                val zVal = tri.p1.z + (tri.p2.z - tri.p1.z) * u + (tri.p3.z - tri.p1.z) * v
                return converter.intoValue(zVal)
            }
        }
        return null
    }

    private fun calculateBarycentric(
        tri: Triangle<Point<Dim3, Flt64>, Dim3, Flt64>,
        px: Flt64,
        py: Flt64
    ): Pair<Flt64?, Flt64?> {
        val x1 = tri.p1.x
        val y1 = tri.p1.y
        val x2 = tri.p2.x
        val y2 = tri.p2.y
        val x3 = tri.p3.x
        val y3 = tri.p3.y

        val det = (y2 - y3) * (x1 - x3) + (x3 - x2) * (y1 - y3)
        val detTolerance = Flt64(1e-12)
        if (det.abs() ls detTolerance || det.abs() eq detTolerance) {
            return null to null
        }
        val u = ((y2 - y3) * (px - x3) + (x3 - x2) * (py - y3)) / det
        val v = ((y3 - y1) * (px - x3) + (x1 - x3) * (py - y3)) / det
        return u to v
    }

    override fun registerAuxiliaryTokens(tokens: AddableTokenCollection<V>): Try {
        return when (val result = tokens.add(helperVariables)) {
            is Ok -> ok
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }

    override fun registerConstraints(model: AbstractLinearMechanismModel<V>): Try {
        val zero = converter.zero
        val one = converter.one
        val three = converter.intoValue(Flt64(3.0))
        val allConstraints = mutableListOf<LinearInequality<V>>()

        // x constraint: x = sum over all triangles and vertices of (x_coord * lambda)
        val xMonos = mutableListOf<LinearMonomial<V>>()
        for (i in triangles.indices) {
            val tri = triangles[i]
            val lambdas = lambdaVars[i]
            xMonos += LinearMonomial(-converter.intoValue(tri.p1.x), lambdas[0])
            xMonos += LinearMonomial(-converter.intoValue(tri.p2.x), lambdas[1])
            xMonos += LinearMonomial(-converter.intoValue(tri.p3.x), lambdas[2])
        }
        xMonos += x.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(xMonos, x.constant),
            LinearPolynomial(emptyList(), zero),
            Comparison.EQ, "${name}_x_eq"
        )

        // y constraint: y = sum over all triangles and vertices of (y_coord * lambda)
        val yMonos = mutableListOf<LinearMonomial<V>>()
        for (i in triangles.indices) {
            val tri = triangles[i]
            val lambdas = lambdaVars[i]
            yMonos += LinearMonomial(-converter.intoValue(tri.p1.y), lambdas[0])
            yMonos += LinearMonomial(-converter.intoValue(tri.p2.y), lambdas[1])
            yMonos += LinearMonomial(-converter.intoValue(tri.p3.y), lambdas[2])
        }
        yMonos += y.monomials.map { LinearMonomial(it.coefficient, it.symbol) }
        allConstraints += LinearInequality(
            LinearPolynomial(yMonos, y.constant),
            LinearPolynomial(emptyList(), zero),
            Comparison.EQ, "${name}_y_eq"
        )

        // sum(all lambda) = 1
        val sumLambdaMonos = lambdaVars.flatMap { it.items.map { l -> LinearMonomial(one, l) } }
        allConstraints += LinearInequality(
            LinearPolynomial(sumLambdaMonos, zero),
            LinearPolynomial(emptyList(), one),
            Comparison.EQ, "${name}_sum_lambda"
        )

        // SOS2: for each triangle, sum(lambda_i_j) <= 3 * z_i
        for (i in triangles.indices) {
            val lambdas = lambdaVars[i]
            val zi = zVars[i]
            val triLambdaMonos = lambdas.items.map { LinearMonomial(one, it) } +
                LinearMonomial(-three, zi)
            allConstraints += LinearInequality(
                LinearPolynomial(triLambdaMonos, zero),
                LinearPolynomial(emptyList(), zero),
                Comparison.LE, "${name}_tri_lambda_$i"
            )
        }

        // sum(z_i) = 1 (exactly one triangle active)
        val sumZMonos = zVars.items.map { LinearMonomial(one, it) }
        allConstraints += LinearInequality(
            LinearPolynomial(sumZMonos, zero),
            LinearPolynomial(emptyList(), one),
            Comparison.EQ, "${name}_sum_z"
        )

        // lambda_i_j >= 0 (PctVariable already enforces this, but let's be explicit)
        // lambda_i_j <= 1 (PctVariable already enforces this)
        // These are handled by the variable type (Percentage = [0, 1])

        return addConstraints(model, allConstraints) ?: ok
    }
    companion object {
        operator fun <V> invoke(
            x: LinearPolynomial<V>,
            y: LinearPolynomial<V>,
            triangles: List<Triangle<Point<Dim3, Flt64>, Dim3, Flt64>>,
            converter: IntoValue<V>,
            name: String,
            displayName: String? = null
        ): BivariateLinearPiecewiseFunction<V> where V : RealNumber<V>, V : NumberField<V> =
            BivariateLinearPiecewiseFunction(x, y, triangles, converter, name, displayName)
    }
}
