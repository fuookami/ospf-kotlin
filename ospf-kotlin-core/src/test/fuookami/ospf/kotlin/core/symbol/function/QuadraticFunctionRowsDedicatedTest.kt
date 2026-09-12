package fuookami.ospf.kotlin.core.symbol.function

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Ok

/** Independent row/helper tests for the quadratic-function group. */
class QuadraticFunctionRowsDedicatedTest {
    @Test
    fun genuinelyQuadraticFunctionsRegisterOnlyTheirExpectedHelpersAndRows() {
        val x = RealVar("quadratic_rows_x")
        val y = RealVar("quadratic_rows_y")
        val z = BinVar("quadratic_rows_z")
        val model = QuadraticMetaModel<Flt64>(
            name = "quadratic-function-rows",
            converter = IntoValue.Identity
        )

        try {
            assertTrue(model.add(listOf(x, y, z)) is Ok)
            val input = QuadraticPolynomial(
                monomials = listOf(
                    QuadraticMonomial.quadratic(Flt64.one, x, y),
                    QuadraticMonomial.linear(Flt64.one, x)
                ),
                constant = Flt64.zero
            )
            assertTrue(model.minimize(input) is Ok)

            val qLinear = QuadraticLinearFunction(
                polynomial = input,
                converter = IntoValue.Identity,
                name = "rows_qlinear"
            )
            val qStep = QuadraticInStepRangeFunction(
                x = input,
                lower = Flt64.zero,
                upper = Flt64(4.0),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "rows_qstep"
            )
            val qMask = QuadraticMaskingRangeFunction(
                polynomial = input,
                z = z,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "rows_qmask"
            )
            val qPositive = QuadraticPositivePartFunction(
                input = input,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "rows_qpositive"
            )
            val qSlack = QuadraticSlackFunction(
                left = input,
                right = QuadraticPolynomial(emptyList(), Flt64.one),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "rows_qslack"
            )
            val qSlackRange = QuadraticSlackRangeFunction(
                input = input,
                lower = Flt64.zero,
                upper = Flt64(4.0),
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "rows_qslack_range"
            )
            val product = ProductFunction(
                left = LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, x)),
                    Flt64.zero
                ),
                right = LinearPolynomial(
                    listOf(LinearMonomial(Flt64.one, y)),
                    Flt64.zero
                ),
                converter = IntoValue.Identity,
                name = "rows_product"
            )

            val functions = listOf(
                qLinear to 1,
                qStep to 4,
                qMask to 1,
                qPositive to 3,
                qSlack to 6,
                qSlackRange to 6,
                product to 0
            )
            val initialTokenCount = model.tokens.tokens.size
            var expectedTokenCount = initialTokenCount
            for ((function, helpers) in functions) {
                assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok, function.name)
                expectedTokenCount += helpers
                assertEquals(expectedTokenCount, model.tokens.tokens.size, function.name)
            }

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value

            val expectedRows = listOf(
                qLinear to 1,
                qStep to 9,
                qMask to 4,
                qPositive to 5,
                qSlack to 10,
                qSlackRange to 10,
                product to 0
            )
            for ((function, rows) in expectedRows) {
                val before = mechanism.constraints.size
                assertTrue(function.registerConstraints(mechanism) is Ok, function.name)
                assertEquals(before + rows, mechanism.constraints.size, function.name)
            }
        } finally {
            model.close()
        }
    }

    @Test
    fun evaluationCoversSignsMissingMaskAndDefaultTolerance() {
        val x = RealVar("quadratic_eval_x")
        val z = BinVar("quadratic_eval_z")
        val linearInput = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.linear(Flt64.one, x)),
            constant = Flt64.zero
        )
        val step = QuadraticInStepRangeFunction(
            x = linearInput,
            lower = Flt64.zero,
            upper = Flt64(4.0),
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "evaluation_qstep"
        )
        val mask = QuadraticMaskingRangeFunction(
            polynomial = QuadraticPolynomial(
                listOf(QuadraticMonomial.quadratic(Flt64(-1.0), x, x)),
                Flt64.zero
            ),
            z = z,
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "evaluation_qmask"
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        try {
            assertTrue(tokens.add(listOf(x, z)) is Ok)
            fun value(xValue: Double, maskValue: Flt64? = null): Flt64? {
                val values = mutableMapOf<Symbol, Flt64>(x to Flt64(xValue))
                if (maskValue != null) values[z] = maskValue
                return step.evaluate(values, tokens, IntoValue.Identity, false)
            }

            assertEquals(Flt64.zero, value(-1.0))
            assertEquals(Flt64.zero, value(0.0))
            assertEquals(Flt64(2.0), value(2.0))
            assertNull(value(4.0 + 0.5e-6))
            assertEquals(Flt64.zero, value(4.0 + 1.0e-6))

            val missingMask = mask.evaluate(
                mapOf<Symbol, Flt64>(x to Flt64(-2.0)),
                tokens,
                IntoValue.Identity,
                false
            )
            assertEquals(Flt64.zero, missingMask)
            assertEquals(
                Flt64(-4.0),
                mask.evaluate(
                    mapOf<Symbol, Flt64>(x to Flt64(-2.0), z to Flt64.one),
                    tokens,
                    IntoValue.Identity,
                    false
                )
            )
            assertEquals(
                Flt64.zero,
                mask.evaluate(
                    mapOf<Symbol, Flt64>(x to Flt64(-2.0), z to Flt64.zero),
                    tokens,
                    IntoValue.Identity,
                    false
                )
            )
        } finally {
            tokens.close()
        }
    }

    @Test
    fun invalidBoundsAreRejectedEagerlyAndInvalidBigMDoesNotWriteRows() {
        val x = RealVar("quadratic_invalid_x")
        val input = QuadraticPolynomial(
            listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            Flt64.zero
        )
        assertFailsWith<IllegalArgumentException> {
            QuadraticInStepRangeFunction(
                x = input,
                lower = Flt64(4.0),
                upper = Flt64.zero,
                converter = IntoValue.Identity,
                name = "invalid_step_bounds"
            )
        }
        assertFailsWith<IllegalArgumentException> {
            QuadraticSlackRangeFunction(
                input = input,
                lower = Flt64(4.0),
                upper = Flt64.zero,
                converter = IntoValue.Identity,
                name = "invalid_slack_range_bounds"
            )
        }

        val model = QuadraticMetaModel<Flt64>(
            name = "quadratic-invalid-big-m",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(x) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val invalid = QuadraticMaskingRangeFunction(
                polynomial = input,
                z = BinVar("invalid_mask"),
                bigM = Flt64.zero,
                converter = IntoValue.Identity,
                name = "invalid_mask_m"
            )
            assertTrue(invalid.registerAuxiliaryTokens(model.tokens) is Ok)
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val before = mechanism.constraints.size
            assertTrue(invalid.registerConstraints(mechanism) is Failed)
            assertEquals(before, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
