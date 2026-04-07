# ospf-kotlin-quantities 交接记录（2026-04-07）

## 1. 审阅范围
- Kotlin 模块：`ospf-kotlin-quantities`
- 对照实现：`E:/workspace/ospf-rust/ospf-rust-quantities`
- 本次状态：仅完成审阅与验证，不改业务代码

## 2. 审阅意见（正确性 / 性能 / 测试）

### 2.1 正确性问题（P0，需优先修复）
1. `Area.kt` 中 `Are` 定义错误，当前为 `Decimeter * Decimeter`（0.01 m²），应为 100 m²。
2. `Volume.kt` 中 `CubicYard` 错写为 `SquareFoot * Foot`，应为 `Yard * Yard * Yard`。
3. `Volume.kt` 中 `UKFluidOunce`、`USFluidOunce` 基于 `Millimeter * 常数`，尺度链路错误，导致量值错误约 1000 倍。
4. `Momentum.kt` 中 `KilogramMeterPerSecond` 使用了 `KilogramForceMeter / Second`，多引入重力因子，维度值错误。
5. `Force.kt` 中 `PoundForce` 使用 `GramForce * Gram.to(Pound)`，换算方向错误，数值明显偏小。
6. `Length.kt` 中 `Rod` 使用 `198.838 in`，应为 `198 in`（5.0292 m）。
7. `Length.kt` 中 `Parsec` 使用 `20265 AU`，应为 `206265 AU`（当前少一位，误差 10 倍）。
8. `Temperature.kt` 中温度转换模型缺少偏移量处理（C/K/F），目前只用比例会导致绝对温标转换错误。
9. `UnitSystem.kt` 的 `SI.baseUnits` 缺失 `SolidAngle -> Steradian` 映射，同时 Kotlin 侧无 `Steradian` 单位定义，影响立体角相关推导。
10. `Quantity.kt` 中 `eq/neq` 对量纲不一致都返回 `false`，违背常见逻辑契约（`neq` 应为 `!eq`）。
11. `Quantity.kt` 中整数数量转换先把比例转整数再乘，存在静默截断（如非整数比例会变 0 或丢精度）。

### 2.2 正确性问题（P1，建议紧随 P0）
1. `Frequency.kt` 中 `Megahertz` 符号写成 `mHz`，`Gigahertz` 写成 `gHz`。
2. `Pressure.kt` 中 `Megapascal` 符号写成 `mPa`。
3. `Torque.kt` 中 `name` 和 `symbol` 对调。
4. `Power.kt` 中马力常数与主流定义有偏差（`Horsepower=735`、`UKHorsepower=550`），需明确采用标准并统一文档。

### 2.3 性能与并发问题（P2）
1. `UnitSystem` 使用可变 `MutableMap` 作为缓存和标准单位表，无并发保护；单例对象在并发访问下存在竞态风险。
2. `DerivedQuantity` 的 `times/div` 高频路径使用 `mutableMap + filter + map + 构造`，对象分配偏重，批量计算下有可观 GC 压力。

### 2.4 测试覆盖问题（P0-P2 全程补齐）
1. 当前测试主要验证少量 happy path，缺失单位常数正确性测试。
2. 缺失 SI 基本单位完整性测试（尤其立体角）。
3. 缺失跨单位比较语义测试（`eq/neq` 契约）。
4. 缺失整数值转换边界测试（截断、失败策略）。
5. 缺失温标偏移行为测试（如果继续支持摄氏/华氏）。

## 3. 详细改进计划（交接执行顺序）

### Phase A（P0）：先修“会算错”的问题
- 目标：消除单位常数与公式错误，保证基础换算可信。
- 任务：修复 `Area.kt`、`Volume.kt`、`Momentum.kt`、`Force.kt`、`Length.kt` 的错误定义。
- 任务：补齐 `Steradian` 单位定义，并加入 `SI.baseUnits`。
- 任务：先统一策略处理 `Temperature.kt`。
- 建议策略：短期只保留 Kelvin 为可转换温标，C/F/R 暂标记为“显示单位（不可直接线性 to）”；中期引入 `offset + scale` 转换模型。
- 验收：对应新增测试全部通过，且与 Rust 对照值一致。

