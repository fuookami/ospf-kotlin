@file:Suppress("unused")

package fuookami.ospf.kotlin.core.function

import fuookami.ospf.kotlin.core.frontend.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.core.frontend.variable.AbstractVariableItem
import fuookami.ospf.kotlin.core.frontend.variable.BinVar
import fuookami.ospf.kotlin.core.frontend.variable.PctVar
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.inequality.LinearInequality
import fuookami.ospf.kotlin.math.symbol.inequality.Comparison
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.geometry.Point2
import fuookami.ospf.kotlin.math.geometry.x
import fuookami.ospf.kotlin.math.geometry.y

/**
 * Univariate piecewise linear function symbol using SOS2 lambda method.
 *
 * Given a set of points `(x_0, y_0), (x_1, y_1), ..., (x_n, y_n)`, the function
 * value y at input x is expressed as:
 *   x = sum(x_i * lambda_i)
 *   sum(lambda_i) = 1
 *   y = sum(y_i * lambda_i)
 *   SOS2 constraint: at most 2 adjacent lambdas can be nonzero
 *
 * The SOS2 constraint is encoded using binary segment-selection variables:
 *   - Create n+1 lambda variables (PctVar, continuous in [0,1])
 *   - Create n binary variables z_0..z_{n-1} (one per segment)
 *   - sum(z_i) = 1  (exactly one segment active)
 *   - lambda_0 <= z_0
 *   - lambda_i <= z_{i-1} + z_i  for i = 1..n-1
 *   - lambda_n <= z_{n-1}
 *
 * @param x the input linear polynomial whose value determines position along the piecewise curve
 * @param points list of break points sorted by increasing x (must have at least 1 point)
 * @param name unique name for this function
 * @param displayName optional human-readable display name
 */
