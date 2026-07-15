/**
 * 仿射转换规则测试
 * Affine conversion rule tests
 *
 * 验收标准 / Acceptance criteria:
 * 1. 所有普通单位转换结果保持兼容
 * 2. 绝对温标转换正确
 * 3. 仿射单位不会参与错误的普通代数运算
 * 4. 现有单位制和自定义单位仍可工作
 */
package fuookami.ospf.kotlin.quantities

import kotlin.test.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

private fun Flt64.assertApprox(expected: Flt64, epsilon: Double = 1e-10) {
    assertTrue(
        (this - expected).abs().toDouble() < epsilon,
        "Expected $expected but was $this (diff=${(this - expected).abs().toDouble()})"
    )
}

private fun FltX.assertApprox(expected: FltX, epsilon: Double = 1e-12) {
    assertTrue(
        (this - expected).abs().toDouble() < epsilon,
        "Expected $expected but was $this (diff=${(this - expected).abs().toDouble()})"
    )
}

class AffineConversionTest {

    // ========================================================================
    // 温度转换正确性 / Temperature conversion correctness
    // ========================================================================

    @Test
    fun testCelsiusToKelvin() {
        // 0 °C -> 273.15 K
        val celsiusZero = Flt64(0.0) * Celsius
        val kelvinValue = celsiusZero.to(Kelvin)
        assertNotNull(kelvinValue)
        kelvinValue.value.assertApprox(Flt64(273.15))
        assertEquals(Kelvin, kelvinValue.unit)
    }

    @Test
    fun testFahrenheitToKelvin() {
        // 32 °F -> 273.15 K
        val fahrenheit32 = Flt64(32.0) * Fahrenheit
        val kelvinValue = fahrenheit32.to(Kelvin)
        assertNotNull(kelvinValue)
        kelvinValue.value.assertApprox(Flt64(273.15))
    }

    @Test
    fun testCelsiusToFahrenheit() {
        // 100 °C -> 212 °F
        val celsius100 = Flt64(100.0) * Celsius
        val fahrenheitValue = celsius100.to(Fahrenheit)
        assertNotNull(fahrenheitValue)
        fahrenheitValue.value.assertApprox(Flt64(212.0))
    }

    @Test
    fun testKelvinToCelsius() {
        // 373.15 K -> 100 °C
        val kelvin373 = Flt64(373.15) * Kelvin
        val celsiusValue = kelvin373.to(Celsius)
        assertNotNull(celsiusValue)
        celsiusValue.value.assertApprox(Flt64(100.0))
    }

    @Test
    fun testKelvinToFahrenheit() {
        // 273.15 K -> 32 °F
        val kelvin273 = Flt64(273.15) * Kelvin
        val fahrenheitValue = kelvin273.to(Fahrenheit)
        assertNotNull(fahrenheitValue)
        fahrenheitValue.value.assertApprox(Flt64(32.0))
    }

    @Test
    fun testFahrenheitToCelsius() {
        // 212 °F -> 100 °C
        val fahrenheit212 = Flt64(212.0) * Fahrenheit
        val celsiusValue = fahrenheit212.to(Celsius)
        assertNotNull(celsiusValue)
        celsiusValue.value.assertApprox(Flt64(100.0))
    }

    @Test
    fun testRankineToKelvin() {
        // Rankine 是线性单位，0 °R -> 0 K
        val rankineZero = Flt64(0.0) * Rankine
        val kelvinValue = rankineZero.to(Kelvin)
        assertNotNull(kelvinValue)
        kelvinValue.value.assertApprox(Flt64(0.0))

        // 491.67 °R -> 273.15 K
        val rankine491 = Flt64(491.67) * Rankine
        val kelvin2 = rankine491.to(Kelvin)
        assertNotNull(kelvin2)
        kelvin2.value.assertApprox(Flt64(273.15), 1e-8)
    }

    @Test
    fun testKelvinToRankine() {
        // 273.15 K -> 491.67 °R
        val kelvin273 = Flt64(273.15) * Kelvin
        val rankineValue = kelvin273.to(Rankine)
        assertNotNull(rankineValue)
        rankineValue.value.assertApprox(Flt64(491.67), 1e-8)
    }

