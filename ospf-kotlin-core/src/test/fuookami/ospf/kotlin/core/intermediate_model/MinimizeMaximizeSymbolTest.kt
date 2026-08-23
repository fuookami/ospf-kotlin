package fuookami.ospf.kotlin.core.intermediate_model

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.variable.RealVar

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

        val symbol = LinearExpressionSymbol(x, Flt64, name = "obj_x")
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

        val symbol = LinearExpressionSymbol(x, Flt64, name = "obj_x")
        val result = model.maximize(symbol = symbol, name = "max_x")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Maximum, model.flattenSubObjects[0].category)
        model.close()
    }

    @Test
    fun `LinearMetaModel objective expands nested intermediate symbols`() {
        val model = LinearMetaModel<Flt64>(name = "test_nested_linear_objective", converter = flt64Converter)
        val x = RealVar("x")
        model.add(x)
        val inner = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(2.0), x)),
                constant = Flt64(5.0)
            ),
            name = "inner"
        )
        val outer = LinearExpressionSymbol<Flt64>(
            _utilsPolynomial = MutableLinearPolynomial(
                monomials = listOf(LinearMonomial(Flt64(4.0), inner)),
                constant = Flt64(7.0)
            ),
            name = "outer"
        )

        val result = model.maximize(LinearMonomial(Flt64(3.0), outer), name = "nested")

        assertTrue(result is Ok)
        val objective = model.flattenSubObjects.single()
        assertEquals(1, objective.cells.size)
        assertEquals(x, objective.cells.single().token.variable)
        assertEquals(Flt64(24.0), objective.cells.single().coefficient)
        assertEquals(Flt64(81.0), objective.constant)
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

        val symbol = QuadraticExpressionSymbol(x, Flt64, name = "obj_x")
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

        val symbol = QuadraticExpressionSymbol(x, Flt64, name = "obj_x")
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
            converter = flt64Converter,
            name = "obj_product"
        )
        val result = model.minimize(symbol = symbol, name = "min_product")

        assertTrue(result is Ok)
        assertEquals(1, model.flattenSubObjects.size)
        assertEquals(ObjectCategory.Minimum, model.flattenSubObjects[0].category)
        model.close()
    }
}
