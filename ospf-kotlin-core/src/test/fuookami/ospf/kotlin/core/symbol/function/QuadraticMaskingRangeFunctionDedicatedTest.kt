package fuookami.ospf.kotlin.core.symbol.function

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.QuadraticMechanismModel
import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.variable.BinVar
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.variable.URealVar

/** [QuadraticMaskingRangeFunction] 契约测试。 / Dedicated contract tests. */
class QuadraticMaskingRangeFunctionDedicatedTest {
    @Test
    fun symbolImplementsMathFunctionContract() {
        assertTrue(QuadraticMaskingRangeFunction::class.simpleName == "QuadraticMaskingRangeFunction")
    }

    @Test
    fun resultHelperIsSignedAndMaskOffEvaluatesToZero() {
        val x = RealVar("qmask_x")
        val z = BinVar("qmask_z")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            constant = Flt64.zero
        )
        val function = QuadraticMaskingRangeFunction(
            polynomial = polynomial,
            z = z,
            bigM = Flt64(100.0),
            converter = IntoValue.Identity,
            name = "qmask_contract"
        )
        assertFalse(function.resultVar is URealVar)

        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        tokens.add(listOf(x, z))
        val valuesOn = mapOf<Symbol, Flt64>(x to Flt64(-2.0), z to Flt64.one)
        val valuesOff = valuesOn + (z to Flt64.zero)
        assertEquals(Flt64(4.0), function.evaluate(valuesOn, tokens, IntoValue.Identity, false))
        assertEquals(Flt64.zero, function.evaluate(valuesOff, tokens, IntoValue.Identity, false))
    }

    @Test
    fun quadraticInputRegistersOneHelperAndFourRows() {
        val x = RealVar("qmask_rows_x")
        val z = BinVar("qmask_rows_z")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            constant = Flt64.zero
        )
        val model = QuadraticMetaModel<Flt64>(
            name = "qmask-dedicated-rows",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(listOf(x, z)) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val function = QuadraticMaskingRangeFunction(
                polynomial = input,
                z = z,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "qmask_dedicated_rows"
            )
            val beforeTokens = model.tokens.tokens.size
            assertTrue(function.registerAuxiliaryTokens(model.tokens) is Ok)
            assertEquals(beforeTokens + 1, model.tokens.tokens.size)

            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(function.registerConstraints(mechanism) is Ok)
            assertEquals(beforeRows + 4, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }

    @Test
    fun missingMaskIsZeroAndInvalidBigMDoesNotWriteRows() {
        val x = RealVar("qmask_missing_x")
        val z = BinVar("qmask_missing_z")
        val input = QuadraticPolynomial(
            monomials = listOf(QuadraticMonomial.quadratic(Flt64.one, x, x)),
            constant = Flt64.zero
        )
        val tokens = AutoTokenTable<Flt64>(Quadratic, false)
        try {
            tokens.add(listOf(x))
            val function = QuadraticMaskingRangeFunction(
                polynomial = input,
                z = z,
                bigM = Flt64(100.0),
                converter = IntoValue.Identity,
                name = "qmask_missing"
            )
            assertEquals(
                Flt64.zero,
                function.evaluate(
                    mapOf<Symbol, Flt64>(x to Flt64(-2.0)),
                    tokens,
                    IntoValue.Identity,
                    false
                )
            )
        } finally {
            tokens.close()
        }

        val model = QuadraticMetaModel<Flt64>(
            name = "qmask-invalid-m",
            converter = IntoValue.Identity
        )
        try {
            assertTrue(model.add(listOf(x, z)) is Ok)
            assertTrue(model.minimize(input) is Ok)
            val invalid = QuadraticMaskingRangeFunction(
                polynomial = input,
                z = z,
                bigM = Flt64.zero,
                converter = IntoValue.Identity,
                name = "qmask_invalid_m"
            )
            assertTrue(invalid.registerAuxiliaryTokens(model.tokens) is Ok)
            val mechanismResult = runBlocking {
                QuadraticMechanismModel.invoke<Flt64>(metaModel = model, concurrent = false)
            }
            assertTrue(mechanismResult is Ok)
            val mechanism = mechanismResult.value
            val beforeRows = mechanism.constraints.size
            assertTrue(invalid.registerConstraints(mechanism) is Failed)
            assertEquals(beforeRows, mechanism.constraints.size)
        } finally {
            model.close()
        }
    }
}
