package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.LinearMechanismModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.RealVar

/** [XorFunction] 契约测试。 / Dedicated contract tests. */
class XorFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(MathFunctionSymbol::class.java.isAssignableFrom(XorFunction::class.java))
    }

    @Test
    fun resultIsOneOnlyForExactlyOneOfThreeNonZeroInputs() {
        val variables = (0 until 3).map { RealVar("xor_x$it") }
        val function = XorFunction(
            polynomials = variables.map { variable ->
                LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, variable)), Flt64.zero
                )
            },
            converter = IntoValue.Identity,
            name = "xor_three"
        )

        fun evaluate(values: List<Flt64>): Flt64? = function.evaluate(
            variables.zip(values).associate { (variable, value) ->
                (variable as Symbol) to value
            }
        )

        assertEquals(Flt64.zero, evaluate(listOf(Flt64.zero, Flt64.zero, Flt64.zero)))
        assertEquals(Flt64.one, evaluate(listOf(Flt64.one, Flt64.zero, Flt64.zero)))
        assertEquals(Flt64.zero, evaluate(listOf(Flt64.one, Flt64.one, Flt64.zero)))
        assertEquals(Flt64.zero, evaluate(listOf(Flt64.one, Flt64.one, Flt64.one)))
    }

    @Test
    fun evaluatorAndSolverShareZeroBandAndConstraintShape() {
        val tolerance = Flt64(0.1)
        val strictBoundary = Flt64(0.2)
        fun constant(value: Flt64) = LinearPolynomial<Flt64>(emptyList(), value)
        val zeroBandFunction = XorFunction(
            polynomials = listOf(constant(Flt64(0.1))),
            converter = IntoValue.Identity,
            tolerance = tolerance,
            strictBoundary = strictBoundary,
            name = "xor_boundary"
        )
        assertEquals(Flt64.zero, zeroBandFunction.evaluate(emptyMap()))
        assertNull(
            XorFunction(
                polynomials = listOf(constant(Flt64(0.15))),
                converter = IntoValue.Identity,
                tolerance = tolerance,
                strictBoundary = strictBoundary,
                name = "xor_gap"
            ).evaluate(emptyMap())
        )
        val strictBoundaryFunction = XorFunction(
            polynomials = listOf(constant(Flt64(0.2))),
            converter = IntoValue.Identity,
            tolerance = tolerance,
            strictBoundary = strictBoundary,
            name = "xor_boundary_on"
        )
        assertEquals(Flt64.one, strictBoundaryFunction.evaluate(emptyMap()))

        val zero = constant(Flt64.zero)
        val metaModel = LinearMetaModel<Flt64>(
            name = "xor-constraint-shape",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(metaModel.minimize(zero) is Ok)
            val mechanism = runBlocking {
                LinearMechanismModel.invoke(metaModel = metaModel, concurrent = false)
            }
            assertTrue(mechanism is Ok)
            val function = XorFunction(
                polynomials = listOf(zero, zero, zero),
                converter = IntoValue.Identity,
                bigM = Flt64(42.0),
                name = "xor_rows"
            )
            val before = mechanism.value.constraints.size
            assertTrue(function.registerConstraints(mechanism.value) is Ok)
            assertEquals(before + 19, mechanism.value.constraints.size)
            val names = mechanism.value.constraints.map { it.name }
            assertTrue("xor_rows_xor_nz_0_band_ub" in names)
            assertTrue("xor_rows_xor_nz_2_out_ub" in names)
            assertTrue("xor_rows_xor_sum_ub" in names)
            assertTrue("xor_rows_xor_single_1" in names)
            assertTrue("xor_rows_xor_pair_1_2" in names)
        } finally {
            metaModel.close()
        }
    }
}
