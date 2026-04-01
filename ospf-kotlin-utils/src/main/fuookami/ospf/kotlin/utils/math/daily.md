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
| `MathValueRangeBenchmark.kt` | - | ValueRange 性能基准 |

### 新增基准文件
```bash
mvn -pl ospf-kotlin-utils -Pbench jmh:run -Dbenchmark=MathValueRangeBenchmark
```

---

## 未完成事项

### U2 ValueRange 类型系统增强：编译期开闭区间语义（高优先级）
- 现状：Rust 同时支持编译期开闭类型 + 运行时区间；Kotlin 当前以运行时 `Interval` 为主。
- 待办：
  - 设计 Kotlin 侧可落地方案（保持现有 API 兼容，优先最小侵入）。
  - 补对应属性测试与性能回归，防止语义回退。

### U6 Symbol Phase 8 / Generic 收口（剩余待办）
- **外部仓库回归**：外部仓库恢复后补跑 core 模块级回归，形成 generic bridge 的完整证据链。
- **parser/serde 增强（低优先级）**：若需支持 Int64/BigDecimal 等类型直接解析，需设计 `NumberParser<T>` 接口，当前推荐使用 Flt64 解析后转换。

---

## 文件变更清单（2026-04-01）

### 源码修改
| 文件 | 变更类型 | 说明 |
|------|---------|------|
| `generic/LinearGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericLinearMonomial/toGenericLinearPolynomial` |
| `generic/QuadraticGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericQuadraticMonomial/toGenericQuadraticPolynomial` |
| `generic/CanonicalGeneric.kt` | 删除冗余代码 | 删除 Flt64-specific `toGenericCanonicalMonomial/toGenericCanonicalPolynomial` |

### 新增测试
| 文件 | 说明 |
|------|------|
| `generic/GenericEndToEndTest.kt` | Symbol generic 端到端回归测试 |
| `benchmark/MathValueRangeBenchmark.kt` | ValueRange 性能基准 |
| `operator/OperatorCoreTest.kt` | operator 核心路径测试 |
| `algebra/value_range/ValueRangeComponentTest.kt` | ValueRange 子组件测试 |

---

## 下一步执行顺序

1. ~~U6 Symbol Phase 8 / Generic 收口~~（核心已完成，剩余外部仓库回归）
2. ~~U3 ValueRange 基准落地~~（已完成）
3. ~~U4 + U5 测试矩阵增强~~（已完成）
4. **U2 编译期开闭区间方案设计与最小实现**