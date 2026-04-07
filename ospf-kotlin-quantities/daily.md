# ospf-kotlin-quantities 交接记录（2026-04-07）

## 0. 执行状态（更新）

### Phase A 已完成 ✅
- [x] Task A1: 修复 Area.kt 中 Are 定义（从 Decimeter² 改为 Meter * 100）
- [x] Task A2: 修复 Volume.kt 中 CubicYard、UK/USFluidOunce、UK/USGallon 定义
- [x] Task A3: 修复 Momentum.kt 中 KilogramMeterPerSecond 定义
- [x] Task A4: 修复 Force.kt 中 PoundForce 定义
- [x] Task A5: 修复 Length.kt 中 Rod 和 Parsec 定义
- [x] Task A6: 新增 Steradian 单位定义并更新 SI.baseUnits
- [x] Task A7: 全量测试通过（26 tests）

提交记录:
- `3a3ad55b`: fix(quantities): correct PoundForce definition
- `149b2e42`: fix(quantities): correct Rod and Parsec definitions  
- `92a2193f`: feat(quantities): add Steradian unit and update SI.baseUnits

### Phase B 已完成 ✅
- [x] Task B1-B3: 修复 Frequency/Pressure/Torque 符号错误（MHz/GHz/MPa，name/symbol swap）
- [x] Task B5: 调整 Quantity.eq/neq 逻辑（neq 返回 true 当量纲不一致）
- [ ] Task B4: Temperature 温标转换策略（待决策，暂跳过）

提交记录:
- `3fe67027`: fix(quantities): correct unit symbols for Megahertz, Gigahertz, Megapascal, and Torque
- `8a282db6`: fix(quantities): correct neq logic to return true on dimension mismatch

### Phase C 已完成 ✅
- [x] Task C1: UnitSystem 并发安全修复（MutableMap → ConcurrentHashMap）
- [ ] Task C2: DerivedQuantity 运算优化（暂缓）

提交记录:
- `d14ec5aa`: feat(quantities): make UnitSystem thread-safe with ConcurrentHashMap

### Phase D 已完成 ✅
- [x] Task D1: 整数值转换边界测试（非整数因子返回 null）
- [x] Task D2: SI 基本单位完整性测试（10 个基本量纲）
- [x] Task D3: 单位常数来源说明文档

提交记录:
- `e2c2f630`: fix(quantities): return null for non-integer conversion factors
- `4c4e8f31`: test(quantities): add SI base unit completeness tests
- `aaf74cad`: docs(quantities): add unit constant source documentation

### 测试统计
- 总测试数: 51
- 覆盖: P0 单位常数、P1 符号/逻辑、P2 并发、D 整数转换/SI 基本单位

### 待完成
- [ ] Phase B4: Temperature 温标转换策略（需要策略决策）
- Phase C（P2）：性能与并发优化
- Phase D（P3）：测试与回归门禁
- Phase E-I：符号运算物理量支持

---

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

## 6. 面向“符号运算 + 物理量支持”的补充审阅意见（新增）

### 6.1 结论（必须补充）
- 仅完成 Phase A-D 后，`ospf-kotlin-quantities` 仍**不能自动让** `ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/symbol` 具备“物理量符号运算”能力。
- 当前 A-D 主要修的是单位常数、语义、并发、测试；而“符号运算物理量支持”还缺少跨模块的适配层与量纲规则层。

### 6.2 关键差距（与 Rust 对照后确认）
1. **依赖方向限制**：Kotlin 当前是 `quantities -> math`，`math` 不能反向依赖 `quantities`，否则形成循环依赖。  
2. **symbol 运行链路偏数值化**：大量求值/编译封装绑定 `Flt64`，且 `ValueProvider` 也是 `Symbol -> Flt64`。  
3. **缺少量纲语义入口**：symbol 的 AST、Parser、Evaluate 没有单位/量纲校验钩子。  
4. **缺少与 Rust E92 对齐能力**：Rust 已验证 `Quantity<Linear<...>, Meter>` 这类“符号值 + 物理单位”可编译可运算；Kotlin 侧尚未补齐对应适配层。

### 6.3 建议策略（推荐）
- 采用 **Adapter First** 路线：先在 `ospf-kotlin-quantities` 提供 `Quantity<Polynomial>` 适配，不强行重构 `math/symbol` 公共模型。
- 先实现运行时量纲约束，再考虑类型级（编译期）量纲约束。

## 7. 面向符号运算物理量的详细改进计划（交接执行顺序，新增）

### Phase E（P0）：先打通 `Quantity<SymbolPolynomial>` 最小闭环
- 目标：让 `Quantity<LinearPolynomial<Flt64>>`、`Quantity<QuadraticPolynomial<Flt64>>`、`Quantity<CanonicalPolynomial<Flt64>>` 可创建、运算、转换、求值。
- 任务：
1. 在 `ospf-kotlin-quantities` 新增适配文件（建议 `quantity/SymbolQuantity.kt` 与 `quantity/SymbolQuantityOps.kt`）。
2. 新增类型别名：  
   `QuantityLinearFlt64`、`QuantityQuadraticFlt64`、`QuantityCanonicalFlt64`。