    @Test
    fun testCelsiusToCelsius() {
        // 相同单位转换应直接返回
        val celsius100 = Flt64(100.0) * Celsius
        val result = celsius100.to(Celsius)
        assertNotNull(result)
        result.value.assertApprox(Flt64(100.0))
    }

    @Test
    fun testNegativeCelsiusToKelvin() {
        // -273.15 °C -> 0 K
        val celsiusNeg = Flt64(-273.15) * Celsius
        val kelvinValue = celsiusNeg.to(Kelvin)
        assertNotNull(kelvinValue)
        kelvinValue.value.assertApprox(Flt64(0.0))
    }

    // ========================================================================
    // FltX 精度转换 / FltX precision conversion
    // ========================================================================

    @Test
    fun testCelsiusToKelvinFltX() {
        val celsiusZero = FltX(0.0) * Celsius
        val kelvinValue = celsiusZero.to(Kelvin)
        assertNotNull(kelvinValue)
        kelvinValue.value.assertApprox(FltX(273.15))
    }

    @Test
    fun testCelsiusToFahrenheitFltX() {
        val celsius100 = FltX(100.0) * Celsius
        val fahrenheitValue = celsius100.to(Fahrenheit)
        assertNotNull(fahrenheitValue)
        fahrenheitValue.value.assertApprox(FltX(212.0))
    }

    // ========================================================================
    // 仿射单位属性 / Affine unit properties
    // ========================================================================

    @Test
    fun testAffineUnitProperties() {
        assertTrue(Celsius.isAffine)
        assertTrue(Fahrenheit.isAffine)
        assertFalse(Kelvin.isAffine)
        assertFalse(Rankine.isAffine)
    }

    @Test
    fun testToReturnsNullForAffineUnits() {
        // PhysicalUnit.to() 返回纯比例因子，仿射单位应返回 null
        assertNull(Celsius.to(Kelvin))
        assertNull(Kelvin.to(Celsius))
        assertNull(Fahrenheit.to(Kelvin))
        assertNull(Celsius.to(Fahrenheit))

        // 线性单位之间的 to() 仍然正常工作
        assertNotNull(Kelvin.to(Rankine))
        assertNotNull(Rankine.to(Kelvin))
    }

    // ========================================================================
    // 仿射单位禁止乘除 / Affine units cannot multiply/divide
    // ========================================================================

    @Test
    fun testAffineMultiplyThrows() {
        val temp1 = Flt64(100.0) * Celsius
        val temp2 = Flt64(50.0) * Kelvin
        assertNull(temp1 * temp2)
        assertTrue(temp1.timesSafe(temp2).failed)
    }

    @Test
    fun testAffineDivideThrows() {
        val temp1 = Flt64(100.0) * Celsius
        val temp2 = Flt64(50.0) * Kelvin
        assertNull(temp1 / temp2)
        assertTrue(temp1.divSafe(temp2).failed)
    }

    // ========================================================================
    // 普通单位转换兼容性 / Linear unit conversion compatibility
    // ========================================================================

    @Test
    fun testLinearConversionStillWorks() {
        val distance = Flt64(1000.0) * Meter
        val km = distance.to(Kilometer)
        assertNotNull(km)
        km.value.assertApprox(Flt64(1.0))
    }

    @Test
    fun testLinearScalePropertyStillWorks() {
        assertEquals(Scale(), Kelvin.scale)
        assertEquals(Scale(), Celsius.scale)  // 摄氏度的线性比例因子为 1（偏移由 offset 处理）
        assertEquals(Scale(RtnX(5, 9)), Fahrenheit.scale)
        assertEquals(Scale(RtnX(5, 9)), Rankine.scale)
    }

