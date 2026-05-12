package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.function.LinearFunctionSymbolAdapter
import fuookami.ospf.kotlin.core.intermediate_symbol.function.ProductFunction
import fuookami.ospf.kotlin.core.intermediate_symbol.function.SlackFunction
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.utils.functional.Ok
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

class MinimizeMaximizeSymbolTest {

    @Test
    fun `LinearMetaModel minimize with LinearIntermediateSymbol`() {
        val model = LinearMetaModel<Flt64>(name = "test_min_linear_symbol", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)

        val symbol = LinearExpressionSymbol(x, name = "obj_x")
        val result = model.minimize(symbol = symbol, name = "min_x")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Minimum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `LinearMetaModel maximize with LinearIntermediateSymbol`() {
        val model = LinearMetaModel<Flt64>(name = "test_max_linear_symbol", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)

        val symbol = LinearExpressionSymbol(x, name = "obj_x")
        val result = model.maximize(symbol = symbol, name = "max_x")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Maximum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `LinearMetaModel minimize with function adapter symbol`() {
        val model = LinearMetaModel<Flt64>(name = "test_min_function_adapter_symbol", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)

        val xPoly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val slack = SlackFunction(
            x = xPoly,
            y = LinearPolynomial(emptyList(), Flt64.zero),
            converter = flt64Converter,
            name = "obj_slack"
        )
        val symbol = LinearFunctionSymbolAdapter(slack, flt64Converter)
        val result = model.minimize(symbol = symbol, name = "min_slack")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Minimum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `QuadraticMetaModel minimize with QuadraticIntermediateSymbol`() {
        val model = QuadraticMetaModel<Flt64>(name = "test_min_quad_symbol", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)

        val symbol = QuadraticExpressionSymbol(x, name = "obj_x")
        val result = model.minimize(symbol = symbol, name = "min_x")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Minimum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `QuadraticMetaModel maximize with QuadraticIntermediateSymbol`() {
        val model = QuadraticMetaModel<Flt64>(name = "test_max_quad_symbol", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)

        val symbol = QuadraticExpressionSymbol(x, name = "obj_x")
        val result = model.maximize(symbol = symbol, name = "max_x")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Maximum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `QuadraticMetaModel minimize with product function symbol`() {
        val model = QuadraticMetaModel<Flt64>(name = "test_min_product_symbol", converter = flt64Converter)
        val x = RealVar("x")
        val y = RealVar("y")
        model.add(listOf(x, y))

        val left = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, x)),
            constant = Flt64.zero
        )
        val right = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64.one, y)),
            constant = Flt64.zero
        )
        val symbol = ProductFunction(
            left = left,
            right = right,
            name = "obj_product"
        )
        val result = model.minimize(symbol = symbol, name = "min_product")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Minimum, model.flattenSubObjects[0].category)
        model.close()
    }
}
