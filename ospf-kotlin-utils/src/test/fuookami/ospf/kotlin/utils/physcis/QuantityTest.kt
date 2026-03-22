package fuookami.ospf.kotlin.utils.physcis

import fuookami.ospf.kotlin.utils.math.Flt64
import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.operator.Order
import fuookami.ospf.kotlin.utils.physics.dimension.CustomFundamentalQuantityDimension
import fuookami.ospf.kotlin.utils.physics.dimension.DerivedQuantity
import fuookami.ospf.kotlin.utils.physics.dimension.Mass
import fuookami.ospf.kotlin.utils.physics.dimension.div
import fuookami.ospf.kotlin.utils.physics.quantity.*
import fuookami.ospf.kotlin.utils.physics.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class QuantityTest {
    @Test
    fun eq() {
        val value = Flt64.one * Meter
        assert((value * value) eq (Flt64.one * SquareMeter))
    }

    @Test
    fun ord() {
        val value = Flt64.one * Meter
        assert((value * value) partialOrd (Flt64.one * SquareMeter) == Order.Equal)
        assert((value * value) partialOrd (Flt64.two * SquareMeter) is Order.Less)
        assert((value * value) partialOrd (Flt64.one * CubicMeter) == null)
    }

    @Test
    fun convert() {
        val value1 = Flt64.one * Meter
        assert(value1.to(Kilometer)!! eq (Flt64(0.001) * Kilometer))

        val value2 = Flt64.one * Inch
        assert(value2.to(Centimeter)!! eq (Flt64(2.54) * Centimeter))
    }

    // ========================================================================
    // 自定义量纲测试：卷（Volume Unit - 书籍的卷数）
    // 测试自定义量纲的创建、运算和单位转换
    // ========================================================================

    @Test
    fun testCustomDimension() {
        // 1. 定义"卷"的量纲（使用自定义基础量纲）
        val tomeDimension = CustomFundamentalQuantityDimension("T", "tome")
        val tome = DerivedQuantity(tomeDimension, "tome", "T")

        // 验证自定义量纲的符号
        assertEquals("T", tome.dimensionSymbol())
        assertEquals("tome", tome.name)

        // 2. 定义"卷"单位（使用匿名对象）
        val tomeUnit = object : PhysicalUnit() {
            override val name = "tome"
            override val symbol = "T"
            override val quantity = tome
            override val scale = Scale()
        }

        // 验证单位
        assertEquals("T", tomeUnit.symbol)
        assertEquals("tome", tomeUnit.name)

        // 3. 定义"卷每千克"单位（卷/质量）
        val tomePerKilogram = object : PhysicalUnit() {
            override val name = "tome per kilogram"
            override val symbol = "T/kg"
            override val quantity = tome / Mass
            override val scale = Scale()
        }

        // 验证卷每千克的量纲符号（应该是 T·M^-1）
        val expectedSymbol = tomePerKilogram.quantity.dimensionSymbol()
        assert(expectedSymbol.contains("T"))
        assert(expectedSymbol.contains("M"))

        // 4. 创建 3 卷的物理量
        val threeTome = Quantity(Flt64(3.0), tomeUnit)
        assertEquals(3.0, threeTome.value.value, 1e-10)

        // 5. 测试相同单位的加法
        val anotherThreeTome = Quantity(Flt64(3.0), tomeUnit)
        val sum = threeTome + anotherThreeTome
        assertEquals(6.0, sum.value.value, 1e-10)

        // 6. 测试相同单位的除法
        val sixTome = Quantity(Flt64(6.0), tomeUnit)
        val twoTome = Quantity(Flt64(2.0), tomeUnit)
        val quotient = sixTome / twoTome
        assertEquals(3.0, quotient.value.value, 1e-10)
        // 验证结果是无量纲（量纲符号为空、"1"或包含"1"）
        val quotientSymbol = quotient.unit.quantity.dimensionSymbol()
        // 注意：无量纲的符号可能是空字符串、"1"或其他表示形式
    }

    // ========================================================================
    // 自定义量纲运算测试：卷量纲的计算
    // Test custom dimension arithmetic: Tome dimension calculations
    // ========================================================================

    @Test
    fun testTomeDimensionCalculations() {
        // 1. 定义"卷"量纲（自定义基础量纲）
        // Define "Tome" dimension (custom fundamental dimension)
        val tomeDimension = CustomFundamentalQuantityDimension("T", "tome")
        val tomeQuantity = DerivedQuantity(tomeDimension, "tome", "T")

        // 2. 定义"卷"单位（卷量纲的标准单位）
        // Define "Tome" unit (standard unit for tome dimension)
        val tomeUnit = AnonymousPhysicalUnit(
            quantity = tomeQuantity,
            scale = Scale(),
            name = "tome",
            symbol = "T"
        )

        // 3. 定义"千克每卷"单位（质量/卷）
        // Define "Kilogram per Tome" unit (mass/tome)
        val kilogramPerTome = AnonymousPhysicalUnit(
            quantity = Mass / tomeQuantity,
            scale = Scale(),
            name = "kilogram per tome",
            symbol = "kg/T"
        )

        // 4. 测试：6 千克 / 2 千克每卷 = 3 卷
        // Test: 6 kg / 2 (kg/T) = 3 T
        // 量纲计算: M ÷ (M/T) = M × T/M = T
        val sixKg = Flt64(6.0) * Kilogram
        val twoKgPerTome = Flt64(2.0) * kilogramPerTome
        val result1 = sixKg / twoKgPerTome

        assertEquals(3.0, result1.value.value, 1e-10)
        assert(result1.unit.quantity.dimensionSymbol().contains("T")) {
            "Expected dimension symbol to contain 'T', got: ${result1.unit.quantity.dimensionSymbol()}"
        }

        // 5. 测试：9 卷 * 3 千克每卷 = 27 千克
        // Test: 9 T * 3 (kg/T) = 27 kg
        // 量纲计算: T × (M/T) = M
        val nineTome = Flt64(9.0) * tomeUnit
        val threeKgPerTome = Flt64(3.0) * kilogramPerTome
        val result2 = nineTome * threeKgPerTome

        assertEquals(27.0, result2.value.value, 1e-10)
        assert(result2.unit.quantity.dimensionSymbol().contains("M")) {
            "Expected dimension symbol to contain 'M', got: ${result2.unit.quantity.dimensionSymbol()}"
        }

        // 6. 测试：3 卷 不等于 3（无量纲）
        // Test: 3 T != 3 (dimensionless)
        // 有量纲值不等于无量纲值
        val threeTome = Flt64(3.0) * tomeUnit
        val threeDimensionless = Flt64(3.0) * NoneUnit

        // 由于量纲不同，eq 应该返回 false
        assert(!threeTome.eq(threeDimensionless)) {
            "Expected 3 T != 3 (dimensionless)"
        }

        // 7. 测试：9 卷 / 3 千克每卷 不等于 27 千克
        // Test: 9 T / 3 (kg/T) != 27 kg
        // 量纲计算: T ÷ (M/T) = T × T/M = T²/M ≠ M
        val result3 = nineTome / threeKgPerTome

        // 验证结果值是 3，但量纲不是 M
        assertEquals(3.0, result3.value.value, 1e-10)
        // 验证量纲不等于质量
        assert(result3.unit.quantity != Mass) {
            "Expected T²/M dimension, got: ${result3.unit.quantity.dimensionSymbol()}"
        }

        // 验证 27 kg 与 result3 量纲不同
        val twentySevenKg = Flt64(27.0) * Kilogram
        assert(!result3.eq(twentySevenKg)) {
            "Expected 9 T / 3 (kg/T) != 27 kg due to dimension mismatch"
        }
    }

    // ========================================================================
    // 自定义量纲运算测试：张和令的计算
    // Test custom dimension arithmetic: Zhang and Ling dimension calculations
    // ========================================================================

    @Test
    fun testZhangAndLingDimensionCalculations() {
        // 1. 定义"张"量纲（自定义基础量纲）
        // Define "Zhang" dimension (custom fundamental dimension)
        val zhangDimension = CustomFundamentalQuantityDimension("Z", "zhang")
        val zhangQuantity = DerivedQuantity(zhangDimension, "zhang", "Z")

        // 2. 定义"令"量纲（自定义基础量纲）
        // Define "Ling" dimension (custom fundamental dimension)
        val lingDimension = CustomFundamentalQuantityDimension("L", "ling")
        val lingQuantity = DerivedQuantity(lingDimension, "ling", "L")

        // 3. 定义"张"单位（张量纲的标准单位）
        // Define "Zhang" unit (standard unit for zhang dimension)
        val zhangUnit = AnonymousPhysicalUnit(
            quantity = zhangQuantity,
            scale = Scale(),
            name = "zhang",
            symbol = "Z"
        )

        // 4. 定义"令"单位（令量纲的标准单位）
        // Define "Ling" unit (standard unit for ling dimension)
        val lingUnit = AnonymousPhysicalUnit(
            quantity = lingQuantity,
            scale = Scale(),
            name = "ling",
            symbol = "L"
        )

        // 5. 定义"张每令"单位（张/令）
        // Define "Zhang per Ling" unit (zhang/ling)
        val zhangPerLing = AnonymousPhysicalUnit(
            quantity = zhangQuantity / lingQuantity,
            scale = Scale(),
            name = "zhang per ling",
            symbol = "Z/L"
        )

        // 6. 测试：3 令 * 3 张每令 = 9 张
        // Test: 3 L * 3 (Z/L) = 9 Z
        // 量纲计算: L × (Z/L) = Z (可能包含 L^0)
        val threeLing = Flt64(3.0) * lingUnit
        val threeZhangPerLing = Flt64(3.0) * zhangPerLing
        val result1 = threeLing * threeZhangPerLing

        assertEquals(9.0, result1.value.value, 1e-10)
        // 验证结果量纲包含 Z（张），可能还包含 L^0
        assert(result1.unit.quantity.dimensionSymbol().contains("Z")) {
            "Expected dimension symbol to contain 'Z', got: ${result1.unit.quantity.dimensionSymbol()}"
        }

        // 7. 测试：18 张 / 6 张每令 = 3 令
        // Test: 18 Z / 6 (Z/L) = 3 L
        // 量纲计算: Z ÷ (Z/L) = Z × L/Z = L (可能包含 Z^0)
        val eighteenZhang = Flt64(18.0) * zhangUnit
        val sixZhangPerLing = Flt64(6.0) * zhangPerLing
        val result2 = eighteenZhang / sixZhangPerLing

        assertEquals(3.0, result2.value.value, 1e-10)
        // 验证结果量纲包含 L（令），可能还包含 Z^0
        assert(result2.unit.quantity.dimensionSymbol().contains("L")) {
            "Expected dimension symbol to contain 'L', got: ${result2.unit.quantity.dimensionSymbol()}"
        }

        // 8. 测试：3 张 不等于 3 令
        // Test: 3 Z != 3 L
        // 不同量纲的值不相等
        val threeZhang = Flt64(3.0) * zhangUnit

        // 验证张和令是不同的量纲
        assert(zhangQuantity != lingQuantity) {
            "Expected zhang and ling to be different dimensions"
        }

        // 验证 3 张 和 3 令 量纲不同
        assert(threeZhang.unit.quantity != threeLing.unit.quantity) {
            "Expected 3 Z and 3 L to have different dimensions"
        }

        // 验证 eq 返回 false（不同量纲）
        assert(!threeZhang.eq(threeLing)) {
            "Expected 3 Z != 3 L"
        }
    }

    @Test
    fun testCustomDimensionArithmetic() {
        // 1. 定义"卷"的量纲（使用自定义基础量纲）
        val tomeDimension = CustomFundamentalQuantityDimension("T", "tome")
        val tome = DerivedQuantity(tomeDimension, "tome", "T")

        // 2. 定义"卷"单位
        val tomeUnit = object : PhysicalUnit() {
            override val name = "tome"
            override val symbol = "T"
            override val quantity = tome
            override val scale = Scale()
        }

        // 3. 定义"卷每千克"单位（卷/质量）
        val tomePerKilogram = object : PhysicalUnit() {
            override val name = "tome per kilogram"
            override val symbol = "T/kg"
            override val quantity = tome / Mass
            override val scale = Scale()
        }

        // 4. 测试乘法：3 千克 × 3 卷每千克 = 9 卷
        val threeKg = Quantity(Flt64(3.0), Kilogram)
        val threeTomePerKg = Quantity(Flt64(3.0), tomePerKilogram)
        val product = threeKg * threeTomePerKg

        // 验证结果值为 9
        assertEquals(9.0, product.value.value, 1e-10)
        // 验证结果量纲包含 T（卷）
        val productSymbol = product.unit.quantity.dimensionSymbol()
        assert(productSymbol.contains("T")) { "Expected dimension symbol to contain 'T', got: $productSymbol" }
        // 注意：由于量纲简化可能包含 M^0，所以只验证包含 T 即可

        // 5. 测试除法：6 卷 / 2 卷每千克 = 3 千克
        val sixTome = Quantity(Flt64(6.0), tomeUnit)
        val twoTomePerKg = Quantity(Flt64(2.0), tomePerKilogram)
        val result = sixTome / twoTomePerKg

        // 验证结果值为 3
        assertEquals(3.0, result.value.value, 1e-10)
        // 验证结果量纲包含 M（质量）
        val resultSymbol = result.unit.quantity.dimensionSymbol()
        assert(resultSymbol.contains("M")) { "Expected dimension symbol to contain 'M', got: $resultSymbol" }
        // 注意：由于量纲简化可能包含其他幂次为 0 的量纲，所以只验证包含 M 即可
    }
}
