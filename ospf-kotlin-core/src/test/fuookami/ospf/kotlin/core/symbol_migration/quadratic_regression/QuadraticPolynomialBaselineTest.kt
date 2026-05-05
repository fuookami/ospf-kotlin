package fuookami.ospf.kotlin.core.frontend.symbol_migration.quadratic_regression

import fuookami.ospf.kotlin.core.token.AutoTokenTable
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.symbol.operation.evaluate
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.MissingValuePolicy
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class QuadraticPolynomialBaselineTest {
    @Test
    fun evaluate_shouldMatchCurrentQuadraticBehavior() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64.one, x, x),
                QuadraticMonomial.quadratic(Flt64.one, x, y),
                QuadraticMonomial.quadratic(Flt64.one, y, x),
                QuadraticMonomial.linear(Flt64(3.0), x)
            ),
            constant = Flt64(4.0)
        )

        val value = polynomial.evaluate(
            values = mapOf(x to Flt64(2.0), y to Flt64(5.0)),
            policy = MissingValuePolicy.ReturnNull
        )

        assertNotNull(value)
        assertTrue(value eq Flt64(34.0))
    }

    @Test
    fun evaluate_shouldRespectZeroIfNoneSwitch() {
        val x = RealVar("x")
        val y = RealVar("y")
        val polynomial = QuadraticPolynomial(
            monomials = listOf(
                QuadraticMonomial.quadratic(Flt64.one, x, x),
                QuadraticMonomial.quadratic(Flt64.one, y, y)
            ),
            constant = Flt64.one
        )

        val missingAsNull = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            policy = MissingValuePolicy.ReturnNull
        )
        assertNull(missingAsNull)

        val missingAsZero = polynomial.evaluate(
            values = mapOf(x to Flt64(3.0)),
            policy = MissingValuePolicy.AsZero
        )
        assertNotNull(missingAsZero)
        assertTrue(missingAsZero eq Flt64(10.0))
    }

    @Test
    fun quadraticExpressionSymbolEvaluate_shouldRespectSelfValueOverrideOnTokenTablePath() {
        val x = RealVar("x")
        val tokenTable = AutoTokenTable<Flt64>(Quadratic, false)
        tokenTable.add(x)
        tokenTable.setSolution(mapOf(x to Flt64(5.0)))

        val symbol = QuadraticExpressionSymbol(
            _utilsPolynomial = fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial(
                monomials = listOf(fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial.quadratic(Flt64.one, x, x)),
                constant = Flt64.one
            ),
            name = "q_linear"
        )

        val overridden = symbol.evaluate(
            values = mapOf<Symbol, Flt64>(
                symbol to Flt64(40.0),
                x to Flt64(3.0)
            ),
            tokenTable = tokenTable,
            converter = flt64Converter,
            zeroIfNone = false
        )
        assertNotNull(overridden)
        assertTrue(overridden eq Flt64(40.0))

        val fromValues = symbol.evaluate(
            values = mapOf<Symbol, Flt64>(x to Flt64(3.0)),
            tokenTable = tokenTable,
            converter = flt64Converter,
            zeroIfNone = false
        )
        assertNotNull(fromValues)
        assertTrue(fromValues eq Flt64(10.0))
    }
}
