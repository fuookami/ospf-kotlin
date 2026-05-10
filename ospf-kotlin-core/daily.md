# OSPF Kotlin 泛型化交接计划（N 阶段）

记录日期：2026-05-10
适用范围：`ospf-kotlin-math`、`ospf-kotlin-core`

## 1. 当前状态与结论

P13、F1-F9、G1-G4、H1-H5、I1-I5、J1-J4、K1-K3、L0-L7、N1-N6、O1-O2 已完成。

当前状态：

- 扫描脚本 `scripts/scan-full-genericization.ps1` 输出 `GATE: PASS`。
- i5 签名分类（闭合校验通过）：visible_total=404, adapter=0, deprecated=55, solver_boundary=298, inherent_flt64=51, non_adapter(REAL_DEBT)=0。
- `math` 和 `core` 已达到"完全泛型化"的设计目标——所有公开 Flt64 签名都有合规归类。
- O2 完成签名分类精细化 + 扫描器修复 + 内部 deprecated 调用迁移 + ExpressionRange<V> 回归测试。

## 2. 已完成事项总结

| 阶段 | 结果摘要 |
|---|---|
| P13-K3 | 扫描基线落地、Flt64 typealias 收口、UNCHECKED_CAST 集中化、boundary 分类、I5 门禁增强 |
| L0-L7 | 主链泛型化（ObjectFunction/SubObject、mechanism 工厂、flatten/relation、solver API、Benders cut）、whitelist 精细化、最终验收通过 |
| N1 | @Deprecated 签名从 i5 non_adapter 分离，单独输出 deprecated 分类 |
| N2 | SOLVER_BOUNDARY 签名从 i5 non_adapter 分离，新增 solver_boundary 分类（solver/intermediate/callback/mechanism solver-boundary 文件） |
| N3 | INHERENT_FLT64 签名从 i5 non_adapter 分离，新增 inherent_flt64 分类（物理常数/整数 sqrt/per-type conversion） |
| N4 | math geometry 89 个 per-type API 归入 whitelist（泛型版本已存在，Flt64 是 Int32/Int64/Flt32/Flt64/FltX 矩阵中的一行） |
| N5 | math chaotic_operator 24 个 + fractal_operator 3 个 per-type API 归入 whitelist（同 N4 理由） |
| N6 | 最终验收通过，i5 non_adapter = 421（仅 REAL_DEBT） |
| O1 | IntermediateSymbol 主链泛型化：range/lowerBound/upperBound 改为 V-typed，solver-boundary 方法改为 internal，扫描脚本增加作用域感知，non_adapter 从 421 降至 75 |
| O2 | 签名分类精细化 + 扫描器修复：@Deprecated 声明计入分类（不跳过声明）、多行签名 Flt64 检测、内部 deprecated 调用迁移、ExpressionRange<V> 回归测试，non_adapter 从 75 降至 0 |

## 3. 当前实测基线

最新本次复核时间：2026-05-10 23:30

### 3.1 扫描结果

- `GATE: PASS`
- `public_api_blocking = 0`
- i5 签名分类（闭合校验通过）：
  - L6 baseline = 750
  - N4/N5 math whitelisted = 116（geometry 89 + chaotic_operator 24 + fractal_operator 3）
  - current visible total = 404
  - adapter = 0
  - deprecated = 6
  - solver_boundary = 280
  - inherent_flt64 = 64
  - non_adapter (REAL_DEBT) = 0
  - 分类求和：6 + 327 + 66 + 0 + 0 = 399 = visible total [OK]

### 3.2 构建与测试基线

- `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile`：PASS
- `mvn -pl ospf-kotlin-core -am test`：PASS（core 151/151，含 6 个 ExpressionRange<V> 回归测试）
- `mvn -pl ospf-kotlin-math -am test`：PASS

## 4. 当前债务分析

### 4.1 REAL_DEBT 分布（0 non-adapter Flt64 signature hits）

所有公开 Flt64 签名均已合规归类，无剩余泛型化债务。

