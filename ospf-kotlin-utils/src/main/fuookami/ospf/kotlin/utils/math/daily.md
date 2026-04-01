# ospf-kotlin-utils/math 对标 ospf-rust-math 改进纪要

最后更新：2026-04-01
范围：`ospf-kotlin-utils/src/main/fuookami/ospf/kotlin/utils/math`

---

## 已完成事项（汇总）

### P0 稳定基线
- 修复 Kotlin 2.2 相关编译阻塞与签名兼容问题。
- 修复 `Compile.kt` canonical compileEval 对 `one` 的透传。
- 修复 `Quantity.kt` 缺失导入导致的解析问题。
- 修复 `Vector.minus` 行为并补回归测试。

### P1 API 补齐（数论 + 组合）
- 数论：`defactorize`、`divisors`、`divisorCount`、`eulerTotient`、`gcdMod`、`extendedGcd`、`lcmByFactorization`。
- 组合：`combineCount/combine/combineSequence`、`permuteCount/permute/permuteSequence`、`crossCount/crossSequence/cross2/cross3`。

### P2 抽象升级（代数 + 几何）
- 新增抽象：`TotallyOrdered`、`VectorSpace`、`NormedSpace`、`InnerProductSpace`。
- `Vector` 对齐：`scale`、`dot`、`angle`、`projectionOn`、2D/3D `cross`、`Flt64 * Vector`。
- 新增 `Quadrilateral` 实体（边/对角线/周长/重心/2D 面积/三角剖分面积/凸性）与专项测试。

### P3 Symbol 体系增强
- 新增：`SymbolId`、`OwnedSymbolLike`、`OwnedSymbol`、`Symbol.stableId()`、`Symbol.owned(...)`。
- 兼容保留：`IdentifiedSymbol`，并统一 `identity()/defaultSymbolComparator` 到稳定 id 语义。
- 完成 symbol round-trip 与高阶项回归补测。

### P4 工程化与补测
- 文档：中英 README 互链已完成。
- 基准：JMH 入口与 `bench` profile 已落地。
- 补测：`chaotic_operator`、`fractal_operator`、`geometry` 主链路已补齐。
- 修复：`Distance.Minkowski` 对负差值奇数幂错误。

### T3/T4 迁移与收敛
- T1（algebra 属性与一致性补测）：已完成。
- T2（symbol round-trip 与高阶项回归补测）：已完成。
- T3（伴生对象常量接口最小落地）：已完成。
- T4（受控 reflection fallback）：已完成。
- T3 扩展（显式 constants 路径）：已完成。

已完成的代表性收敛：
- `Collection/Fold/Precision/Ordinary/ValueRange` 显式 constants 入口与调用点迁移。
- fallback 关闭语义下的专项回归矩阵（显式路径可用、reified 默认路径抛错）。
- `Log/Pow/Precision` resolver 调用语义收敛（避免不必要触发）。
- `ValueRange.contains`、`NegativeInfinity.copy/clone` 等语义修复。

---

## 2026-04-01 新增完成事项

### U6 Symbol Phase 8 / Generic 收口
- **修复 JVM 签名冲突**：删除冗余的 Flt64-specific `toGeneric*` 扩展函数，保留泛型版本（`LinearGeneric.kt`、`QuadraticGeneric.kt`、`CanonicalGeneric.kt`）。
- **验证泛型化架构**：`operation/Compile.kt`、`Differentiate.kt`、`Evaluate.kt`、`Latex.kt`、`MatrixForm.kt` 均已通过泛型委托实现 Flt64 支持。
- **新增端到端回归测试**：`GenericEndToEndTest.kt`（10 tests）覆盖 `evaluate -> gradient -> matrixForm -> compile` 一致性校验（Linear/Quadratic/Canonical 三种路径）。
- **Generic MatrixForm 评估**：当前 `MatrixFormGeneric.kt` 仅支持 Flt64，因数值计算特性保持 Flt64-focused 为合理设计。
- **parser/serde 策略评估**：保持 Flt64-focused，通过 `toGeneric*Polynomial()` 转换函数支持 generic 使用场景，为最小落地路径。