3. 实现单位转换：单位换算因子作用于多项式**所有系数与常数项**，保持符号结构不变。
4. 实现数量运算：  
   - 同量纲 `+/-`（必要时先转单位）；  
   - `* /` 生成新单位（复用现有 `PhysicalUnit` 乘除逻辑）；  
   - 标量乘除沿用 `symbol` 既有算子。
5. 提供求值桥接：接入 `symbol.operation.evaluate/compileEval`，输出仍为 `Quantity<Flt64>`。
- 验收：
1. 可构建并运行示例：`(2x+1) m + (300 cm)`。  
2. `to(unit)` 后多项式系数按比例变化且表达式结构不变。  
3. 与 Rust E92a 的“符号值作为 Quantity.value”能力对齐（先覆盖 Flt64）。

### Phase F（P1）：补量纲语义层（变量级）
- 目标：约束“同类项可加、异量纲不可加”，让符号表达式具备量纲安全检查。
- 任务：
1. 在 `ospf-kotlin-quantities` 增加 `DimensionedSymbol`（实现 `Symbol`，携带 `DerivedQuantity` 与可选 `preferredUnit`）。
2. 增加 `SymbolDimensionRegistry`（符号到量纲映射），用于表达式构造前/后校验。
3. 增加校验 API：  
   - `validateAddSubDimension(expr)`；  
   - `inferDimension(expr)`（对 `* /` 推导结果量纲）。
4. 明确错误策略：抛异常或返回 `Ret`（建议与现有 `Quantity` 行为保持一致）。
- 验收：
1. `x(m) + y(s)` 明确失败；  
2. `x(m) * y(s)` 推导为 `L·T`；  
3. 校验可在 DSL 构造阶段触发，而非仅运行期。

### Phase G（P1/P2）：扩展 symbol 求值接口的泛型能力（减少 Flt64 绑定）
- 目标：减少“物理量符号运算只能 Flt64”的限制，为 `FltX`/其他 Ring 值类型铺路。
- 任务：
1. 新增泛型值提供器（例如 `ValueProvider<T>`），保留旧接口兼容层。  
2. 为 `evaluate/compile` 增加泛型入口（保持原有 `Flt64` 快捷 API 不破坏）。  
3. 对 `expression/operation/EvaluateBoolean.kt` 增加可扩展的数值比较策略（避免仅 `Int/Long/Double`）。
- 验收：
1. 原有测试全绿；  
2. 新增 `FltX` 场景可跑通基本求值；  
3. 没有破坏旧调用方二进制兼容（至少源码兼容）。

### Phase H（P2，可选增强）：类型级量纲约束（Rust CTUnit 思路）
- 目标：在 Kotlin 可表达范围内提供“更强静态约束”。
- 任务（可选）：
1. 引入 `DimensionTag`/`TypedQuantity<V, D>` 形式的轻量类型标记。  
2. 对常见基础量纲建立 marker 类型与运算映射（乘除得到组合 tag）。  
3. 与运行时 `PhysicalUnit` 并存，不替换现有 API。
- 验收：
1. 常见误用在编译阶段即可暴露；  
2. 不影响原始 `Quantity<V>` 用法；  
3. 文档明确“运行时模式 vs 类型标记模式”。

### Phase I（P3）：测试与回归门禁（symbol × quantities 专项）
- 目标：确保“修完即稳”，后续可持续演进。
- 任务：
1. 新增专项测试并加入 CI 必跑。  
2. 覆盖运行时量纲、单位转换、符号求值、并发缓存、错误契约。
- 建议新增测试名：
1. `quantitySymbol_linearFlt64_shouldCompileAndEvaluate`
2. `quantitySymbol_unitConversion_shouldScaleAllCoefficients`
3. `quantitySymbol_addition_shouldConvertToCommonUnitBeforeSum`
4. `quantitySymbol_addition_dimensionMismatch_shouldFail`
5. `quantitySymbol_mulDiv_shouldDeriveResultUnit`
6. `quantitySymbol_compileEval_shouldMatchDirectEvaluate`
7. `quantitySymbol_partialEvaluate_shouldPreserveUnit`
8. `quantitySymbol_toStandardUnit_shouldKeepExpressionShape`
9. `quantitySymbol_dimensionRegistry_shouldRejectInvalidAddSub`
10. `quantitySymbol_concurrentRegistryAndCache_shouldBeSafe`

## 8. 下个环境执行清单（可直接照做，新增）
- [ ] 先完成原计划 Phase A-D 并回归全测。  
- [ ] 启动 Phase E（不改 `math` 公共 API，仅在 `quantities` 增适配）。  
- [ ] Phase E 通过后再做 Phase F（变量级量纲语义）。  
- [ ] 评估是否进入 Phase G（泛型化）与 Phase H（类型级约束）。  
- [ ] 完成 Phase I 并将新增专项测试纳入 CI。

## 9. 交接补充说明（新增）
- 本补充计划的核心目标是：在不破坏现有模块依赖结构的前提下，最终实现“符号值可作为物理量值类型”的能力。  
- 与 Rust 对齐策略：先对齐运行时能力（E/F），再评估编译期类型约束（H）。  
- 执行建议：每个 Phase 独立提交，避免一次性大改导致回归定位困难。