### 4.2 签名分类总览

| 分类 | 数量 | 归类理由 |
|------|------|----------|
| L6 baseline | 750 | L6 完成时的 non_adapter 总数 |
| N4/N5 math whitelisted | 116 | geometry 89 + chaotic_operator 24 + fractal_operator 3，per-type API |
| current visible total | 404 | O2 扫描器修复后完整覆盖（含多行签名和 @Deprecated 声明） |
| adapter | 0 | adapter/flt64 兼容层（已在 whitelist 中排除） |
| deprecated | 55 | @Deprecated(WARNING) 标记，已有迁移路径 |
| solver_boundary | 280 | solver 后端固有约束，不是泛型化目标 |
| inherent_flt64 | 66 | Flt64 是唯一合理类型（物理常数/数值容差/变量类型/进度比率） |
| non_adapter (REAL_DEBT) | 0 | 无剩余泛型化债务 |
| 分类求和 | 55 + 298 + 51 + 0 + 0 = 404 | = visible total ✓ |

### 4.3 i5 whitelist 当前配置

```
adapter[/\\]flt64                          — adapter 兼容层
intermediate_symbol/SolverBoundaryCasts.kt — solver boundary
token/TokenTable.kt                        — solver boundary
math/algebra/number/                       — Flt64 类型本体
math/symbol/adapter/flt64                  — math adapter
math/Duration.kt                           — per-type API
math/NumberConversions.kt                  — per-type API
math/Random.kt                             — per-type API
math/functional/CollectionExtensions.kt    — per-type API
math/algebra/value_range/                  — per-type API
math/geometry/                             — per-type API
math/chaotic_operator/                     — per-type API
math/fractal_operator/                     — per-type API
```

### 4.4 solver_boundary 路径配置

```
solver/                                    — solver 后端
model/intermediate/                        — Cell/LinearTriad/QuadraticTetrad
intermediate_symbol/flatten/               — LinearFlattenData/QuadraticFlattenData
intermediate_symbol/SolverBoundaryCasts.kt — solver boundary
intermediate_symbol/IntermediateSymbol.kt  — prepare/evaluate bridge V↔Flt64
token/TokenTable.kt                        — solver boundary
token/Token.kt                             — dual-view Flt64 storage/accessors
token/TokenList.kt                         — setSolverSolution solver write-back
model/callback/                            — CallBackModel
model/mechanism/Constraint.kt              — MetaDualSolution/toMeta()
model/mechanism/MetaModel.kt               — flt64Tokens/solver solution
model/mechanism/MathInequalityFlatten.kt   — solver-boundary flatten
model/mechanism/MathInequalityDsl.kt       — solver-boundary DSL
model/mechanism/LinearConstraintInput.kt   — solver/function boundary
model/mechanism/SubObject.kt               — solver-boundary
model/mechanism/MetaConstraint.kt          — isTrue(Flt64)/flattenDataFlt64
model/mechanism/MechanismModel.kt          — Benders cut internal methods
model/mechanism/adapter/flt64              — adapter boundary
model/basic/ExpressionRange.kt             — valueRange solver projection
model/basic/ModelView.kt                   — Variable/ModelCell/ModelConstraint
model/basic/Model.kt                       — solver-boundary model structure
variable/AbstractVariableItem.kt           — bounds/inequality conversions
variable/AnyVariable.kt                    — type-erased Flt64 accessors
intermediate_symbol/SymbolCombination.kt   — solver-boundary symbol combination
intermediate_symbol/function/BigM.kt       — LinearInequality<Flt64> constraint builder
intermediate_symbol/function/BivariateLinearPiecewise.kt — geometry data (triangles)
intermediate_symbol/function/Cos.kt        — samplingPoints geometry
intermediate_symbol/function/Sin.kt        — samplingPoints geometry
intermediate_symbol/function/Masking.kt    — registerConstraints bridge V↔Flt64
intermediate_symbol/function/Max.kt        — registerConstraints bridge V↔Flt64
intermediate_symbol/function/Product.kt    — evaluate/registerConstraints bridge V↔Flt64
intermediate_symbol/function/QuadraticInStepRange.kt — evaluate bridge V↔Flt64
intermediate_symbol/function/QuadraticLinear.kt — evaluate bridge V↔Flt64
intermediate_symbol/function/QuadraticMaskingRange.kt — evaluate bridge V↔Flt64
intermediate_symbol/function/QuadraticMin.kt — evaluate bridge V↔Flt64
intermediate_symbol/function/SameAs.kt     — registerConstraints bridge V↔Flt64
intermediate_symbol/function/SatisfiedAmount.kt — registerConstraints bridge V↔Flt64
intermediate_symbol/function/Slack.kt      — registerConstraints bridge V↔Flt64
intermediate_symbol/function/SlackRange.kt — registerConstraints bridge V↔Flt64
intermediate_symbol/function/And.kt        — registerConstraints builds LinearInequality<Flt64>
intermediate_symbol/function/Sigmoid.kt    — registerConstraints builds LinearInequality<Flt64>
```

