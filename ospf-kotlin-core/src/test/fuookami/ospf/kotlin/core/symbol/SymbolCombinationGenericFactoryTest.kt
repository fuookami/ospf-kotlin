package fuookami.ospf.kotlin.core.symbol

import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class SymbolCombinationGenericFactoryTest {
    @Test
    fun linearIntermediateSymbolsShouldBuildRtn64Symbols() {
        val numberCase = GenericNumberCases.rtn64

        val symbols: LinearExpressionSymbols1<Rtn64> =
            LinearIntermediateSymbols("linear", Shape1(2), Rtn64)

        assertEquals("linear_0", symbols[0].name)
        assertEquals(numberCase.zero, symbols[0].polynomial.constant)
    }

    @Test
    fun quadraticIntermediateSymbolsShouldBuildRtn64Symbols() {
        val numberCase = GenericNumberCases.rtn64

        val symbols: QuadraticExpressionSymbols2<Rtn64> =
            QuadraticIntermediateSymbols("quadratic", Shape2(1, 1), Rtn64)

        assertEquals("quadratic_0_0", symbols.first().name)
        assertEquals(Quadratic, symbols.first().category)
        assertEquals(numberCase.zero, symbols.first().polynomial.constant)
    }

    @Test
    fun mapShouldPreserveGenericLinearPolynomialTypeForFourDimensions() {
        val numberCase = GenericNumberCases.rtn64

        val symbols = map<String, String, String, String, Rtn64>(
            name = "mapped",
            objs1 = listOf("a"),
            objs2 = listOf("b"),
            objs3 = listOf("c"),
            objs4 = listOf("d"),
            ctor = { _, _, _, _ -> LinearPolynomial(emptyList(), numberCase.five) }
        )

        assertEquals("mapped_0_0_0_0", symbols.first().name)
        assertEquals(numberCase.five, symbols.first().polynomial.constant)
    }

    @Test
    fun quantityLinearIntermediateSymbolAliasShouldStayGeneric() {
        val numberCase = GenericNumberCases.rtn64
        val symbol = LinearIntermediateSymbol.empty(Rtn64, name = "quantity")

        val quantity: QuantityLinearIntermediateSymbol<Rtn64> = Quantity(symbol, Meter)

        assertEquals(Meter, quantity.unit)
        assertEquals(numberCase.zero, quantity.value.polynomial.constant)
    }
}
