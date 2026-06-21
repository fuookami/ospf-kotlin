package fuookami.ospf.kotlin.math.symbol

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.orFail
import fuookami.ospf.kotlin.quantities.valueOrFail
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*

class SymbolQuantityTest {
    @Test
    fun `quantitySymbol_linearFlt64_shouldCompileAndEvaluate`() {
        // 创建符号 x, y
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }
        val y = object : Symbol {
            override val name = "y"
            override val displayName = "y"
        }

        // 创建线性多项式: 2x + 3y + 1.0
        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64.one
        )

        // 创建物理量: (2x + 3y + 1.0) m
        val distance: Quantity<LinearPolynomial<Flt64>> = Quantity(poly, Meter)

        // 验证结构
        assertEquals(2, distance.value.monomials.size)
        assertEquals(Flt64.one, distance.value.constant)
    }

    @Test
    fun `quantitySymbol_unitConversion_shouldScaleAllCoefficients`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // 创建物理量: (2x + 1) m
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance: Quantity<LinearPolynomial<Flt64>> = Quantity(poly, Meter)

        // 转换到厘米: (200x + 100) cm
        val inCm = distance.to(Centimeter).orFail()
        assertEquals(Flt64(200.0), inCm.value.monomials[0].coefficient)
        assertEquals(Flt64(100.0), inCm.value.constant)
    }

    @Test
    fun `quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // (2x + 1) m + (3x + 2) cm
        // After conversion: (2x + 1) m + (0.03x + 0.02) m
        // Result monomials: [2x, 0.03x], constant: 1.02
        val poly1 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance1: Quantity<LinearPolynomial<Flt64>> = Quantity(poly1, Meter)

        val poly2 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(3.0), x)),
            constant = Flt64(2.0)
        )
        val distance2: Quantity<LinearPolynomial<Flt64>> = Quantity(poly2, Centimeter)

        val sum = (distance1 + distance2).valueOrFail()
        assertEquals(Meter, sum.unit)
        // LinearPolynomial.plus doesn't combine like terms automatically
        assertEquals(2, sum.value.monomials.size)  // [2x, 0.03x]
        assertEquals(Flt64(2.0), sum.value.monomials[0].coefficient)
        assertEquals(Flt64(0.03), sum.value.monomials[1].coefficient)
        assertEquals(Flt64(1.02), sum.value.constant)
    }

    @Test
    fun `quantitySymbol_scalarMulDiv_shouldScalePolynomial`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // (2x + 1) m * 5 = (10x + 5) m
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance: Quantity<LinearPolynomial<Flt64>> = Quantity(poly, Meter)

        val scaled = distance * Flt64(5.0)
        assertEquals(Meter, scaled.unit)
        assertEquals(Flt64(10.0), scaled.value.monomials[0].coefficient)
        assertEquals(Flt64(5.0), scaled.value.constant)

        // (10x + 5) m / 2 = (5x + 2.5) m
        val divided = scaled / Flt64(2.0)
        assertEquals(Flt64(5.0), divided.value.monomials[0].coefficient)
        assertEquals(Flt64(2.5), divided.value.constant)
    }

    @Test
    fun `quantitySymbol_evaluate_shouldReturnNumericQuantity`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // (2x + 1) m with x = 3.0 -> 7.0 m
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance: Quantity<LinearPolynomial<Flt64>> = Quantity(poly, Meter)

        val evaluated = distance.evaluate(mapOf(x to Flt64(3.0))).orFail()
        assertEquals(Meter, evaluated.unit)
        assertEquals(Flt64(7.0), evaluated.value)
    }
}