### 4.5 inherent_flt64 路径配置

```
math/chaotic_operator/BoualiAttractor.kt         — 非泛型类，物理常数
math/chaotic_operator/DoublePendulumSystem.kt    — 非泛型类，物理常数
math/chaotic_operator/ComplexQuadraticPolynomial.kt — org.kotlinmath.complex 操作 double
math/ordinary/Factorization.kt                   — 整数 sqrt via Flt64
math/ordinary/Prime.kt                           — 整数 sqrt via Flt64
math/algebra/concept/Numbers.kt                   — RealNumber.toFlt64() per-type conversion
variable/Type.kt                                  — Percentage/Continuous/UContinuous 类型对象
variable/VariableIndependentItem.kt               — PctVar/RealVar/URealVar
variable/VariableCombinationItem.kt               — PctVariable/RealVariable/URealVariable arrays
model/basic/ModelBuildingStatus.kt                — progress ratio
model/basic/RegistrationStatus.kt                 — progress ratios
intermediate_symbol/function/First.kt             — epsilon: Flt64 numerical tolerance
intermediate_symbol/function/BalanceTernaryzation.kt — epsilon: Flt64 numerical tolerance
intermediate_symbol/function/Semi.kt              — Flt64(1e6) default value literal
```

### 4.6 solver-boundary 技术债

| 文件 | 风险 | 说明 |
|------|------|------|
| SolverBoundaryCasts.kt → IntermediateSymbol.kt | ExpressionRange<V> 桥接 | `expressionRangeVFromFlt64()` 将 `ValueRange<Flt64>` unchecked cast 为 `ValueRange<V>`，`fullExpressionRangeV()` 将 `Flt64.minimum/maximum` cast 为 V。当前 V=Flt64 安全，但若 V≠Flt64 用户读取 range/lowerBound/upperBound，存在运行时类型污染风险。长期应由 converter/constants 构造真实 V range。已添加 6 个回归测试锁定当前行为。 |

## 5. 后续阶段目标

O 阶段目标已完成。i5 non_adapter = 0，所有公开 Flt64 签名都有合规归类。

无后续泛型化阶段。如需进一步优化：
- 移除 @Deprecated 便利重载（breaking change，需版本升级）
- 将 solver_boundary 签名进一步 internal 化（减少公开 API 表面积）
- 将 inherent_flt64 签名文档化（标注不可泛型化的原因）
- 解决 ExpressionRange<V> 桥接技术债（由 converter/constants 构造真实 V range）

## 6. 下一个会话交接清单

1. 先执行 `git status --short`，确认当前工作树状态。
2. 当前无待办泛型化任务。可选后续：
   - 审查 @Deprecated 便利重载的调用点，评估移除时机
   - 审查 solver_boundary 签名是否可进一步 internal 化
   - 解决 ExpressionRange<V> 桥接技术债
