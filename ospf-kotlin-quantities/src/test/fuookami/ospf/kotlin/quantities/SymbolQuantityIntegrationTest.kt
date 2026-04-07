package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.dimension.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SymbolQuantityIntegrationTest {
    private val x = object : Symbol {
        override val name = "x"
        override val displayName = "x"
    }
    private val y = object : Symbol {
        override val name = "y"
        override val displayName = "y"
    }

    @Test
    fun `quantitySymbol_linearFlt64_shouldCompileAndEvaluate`() {
        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64.one
        )
        val distance: QuantityLinearFlt64 = Quantity(poly, Meter)
        assertEquals(2, distance.value.monomials.size)
    }

    @Test
    fun `quantitySymbol_unitConversion_shouldScaleAllCoefficients`() {
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance = Quantity(poly, Meter)
        val inCm = distance.to(Centimeter)
        assertNotNull(inCm)
        assertEquals(Flt64(200.0), inCm.value.monomials[0].coefficient)
        assertEquals(Flt64(100.0), inCm.value.constant)
    }

    @Test
    fun `quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum`() {
        val poly1 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance1 = Quantity(poly1, Meter)

        val poly2 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(3.0), x)),
            constant = Flt64(2.0)
        )
        val distance2 = Quantity(poly2, Centimeter)

        val sum = distance1 + distance2
        assertEquals(Meter, sum.unit)
    }

    @Test
    fun `quantitySymbol_addition_dimensionMismatch_shouldFail`() {
        val polyLength = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(1.0), x)),
            constant = Flt64.zero
        )
        val length = Quantity(polyLength, Meter)

        val polyTime = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(1.0), x)),
            constant = Flt64.zero
        )
        val time = Quantity(polyTime, Second)

        assertThrows<DimensionMismatchException> {
            length + time
        }
    }

    @Test
    fun `quantitySymbol_mulDiv_shouldScaleCorrectly`() {
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val length = Quantity(poly, Meter)

        // Scalar multiplication
        val doubled = length * Flt64(5.0)
        assertEquals(Flt64(10.0), doubled.value.monomials[0].coefficient)
        assertEquals(Flt64(5.0), doubled.value.constant)

        // Scalar division
        val halved = doubled / Flt64(2.0)
        assertEquals(Flt64(5.0), halved.value.monomials[0].coefficient)
        assertEquals(Flt64(2.5), halved.value.constant)
    }

    @Test
    fun `quantitySymbol_toStandardUnit_shouldKeepExpressionShape`() {
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance = Quantity(poly, Centimeter)

        val standard = distance.to(Meter)
        assertNotNull(standard)
        assertEquals(Flt64(0.02), standard.value.monomials[0].coefficient)
        assertEquals(Flt64(0.01), standard.value.constant)
    }

    @Test
    fun `quantitySymbol_concurrentRegistryAndCache_shouldBeSafe`() {
        val executor = Executors.newFixedThreadPool(10)
        val registry = SymbolDimensionRegistry()

        for (i in 1..100) {
            val symbol = DimensionedSymbol(
                name = "sym_$i",
                quantity = if (i % 2 == 0) Length else Time,
                preferredUnit = if (i % 2 == 0) Meter else Second
            )
            executor.submit {
                registry.register(symbol)
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }

    @Test
    fun `dimensionedSymbol_fullWorkflow`() {
        // 创建带量纲的符号
        val dx = DimensionedSymbol("distance_x", quantity = Length, preferredUnit = Meter)
        val dy = DimensionedSymbol("distance_y", quantity = Length, preferredUnit = Kilometer)
        val t = DimensionedSymbol("time", quantity = Time, preferredUnit = Second)

        // 注册到注册表
        val registry = SymbolDimensionRegistry()
        registry.register(dx)
        registry.register(dy)
        registry.register(t)

        // 验证可以相加（相同量纲）
        registry.validateAddSubDimension(listOf(dx, dy))

        // 推导乘法结果量纲
        val velocityDim = registry.inferDimension(dx, t, Operation.Divide)
        assert(velocityDim.dimensionSymbol().contains("L"))
        assert(velocityDim.dimensionSymbol().contains("T"))

        // 创建普通符号用于多项式
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // 创建符号多项式并求值
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(10.0), x)),
            constant = Flt64(5.0)
        )
        val quantity = Quantity(poly, Meter)

        // 求值
        val evaluated = quantity.evaluate(mapOf(x to Flt64(3.0)))
        assertNotNull(evaluated)
        assertEquals(Flt64(35.0), evaluated.value) // 10*3 + 5 = 35
    }
}