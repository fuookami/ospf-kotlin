package fuookami.ospf.kotlin.core.intermediate_model

import fuookami.ospf.kotlin.core.intermediate_symbol.LinearExpressionSymbol
import fuookami.ospf.kotlin.core.intermediate_symbol.QuadraticExpressionSymbol
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
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
}