### U3 ValueRange 基准补齐
- 新增 `MathValueRangeBenchmark.kt`：
  - 构造基准：FiniteClosedRange、FiniteOpenRange、HalfInfiniteLower、HalfInfiniteUpper、InfiniteRange。
  - Contains 基准：单值、边界值、范围、批量操作（1000 elements）。
  - Copy 基准：各类型 ValueRange 的复制性能。
- 与已有 `bench` profile 对接。

### U4 operator 包专项测试粒度提升
- 新增 `operator/OperatorCoreTest.kt`（29 tests）：
  - `Abs`：Flt64/Int64 abs 函数。
  - `Ord`：Order 类型、negation、ifEqual、orderOf、orderBetween、Flt64 ord。
  - `Tolerance`：AbsoluteTolerance 包装。
  - `Reciprocal`：Flt64/Int64 reciprocal。
  - `Pow`：pow/sqr/cub 函数、边界情况（0^0、负指数、大指数）。
  - `Log`：ln/lg2/lg/logBase 函数。

### U5 ValueRange 子组件测试拆分
- 新增 `ValueRangeComponentTest.kt`（26 tests）：
  - `Interval`：signs、union、intersect、outer、lowerBoundOperator、upperBoundOperator。
  - `ValueWrapper`：finite value wrap、Infinity、NegativeInfinity、ord、unwrap、copy。
  - `Bound`：value/interval 存储、eq、partialOrd、加减乘除、copy、unaryMinus。

### U8 geometry 先行修复（isolines 高度映射）
- 修复 `Triangulation.kt` 中 `triangulate(isolines)` 对 `nextLine` 点错误使用 `thisLine.first` 的问题。
- 在 `TriangulationTest.kt` 新增回归用例，校验结果同时包含两条 isoline 的高度值。
- 在 `TriangulationTest.kt` 增补边界场景：重复点输入、近共线点输入，确保三角剖分主链路稳定。

### U2 M1 编译期开闭区间最小落地
- 新增 `TypedValueRange.kt`：
  - 引入编译期开闭标记 `ClosedIntervalKind/OpenIntervalKind` 与运行时标记 `RuntimeIntervalKind`。
  - 引入 `TypedValueRange<T, LB, UB>` 双轨模型，并提供 `toDynamic()/fromDynamic()`。
  - 补齐 `contains/union/intersect/plus/minus/times/div` typed 路径最小实现（运算结果收敛到 dynamic typed）。
- 新增 `TypedValueRangeTest.kt`（6 tests）：
  - 覆盖开闭边界 contains、interval mismatch、防回归的 union/intersect、以及加减乘除语义。

### U7 M1 Symbol MatrixForm 泛型化最小落地
- 扩展 `MatrixFormGeneric.kt`：
  - 新增 `GenericLinearMatrixForm<T>` / `GenericQuadraticMatrixForm<T>`。
  - 新增 `toGenericMatrixForm(...)`（Linear/Quadratic）generic 路径，支持自定义二次项拆分策略。
  - 现有 `Flt64` 快速路径 `toMatrixVector/toMatrixPair` 保留并复用 generic 实现。
- 新增 `MatrixFormGenericTest.kt`（3 tests）：
  - 覆盖 generic 线性矩阵化、generic 二次矩阵化、以及与 `Flt64` 快速路径一致性校验。

---

## 当前验证基线

### 全量测试
```bash
mvn -pl ospf-kotlin-utils clean test
```
通过。

### 新增测试文件
| 文件 | 测试数 | 覆盖范围 |
|------|--------|---------|
| `GenericEndToEndTest.kt` | 10 | generic 路径端到端一致性 |
| `OperatorCoreTest.kt` | 29 | operator 核心路径 |
| `ValueRangeComponentTest.kt` | 26 | Bound/Interval/ValueWrapper |
| `TypedValueRangeTest.kt` | 6 | 编译期开闭 typed range 最小语义 |
| `MatrixFormGenericTest.kt` | 3 | generic matrix form 最小语义 |
| `MathValueRangeBenchmark.kt` | - | ValueRange 性能基准 |

### 新增基准文件
```bash
mvn -pl ospf-kotlin-utils -Pbench jmh:run -Dbenchmark=MathValueRangeBenchmark
```

---

## 未完成事项

