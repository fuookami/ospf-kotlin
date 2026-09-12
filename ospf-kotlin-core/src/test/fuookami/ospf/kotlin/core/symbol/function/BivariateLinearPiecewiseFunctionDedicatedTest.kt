package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.Triangle
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.geometry.point3
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok

/** Dedicated contract test for the BivariateLinearPiecewiseFunction symbol. */
class BivariateLinearPiecewiseFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(BivariateLinearPiecewiseFunction::class.java))
    }

    @Test
    fun acceptsBoundaryWithinSharedToleranceAndRejectsDegenerateTriangle() {
        val triangle = Triangle(
            point3(Flt64.zero, Flt64.zero, Flt64.zero),
            point3(Flt64.one, Flt64.zero, Flt64(10.0)),
            point3(Flt64.zero, Flt64.one, Flt64(20.0))
        )
        fun functionAt(x: Flt64): BivariateLinearPiecewiseFunction<Flt64> {
            return BivariateLinearPiecewiseFunction(
                x = LinearPolynomial(emptyList(), x),
                y = LinearPolynomial(emptyList(), Flt64.zero),
                triangles = listOf(triangle),
                converter = IntoValue.Identity,
                name = "blp_boundary_$x"
            )
        }
        assertTrue(functionAt(Flt64(-0.5e-12)).evaluate(emptyMap()) != null)
        assertNull(functionAt(Flt64(-2.0e-12)).evaluate(emptyMap()))

        assertFailsWith<IllegalArgumentException> {
            BivariateLinearPiecewiseFunction(
                x = LinearPolynomial(emptyList(), Flt64.zero),
                y = LinearPolynomial(emptyList(), Flt64.zero),
                triangles = listOf(
                    Triangle(
                        point3(Flt64.zero, Flt64.zero, Flt64.zero),
                        point3(Flt64.one, Flt64.one, Flt64.one),
                        point3(Flt64(2.0), Flt64(2.0), Flt64(2.0))
                    )
                ),
                converter = IntoValue.Identity,
                name = "blp_degenerate"
            )
        }
    }

    @Test
    fun registersSelectorLambdaAndSurfaceRows() {
        val zero = LinearPolynomial<Flt64>(emptyList(), Flt64.zero)
        val triangle = Triangle(
            point3(Flt64.zero, Flt64.zero, Flt64.zero),
            point3(Flt64.one, Flt64.zero, Flt64(10.0)),
            point3(Flt64.zero, Flt64.one, Flt64(20.0))
        )
        val metaModel = LinearMetaModel<Flt64>(
            name = "blp-constraints",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            val function = BivariateLinearPiecewiseFunction(
                x = zero,
                y = zero,
                triangles = listOf(triangle),
                converter = IntoValue.Identity,
                name = "blp_rows"
            )
            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            assertEquals(before + 5, mechanism.value.constraints.size)
            val names = mechanism.value.constraints.map { it.name }
            assertTrue("blp_rows_x_eq" in names)
            assertTrue("blp_rows_y_eq" in names)
            assertTrue("blp_rows_sum_lambda" in names)
            assertTrue("blp_rows_tri_lambda_0" in names)
            assertTrue("blp_rows_sum_z" in names)
        } finally {
            metaModel.close()
        }
    }
}
