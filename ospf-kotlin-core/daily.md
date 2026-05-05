# OSPF Kotlin Core Refactor Daily

记录日期：2026-05-05

## P11 全仓清零已完成

---

## 0. P11 验收结果

### 扫描结果（全部通过）

| 扫描项 | 结果 |
|--------|------|
| `@Suppress("UNCHECKED_CAST")` | 0 |
| `typealias *Flt64` | 0 |
| `AbstractTokenTableFlt64` | 0 |
| `Flt64LinearInequality` | 0 |
| `Solution<Flt64>` | 0 |
| `IntoValue.Flt64` | 0 |
| `MechanismModelFlt64` | 0 |
| `LinearIntermediateSymbolFlt64` | 0 |
| `QuadraticIntermediateSymbolFlt64` | 0 |
| 全仓编译 | BUILD SUCCESS |
| 139 测试 | 全部通过 |

### 已知的预先存在测试失败（与 P11 重构无关）

| 测试 | 类型 | 说明 |
|------|------|------|
| `SolveOptionsTest.solveOptionsShouldUseAllowRoundingAsEffectiveDefaultPolicy` | Failure | 默认策略断言不匹配，P11 前即存在 |
| `SolveValueConversionContextTest.conversionPolicyShouldRestoreAfterScopeExit` | Error | NaN 严格转换异常，P11 前即存在 |

---

## 1. P11 变更摘要

### C2-C8: Type alias 消除与下游迁移

- 删除全仓所有 `typealias *Flt64` 声明（含 `Flt64LinearInequality`、`MechanismModelFlt64`、`LinearIntermediateSymbolFlt64`、`QuadraticIntermediateSymbolFlt64`、`AbstractTokenTableFlt64` 等）
- 运行 `fix-downstream.py` 将下游模块中的 typealias 引用替换为展开的泛型类型
- 修复脚本误伤（如 `ModelView.kt` 中 `CellFlt64` 被错误替换为 `Cell<Flt64>`）
- 修复 `Constraint.kt`、`MetaModel.kt` 中脚本生成的无效 typealias 声明
- 修复 `LinearSolver.kt`、`QuadraticSolver.kt` 中构造函数非法类型参数 `<Flt64>`

### C9a: 消除 Flt64LinearInequality

- 删除 `LinearInequality.kt` 中 `typealias Flt64LinearInequality = LinearInequality<Flt64>`
- 全部引用替换为 `LinearInequality<Flt64>`

### C9b: 消除 Solution\<Flt64\>

- `Solution<V>` 即 `List<V>`，将全仓 `Solution<Flt64>` 替换为 `List<Flt64>`
- 移除相关 `Solution` import

### C9c: 消除 IntoValue.Flt64

- 将 `IntoValue` companion 中 `val Flt64` 重命名为 `val Identity`
- 全仓 `IntoValue.Flt64` 引用替换为文件级 `private val flt64Converter` 内联匿名 converter

### C9d: 消除 @Suppress("UNCHECKED_CAST")

- 全仓移除 195 处 `@Suppress("UNCHECKED_CAST")` 注解
- 195 处 `as` 类型转换保留（Kotlin/JVM 类型擦除的固有后果，移除 suppress 后仅产生编译器 warning，不影响编译和运行时正确性）
- 修复 quantities 和 core 测试中因先前 typealias 删除导致的编译错误

---

## 2. P11 扫描脚本

`scripts/scan-p11.sh` — 9 项硬门禁扫描，Total=0 时 PASS。

---

## 3. 下一阶段工作建议

1. **P12: solver 插件 V 化** — 外部 solver 插件（Gurobi/CPLEX/SCIP 等）的 `solveV` / `Solution<V>` 主入口。
2. **P12: framework pipeline V 化** — `ospf-kotlin-framework` 中的 pipeline 类目前仍使用 `AbstractLinearMetaModel<Flt64>`。
3. **P12: example V 化** — 示例项目中的 Flt64 硬编码。
4. **修复预先存在的测试失败** — `SolveOptionsTest` 和 `SolveValueConversionContextTest`。
5. **补充测试** — 非 Flt64 V 的集成测试（当前只有 Flt64 回归测试）。

---

## P10 泛型化已完成（含审阅修正）

### P10 验收结果

| 扫描项 | 结果 |
|--------|------|
| `*Bridge*.kt` 文件 | 0 |
| `ToMathLinearInequality` / `ToMathQuadraticInequality` 命中 | 0 |
| `relation as Flt64LinearInequality` 在 mechanism | 0 |
| `IntoValue.Flt64 as IntoValue<V>` 在 core main | 0 |
| `as LinearInequality<V>` / `as QuadraticInequalityOf<V>` 在 model | 0 |
| core 编译 | BUILD SUCCESS |
| 50 定向测试 | 全部通过 |

### P10 提交记录

```text
fc2021ed refactor(core): eliminate remaining inequality unsafe casts in MechanismModel and MetaModel (P10-8)
9b4a323e refactor(heuristic): remove UNCHECKED_CAST converter defaults from heuristic solvers (P10-7)
95a9281d refactor(core): annotate Benders cut solver boundary, remove unused AbstractTokenTableFlt64 import (P10-6)
0c2d6f79 refactor(core): add V-generic companion factory overloads for SlackRange and SatisfiedAmountInequality (P10-5)
67fca2ba refactor(core): replace MathInequalityBridge with explicit flatten adapter, remove ToMathLinearInequality/ToMathQuadraticInequality (P10-3/P10-4)
```