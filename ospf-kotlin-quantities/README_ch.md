# ospf-kotlin-quantities / OSPF Kotlin 物理量模块

:us: [English](README.md) | :cn: 简体中文

OSPF Kotlin 的综合物理量和单位库。提供强类型物理量、单位转换和量纲分析，专注于类型安全、正确性和易用性。

## 概述

`ospf-kotlin-quantities` 旨在为科学计算、工程应用和任何需要物理量计算的领域提供稳健的基础。核心设计原则包括：

- **类型安全**：强类型配合量纲分析，防止无效操作
- **正确性**：基于权威来源（NIST、BIPM）的精确单位转换
- **可扩展性**：易于添加新的单位和物理量
- **性能**：针对编译期和运行时效率进行优化

## 模块结构

| 包 | 描述 | 关键类型 |
|---|------|---------|
| `dimension` | 物理量纲和物理量 | `FundamentalQuantity`、`DerivedQuantity`、`Dimensions`、`QuantityDomain` |
| `quantity` | 物理量类型和运算 | `Quantity<V>`、`DurationExtensions`、`MinMax`、`ValueRange` |
| `math/symbol` | 带物理量语义的符号辅助类型 | `SymbolQuantity`、`DimensionedSymbol`、`SymbolDimensionRegistry`、`SymbolQuantityOps` |
| `math/geometry` | 支持物理量的几何图元 | `Axis2`、`Axis3`、`Box2`、`Box3`、`Cuboid3`、`Cylinder3`、`Placement3`、`Shape3` |
| `unit` | 物理单位定义 | `PhysicalUnit`、`UnitSystem`、300+ 预定义单位 |

## 架构设计

### 量纲层次结构

```
FundamentalQuantity（SI 基本物理量 + 辅助量纲）
├── Length（长度）、Mass（质量）、Time（时间）、Current（电流）
├── Temperature（温度）、SubstanceAmount（物质的量）、LuminousIntensity（发光强度）
├── PlaneAngle（平面角）、SolidAngle（立体角）
└── Information（信息）

DerivedQuantity（导出物理量）
├── Area（面积）、Volume（体积）、Velocity（速度）、Acceleration（加速度）
├── Force（力）、Energy（能量）、Power（功率）、Pressure（压强）
├── Frequency（频率）、Momentum（动量）、Torque（扭矩）
├── Voltage（电压）、Resistance（电阻）、ElectricCharge（电荷）、Capacitance（电容）
├── Bandwidth（带宽）、FlowRate（流量）、MassDensity（质量密度）、SurfaceDensity（面密度）
└── 更多（Dimensions.kt 中预定义 50+ 种）...
```

### 单位系统

```
PhysicalUnit（抽象类）
├── 基本单位（Meter、Kilogram、Second 等）
├── 导出单位（Newton、Joule、Watt 等）
├── SI 前缀单位（Kilometer、Megahertz 等）
└── 非 SI 单位（Foot、Pound、Horsepower 等）

UnitSystem（接口）
├── SI  - 国际单位制（7 个基本量纲 + 3 个辅助量纲）
├── MKS - 米-千克-秒制（力学子集）
└── CGS - 厘米-克-秒制（力学子集）
```

## 核心功能

### 物理量创建与运算

```kotlin
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.math.algebra.number.*

// 创建带单位的物理量
val length = Flt64(5.0) * Meter
val time = Flt64(2.0) * Second
val velocity = length / time  // 2.5 m/s

// 单位转换
val distanceInFeet = length.to(Foot)  // 转换为英尺
val timeInMinutes = time.to(Minute)   // 转换为分钟

// 算术运算
val totalLength = length + Flt64(3.0) * Meter  // 8.0 m
val doubledLength = length * Flt64(2.0)        // 10.0 m
```

### 量纲安全

```kotlin
// 编译期和运行时的量纲检查
val mass = Flt64(10.0) * Kilogram
val length = Flt64(5.0) * Meter

// 编译和运行都正确（力 = 质量 × 加速度）
val acceleration = Flt64(9.81) * MeterPerSecondSquared
val force = mass * acceleration  // 98.1 N

// 运行时会失败（量纲不匹配）
// val invalid = mass + length  // 不能将质量与长度相加
```

### 单位转换

```kotlin
// 长度转换
val meters = Flt64(1000.0) * Meter
val kilometers = meters.to(Kilometer)  // 1.0 km
val feet = meters.to(Foot)             // 3280.84 ft
val inches = meters.to(Inch)           // 39370.1 in

// 压强转换
val pascals = Flt64(101325.0) * Pascal
val atmospheres = pascals.to(Atmosphere)  // 1.0 atm
val bars = pascals.to(Bar)               // 1.01325 bar

// 整数物理量转换（非整数因子返回 null）
val intMeters = Int64(1000) * Meter
val intKilometers = intMeters.to(Kilometer)  // 1 km（成功）
val intFeet = intMeters.to(Foot)             // null（非整数因子）
```

### Duration 互操作

```kotlin
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

// Quantity 与 kotlin.time.Duration 互转
val time = Flt64(5.0) * Second
val result = time.toDuration()
when (result) {
    is Ok -> {
        val duration: Duration = result.value  // 5 秒
        val quantity = duration.toQuantity<Flt64>(Minute)
        // 0.0833... 分钟
    }
    is Failed -> { /* 处理错误 */ }
    is Fatal -> { /* 处理严重错误 */ }
}

// 便捷方法
val duration = 90.seconds
val hours = duration.toQuantityHoursFlt64()     // Ok(0.025 h)
val bestFit = duration.toQuantityBestFit<Flt64>()  // 自动选择最佳单位
```