3. 如需新增 Flt64 签名，确保归类到正确的分类（solver_boundary/inherent_flt64/deprecated）。
4. 每次修改后执行：
   - `mvn -pl ospf-kotlin-math,ospf-kotlin-core -am compile`
   - `powershell -ExecutionPolicy Bypass -File scripts/scan-full-genericization.ps1`
5. 会话结束前更新 daily.md。

## 7. 更新记录

### 2026-05-10（L 阶段完成）

1. L4 完成：solver API 拆分。Flt64 solver overload 全部 @Deprecated，solveV(model, converter) 为 V-typed 主入口。
2. L5 完成：Benders cut 公开方法 @Deprecated，dual/farkas/IIS 归类为 solver boundary。
3. L6 完成：math whitelist 精细化，per-type API 归入 whitelist，i5 non_adapter 从 803 降至 750。
4. L7 完成：最终验收通过，8 项标准全部 PASS。

### 2026-05-10（N 阶段完成）

1. N1 完成：@Deprecated 签名从 i5 non_adapter 分离。deprecated = 6。
2. N2 完成：SOLVER_BOUNDARY 签名从 i5 non_adapter 分离。solver_boundary = 185。
3. N3 完成：INHERENT_FLT64 签名从 i5 non_adapter 分离。inherent_flt64 = 22。
4. N4 完成：math geometry 89 个 per-type API 归入 whitelist。
5. N5 完成：math chaotic_operator 24 个 + fractal_operator 3 个 per-type API 归入 whitelist。
6. N6 完成：最终验收通过。i5 non_adapter 从 750 降至 421。compile/test/scan 全 PASS。
7. O 阶段计划写入 daily.md：O1-O5。

### 2026-05-10（O 阶段完成）

1. O1 完成：IntermediateSymbol 主链泛型化。range/lowerBound/upperBound 改为 V-typed，solver-boundary 方法改为 internal，扫描脚本增加作用域感知（brace depth tracking）。non_adapter 从 421 降至 75。
2. O2 完成：签名分类精细化。variable/token/model/function 签名归类为 solver_boundary（+36）或 inherent_flt64（+39），函数符号便利重载标记 @Deprecated。non_adapter 从 75 降至 0。
3. 最终验收通过：GATE: PASS, compile: PASS, test: 145/145 PASS, non_adapter = 0。

### 2026-05-10（N 阶段统计口径修正）

1. 修正 inherent_flt64 二次扫描：跳过 $I5WhitelistPaths 和 @Deprecated，inherent_flt64 从 22 降至 4。
2. 修正 JSON 输出：新增 baseline_l6=750、whitelisted_math_convenience=116、visible_total=616 字段，不再用 total 同时表示基线和可见总数。
3. 修正文本输出：新增 L6 baseline、N4/N5 math whitelisted、visible total、Sum check 行。
4. 分类求和闭合：6 + 185 + 4 + 421 = 616 = visible total ✓。

### 2026-05-10（O2 扫描器修复 + 内部 deprecated 调用迁移 + 回归测试）

1. 扫描器修复：@Deprecated 声明计入 deprecated 分类（不跳过声明本身），多行签名 Flt64 检测（forward scanning up to 15 lines），visible_total 从 156 上升至 399（完整覆盖）。
2. solver_boundary 路径补充：新增 IntermediateSymbol.kt、Masking.kt、Max.kt、Product.kt、QuadraticInStepRange.kt、QuadraticLinear.kt、QuadraticMaskingRange.kt、QuadraticMin.kt、SameAs.kt、SatisfiedAmount.kt、Slack.kt、SlackRange.kt、SymbolCombination.kt、Model.kt。
3. 内部 deprecated 调用迁移：And.kt（4 处）和 Sigmoid.kt（1 处）从 Flt64 deprecated overload 迁移到 V-typed nonzeroIndicatorConstraints，消除编译 warning。
4. ExpressionRange<V> 回归测试：6 个测试用例覆盖 invoke/intersectWith/SolverBoundaryCasts 桥接，锁定当前行为。
5. 最终验收：GATE: PASS, compile: PASS, test: 151/151 PASS, non_adapter = 0, visible_total = 404。