class UnivariateLinearPiecewiseFunction<T : Field<T>>(
    val x: LinearPolynomial<T>,
    val points: List<Point2>,
    override var name: String,
    override var displayName: String? = null
) : MathFunctionSymbol<T> {

    init {
        require(points.size >= 1) { "points must have at least 1 element" }
    }

    private val n: Int = points.size - 1  // number of segments

    // Lambda variables: lambda_0 .. lambda_n (PctVar, each in [0,1])
    private val lambdas: List<AbstractVariableItem<*, *>> by lazy {
        (0..n).map { i -> PctVar("${name}_lambda_${i}") } as List<AbstractVariableItem<*, *>>
    }

    // SOS2 binary variables: z_0 .. z_{n-1}
    private val zBins: List<AbstractVariableItem<*, *>> by lazy {
        if (n >= 1) {
            (0 until n).map { i -> BinVar("${name}_z_${i}") } as List<AbstractVariableItem<*, *>>
        } else {
            emptyList()
        }
    }

    override val helperVariables: List<AbstractVariableItem<*, *>>
        get() = lambdas + zBins

    /**
     * The output polynomial y = sum(y_i * lambda_i).
     * Exposed for use in objectives or further constraints.
     */
    val result: LinearPolynomial<T> by lazy {
        val monos = lambdas.mapIndexed { i, lambda ->
            LinearMonomial(oneOf<T>() * points[i].y.asFlt64().asGeneric(), lambda)
        }
        LinearPolynomial(monos, zeroOf())
    }

    /**
     * Evaluate the piecewise linear function at a given x value via linear interpolation.
     * Finds the segment containing xValue and interpolates between adjacent points.
     */
    override fun evaluate(values: Map<Symbol, T>): T? {
        val xValue = x.evaluate(values)?.asFlt64()?.toDouble() ?: return null

        val pts = points.map { it.x.toDouble() to it.y.toDouble() }

        // Clamp to endpoints if outside range
        if (xValue <= pts.first().first) {
            @Suppress("UNCHECKED_CAST")
            return Flt64(pts.first().second) as T
        }
        if (xValue >= pts.last().first) {
            @Suppress("UNCHECKED_CAST")
            return Flt64(pts.last().second) as T
        }

        // Find the segment containing xValue
        for (i in 0 until points.size - 1) {
            val xNext = pts[i + 1].first
            if (xNext >= xValue) {
                val xCurr = pts[i].first
                val dx = xNext - xCurr
                if (dx == 0.0) {
                    @Suppress("UNCHECKED_CAST")
                    return Flt64(pts[i].second) as T
                }
                val dy = pts[i + 1].second - pts[i].second
                val yVal = pts[i].second + dy / dx * (xValue - xCurr)
                @Suppress("UNCHECKED_CAST")
                return Flt64(yVal) as T
            }
        }

        @Suppress("UNCHECKED_CAST")
        return Flt64(pts.last().second) as T
    }

    override fun register(model: AbstractLinearMetaModel): Try {
        // Add all helper variables
        val varsToAdd = lambdas + zBins
        if (varsToAdd.isNotEmpty()) {
            when (val result = model.add(varsToAdd)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        val xPoly = x.asFlt64Poly()

        // Constraint: x = sum(x_i * lambda_i)
        val xLhs = lambdas.mapIndexed { i, lambda ->
            LinearMonomial(points[i].x, lambda)
        }.toMutableList()
        val xConstraint = LinearInequality<Flt64>(
            LinearPolynomial(xLhs, Flt64.zero),
            xPoly,
            Comparison.EQ,
            "${name}_x_def"
        )
        when (val result = model.addConstraint(relation = xConstraint, name = xConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // Constraint: sum(lambda_i) = 1
        val sumLambdaLhs = lambdas.map { lambda ->
            LinearMonomial(Flt64.one, lambda)
        }.toMutableList()
        val sumLambdaConstraint = LinearInequality<Flt64>(
            LinearPolynomial(sumLambdaLhs, Flt64.zero),
            LinearPolynomial(emptyList(), Flt64.one),
            Comparison.EQ,
            "${name}_lambda_sum"
        )
        when (val result = model.addConstraint(relation = sumLambdaConstraint, name = sumLambdaConstraint.name)) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // SOS2 constraints (only when there are 2+ points, i.e. n >= 1)
        if (n >= 1) {
            // sum(z_i) = 1 (exactly one segment active)
            val sumZLhs = zBins.map { z ->
                LinearMonomial(Flt64.one, z)
            }.toMutableList()
            val sumZConstraint = LinearInequality<Flt64>(
                LinearPolynomial(sumZLhs, Flt64.zero),
                LinearPolynomial(emptyList(), Flt64.one),
                Comparison.EQ,
                "${name}_sos2_sum"
            )
            when (val result = model.addConstraint(relation = sumZConstraint, name = sumZConstraint.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // lambda_0 <= z_0
            val lam0 = lambdas[0]
            val z0 = zBins[0]
            val c0 = LinearInequality<Flt64>(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, lam0)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, z0)), Flt64.zero),
                Comparison.LE,
                "${name}_sos2_lambda_0"
            )
            when (val result = model.addConstraint(relation = c0, name = c0.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }

            // lambda_i <= z_{i-1} + z_i  for i = 1..n-1
            for (i in 1 until n) {
                val lam = lambdas[i]
                val zPrev = zBins[i - 1]
                val zCurr = zBins[i]
                val rhsMono = listOf(
                    LinearMonomial(Flt64.one, zPrev),
                    LinearMonomial(Flt64.one, zCurr)
                )
                val c = LinearInequality<Flt64>(
                    LinearPolynomial(listOf(LinearMonomial(Flt64.one, lam)), Flt64.zero),
                    LinearPolynomial(rhsMono, Flt64.zero),
                    Comparison.LE,
                    "${name}_sos2_lambda_${i}"
                )
                when (val result = model.addConstraint(relation = c, name = c.name)) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }

            // lambda_n <= z_{n-1}
            val lamN = lambdas[n]
            val zN = zBins[n - 1]
            val cN = LinearInequality<Flt64>(
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, lamN)), Flt64.zero),
                LinearPolynomial(listOf(LinearMonomial(Flt64.one, zN)), Flt64.zero),
                Comparison.LE,
                "${name}_sos2_lambda_${n}"
            )
            when (val result = model.addConstraint(relation = cN, name = cN.name)) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }

    companion object {
        /**
         * Factory for Flt64-typed piecewise function.
         */
        operator fun invoke(
            x: LinearPolynomial<Flt64>,
            points: List<Point2>,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<Flt64> = UnivariateLinearPiecewiseFunction(
            x = x,
            points = points,
            name = name,
            displayName = displayName
        )

        /**
         * Factory for a single monomial input.
         */
        operator fun invoke(
            x: LinearMonomial<Flt64>,
            points: List<Point2>,
            name: String,
            displayName: String? = null
        ): UnivariateLinearPiecewiseFunction<Flt64> = UnivariateLinearPiecewiseFunction(
            x = LinearPolynomial(listOf(x), Flt64.zero),
            points = points,
            name = name,
            displayName = displayName
        )
    }
}

/**
 * Helper: cast a Flt64 value to a generic Field<T> coefficient.
 * Used internally since the piecewise points are always Flt64.
 */
@Suppress("UNCHECKED_CAST")
private fun <T : Field<T>> Flt64.asGeneric(): T = this as T
