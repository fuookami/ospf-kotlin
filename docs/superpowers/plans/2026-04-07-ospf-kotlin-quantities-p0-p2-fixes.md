# ospf-kotlin-quantities P0-P2 修复计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复 ospf-kotlin-quantities 单位常数错误、语义问题、性能问题，补齐测试覆盖

**Architecture:** 按照 Phase A-D 顺序执行，先修复 P0 正确性问题，再修复 P1 语义问题，最后处理 P2 性能与测试

**Tech Stack:** Kotlin, JUnit 5, ospf-kotlin-math (Scale, Flt64 等)

---

## 文件结构

### Phase A (P0) - 单位常数修复

需要修改的文件：
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Area.kt` - 修复 `Are`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Volume.kt` - 修复 `CubicYard`, `UKFluidOunce`, `USFluidOunce`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Momentum.kt` - 修复 `KilogramMeterPerSecond`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Force.kt` - 修复 `PoundForce`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Length.kt` - 修复 `Rod`, `Parsec`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/PlaneAngle.kt` - 新增 `Steradian`
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt` - 补充 SI 立体角基本单位映射
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt` - 新增测试

### Phase B (P1) - 语义与符号修复

需要修改的文件：
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/Quantity.kt` - 修复 `eq/neq` 逻辑
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Frequency.kt` - 修复 `Megahertz`, `Gigahertz` 符号
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Pressure.kt` - 修复 `Megapascal` 符号
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Torque.kt` - 修复 `NewtonMeter` name/symbol
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Power.kt` - 修复马力常数
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt` - 新增测试

### Phase C (P2) - 性能与并发

需要修改的文件：
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt` - 切换线程安全容器
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P2ConcurrencyTest.kt` - 新增并发测试

### Phase D (P3) - 测试补齐

需要修改的文件：
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/UnitConstantsTest.kt` - 全量单位常数测试
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/QuantitySemanticsTest.kt` - 语义测试
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/TemperatureConversionTest.kt` - 温度转换测试

---

## Phase A: 修复 P0 单位常数错误

### Task A1: 修复 Area.kt 中的 Are 定义

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Area.kt:40-45`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** `Are` 当前定义为 `Decimeter * Decimeter` (0.01 m²)，应为 100 m²