### 符号物理量支持

```kotlin
import fuookami.ospf.kotlin.math.symbol.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*

// 带符号值的物理量
val x = Symbol("x")
val quantity: Quantity<LinearPolynomial<Flt64>> =
    (LinearMonomial(Flt64(2.0), x) + Flt64(3.0)) * Meter

// 在特定值处求值
val result = quantity.evaluate(mapOf(x to Flt64(5.0)))
// (2*5 + 3) m = 13 m
```

### 支持的物理量和单位

#### 基本物理量（10 种）
| 物理量 | SI 单位 | 常用单位 |
|-------|---------|---------|
| 长度 | 米 (m) | 千米、英尺、英寸、英里、海里、天文单位、光年、秒差距 |
| 质量 | 千克 (kg) | 克、吨、磅、盎司 |
| 时间 | 秒 (s) | 分、时、天、年、毫秒、纳秒 |
| 电流 | 安培 (A) | 毫安、千安 |
| 温度 | 开尔文 (K) | 摄氏度、华氏度 |
| 物质的量 | 摩尔 (mol) | 毫摩尔、千摩尔 |
| 发光强度 | 坎德拉 (cd) | 毫坎德拉 |
| 平面角 | 弧度 (rad) | 度、角分、角秒 |
| 立体角 | 球面度 (sr) | - |
| 信息 | 比特 (bit) | 字节、千比特、兆比特、吉比特 |

#### 导出物理量（50+ 种）
| 物理量 | SI 单位 | 示例 |
|-------|---------|------|
| 面积 | m² | 公顷、英亩、平方英尺 |
| 体积 | m³ | 升、加仑、立方码 |
| 速度 | m/s | km/h、mph、节 |
| 加速度 | m/s² | 重力加速度、ft/s² |
| 力 | 牛顿 (N) | 达因、磅力 |
| 能量 | 焦耳 (J) | 卡路里、BTU、电子伏特 |
| 功率 | 瓦特 (W) | 马力、兆瓦 |
| 压强 | 帕斯卡 (Pa) | 巴、大气压、PSI |
| 频率 | 赫兹 (Hz) | 千赫、兆赫 |
| 动量 | N·s | kg·m/s |
| 扭矩 | N·m | ft·lbf |
| 角速度 | rad/s | deg/s |
| 角加速度 | rad/s² | deg/s² |
| 催化活度 | 开特 (kat) | 酶单位 |
| 电压 | 伏特 (V) | 毫伏、千伏 |
| 电阻 | 欧姆 | 毫欧、千欧 |
| 电荷 | 库仑 (C) | 安时 |
| 电容 | 法拉 (F) | 微法、皮法 |
| 带宽 | bit/s | Kbit/s、Mbit/s、Gbit/s |
| 流量 | m³/s | L/s、Gallon/min |
| 质量密度 | kg/m³ | g/cm³ |
| 面密度 | kg/m² | - |
| 应力 | 帕斯卡 (Pa) | 兆帕、吉帕 |
| 波数 | m⁻¹ | cm⁻¹ |

### 错误处理

所有转换操作返回 `Ret<T>` 以实现安全的错误处理：

```kotlin
import fuookami.ospf.kotlin.utils.functional.*

val result = quantity.toDuration()
when (result) {
    is Ok -> {
        val duration = result.value
        // 使用 duration
    }
    is Failed -> {
        println("错误: ${result.code} - ${result.message}")
    }
    is Fatal -> {
        println("严重错误")
        result.errors.forEach { println(it.message) }
    }
}
```

## 性能优化

| 功能 | 优化方式 | 说明 |
|------|---------|------|
| 单位转换 | 缓存转换因子 | 减少重复计算 |
| 量纲分析 | 延迟求值 | 仅在需要时计算 |
| 整数转换 | 提前返回 null | 非整数因子快速路径 |
| 单位系统 | ConcurrentHashMap | 线程安全单例 |

## 测试

```powershell
# 运行所有测试
mvn -pl ospf-kotlin-quantities test

# 运行特定测试类
mvn -pl ospf-kotlin-quantities -Dtest=DurationExtensionsTest test

# 详细输出运行测试
mvn -pl ospf-kotlin-quantities test -Dsurefire.useFile=false
```

测试覆盖包括：
- 单位常数正确性（NIST/BIPM 值）
- 量纲分析
- 单位转换精度
- 整数类型处理
- Duration 互操作
- 符号物理量运算
- 线程安全

## 依赖

| 模块 | 用途 |
|------|------|
| `ospf-kotlin-math` | 数学类型（Flt64、Int64、Symbol 等） |
| `ospf-kotlin-utils` | 错误处理（Ret、Error）、函数式类型 |

## 单位常数来源

所有单位转换常数均来自权威来源：
- **SI 单位**：BIPM（国际计量局）
- **非 SI 单位**：NIST Special Publication 811
- **天文单位**：IAU（国际天文学联合会）

具体来源引用见各单位文件。

## 相关模块

- [ospf-kotlin-math](../ospf-kotlin-math) - 数学代数与符号系统
- [ospf-kotlin-utils](../ospf-kotlin-utils) - 工具函数与错误处理

## 许可证

本模块是 OSPF Kotlin 项目的一部分，采用 Apache License 2.0 许可证。
