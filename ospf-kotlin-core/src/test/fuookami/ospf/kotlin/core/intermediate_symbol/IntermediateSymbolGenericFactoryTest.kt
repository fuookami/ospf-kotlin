package fuookami.ospf.kotlin.core.intermediate_symbol

import fuookami.ospf.kotlin.core.testing.GenericNumberCases
import fuookami.ospf.kotlin.core.variable.RealVar
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Rtn64
import fuookami.ospf.kotlin.math.symbol.Linear
import fuookami.ospf.kotlin.math.symbol.Quadratic
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableLinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.MutableQuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class IntermediateSymbolGenericFactoryTest {
    @Test
    fun emptyIntermediateSymbolsShouldUseProvidedConstants() {
        val numberCase = GenericNumberCases.rtn64

        val linear = LinearIntermediateSymbol.empty(Rtn64, name = "linear_empty")
        val quadratic = QuadraticIntermediateSymbol.empty(Rtn64, name = "quadratic_empty")

        assertEquals(numberCase.zero, linear.polynomial.constant)
        assertEquals(numberCase.zero, quadratic.polynomial.constant)
        assertEquals(Quadratic, quadratic.category)
    }

    @Test
    fun linearExpressionFactoriesShouldPreserveRtn64() {
        val numberCase = GenericNumberCases.rtn64
        val variable = RealVar("x")
        val monomial = LinearMonomial(numberCase.five, variable)
        val polynomial = LinearPolynomial(listOf(monomial), numberCase.two)
        val mutablePolynomial = MutableLinearPolynomial(listOf(monomial), numberCase.ten)

        val fromVariable = LinearExpressionSymbol(variable, Rtn64, name = "from_variable")
        val fromSymbol = LinearExpressionSymbol(fromVariable, Rtn64, name = "from_symbol")
        val fromPolynomial = LinearExpressionSymbol(polynomial, name = "from_polynomial")
        val fromMonomial = LinearExpressionSymbol(monomial, name = "from_monomial")
        val fromMutablePolynomial = LinearExpressionSymbol(mutablePolynomial, name = "from_mutable_polynomial")
        val fromConstant = LinearExpressionSymbol(numberCase.five, name = "from_constant")
        val fromEmpty = LinearExpressionSymbol(Rtn64, name = "from_empty")

        assertEquals(numberCase.zero, fromVariable.polynomial.constant)
        assertEquals(numberCase.zero, fromSymbol.polynomial.constant)
        assertEquals(numberCase.two, fromPolynomial.polynomial.constant)
        assertEquals(numberCase.zero, fromMonomial.polynomial.constant)
        assertEquals(numberCase.ten, fromMutablePolynomial.polynomial.constant)
        assertEquals(numberCase.five, fromConstant.polynomial.constant)
        assertEquals(numberCase.zero, fromEmpty.polynomial.constant)
    }

    @Test
    fun quadraticExpressionFactoriesShouldPreserveRtn64() {
        val numberCase = GenericNumberCases.rtn64
        val variable = RealVar("x")
        val linearMonomial = LinearMonomial(numberCase.five, variable)
        val quadraticMonomial = QuadraticMonomial.quadratic(numberCase.two, variable, variable)
        val linearPolynomial = LinearPolynomial(listOf(linearMonomial), numberCase.two)
        val quadraticPolynomial = QuadraticPolynomial(listOf(quadraticMonomial), numberCase.five)
        val mutableQuadraticPolynomial = MutableQuadraticPolynomial(listOf(quadraticMonomial), numberCase.ten)

        val fromVariable = QuadraticExpressionSymbol(variable, Rtn64, name = "from_variable")
        val fromLinearSymbol = QuadraticExpressionSymbol(
            LinearExpressionSymbol(linearPolynomial, name = "linear"),
            Rtn64,
            name = "from_linear_symbol"
        )
        val fromQuadraticSymbol = QuadraticExpressionSymbol(fromVariable, Rtn64, name = "from_quadratic_symbol")
        val fromLinearPolynomial = QuadraticExpressionSymbol(linearPolynomial, name = "from_linear_polynomial")
        val fromLinearMonomial = QuadraticExpressionSymbol(linearMonomial, name = "from_linear_monomial")
        val fromQuadraticPolynomial = QuadraticExpressionSymbol(quadraticPolynomial, name = "from_quadratic_polynomial")
        val fromQuadraticMonomial = QuadraticExpressionSymbol(quadraticMonomial, name = "from_quadratic_monomial")
        val fromMutableQuadraticPolynomial = QuadraticExpressionSymbol(mutableQuadraticPolynomial, name = "from_mutable_quadratic")
        val fromConstant = QuadraticExpressionSymbol(numberCase.five, name = "from_constant")
        val fromEmpty = QuadraticExpressionSymbol(Rtn64, name = "from_empty")

        assertEquals(numberCase.zero, fromVariable.polynomial.constant)
        assertEquals(numberCase.zero, fromLinearSymbol.polynomial.constant)
        assertEquals(numberCase.zero, fromQuadraticSymbol.polynomial.constant)
        assertEquals(numberCase.two, fromLinearPolynomial.polynomial.constant)
        assertEquals(numberCase.zero, fromLinearMonomial.polynomial.constant)
        assertEquals(numberCase.five, fromQuadraticPolynomial.polynomial.constant)
        assertEquals(numberCase.zero, fromQuadraticMonomial.polynomial.constant)
        assertEquals(numberCase.ten, fromMutableQuadraticPolynomial.polynomial.constant)
        assertEquals(numberCase.five, fromConstant.polynomial.constant)
        assertEquals(numberCase.zero, fromEmpty.polynomial.constant)
        assertEquals(Linear, fromLinearPolynomial.category)
        assertEquals(Quadratic, fromQuadraticPolynomial.category)
    }

    @Test
    fun flt64VariableFactoriesShouldRequireExplicitConstants() {
        val variable = RealVar("x")

        val linear = LinearExpressionSymbol(variable, Flt64, name = "linear")
        val quadratic = QuadraticExpressionSymbol(variable, Flt64, name = "quadratic")

        assertEquals(Flt64.zero, linear.polynomial.constant)
        assertEquals(Flt64.zero, quadratic.polynomial.constant)
    }
}