### Phase B（P1）：修语义与 API 行为
- 目标：让比较和转换行为符合直觉且可预测。
- 任务：调整 `Quantity.eq/neq` 逻辑，保证 `neq == !eq`（至少在可比较范围内成立）。
- 任务：重构整数类型转换策略。
- 建议策略：
- 方案 1：整数转换返回失败（`null` 或异常）当比例非整数。
- 方案 2：统一提升到浮点（`FltX/Flt64`）后再转换，显式由调用方决定是否取整。
- 验收：新增语义测试通过，行为写入文档。

### Phase C（P2）：性能与并发优化
- 目标：降低竞态风险与运行时分配开销。
- 任务：`UnitSystem.standardUnits/derivedCache` 切换为线程安全容器（如 `ConcurrentHashMap`）或加读写锁。
- 任务：为 `DerivedQuantity` 引入更轻量的内部结构（有序数组或小型 map）并减少中间对象。
- 任务：为 `dimensionSymbol()` 增加缓存并验证缓存失效策略（如需要）。
- 验收：并发测试稳定、基准测试（或简易压测）相对当前版本有可见收益。

### Phase D（P3）：测试与回归门禁
- 目标：避免同类问题再次回归。
- 任务：补齐下方测试清单。
- 任务：把关键换算测试放入 CI 必跑集。
- 任务：补一份“单位常数来源说明”（注释或文档）并标注标准来源。

## 4. 需要新增的测试清单（测试名称 + 简述）

| 测试名称（建议） | 简单描述 |
| --- | --- |
| `unitArea_areShouldEqual100SquareMeter` | 验证 `1 are == 100 m²`。 |
| `unitArea_hectareShouldEqual10000SquareMeter` | 验证 `1 ha == 10000 m²`。 |
| `unitArea_acreShouldEqual4046_8564224SquareMeter` | 验证 acre 常数精度。 |
| `unitVolume_cubicYardShouldEqual27CubicFoot` | 验证立方码和立方英尺关系。 |
| `unitVolume_ukFluidOunceShouldEqual28_4130625Milliliter` | 验证英制液盎司换算。 |
| `unitVolume_usFluidOunceShouldEqual29_5735295625Milliliter` | 验证美制液盎司换算。 |
| `unitVolume_ukGallonShouldEqual4_54609Liter` | 验证英制加仑换算。 |
| `unitVolume_usGallonShouldEqual3_78541178Liter` | 验证美制加仑换算。 |
| `unitMomentum_kilogramMeterPerSecondShouldEqualNewtonSecond` | 验证 `kg·m/s` 与 `N·s` 一致。 |
| `unitForce_poundForceShouldEqual4_4482216152605Newton` | 验证 lbf 常数。 |
| `unitLength_rodShouldEqual5_0292Meter` | 验证 rod 常数。 |
| `unitLength_parsecShouldEqual206265AstronomicalUnit` | 验证 parsec 常数。 |
| `unitSystem_siShouldContainSolidAngleBaseUnit` | 验证 SI 基本单位包含立体角映射。 |
| `unitSystem_luminousFluxShouldBeDerivableInSI` | 验证 `LuminousFlux` 在 SI 下可推导标准单位。 |
| `quantityEqNeq_shouldBeLogicalComplementOnDimensionMismatch` | 验证量纲不一致时 `eq/neq` 契约。 |
| `quantityConvert_intShouldFailOrPromoteWhenFactorIsNonInteger` | 验证整数转换遇到非整数系数时的策略一致性。 |
| `temperature_celsiusKelvinFahrenheit_shouldHandleOffsetCorrectly` | 若保留 C/F，验证偏移量转换正确。 |
| `unitSymbol_megahertzShouldBeMHz` | 验证 MHz 符号。 |
| `unitSymbol_gigahertzShouldBeGHz` | 验证 GHz 符号。 |
| `unitSymbol_megapascalShouldBeMPa` | 验证 MPa 符号。 |
| `unitTorque_nameAndSymbolShouldBeCorrect` | 验证扭矩单位 name/symbol 没有对调。 |
| `unitSystem_concurrentReadWriteCacheShouldBeSafe` | 并发访问标准单位/缓存时不出现异常和脏数据。 |

## 5. 交接说明
- 建议先完成 Phase A，再跑一轮全量测试与回归。
- Phase B 的“整数转换策略”和“温标策略”需要先拍板行为规范，再落实现。
- 每修完一类问题，立即补对应测试，避免再次回归。