### U2 ValueRange 类型系统增强：编译期开闭区间语义（高优先级）
- 现状：M1 已完成 typed range 最小实现，现有运算结果仍主要收敛到 dynamic typed，尚未完成全链路类型化。
- 待办：
  - 继续推进运算结果类型化收敛（在可行场景下保留 compile-time interval 信息）。
  - 补更多边界语义测试（Infinity/NegativeInfinity、半开半闭组合、contains(range) 复杂场景）。
  - 增加 compile-time 路径与 runtime 路径的 JMH 对比基准，形成性能基线。

### U6 Symbol Phase 8 / Generic 收口（剩余待办）
- **模块内回归补齐**：补跑并固化 symbol core 回归矩阵（`evaluate/gradient/matrixForm/compile/roundtrip`），形成 generic bridge 证据链。
- **parser/serde 增强（低优先级）**：若需支持 Int64/BigDecimal 等类型直接解析，需设计 `NumberParser<T>` 接口，当前推荐使用 Flt64 解析后转换。

### U7 Symbol MatrixForm 泛型化增强（中优先级）
- 现状：M1 已完成 generic matrix form 数据结构与转换函数；`operation/MatrixForm.kt` 仍以 `Flt64` + `DoubleArray` 为主。
- 待办：
  - 设计并落地 generic matrix form 到 `operation` 层的桥接 API（避免业务调用侧重复转换）。
  - 评估并补齐 generic 路径 round-trip 与 Hessian 一致性测试（generic + Flt64 双路径）。

### U8 geometry 模块健壮性与类型约束增强（中优先级）
- 现状：`Point/Vector` 以 `List<Flt64>` + 运行时 assert 为主；`triangulate(isolines)` 存在高度映射风险。
- 待办：
  - 修复 `triangulate(isolines)` 中 nextLine 点的 z 值映射问题并补回归测试。
  - 评估并落地 `Dim2/Dim3` 的固定结构表示（在兼容 API 前提下减少运行时检查开销）。
  - 增补 Delaunay 边界场景测试（重复点、近共线点、浮点边界）。

### U9 基准矩阵扩展（低优先级）
- 现状：`MathOrdinaryBenchmark` 与 `MathValueRangeBenchmark` 已落地。
- 待办：
  - 补 `symbol` 路径基准（compile/evaluate/gradient/matrixForm）。
  - 补 `geometry` 路径基准（distance/triangulation）。
  - 输出基准运行模板与结果留档格式，便于持续回归对比。

---

## 文件变更清单（2026-04-01）

### 源码修改
| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `generic/LinearGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericLinearMonomial/toGenericLinearPolynomial` |
| `generic/QuadraticGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericQuadraticMonomial/toGenericQuadraticPolynomial` |
| `generic/CanonicalGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericCanonicalMonomial/toGenericCanonicalPolynomial` |
| `geometry/Triangulation.kt` | 逻辑修复 | 修复 isolines 三角化 nextLine 高度映射 |
| `algebra/value_range/TypedValueRange.kt` | 新增文件 | 编译期开闭 typed range 最小实现 |
| `symbol/generic/MatrixFormGeneric.kt` | 功能扩展 | generic matrix form 最小实现与 Flt64 路径复用 |

### 新增测试
| 文件 | 说明 |
|------|------|
| `generic/GenericEndToEndTest.kt` | Symbol generic 端到端回归测试 |
| `benchmark/MathValueRangeBenchmark.kt` | ValueRange 性能基准 |
| `operator/OperatorCoreTest.kt` | operator 核心路径测试 |
| `algebra/value_range/ValueRangeComponentTest.kt` | ValueRange 子组件测试 |
| `geometry/TriangulationTest.kt` | isolines + 边界场景回归测试（新增 3 cases） |
| `algebra/value_range/TypedValueRangeTest.kt` | typed range 语义回归测试（新增 6 cases） |
| `symbol/generic/MatrixFormGenericTest.kt` | generic matrix form 回归测试（新增 3 cases） |

---

## 下一步执行顺序

1. **U2 ValueRange 编译期开闭区间方案设计与最小实现**
2. **U7 Symbol MatrixForm 泛型化最小落地**
3. **U8 geometry 健壮性修复与类型约束增强**
4. **U6 parser/serde NumberParser<T> 方案设计（先设计后落地）**
5. **U9 benchmark 矩阵扩展与留档规范**