    @Test
    fun testConversionRuleProperties() {
        val kelvin: PhysicalUnit = Kelvin
        val celsius: PhysicalUnit = Celsius
        val fahrenheit: PhysicalUnit = Fahrenheit
        val rankine: PhysicalUnit = Rankine

        assertTrue(kelvin.conversionRule is UnitConversionRule.Linear)
        assertTrue(celsius.conversionRule is UnitConversionRule.Affine)
        assertTrue(fahrenheit.conversionRule is UnitConversionRule.Affine)
        assertTrue(rankine.conversionRule is UnitConversionRule.Linear)
    }

    @Test
    fun testCelsiusAffineOffset() {
        val rule = Celsius.conversionRule as UnitConversionRule.Affine
        rule.offset.assertApprox(FltX(273.15))
    }

    @Test
    fun testFahrenheitAffineOffset() {
        val rule = Fahrenheit.conversionRule as UnitConversionRule.Affine
        // offset = 273.15 - 32 * 5/9 = 273.15 - 17.777... = 255.372...
        rule.offset.assertApprox(FltX("255.3722222222222222222222222222222222"))
    }

    // ========================================================================
    // 整数类型的仿射转换返回 null / Integer type affine conversion returns null
    // ========================================================================

    @Test
    fun testInt64AffineConversionReturnsNull() {
        val temp = Int64(0L) * Celsius
        assertNull(temp.to(Kelvin))
    }

    @Test
    fun testUInt64AffineConversionReturnsNull() {
        val temp = UInt64(0UL) * Celsius
        assertNull(temp.to(Kelvin))
    }

    @Test
    fun testIntXAffineConversionReturnsNull() {
        val temp = IntX(0L) * Celsius
        assertNull(temp.to(Kelvin))
    }

    // ========================================================================
    // 温度比较 / Temperature comparison
    // ========================================================================

    @Test
    fun testTemperatureComparison() {
        val zeroC = Flt64(0.0) * Celsius
        val zeroF = Flt64(32.0) * Fahrenheit
        val kelvin273 = Flt64(273.15) * Kelvin

        assertTrue(zeroC eq zeroF)
        assertTrue(zeroC eq kelvin273)
        assertTrue(kelvin273 eq zeroF)
    }

    // ========================================================================
    // 温度加减 / Temperature addition and subtraction
    // ========================================================================

    @Test
    fun testSameAffineUnitAddition() {
        val temp1 = Flt64(10.0) * Celsius
        val temp2 = Flt64(20.0) * Celsius
        assertNull(temp1 + temp2)
        assertTrue(temp1.plusSafe(temp2).failed)
    }

    @Test
    fun testSameAffineUnitSubtraction() {
        val temp1 = Flt64(100.0) * Celsius
        val temp2 = Flt64(0.0) * Celsius
        val result = (temp1 - temp2)!!
        result.value.assertApprox(Flt64(100.0))
        assertFalse(result.unit.isAffine)
        assertEquals(Celsius.quantity, result.unit.quantity)
    }

    // ========================================================================
    // 仿射单位禁止普通标量乘除 / Affine units cannot use ordinary scalar multiplication/division
    // ========================================================================

    @Test
    fun testScalarMultiplyWithAffineUnit() {
        val temp = Flt64(100.0) * Celsius
        assertNull(temp * Flt64(2.0))
        assertTrue(temp.timesSafe(Flt64(2.0)).failed)
    }

    @Test
    fun testScalarDivideWithAffineUnit() {
        val temp = Flt64(100.0) * Celsius
        assertNull(temp / Flt64(2.0))
        assertTrue(temp.divSafe(Flt64(2.0)).failed)
    }

    // ========================================================================
    // convertValue 方法测试 / convertValue method tests
    // ========================================================================

    @Test
    fun testConvertValueCelsiusToKelvin() {
        val result = Celsius.convertValue(FltX(0.0), Kelvin)
        assertNotNull(result)
        result.assertApprox(FltX(273.15))
    }

    @Test
    fun testConvertValueKelvinToCelsius() {
        val result = Kelvin.convertValue(FltX(273.15), Celsius)
        assertNotNull(result)
        result.assertApprox(FltX(0.0))
    }

    @Test
    fun testConvertValueDifferentDimensionReturnsNull() {
        val result = Celsius.convertValue(FltX(0.0), Meter)
        assertNull(result)
    }
}