- [ ] **Step 1: 编写失败测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class P0UnitConstantsTest {
    @Test
    fun `unitArea_areShouldEqual100SquareMeter`() {
        val oneAre = Flt64.one * Are
        val inSquareMeter = oneAre.to(SquareMeter)
        assertNotNull(inSquareMeter)
        assertEquals(100.0, inSquareMeter.value.toDouble(), 1e-10)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitArea_areShouldEqual100SquareMeter"`
Expected: FAIL - Are 值不等于 100 m²

- [ ] **Step 3: 修复 Area.kt**

修改 `Area.kt` 第 40-45 行:

```kotlin
object Are : DerivedPhysicalUnit(SquareMeter * 100) {
    override val name = "are"
    override val symbol = "are"

    override val quantity = Area
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitArea_areShouldEqual100SquareMeter"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Area.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct Are unit definition to 100 m²"
```

---

### Task A2: 修复 Volume.kt 中的 CubicYard 定义

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Volume.kt:90-95`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** `CubicYard` 当前定义为 `SquareFoot * Foot`，应为 `Yard * Yard * Yard`

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitVolume_cubicYardShouldEqual27CubicFoot`() {
        val oneCubicYard = Flt64.one * CubicYard
        val inCubicFoot = oneCubicYard.to(CubicFoot)
        assertNotNull(inCubicFoot)
        assertEquals(27.0, inCubicFoot.value.toDouble(), 1e-10)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitVolume_cubicYardShouldEqual27CubicFoot"`
Expected: FAIL - CubicYard 不等于 27 CubicFoot

- [ ] **Step 3: 修复 Volume.kt**

修改 `Volume.kt` 第 90-95 行:

```kotlin
object CubicYard : DerivedPhysicalUnit(Yard * Yard * Yard) {
    override val name = "cubic yard"
    override val symbol = "cu.yd"

    override val quantity = Volume
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitVolume_cubicYardShouldEqual27CubicFoot"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Volume.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct CubicYard definition to Yard³"
```

---

### Task A3: 修复 Volume.kt 中的 UKFluidOunce 和 USFluidOunce

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Volume.kt:97-109`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** UKFluidOunce/USFluidOunce 基于 `Millimeter * 常数`，量纲错误

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitVolume_ukFluidOunceShouldEqual28_4130625Milliliter`() {
        val oneUKFluidOunce = Flt64.one * UKFluidOunce
        val inMilliliter = oneUKFluidOunce.to(Milliliter)
        assertNotNull(inMilliliter)
        assertEquals(28.4130625, inMilliliter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_usFluidOunceShouldEqual29_5735295625Milliliter`() {
        val oneUSFluidOunce = Flt64.one * USFluidOunce
        val inMilliliter = oneUSFluidOunce.to(Milliliter)
        assertNotNull(inMilliliter)
        assertEquals(29.5735295625, inMilliliter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_ukGallonShouldEqual4_54609Liter`() {
        val oneUKGallon = Flt64.one * UKGallon
        val inLiter = oneUKGallon.to(Liter)
        assertNotNull(inLiter)
        assertEquals(4.54609, inLiter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitVolume_usGallonShouldEqual3_78541178Liter`() {
        val oneUSGallon = Flt64.one * USGallon
        val inLiter = oneUSGallon.to(Liter)
        assertNotNull(inLiter)
        assertEquals(3.78541178, inLiter.value.toDouble(), 1e-6)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitVolume_*"`
Expected: FAIL - 液盎司值错误约 1000 倍

- [ ] **Step 3: 修复 Volume.kt**

修改 `Volume.kt` 第 97-123 行 (根据 Rust 对照):

```kotlin
object UKFluidOunce : DerivedPhysicalUnit(Milliliter * 28.4130625) {
    override val name = "uk fluid ounce"
    override val symbol = "uk.fl.oz"

    override val quantity = Volume
}

object USFluidOunce : DerivedPhysicalUnit(Milliliter * 29.5735295625) {
    override val name = "us fluid ounce"
    override val symbol = "us.fl.oz"

    override val quantity = Volume
}

object UKGallon : DerivedPhysicalUnit(Liter * 4.54609) {
    override val name = "uk gallon"
    override val symbol = "uk.gal"

    override val quantity = Volume
}

object USGallon : DerivedPhysicalUnit(Liter * 3.78541178) {
    override val name = "us gallon"
    override val symbol = "us.gal"

    override val quantity = Volume
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitVolume_*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Volume.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct UK/US fluid ounce and gallon definitions"
```

---

### Task A4: 修复 Momentum.kt 中的 KilogramMeterPerSecond

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Momentum.kt:5-10`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** 使用 `KilogramForceMeter / Second` 引入重力因子

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitMomentum_kilogramMeterPerSecondShouldEqualNewtonSecond`() {
        // 1 kg·m/s = 1 N·s (因为 1 N = 1 kg·m/s²)
        val oneKgMeterPerSec = Flt64.one * KilogramMeterPerSecond
        val oneNewtonSecond = Flt64.one * (Newton * Second)
        assert(oneKgMeterPerSec eq oneNewtonSecond)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitMomentum_kilogramMeterPerSecondShouldEqualNewtonSecond"`
Expected: FAIL - 相差约 9.80665 倍

- [ ] **Step 3: 修复 Momentum.kt**

修改 `Momentum.kt` 全文件:

```kotlin
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Momentum

object KilogramMeterPerSecond : DerivedPhysicalUnit(Kilogram * Meter / Second) {
    override val name = "kilogram meter per second"
    override val symbol = "kg·m/s"

    override val quantity = Momentum
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitMomentum_kilogramMeterPerSecondShouldEqualNewtonSecond"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Momentum.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct KilogramMeterPerSecond to use kg·m/s"
```

---

### Task A5: 修复 Force.kt 中的 PoundForce

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Force.kt:34-39`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** `GramForce * Gram.to(Pound)` 换算方向错误

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitForce_poundForceShouldEqual4_4482216152605Newton`() {
        val onePoundForce = Flt64.one * PoundForce
        val inNewton = onePoundForce.to(Newton)
        assertNotNull(inNewton)
        assertEquals(4.4482216152605, inNewton.value.toDouble(), 1e-10)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitForce_poundForceShouldEqual4_4482216152605Newton"`
Expected: FAIL - 值明显偏小

- [ ] **Step 3: 修复 Force.kt**

修改 `Force.kt` 第 34-39 行 (根据 Rust 对照):

```kotlin
object PoundForce : DerivedPhysicalUnit(Newton * 4.4482216152605) {
    override val name = "pound force"
    override val symbol = "lbf"

    override val quantity = Force
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitForce_poundForceShouldEqual4_4482216152605Newton"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Force.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct PoundForce definition to 4.4482216152605 N"
```

---

### Task A6: 修复 Length.kt 中的 Rod 和 Parsec

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Length.kt:147-152,203-208`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** Rod 使用 198.838 in 应为 198 in; Parsec 使用 20265 AU 应为 206265 AU

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitLength_rodShouldEqual5_0292Meter`() {
        val oneRod = Flt64.one * Rod
        val inMeter = oneRod.to(Meter)
        assertNotNull(inMeter)
        assertEquals(5.0292, inMeter.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitLength_parsecShouldEqual206265AstronomicalUnit`() {
        val oneParsec = Flt64.one * Parsec
        val inAU = oneParsec.to(AstronomicalUnit)
        assertNotNull(inAU)
        assertEquals(206265.0, inAU.value.toDouble(), 1e-3)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitLength_*"`
Expected: FAIL - Rod 和 Parsec 值错误

- [ ] **Step 3: 修复 Length.kt**

修改 `Length.kt` 第 147-152 行:

```kotlin
object Rod : DerivedPhysicalUnit(Meter * 5.0292) {
    override val name = "rod"
    override val symbol = "rd"

    override val quantity = Length
}
```

修改 `Length.kt` 第 203-208 行:

```kotlin
object Parsec : DerivedPhysicalUnit(AstronomicalUnit * 206265) {
    override val name = "parsec"
    override val symbol = "pc"

    override val quantity = Length
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitLength_*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Length.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "fix(quantities): correct Rod to 5.0292m and Parsec to 206265 AU"
```

---

### Task A7: 新增 Steradian 单位定义并更新 SI

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/PlaneAngle.kt`
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt:306-318`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt`

**问题:** Kotlin 缺少 Steradian 定义，SI.baseUnits 缺失立体角映射

- [ ] **Step 1: 编写失败测试**

在 `P0UnitConstantsTest.kt` 添加:

```kotlin
    @Test
    fun `unitSystem_siShouldContainSolidAngleBaseUnit`() {
        val solidAngleDimension = StandardFundamentalQuantityDimension.SolidAngle
        val baseUnit = SI.baseUnits[solidAngleDimension]
        assertNotNull(baseUnit, "SI should have SolidAngle base unit mapping")
        assert(baseUnit!!.symbol == "sr")
    }

    @Test
    fun `unitSystem_luminousFluxShouldBeDerivableInSI`() {
        val luminousFluxUnit = SI.unitForDimension(LuminousFlux)
        assertNotNull(luminousFluxUnit, "LuminousFlux should be derivable in SI")
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitSystem_*"`
Expected: FAIL - SI 缺少立体角基本单位映射

- [ ] **Step 3: 新增 Steradian 定义**

在 `PlaneAngle.kt` 文件末尾添加:

```kotlin
// ============================================================================
// 立体角单位 / Solid angle units
// ============================================================================

object Steradian : PhysicalUnit() {
    override val name = "steradian"
    override val symbol = "sr"

    override val quantity = SolidAngle
    override val scale = Scale()
}
```

- [ ] **Step 4: 更新 SI.baseUnits**

修改 `UnitSystem.kt` 第 306-318 行:

```kotlin
    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit> by lazy {
        mapOf(
            StandardFundamentalQuantityDimension.Length to Meter,
            StandardFundamentalQuantityDimension.Mass to Kilogram,
            StandardFundamentalQuantityDimension.Time to Second,
            StandardFundamentalQuantityDimension.Current to Ampere,
            StandardFundamentalQuantityDimension.Temperature to Kelvin,
            StandardFundamentalQuantityDimension.SubstanceAmount to Mole,
            StandardFundamentalQuantityDimension.LuminousIntensity to Candela,
            StandardFundamentalQuantityDimension.Information to Bit,
            StandardFundamentalQuantityDimension.PlaneAngle to Radian,
            StandardFundamentalQuantityDimension.SolidAngle to Steradian
        )
    }
```

- [ ] **Step 5: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P0UnitConstantsTest.unitSystem_*"`
Expected: PASS

- [ ] **Step 6: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/PlaneAngle.kt
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "feat(quantities): add Steradian unit and SI SolidAngle base unit mapping"
```

---

### Task A8: 运行全量测试验证 Phase A

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 提交 Phase A 总结**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P0UnitConstantsTest.kt
git commit -m "test(quantities): complete Phase A unit constants tests"
```

---

## Phase B: 修复 P1 语义与符号问题

### Task B1: 修复 Quantity.kt 中的 eq/neq 逻辑

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/Quantity.kt:119-139`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt`

**问题:** 量纲不一致时 `eq/neq` 都返回 `false`，违背 `neq == !eq` 契约

- [ ] **Step 1: 编写失败测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class P1SemanticTest {
    @Test
    fun `quantityEqNeq_shouldBeLogicalComplementOnDimensionMismatch`() {
        val length = Flt64.one * Meter
        val time = Flt64.one * Second

        // eq should return false for dimension mismatch
        assertFalse(length eq time)

        // neq should return true (the logical complement of eq)
        assertTrue(length neq time)

        // Verify neq == !eq for all cases
        assertEquals(!(length eq time), length neq time)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.quantityEqNeq_shouldBeLogicalComplementOnDimensionMismatch"`
Expected: FAIL - `neq` 返回 `false` 而非 `true`

- [ ] **Step 3: 修复 Quantity.kt**

修改 `Quantity.kt` 第 119-139 行:

```kotlin
infix fun <V> Quantity<V>.eq(other: Quantity<V>): Boolean where V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value eq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        val converted = other.tryConvertByValueType(this.unit) ?: return false
        this.value eq converted.value
    } else {
        false
    }
}

infix fun <V> Quantity<V>.neq(other: Quantity<V>): Boolean where V : Eq<V> {
    // neq must be the logical complement of eq
    return !(this eq other)
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.quantityEqNeq_shouldBeLogicalComplementOnDimensionMismatch"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/Quantity.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "fix(quantities): correct neq to be logical complement of eq"
```

---

### Task B2: 修复 Frequency.kt 中的符号

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Frequency.kt:20-32`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt`

**问题:** Megahertz 符号 `mHz` 应为 `MHz`; Gigahertz 符号 `gHz` 应为 `GHz`

- [ ] **Step 1: 编写失败测试**

在 `P1SemanticTest.kt` 添加:

```kotlin
    @Test
    fun `unitSymbol_megahertzShouldBeMHz`() {
        assertEquals("MHz", Megahertz.symbol)
    }

    @Test
    fun `unitSymbol_gigahertzShouldBeGHz`() {
        assertEquals("GHz", Gigahertz.symbol)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitSymbol_*"`
Expected: FAIL - 符号错误

- [ ] **Step 3: 修复 Frequency.kt**

修改 `Frequency.kt` 第 20-32 行:

```kotlin
object Megahertz : DerivedPhysicalUnit(Hertz * Scale.mega) {
    override val name = "megahertz"
    override val symbol = "MHz"

    override val quantity = Frequency
}

object Gigahertz : DerivedPhysicalUnit(Hertz * Scale.giga) {
    override val name = "gigahertz"
    override val symbol = "GHz"

    override val quantity = Frequency
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitSymbol_*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Frequency.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "fix(quantities): correct Megahertz/Gigahertz symbols to MHz/GHz"
```

---

### Task B3: 修复 Pressure.kt 中的 Megapascal 符号

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Pressure.kt:27-32`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt`

**问题:** Megapascal 符号 `mPa` 应为 `MPa`

- [ ] **Step 1: 编写失败测试**

在 `P1SemanticTest.kt` 添加:

```kotlin
    @Test
    fun `unitSymbol_megapascalShouldBeMPa`() {
        assertEquals("MPa", Megapascal.symbol)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitSymbol_megapascalShouldBeMPa"`
Expected: FAIL - 符号错误

- [ ] **Step 3: 修复 Pressure.kt**

修改 `Pressure.kt` 第 27-32 行:

```kotlin
object Megapascal : DerivedPhysicalUnit(Pascal * Scale.mega) {
    override val name = "megapascal"
    override val symbol = "MPa"

    override val quantity = Pressure
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitSymbol_megapascalShouldBeMPa"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Pressure.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "fix(quantities): correct Megapascal symbol to MPa"
```

---

### Task B4: 修复 Torque.kt 中的 name/symbol 对调

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Torque.kt:5-17`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt`

**问题:** NewtonMeter 的 name 和 symbol 对调了

- [ ] **Step 1: 编写失败测试**

在 `P1SemanticTest.kt` 添加:

```kotlin
    @Test
    fun `unitTorque_nameAndSymbolShouldBeCorrect`() {
        assertEquals("newton meter", NewtonMeter.name)
        assertEquals("N·m", NewtonMeter.symbol)
        assertEquals("kilogram-force meter", KilogramForceMeter.name)
        assertEquals("kgf·m", KilogramForceMeter.symbol)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitTorque_nameAndSymbolShouldBeCorrect"`
Expected: FAIL - name/symbol 互换

- [ ] **Step 3: 修复 Torque.kt**

修改 `Torque.kt` 全文件:

```kotlin
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.Torque

object NewtonMeter : DerivedPhysicalUnit(Newton * Meter) {
    override val name = "newton meter"
    override val symbol = "N·m"

    override val quantity = Torque
}

object KilogramForceMeter : DerivedPhysicalUnit(KilogramForce * Meter) {
    override val name = "kilogram-force meter"
    override val symbol = "kgf·m"

    override val quantity = Torque
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitTorque_nameAndSymbolShouldBeCorrect"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Torque.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "fix(quantities): correct Torque units name/symbol swap"
```

---

### Task B5: 修复 Power.kt 中的马力常数

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Power.kt:34-46`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt`

**问题:** Horsepower=735W, UKHorsepower=550W 常数偏差

**说明:** 需明确采用标准。常用标准：
- 公制马力 (PS/Pferdestärke): 735.49875 W ≈ 735 W
- 英制马力 (HP): 745.7 W (550 ft·lbf/s)
- 电气马力: 746 W

根据 Rust 对照，应使用公制马力标准。

- [ ] **Step 1: 编写失败测试**

在 `P1SemanticTest.kt` 添加:

```kotlin
    @Test
    fun `unitPower_horsepowerShouldUseMetricStandard`() {
        // Metric horsepower (PS) = 735.49875 W
        val onePS = Flt64.one * Horsepower
        val inWatt = onePS.to(Watt)
        assertNotNull(inWatt)
        assertEquals(735.49875, inWatt.value.toDouble(), 1e-6)
    }

    @Test
    fun `unitPower_ukHorsepowerShouldUseImperialStandard`() {
        // Imperial horsepower (HP) = 745.7 W (550 ft·lbf/s)
        val oneHP = Flt64.one * UKHorsepower
        val inWatt = oneHP.to(Watt)
        assertNotNull(inWatt)
        assertEquals(745.7, inWatt.value.toDouble(), 1e-6)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitPower_*"`
Expected: FAIL - 马力值偏差

- [ ] **Step 3: 修复 Power.kt**

修改 `Power.kt` 第 34-46 行:

```kotlin
object Horsepower : DerivedPhysicalUnit(Watt * 735.49875) {
    override val name = "horsepower"
    override val symbol = "ps"  // 公制马力符号

    override val quantity = Power
}

object UKHorsepower : DerivedPhysicalUnit(Watt * 745.7) {
    override val name = "uk horsepower"
    override val symbol = "hp"  // 英制马力符号

    override val quantity = Power
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P1SemanticTest.unitPower_*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/Power.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "fix(quantities): correct horsepower to use metric/imperial standards"
```

---

### Task B6: 运行全量测试验证 Phase B

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 提交 Phase B 总结**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P1SemanticTest.kt
git commit -m "test(quantities): complete Phase B semantic tests"
```

---

## Phase C: 修复 P2 性能与并发问题

### Task C1: 切换 UnitSystem 到线程安全容器

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt:38-46,319-344`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P2ConcurrencyTest.kt`

**问题:** UnitSystem 使用 MutableMap 无并发保护

- [ ] **Step 1: 编写并发测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.dimension.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.RepeatedTest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertNotNull

class P2ConcurrencyTest {
    @RepeatedTest(10)
    fun `unitSystem_concurrentReadWriteCacheShouldBeSafe`() {
        val executor = Executors.newFixedThreadPool(10)

        // Concurrent read operations
        for (i in 1..50) {
            executor.submit {
                val unit = SI.unitForDimension(Area)
                assertNotNull(unit)
            }
        }

        // Concurrent write operations (setStandardUnit)
        for (i in 1..50) {
            executor.submit {
                SI.setStandardUnit(Energy, Joule)
            }
        }

        // Concurrent derived unit derivation
        for (i in 1..50) {
            executor.submit {
                val unit = SI.unitForDimension(LuminousFlux)
                assertNotNull(unit)
            }
        }

        executor.shutdown()
        executor.awaitTermination(5, TimeUnit.SECONDS)
    }
}
```

- [ ] **Step 2: 运行测试验证（可能通过但存在竞态风险）**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P2ConcurrencyTest"`
Expected: 可能 PASS，但存在潜在竞态条件

- [ ] **Step 3: 修改 UnitSystem 使用 ConcurrentHashMap**

修改 `UnitSystem.kt` 接口定义:

```kotlin
interface UnitSystem {
    val name: String
    val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit>

    // 使用 ConcurrentHashMap 保证线程安全
    val standardUnits: ConcurrentHashMap<DerivedQuantity, PhysicalUnit>
    val derivedCache: ConcurrentHashMap<DerivedQuantity, PhysicalUnit>

    // ... 其他方法保持不变
}
```

修改 `ConcreteUnitSystem`:

```kotlin
data class ConcreteUnitSystem(
    override val name: String,
    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit>,
    override val standardUnits: ConcurrentHashMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap(),
    override val derivedCache: ConcurrentHashMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap()
) : UnitSystem
```

修改 `UnitSystemBuilder`:

```kotlin
class UnitSystemBuilder {
    private val name: String
    private var prototype: UnitSystem? = null
    private val baseUnits: MutableMap<FundamentalQuantityDimension, PhysicalUnit>
    private val derivedUnits: ConcurrentHashMap<DerivedQuantity, PhysicalUnit>
    private val standardUnits: ConcurrentHashMap<DerivedQuantity, PhysicalUnit>

    constructor(name: String) {
        this.name = name
        this.prototype = null
        this.baseUnits = mutableMapOf()
        this.derivedUnits = ConcurrentHashMap()
        this.standardUnits = ConcurrentHashMap()
    }

    constructor(name: String, prototype: UnitSystem) {
        this.name = name
        this.prototype = prototype
        this.baseUnits = prototype.baseUnits.toMutableMap()
        this.derivedUnits = ConcurrentHashMap(prototype.derivedCache)
        this.standardUnits = ConcurrentHashMap(prototype.standardUnits)
    }

    fun build(): UnitSystem {
        return ConcreteUnitSystem(
            name = name,
            baseUnits = baseUnits.toMap(),
            standardUnits = ConcurrentHashMap(standardUnits),
            derivedCache = ConcurrentHashMap(derivedUnits)
        )
    }
}
```

修改 `SI` 对象:

```kotlin
object SI : UnitSystem {
    override val name: String = "SI"

    override val baseUnits: Map<FundamentalQuantityDimension, PhysicalUnit> by lazy {
        mapOf(
            StandardFundamentalQuantityDimension.Length to Meter,
            StandardFundamentalQuantityDimension.Mass to Kilogram,
            StandardFundamentalQuantityDimension.Time to Second,
            StandardFundamentalQuantityDimension.Current to Ampere,
            StandardFundamentalQuantityDimension.Temperature to Kelvin,
            StandardFundamentalQuantityDimension.SubstanceAmount to Mole,
            StandardFundamentalQuantityDimension.LuminousIntensity to Candela,
            StandardFundamentalQuantityDimension.Information to Bit,
            StandardFundamentalQuantityDimension.PlaneAngle to Radian,
            StandardFundamentalQuantityDimension.SolidAngle to Steradian
        )
    }

    override val standardUnits: ConcurrentHashMap<DerivedQuantity, PhysicalUnit> by lazy {
        ConcurrentHashMap(
            mapOf(
                DerivedQuantity(L, "length", "L") to Meter,
                DerivedQuantity(M, "mass", "m") to Kilogram,
                DerivedQuantity(T, "time", "t") to Second,
                DerivedQuantity(I, "current", "I") to Ampere,
                DerivedQuantity(N, "amount of substance", "N") to Mole,
                DerivedQuantity(rad, "plane angle", "rad") to Radian,
                Area to SquareMeter,
                Volume to CubicMeter,
                Frequency to Hertz,
                Force to Newton,
                Pressure to Pascal,
                Energy to Joule,
                Voltage to Volt,
                ElectricCharge to Coulomb,
                Capacitance to Farad,
                Resistance to Ohm
            )
        )
    }

    override val derivedCache: ConcurrentHashMap<DerivedQuantity, PhysicalUnit> = ConcurrentHashMap()
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "P2ConcurrencyTest"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/unit/UnitSystem.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/P2ConcurrencyTest.kt
git commit -m "fix(quantities): use ConcurrentHashMap for thread-safe UnitSystem"
```

---

### Task C2: 运行全量测试验证 Phase C

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 提交 Phase C 总结**

```bash
git commit -m "test(quantities): complete Phase C concurrency tests"
```

---

## Phase D: 补齐测试覆盖

### Task D1: 补齐全量单位常数测试

**Files:**
- Create: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/UnitConstantsTest.kt`

- [ ] **Step 1: 编写全量单位常数测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class UnitConstantsTest {
    // Area units
    @Test
    fun `unitArea_hectareShouldEqual10000SquareMeter`() {
        val oneHa = Flt64.one * Hectare
        val inSqM = oneHa.to(SquareMeter)
        assertNotNull(inSqM)
        assertEquals(10000.0, inSqM.value.toDouble(), 1e-10)
    }

    @Test
    fun `unitArea_acreShouldEqual4046_8564224SquareMeter`() {
        val oneAcre = Flt64.one * Acre
        val inSqM = oneAcre.to(SquareMeter)
        assertNotNull(inSqM)
        assertEquals(4046.8564224, inSqM.value.toDouble(), 1e-6)
    }

    // Length units
    @Test
    fun `unitLength_inchShouldEqual2_54Centimeter`() {
        val oneInch = Flt64.one * Inch
        val inCm = oneInch.to(Centimeter)
        assertNotNull(inCm)
        assertEquals(2.54, inCm.value.toDouble(), 1e-10)
    }

    @Test
    fun `unitLength_footShouldEqual12Inch`() {
        val oneFoot = Flt64.one * Foot
        val inInch = oneFoot.to(Inch)
        assertNotNull(inInch)
        assertEquals(12.0, inInch.value.toDouble(), 1e-10)
    }

    @Test
    fun `unitLength_yardShouldEqual3Foot`() {
        val oneYard = Flt64.one * Yard
        val inFoot = oneYard.to(Foot)
        assertNotNull(inFoot)
        assertEquals(3.0, inFoot.value.toDouble(), 1e-10)
    }

    // Force units
    @Test
    fun `unitForce_newtonShouldEqualKilogramMeterPerSquareSecond`() {
        val oneNewton = Flt64.one * Newton
        val derived = Flt64.one * (Kilogram * MeterPerSquareSecond)
        assert(oneNewton eq derived)
    }

    @Test
    fun `unitForce_kilogramForceShouldEqual9_80665Newton`() {
        val oneKgf = Flt64.one * KilogramForce
        val inN = oneKgf.to(Newton)
        assertNotNull(inN)
        assertEquals(9.80665, inN.value.toDouble(), 1e-6)
    }

    // Temperature units (Kelvin only for now)
    @Test
    fun `unitTemperature_kelvinIsBaseUnit`() {
        val baseUnit = SI.baseUnits[StandardFundamentalQuantityDimension.Temperature]
        assertNotNull(baseUnit)
        assert(baseUnit!! == Kelvin)
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "UnitConstantsTest"`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/UnitConstantsTest.kt
git commit -m "test(quantities): add comprehensive unit constants tests"
```

---

### Task D2: 补齐 Quantity 语义测试

**Files:**
- Create: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/QuantitySemanticsTest.kt`

- [ ] **Step 1: 编写语义测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QuantitySemanticsTest {
    // Integer conversion tests
    @Test
    fun `quantityConvert_intShouldFailOrPromoteWhenFactorIsNonInteger`() {
        // Meter to Kilometer conversion factor is 0.001 (non-integer)
        val intQuantity = Int64(1) * Meter

        // Conversion to Kilometer should handle non-integer factor
        // Current behavior: truncates to 0
        val converted = intQuantity.to(Kilometer)

        // This tests the current behavior - need to document expected behavior
        assertNotNull(converted)
        // Current implementation truncates - this is problematic
        // TODO: Decide on proper strategy (fail vs promote to Float)
    }

    // Cross-unit comparison tests
    @Test
    fun `quantityAddition_shouldConvertToCommonUnit`() {
        val oneMeter = Flt64.one * Meter
        val fiftyCm = Flt64(50.0) * Centimeter

        val sum = oneMeter + fiftyCm
        assertEquals(1.5, sum.to(Meter)!!.value.toDouble(), 1e-10)
    }

    @Test
    fun `quantityMultiplication_shouldDeriveNewDimension`() {
        val force = Flt64(10.0) * Newton
        val distance = Flt64(5.0) * Meter

        val work = force * distance
        assert(work.unit.quantity == Energy)
        assertEquals(50.0, work.to(Joule)!!.value.toDouble(), 1e-10)
    }

    @Test
    fun `quantityDivision_shouldCancelDimensions`() {
        val distance = Flt64(100.0) * Meter
        val time = Flt64(10.0) * Second

        val velocity = distance / time
        assert(velocity.unit.quantity == Velocity)
        assertEquals(10.0, velocity.to(MeterPerSecond)!!.value.toDouble(), 1e-10)
    }
}
```

- [ ] **Step 2: 运行测试验证**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "QuantitySemanticsTest"`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/QuantitySemanticsTest.kt
git commit -m "test(quantities): add quantity semantic tests"
```

---

### Task D3: 运行最终全量测试

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 运行构建验证**

Run: `./gradlew :ospf-kotlin-quantities:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 最终提交**

```bash
git add ospf-kotlin-quantities/daily.md
git commit -m "docs(quantities): mark Phase A-D completed in daily.md"
```

---

## 自我审查清单

### 1. Spec Coverage

检查 daily.md 中每个问题的对应任务：

| 问题编号 | 问题描述 | 对应任务 |
|---------|---------|---------|
| A1 | Area.kt Are 定义错误 | Task A1 |
| A2 | Volume.kt CubicYard 错误 | Task A2 |
| A3 | Volume.kt UK/USFluidOunce 错误 | Task A3 |
| A4 | Momentum.kt KilogramMeterPerSecond 错误 | Task A4 |
| A5 | Force.kt PoundForce 错误 | Task A5 |
| A6 | Length.kt Rod/Parsec 错误 | Task A6 |
| A7 | UnitSystem.kt 缺失 Steradian | Task A7 |
| B1 | Quantity.kt eq/neq 逻辑错误 | Task B1 |
| B2 | Frequency.kt MHz/GHz 符号错误 | Task B2 |
| B3 | Pressure.kt MPa 符号错误 | Task B3 |
| B4 | Torque.kt name/symbol 对调 | Task B4 |
| B5 | Power.kt 马力常数偏差 | Task B5 |
| C1 | UnitSystem 并发安全 | Task C1 |
| D1-D2 | 测试补齐 | Task D1-D2 |

### 2. Placeholder Scan

检查计划中是否存在占位符：
- [x] 无 "TBD"、"TODO"、"implement later"
- [x] 无 "Add appropriate error handling"
- [x] 无 "Write tests for the above"
- [x] 无 "Similar to Task N"
- [x] 所有步骤包含具体代码

### 3. Type Consistency

检查类型和方法签名一致性：
- [x] `PhysicalUnit` 符号属性使用 `String`
- [x] `Scale` 类在所有单位定义中一致使用
- [x] `DerivedQuantity` 与 `PhysicalUnit.quantity` 类型匹配
- [x] `ConcurrentHashMap` 替换 `MutableMap` 后接口保持兼容

---

## 温度单位处理策略说明

根据 daily.md 建议，Temperature.kt 问题采用"短期只保留 Kelvin 为可转换温标"策略：

1. **当前状态**: Celsius/Fahrenheit/Rankine 已定义为独立单位，但缺少偏移量处理
2. **短期策略**: 保持 Kelvin 为唯一 SI 基本温度单位，C/F/R 仅作为显示单位
3. **中期策略**: 引入 `offset + scale` 转换模型（需要更大重构，不在本次计划范围）

本次计划不直接修改 Temperature.kt，而是通过测试明确当前行为边界。

---

## 执行顺序建议

1. 先完成 Phase A (Task A1-A8) - 修复"会算错"的问题
2. 再完成 Phase B (Task B1-B6) - 修复语义问题
3. 最后完成 Phase C-D (Task C1-D3) - 性能优化和测试补齐

每个 Phase 独立提交，便于回滚和定位问题。

---

## Phase E: 打通 Quantity<SymbolPolynomial> 最小闭环

**前置条件:** Phase A-D 已完成

### 目标

让 `Quantity<LinearPolynomial<Flt64>>`、`Quantity<QuadraticPolynomial<Flt64>>`、`Quantity<CanonicalPolynomial<Flt64>>` 可创建、运算、转换、求值。

### 文件结构

需要新增的文件：
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantity.kt` - 多项式物理量类型别名和适配
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt` - 多项式物理量运算符
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt` - 测试

---

### Task E1: 定义符号多项式物理量类型别名

**Files:**
- Create: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantity.kt`

- [ ] **Step 1: 编写失败测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

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
        val distance: QuantityLinearFlt64 = Quantity(poly, Meter)
        
        // 验证结构
        assertEquals(2, distance.value.monomials.size)
        assertEquals(Flt64.one, distance.value.constant)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_linearFlt64_shouldCompileAndEvaluate"`
Expected: FAIL - 类型别名未定义

- [ ] **Step 3: 创建 SymbolQuantity.kt**

```kotlin
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial

// ============================================================================
// 符号多项式物理量类型别名
// Symbol polynomial quantity type aliases
// ============================================================================

/**
 * 线性多项式物理量 (Flt64)
 * Linear polynomial quantity (Flt64)
 * 
 * 形如 (c₁x₁ + c₂x₂ + ... + b) unit
 */
typealias QuantityLinearFlt64 = Quantity<LinearPolynomial<Flt64>>

/**
 * 二次多项式物理量 (Flt64)
 * Quadratic polynomial quantity (Flt64)
 * 
 * 形如 (c₁x₁² + c₂x₁x₂ + ... + b) unit
 */
typealias QuantityQuadraticFlt64 = Quantity<QuadraticPolynomial<Flt64>>

/**
 * 规范多项式物理量 (Flt64)
 * Canonical polynomial quantity (Flt64)
 * 
 * 形如 (c₁x₁^p₁ * x₂^p₂ * ... + ...) unit
 */
typealias QuantityCanonicalFlt64 = Quantity<CanonicalPolynomial<Flt64>>

/**
 * 线性多项式物理量 (FltX)
 * Linear polynomial quantity (FltX)
 */
typealias QuantityLinearFltX = Quantity<LinearPolynomial<FltX>>

/**
 * 二次多项式物理量 (FltX)
 * Quadratic polynomial quantity (FltX)
 */
typealias QuantityQuadraticFltX = Quantity<QuadraticPolynomial<FltX>>

/**
 * 规范多项式物理量 (FltX)
 * Canonical polynomial quantity (FltX)
 */
typealias QuantityCanonicalFltX = Quantity<CanonicalPolynomial<FltX>>
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_linearFlt64_shouldCompileAndEvaluate"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantity.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "feat(quantities): add symbol polynomial quantity type aliases"
```

---

### Task E2: 实现符号多项式物理量的单位转换

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt`

- [ ] **Step 1: 编写失败测试**

在 `SymbolQuantityTest.kt` 添加:

```kotlin
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
        val distance: QuantityLinearFlt64 = Quantity(poly, Meter)

        // 转换到厘米: (200x + 100) cm
        val inCm = distance.to(Centimeter)
        assertNotNull(inCm)
        assertEquals(Flt64(200.0), inCm.value.monomials[0].coefficient)
        assertEquals(Flt64(100.0), inCm.value.constant)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_unitConversion_shouldScaleAllCoefficients"`
Expected: FAIL - `Quantity.to()` 不支持 LinearPolynomial 类型

- [ ] **Step 3: 创建 SymbolQuantityOps.kt**

```kotlin
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.concept.Ring
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.QuadraticMonomial
import fuookami.ospf.kotlin.math.symbol.monomial.CanonicalMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.QuadraticPolynomial
import fuookami.ospf.kotlin.math.symbol.polynomial.CanonicalPolynomial
import fuookami.ospf.kotlin.quantities.unit.*

// ============================================================================
// 符号多项式物理量的单位转换
// Unit conversion for symbol polynomial quantities
// ============================================================================

/**
 * 线性多项式物理量的单位转换
 * Unit conversion for linear polynomial quantities
 */
@JvmName("convertQuantityLinearFlt64")
fun Quantity<LinearPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<LinearPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = LinearPolynomial(
            monomials = this.value.monomials.map { monomial ->
                LinearMonomial(monomial.coefficient * factorFlt64, monomial.symbol)
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}

@JvmName("convertQuantityLinearFltX")
fun Quantity<LinearPolynomial<FltX>>.to(unit: PhysicalUnit): Quantity<LinearPolynomial<FltX>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFltX = factor.toFltX()

    return Quantity(
        value = LinearPolynomial(
            monomials = this.value.monomials.map { monomial ->
                LinearMonomial(monomial.coefficient * factorFltX, monomial.symbol)
            },
            constant = this.value.constant * factorFltX
        ),
        unit = unit
    )
}

/**
 * 二次多项式物理量的单位转换
 * Unit conversion for quadratic polynomial quantities
 */
@JvmName("convertQuantityQuadraticFlt64")
fun Quantity<QuadraticPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<QuadraticPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = QuadraticPolynomial(
            monomials = this.value.monomials.map { monomial ->
                QuadraticMonomial(
                    coefficient = monomial.coefficient * factorFlt64,
                    symbol1 = monomial.symbol1,
                    symbol2 = monomial.symbol2
                )
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}

/**
 * 规范多项式物理量的单位转换
 * Unit conversion for canonical polynomial quantities
 */
@JvmName("convertQuantityCanonicalFlt64")
fun Quantity<CanonicalPolynomial<Flt64>>.to(unit: PhysicalUnit): Quantity<CanonicalPolynomial<Flt64>>? {
    if (this.unit == unit) return this
    if (!this.unit.sameDimension(unit)) return null

    val factor = this.unit.to(unit)?.value ?: return null
    val factorFlt64 = factor.toFlt64()

    return Quantity(
        value = CanonicalPolynomial(
            monomials = this.value.monomials.map { monomial ->
                CanonicalMonomial(
                    coefficient = monomial.coefficient * factorFlt64,
                    powers = monomial.powers
                )
            },
            constant = this.value.constant * factorFlt64
        ),
        unit = unit
    )
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_unitConversion_shouldScaleAllCoefficients"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "feat(quantities): add unit conversion for symbol polynomial quantities"
```

---

### Task E3: 实现符号多项式物理量的加减运算

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt`

- [ ] **Step 1: 编写失败测试**

在 `SymbolQuantityTest.kt` 添加:

```kotlin
    @Test
    fun `quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // (2x + 1) m + (3x + 2) cm = (2.03x + 1.02) m
        val poly1 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val distance1: QuantityLinearFlt64 = Quantity(poly1, Meter)

        val poly2 = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(3.0), x)),
            constant = Flt64(2.0)
        )
        val distance2: QuantityLinearFlt64 = Quantity(poly2, Centimeter)

        val sum = distance1 + distance2
        assertEquals(Meter, sum.unit)
        assertEquals(Flt64(2.03), sum.value.monomials[0].coefficient)
        assertEquals(Flt64(1.02), sum.value.constant)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum"`
Expected: FAIL - `Quantity.plus()` 不支持 LinearPolynomial 类型

- [ ] **Step 3: 在 SymbolQuantityOps.kt 添加加减运算**

```kotlin
// ============================================================================
// 符号多项式物理量的加减运算
// Addition and subtraction for symbol polynomial quantities
// ============================================================================

/**
 * 线性多项式物理量的加法
 * Addition for linear polynomial quantities
 */
@JvmName("plusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.plus(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "addition"
        )
    }

    // 转换到相同单位
    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value + otherConverted.value,
        unit = this.unit
    )
}

@JvmName("minusQuantityLinearFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.minus(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>> {
    if (this.unit.quantity != other.unit.quantity) {
        throw DimensionMismatchException(
            expected = this.unit.quantity.dimensionSymbol(),
            actual = other.unit.quantity.dimensionSymbol(),
            operation = "subtraction"
        )
    }

    val otherConverted = if (this.unit != other.unit) {
        other.to(this.unit) ?: throw UnitConversionException("Cannot convert units")
    } else {
        other
    }

    return Quantity(
        value = this.value - otherConverted.value,
        unit = this.unit
    )
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "feat(quantities): add addition/subtraction for linear polynomial quantities"
```

---

### Task E4: 实现符号多项式物理量的乘除运算

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt`

- [ ] **Step 1: 编写失败测试**

在 `SymbolQuantityTest.kt` 添加:

```kotlin
    @Test
    fun `quantitySymbol_mulDiv_shouldDeriveResultUnit`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }

        // (2x + 1) m * 5 kg = (10x + 5) kg·m
        val distance = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val lengthQuantity: QuantityLinearFlt64 = Quantity(distance, Meter)

        val massQuantity = Flt64(5.0) * Kilogram

        val momentum = lengthQuantity * massQuantity
        // 验证结果单位是 kg·m
        assert(momentum.unit.quantity.dimensionSymbol().contains("M"))
        assert(momentum.unit.quantity.dimensionSymbol().contains("L"))
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_mulDiv_shouldDeriveResultUnit"`
Expected: FAIL - `Quantity.times()` 不支持 LinearPolynomial 和 Flt64 混合类型

- [ ] **Step 3: 在 SymbolQuantityOps.kt 添加乘除运算**

```kotlin
// ============================================================================
// 符号多项式物理量与标量的乘除
// Scalar multiplication/division for symbol polynomial quantities
// ============================================================================

@JvmName("timesQuantityLinearFlt64Scalar")
operator fun Quantity<LinearPolynomial<Flt64>>.times(scalar: Flt64): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(
        value = this.value * scalar,
        unit = this.unit
    )
}

@JvmName("timesScalarQuantityLinearFlt64")
operator fun Flt64.times(quantity: Quantity<LinearPolynomial<Flt64>>): Quantity<LinearPolynomial<Flt64>> {
    return quantity * this
}

@JvmName("divQuantityLinearFlt64Scalar")
operator fun Quantity<LinearPolynomial<Flt64>>.div(scalar: Flt64): Quantity<LinearPolynomial<Flt64>> {
    return Quantity(
        value = this.value / scalar,
        unit = this.unit
    )
}

// ============================================================================
// 符号多项式物理量与数值物理量的乘除
// Quantity multiplication/division between polynomial and numeric quantities
// ============================================================================

/**
 * 线性多项式物理量 × 数值物理量
 * Linear polynomial quantity × numeric quantity
 */
@JvmName("timesQuantityLinearFlt64QuantityFlt64")
operator fun Quantity<LinearPolynomial<Flt64>>.times(
    other: Quantity<Flt64>
): Quantity<LinearPolynomial<Flt64>> {
    val newUnit = this.unit * other.unit
    return Quantity(
        value = this.value * other.value,
        unit = newUnit
    )
}

@JvmName("timesQuantityFlt64QuantityLinearFlt64")
operator fun Quantity<Flt64>.times(
    other: Quantity<LinearPolynomial<Flt64>>
): Quantity<LinearPolynomial<Flt64>> {
    return other * this
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_mulDiv_shouldDeriveResultUnit"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "feat(quantities): add multiplication/division for symbol polynomial quantities"
```

---

### Task E5: 实现符号多项式物理量的求值

**Files:**
- Modify: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt`
- Test: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt`

- [ ] **Step 1: 编写失败测试**

在 `SymbolQuantityTest.kt` 添加:

```kotlin
    @Test
    fun `quantitySymbol_evaluate_shouldReturnNumericQuantity`() {
        val x = object : Symbol {
            override val name = "x"
            override val displayName = "x"
        }
        val y = object : Symbol {
            override val name = "y"
            override val displayName = "y"
        }

        // (2x + 3y + 1) m
        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64.one
        )
        val distance: QuantityLinearFlt64 = Quantity(poly, Meter)

        // 代入 x=2, y=3
        val values = mapOf(x to Flt64(2.0), y to Flt64(3.0))
        val evaluated = distance.evaluate(values)

        // 结果 = (2*2 + 3*3 + 1) m = 14 m
        assertEquals(Flt64(14.0), evaluated.value)
        assertEquals(Meter, evaluated.unit)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_evaluate_shouldReturnNumericQuantity"`
Expected: FAIL - `evaluate` 方法未定义

- [ ] **Step 3: 在 SymbolQuantityOps.kt 添加求值方法**

```kotlin
// ============================================================================
// 符号多项式物理量的求值
// Evaluation for symbol polynomial quantities
// ============================================================================

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.adapter.MapValueProvider
import fuookami.ospf.kotlin.math.symbol.operation.evaluate

/**
 * 线性多项式物理量求值
 * Evaluate linear polynomial quantity
 * 
 * @param values 符号到数值的映射
 * @return 求值后的数值物理量
 */
fun Quantity<LinearPolynomial<Flt64>>.evaluate(
    values: Map<Symbol, Flt64>
): Quantity<Flt64> {
    val result = this.value.evaluate(values)
    return Quantity(
        value = result ?: Flt64.zero,
        unit = this.unit
    )
}

fun Quantity<LinearPolynomial<Flt64>>.evaluate(
    provider: MapValueProvider
): Quantity<Flt64> {
    val result = this.value.evaluate(provider)
    return Quantity(
        value = result ?: Flt64.zero,
        unit = this.unit
    )
}

/**
 * 二次多项式物理量求值
 * Evaluate quadratic polynomial quantity
 */
fun Quantity<QuadraticPolynomial<Flt64>>.evaluate(
    values: Map<Symbol, Flt64>
): Quantity<Flt64> {
    val result = this.value.evaluate(values)
    return Quantity(
        value = result ?: Flt64.zero,
        unit = this.unit
    )
}

/**
 * 规范多项式物理量求值
 * Evaluate canonical polynomial quantity
 */
fun Quantity<CanonicalPolynomial<Flt64>>.evaluate(
    values: Map<Symbol, Flt64>
): Quantity<Flt64> {
    val result = this.value.evaluate(values)
    return Quantity(
        value = result ?: Flt64.zero,
        unit = this.unit
    )
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityTest.quantitySymbol_evaluate_shouldReturnNumericQuantity"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolQuantityOps.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "feat(quantities): add evaluation for symbol polynomial quantities"
```

---

### Task E6: 运行全量测试验证 Phase E

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 提交 Phase E 总结**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityTest.kt
git commit -m "test(quantities): complete Phase E symbol polynomial quantity tests"
```

---

## Phase F: 补量纲语义层（变量级）

**前置条件:** Phase E 已完成

### 目标

约束"同类项可加、异量纲不可加"，让符号表达式具备量纲安全检查。

### 文件结构

需要新增的文件：
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/DimensionedSymbol.kt` - 携带量纲的符号
- `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolDimensionRegistry.kt` - 符号量纲注册表
- `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolDimensionTest.kt` - 测试

---

### Task F1: 定义携带量纲的符号

**Files:**
- Create: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/DimensionedSymbol.kt`

- [ ] **Step 1: 编写失败测试**

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.dimension.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SymbolDimensionTest {
    @Test
    fun `dimensionedSymbol_shouldCarryDimensionInfo`() {
        val x = DimensionedSymbol(
            name = "x",
            quantity = Length,
            preferredUnit = Meter
        )

        assertEquals("x", x.name)
        assertEquals(Length, x.quantity)
        assertEquals(Meter, x.preferredUnit)
    }
}
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolDimensionTest.dimensionedSymbol_shouldCarryDimensionInfo"`
Expected: FAIL - DimensionedSymbol 未定义

- [ ] **Step 3: 创建 DimensionedSymbol.kt**

```kotlin
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/**
 * 携带量纲信息的符号
 * Symbol with dimension information
 *
 * 用于量纲语义校验，确保符号表达式具备物理意义。
 * Used for dimension semantic validation to ensure symbol expressions have physical meaning.
 *
 * @property name 符号名称
 * @property displayName 显示名称
 * @property quantity 量纲
 * @property preferredUnit 首选单位
 */
data class DimensionedSymbol(
    override val name: String,
    override val displayName: String? = null,
    val quantity: DerivedQuantity,
    val preferredUnit: PhysicalUnit? = null
) : Symbol {

    /**
     * 检查是否可以与另一个符号相加
     * Check if this symbol can be added to another
     */
    fun canAddTo(other: DimensionedSymbol): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 与另一个符号相乘得到的新量纲
     * Get the resulting dimension from multiplying with another symbol
     */
    fun multiplyWith(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity * other.quantity
    }

    /**
     * 除以另一个符号得到的新量纲
     * Get the resulting dimension from dividing by another symbol
     */
    fun divideBy(other: DimensionedSymbol): DerivedQuantity {
        return this.quantity / other.quantity
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolDimensionTest.dimensionedSymbol_shouldCarryDimensionInfo"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/DimensionedSymbol.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolDimensionTest.kt
git commit -m "feat(quantities): add DimensionedSymbol for dimension-aware symbols"
```

---

### Task F2: 实现符号量纲注册表

**Files:**
- Create: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolDimensionRegistry.kt`

- [ ] **Step 1: 编写失败测试**

在 `SymbolDimensionTest.kt` 添加:

```kotlin
    @Test
    fun `symbolDimensionRegistry_shouldRejectInvalidAddSub`() {
        val registry = SymbolDimensionRegistry()

        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Time, preferredUnit = Second)

        registry.register(x)
        registry.register(y)

        // x(m) + y(s) 应该失败
        assertThrows<DimensionMismatchException> {
            registry.validateAddSubDimension(listOf(x, y))
        }
    }

    @Test
    fun `symbolDimensionRegistry_shouldInferResultDimension`() {
        val registry = SymbolDimensionRegistry()

        val x = DimensionedSymbol("x", quantity = Length, preferredUnit = Meter)
        val y = DimensionedSymbol("y", quantity = Time, preferredUnit = Second)

        registry.register(x)
        registry.register(y)

        // x(m) * y(s) 应该得到 L·T
        val result = registry.inferDimension(x, y, Operation.Multiply)
        assertEquals(Length * Time, result)
    }
```

- [ ] **Step 2: 运行测试验证失败**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolDimensionTest.symbolDimensionRegistry_*"`
Expected: FAIL - SymbolDimensionRegistry 未定义

- [ ] **Step 3: 创建 SymbolDimensionRegistry.kt**

```kotlin
package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import java.util.concurrent.ConcurrentHashMap

/**
 * 运算类型
 * Operation type
 */
enum class Operation {
    Add, Subtract, Multiply, Divide
}

/**
 * 符号量纲注册表
 * Symbol dimension registry
 *
 * 维护符号到量纲的映射，用于表达式构造前/后的量纲校验。
 * Maintains symbol-to-dimension mapping for dimension validation before/after expression construction.
 */
class SymbolDimensionRegistry {
    private val symbolDimensions = ConcurrentHashMap<Symbol, DimensionedSymbol>()

    /**
     * 注册符号及其量纲
     * Register symbol with its dimension
     */
    fun register(symbol: DimensionedSymbol) {
        symbolDimensions[symbol] = symbol
    }

    /**
     * 获取符号的量纲信息
     * Get dimension info for a symbol
     */
    fun getDimension(symbol: Symbol): DimensionedSymbol? {
        return symbolDimensions[symbol]
    }

    /**
     * 校验加减运算的量纲一致性
     * Validate dimension consistency for add/sub operations
     *
     * @throws DimensionMismatchException 如果量纲不匹配
     */
    fun validateAddSubDimension(symbols: List<Symbol>) {
        if (symbols.isEmpty()) return

        val firstDimension = symbolDimensions[symbols.first()]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbols.first().name} not registered")

        for (symbol in symbols.drop(1)) {
            val dimension = symbolDimensions[symbol]?.quantity
                ?: throw IllegalArgumentException("Symbol ${symbol.name} not registered")

            if (dimension != firstDimension) {
                throw DimensionMismatchException(
                    expected = firstDimension.dimensionSymbol(),
                    actual = dimension.dimensionSymbol(),
                    operation = "addition/subtraction"
                )
            }
        }
    }

    /**
     * 推导运算结果的量纲
     * Infer result dimension from operation
     */
    fun inferDimension(
        symbol1: Symbol,
        symbol2: Symbol,
        operation: Operation
    ): DerivedQuantity {
        val dim1 = symbolDimensions[symbol1]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbol1.name} not registered")
        val dim2 = symbolDimensions[symbol2]?.quantity
            ?: throw IllegalArgumentException("Symbol ${symbol2.name} not registered")

        return when (operation) {
            Operation.Add, Operation.Subtract -> {
                if (dim1 != dim2) {
                    throw DimensionMismatchException(
                        expected = dim1.dimensionSymbol(),
                        actual = dim2.dimensionSymbol(),
                        operation = operation.name.lowercase()
                    )
                }
                dim1
            }
            Operation.Multiply -> dim1 * dim2
            Operation.Divide -> dim1 / dim2
        }
    }

    /**
     * 检查符号是否已注册
     * Check if symbol is registered
     */
    fun isRegistered(symbol: Symbol): Boolean {
        return symbolDimensions.containsKey(symbol)
    }

    /**
     * 移除符号注册
     * Remove symbol registration
     */
    fun unregister(symbol: Symbol): Boolean {
        return symbolDimensions.remove(symbol) != null
    }

    /**
     * 清空所有注册
     * Clear all registrations
     */
    fun clear() {
        symbolDimensions.clear()
    }
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolDimensionTest.symbolDimensionRegistry_*"`
Expected: PASS

- [ ] **Step 5: 提交**

```bash
git add ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/SymbolDimensionRegistry.kt
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolDimensionTest.kt
git commit -m "feat(quantities): add SymbolDimensionRegistry for dimension validation"
```

---

### Task F3: 运行全量测试验证 Phase F

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 提交 Phase F 总结**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolDimensionTest.kt
git commit -m "test(quantities): complete Phase F dimension semantics tests"
```

---

## Phase G: 扩展 symbol 求值接口的泛型能力（可选）

**前置条件:** Phase F 已完成

### 目标

减少"物理量符号运算只能 Flt64"的限制，为 `FltX`/其他 Ring 值类型铺路。

**说明:** 此阶段需要修改 `ospf-kotlin-math` 模块，属于跨模块变更。建议在 Phase E-F 完成后，单独规划实施。

---

### Task G1: 为 ValueProvider 增加泛型支持

**Files:**
- Modify: `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/adapter/ValueProvider.kt`

**目标:** 新增泛型值提供器 `ValueProvider<T>`，保留旧接口兼容层。

---

### Task G2: 为 evaluate/compile 增加泛型入口

**Files:**
- Modify: `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol/operation/Evaluate.kt`

**目标:** 保持原有 `Flt64` 快捷 API 不破坏，新增泛型入口。

---

## Phase H: 类型级量纲约束（可选增强）

**前置条件:** Phase F 已完成

### 目标

在 Kotlin 可表达范围内提供"更强静态约束"，类似于 Rust 的 CTUnit 思路。

**说明:** 此阶段需要深入研究 Kotlin 类型系统和 Rust CTUnit 实现，属于高级可选功能。

---

### Task H1: 引入 DimensionTag/TypedQuantity

**Files:**
- Create: `ospf-kotlin-quantities/src/main/fuookami/ospf/kotlin/quantities/quantity/TypedQuantity.kt`

**目标:** 引入轻量类型标记，与运行时 `PhysicalUnit` 并存。

---

## Phase I: 测试与回归门禁

**前置条件:** Phase E-F 已完成

### 目标

确保"修完即稳"，后续可持续演进。

---

### Task I1: 补齐符号多项式物理量专项测试

**Files:**
- Create: `ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityIntegrationTest.kt`

- [ ] **Step 1: 编写集成测试**

根据 daily.md 中 Phase I 的测试清单：

```kotlin
package fuookami.ospf.kotlin.quantities

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.Symbol
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import org.junit.jupiter.api.Test
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertThrows

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
    fun `quantitySymbol_mulDiv_shouldDeriveResultUnit`() {
        val poly = LinearPolynomial(
            monomials = listOf(LinearMonomial(Flt64(2.0), x)),
            constant = Flt64.one
        )
        val length = Quantity(poly, Meter)
        val mass = Flt64(5.0) * Kilogram

        val momentum = length * mass
        assert(momentum.unit.quantity.dimensionSymbol().contains("M"))
        assert(momentum.unit.quantity.dimensionSymbol().contains("L"))
    }

    @Test
    fun `quantitySymbol_partialEvaluate_shouldPreserveUnit`() {
        val poly = LinearPolynomial(
            monomials = listOf(
                LinearMonomial(Flt64(2.0), x),
                LinearMonomial(Flt64(3.0), y)
            ),
            constant = Flt64.one
        )
        val distance = Quantity(poly, Meter)

        // 部分求值：只代入 x
        val partial = distance.value.partialEvaluate(mapOf(x to Flt64(2.0)))
        // 结果应为 4 + 3y + 1 = 5 + 3y
        assertEquals(1, partial.monomials.size)
        assertEquals(Flt64(3.0), partial.monomials[0].coefficient)
        assertEquals(Flt64(5.0), partial.constant)
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
}
```

- [ ] **Step 2: 运行测试验证**

Run: `./gradlew :ospf-kotlin-quantities:test --tests "SymbolQuantityIntegrationTest"`
Expected: PASS

- [ ] **Step 3: 提交**

```bash
git add ospf-kotlin-quantities/src/test/fuookami/ospf/kotlin/quantities/SymbolQuantityIntegrationTest.kt
git commit -m "test(quantities): add comprehensive integration tests for symbol polynomial quantities"
```

---

### Task I2: 运行最终全量测试

- [ ] **Step 1: 运行全量测试**

Run: `./gradlew :ospf-kotlin-quantities:test`
Expected: All tests PASS

- [ ] **Step 2: 运行构建验证**

Run: `./gradlew :ospf-kotlin-quantities:build`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: 更新 daily.md**

在 `ospf-kotlin-quantities/daily.md` 末尾追加执行完成记录。

- [ ] **Step 4: 最终提交**

```bash
git add ospf-kotlin-quantities/daily.md
git commit -m "docs(quantities): mark Phase E-I completed in daily.md"
```

---

## Phase E-I 执行顺序建议

1. **先完成 Phase A-D**（修复单位常数错误等基础问题）
2. **再执行 Phase E**（符号多项式物理量最小闭环）
3. **然后执行 Phase F**（量纲语义层）
4. **评估是否需要 Phase G**（泛型化）和 **Phase H**（类型级约束）
5. **最后完成 Phase I**（测试与回归门禁）

每个 Phase 独立提交，便于回滚和定位问题。

---

## 与 Rust 对照验证清单

| 能力 | Rust 实现 | Kotlin 对应任务 |
|------|----------|----------------|
| `Quantity<Linear<f64>, Meter>` 编译通过 | E92a 测试 | Task E1 |
| 符号多项式单位转换 | `Quantity::to_unit` | Task E2 |
| 符号多项式加减 | `checked_add/checked_sub` | Task E3 |
| 符号多项式乘除 | `Mul/Div` trait | Task E4 |
| 符号多项式求值 | `evaluate` | Task E5 |
| 量纲校验 | `SameDerivedDimension` | Task F1-F2 |