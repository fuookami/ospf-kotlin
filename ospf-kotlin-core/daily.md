# OSPF Kotlin Core Refactor Daily

记录日期：2026-05-03

本文是下一会话交接文档。当前代码处于大规模 dirty tree 状态，编译和部分测试通过，但不能判定 P10 泛型化完成。下一会话必须以本文为准继续收口，尤其不能接受“新增 solveV 包装层即可视为 solver 主链 V 化”的说法。

---

## 0. 结论

1. P10 报告中“Commit-5 到 Commit-12 全部完成”不成立。`git log` 当前只显示到 `1fc17b46 ... Commit-8`，Commit-9 到 Commit-12 没有真实提交记录。
2. 编译门禁大部分属实：core、framework、example 以及 example reactor 中的 heuristic plugin 均已复现 `BUILD SUCCESS`。
3. 测试门禁部分属实：已复现 8 个 core 测试类共 50 个测试通过。
4. 扫描门禁不属实：callback、function、mechanism 中仍有大量 Flt64 主路径或桥接路径。
5. 设计目标仍未满足：公开主链、callback 主链、core solver 主链、mechanism 主链仍存在 `Solution<Flt64>`、`AbstractTokenTableFlt64`、`Flt64LinearInequality`、`List<Flt64> as List<V>` 等问题。
6. 启发式业务解向量已有明显进展：`Individual<V>.solution`、PSO `solution/bestPosition` 已改为 `Solution<V>`，策略数值层 `Flt64` 可以保留。
7. 下一阶段目标不是继续添加兼容包装，而是把 Flt64 从 core 主链移到明确的 adapter/compat 边界。

---

## 1. 已复现结果

### 1.1 真实提交状态

最近提交：

```text
1fc17b46 refactor(core): add RealNumber & NumberField bounds to heuristic solver V parameters — Commit-8
9b9b5a16 refactor(core): genericize callback main chain with IntoValue<V> converter — Commit-7
54b83dab refactor(core): remove bridge typealiases from MathInequalityDsl, deduplicate against math layer — Commit-6
ff07b0ef refactor(core): eliminate pseudo-generic patterns in all function symbols — Commit-5
01d6df25 refactor(core): correct Slack/SlackRange/FunctionSymbol genericization — Commit-4
0003760f refactor(core): split MathFunctionSymbol.register into dual-phase registerAuxiliaryTokens + registerConstraints — Commit-3
6a570509 refactor(core): extend IntoValue<V> with zero/one/fromValue, deprecate pseudo-generic tools — Commit-1
```

结论：报告中的 Commit-9、Commit-10、Commit-11、Commit-12 当前不是可验收的真实提交。

### 1.2 编译门禁

已复现通过：

```powershell
mvn -pl ospf-kotlin-core -DskipTests compile
mvn -pl ospf-kotlin-framework -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

说明：`ospf-kotlin-example -am` 的 reactor 包含 `ospf-kotlin-core-plugin-heuristic`，该模块在本次验证中编译通过。

仍需注意：core 编译仍报告 `MetaModel.kt` 中 `AbstractMutableTokenTable<V>` 到 `MutableTokenTable<Flt64>` / `ConcurrentMutableTokenTable<Flt64>` 的 unchecked cast 警告。

### 1.3 测试门禁

已复现通过：

```powershell
mvn -pl ospf-kotlin-core "-Dtest=BendersCutApiTest,GenericTokenTableRegressionTest,SolverExtIISOptionsTest,SolverOutputWithIISTest,ProductFunctionTest,FunctionSymbolMigrationTest,ParticleSwarmHeuristicSolverTest,QuadraticMechanismModelCutTest" test
```

结果：8 个测试类，50 个测试通过。

### 1.4 扫描结果

当前扫描结果：

| 扫描项 | 命中数 | 判定 |
|---|---:|---|
| `import ... as ...` | 0 | 通过 |
| `evaluateFlt64/evaluateAsFlt64/constantFlt64` | 0 | 通过 |
| `Flt64.zero as V/Flt64.one as V/Flt64(...) as V/this as Flt64` | 1 | 仅注释命中 |
| callback 中 `Solution<Flt64>/List<Flt64>` | 6 | 不通过 |
| heuristic 中业务 `Solution<Flt64>/as List<Flt64>/UNCHECKED_CAST` | 0 | 通过，策略 Flt64 另行白名单 |
| function 目录结构性 Flt64 命中 | 127 | 不通过 |
| mechanism 目录结构性 Flt64 命中 | 202 | 不通过 |
| `*Bridge*.kt` | 1 | 不通过，存在 `MathInequalityBridge.kt` |

---

## 2. 当前主要问题

### 2.1 dirty tree 不能当作完成提交

当前工作区有大量未提交文件，涉及 core、plugin、framework、example、test 和 `daily.md`。下一会话必须先做基线确认，不得把 dirty tree 中的改动描述为已完成 commit。

需要执行：

```powershell
git status --short
git log --oneline -n 30
git diff --stat
```

验收要求：

1. 每个“完成的 Commit”必须能在 `git log` 中找到对应提交。
2. 未提交工作只能标记为 dirty tree/WIP，不得标为 done。
3. 每个提交必须有独立验收记录。

### 2.2 callback 仍有 Flt64 主链残留

重点文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModel.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback/CallBackModelInterface.kt`

已确认问题：

1. `CallBackModelPolicy.initialSolutions(...)` 仍返回 `List<Solution<Flt64>>`。
2. `DumpCallBackModelPolicy.initialSolutions(...)` 仍返回 `List<Solution<Flt64>>`。
3. dump 构造路径中仍有 `{ solution: Solution<Flt64> -> ... }` lambda。
4. dump 构造路径中仍有 `(constraint as ConstraintImpl<Flt64, *>)`。
5. `initialSolutionsGenerator` 仍固定返回 `Flt64`。

设计要求：

1. callback 主接口必须使用 `Solution<V>`。
2. Flt64 initial solution generator 只能是 compat/adapter 策略，不得作为泛型主策略。
3. dump 到 callback 时不应把 `ConstraintImpl<V, *>` 降级为 `ConstraintImpl<Flt64, *>`。
4. 需要为 Flt64 旧用法提供显式兼容层或命名清晰的 adapter，不要混在主实现里。

验收标准：

1. `model/callback` 中业务 `Solution<Flt64>` 命中为 0。
2. `model/callback` 中业务 lambda 的 `List<Flt64>` 命中为 0。
3. `ConstraintImpl<Flt64, *>` 强转不出现在泛型 callback 主链。
4. callback 测试覆盖 `Solution<V>` 初始解、objective、constraint 检查。

### 2.3 core solver 只是新增 solveV 包装，不是主链 V 化

重点文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/LinearSolver.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/QuadraticSolver.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/output/SolverOutput.kt`
4. `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/*.kt`
5. `ospf-kotlin-core-plugin-*` 外部 solver adapter

已确认问题：

1. `AbstractLinearSolver.invoke(...)` 仍返回 `Ret<FeasibleSolverOutputFlt64>`。
2. solution pool 接口仍返回 `List<Solution<Flt64>>`。
3. `solveV<V>()` 只是调用 Flt64 接口后 `convertTo(converter)`。
4. `LinearMechanismModelFlt64` / `QuadraticMechanismModelFlt64` 仍是 solver 主输入路径。

设计要求：

1. core solver 主接口应返回 `FeasibleSolverOutput<V>` 或等价泛型输出。
2. 外部 solver plugin 可以内部使用 Flt64/double，但返回 core 前必须转换为 `V`。
3. 如果保留旧 `invoke`，它必须被明确标为 Flt64 compat/adapter，不得作为主接口。
4. solution pool 的主返回类型必须是 `List<Solution<V>>`。

验收标准：

1. core solver 主接口不以 `FeasibleSolverOutputFlt64` 为唯一返回类型。
2. core solver 主接口不返回业务 `List<Solution<Flt64>>`。
3. `solveV` 不只是薄包装旧 Flt64 主接口，而是主接口或由主接口自然实现。
4. plugin 的 Flt64 转换集中在 adapter 文件并有边界说明。

### 2.4 mechanism 仍有 Flt64 tokenTable/inequality 主路径

重点文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MechanismModel.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaModel.kt`
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/Constraint.kt`
4. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/LinearConstraintInput.kt`
5. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MetaConstraint.kt`
6. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityDsl.kt`

已确认问题：

1. `MechanismModel.kt` 仍使用 `AbstractTokenTableFlt64`。
2. `MechanismModel.kt` 仍使用 `Flt64LinearInequality`。
3. `dualValues` / `farkasDualValues` 仍接收 `Solution<Flt64>`。
4. `LinearConstraintInput.isTrue` 仍接收 `Solution<Flt64>`。
5. `MetaModel.setSolverSolution` 仍接收 `Solution<Flt64>`。
6. `MetaModel` 默认 converter 仍有 `IntoValue.Flt64 as IntoValue<V>`。
7. `MetaModel.flt64Tokens` 仍把 `tokens` 强转为 `AbstractMutableTokenTableFlt64`。

设计要求：

1. `MechanismModel<V>` 主链持有 `AbstractTokenTable<V>`。
2. `Constraint<V, *>`、`LinearConstraintInput<V>`、`MetaConstraint<V>` 的主求值接收 `Solution<V>`。
3. `Flt64LinearInequality` 只能在 Flt64 compat/adapter 中出现。
4. `IntoValue.Flt64 as IntoValue<V>` 不能作为泛型默认值。
5. `setSolverSolution` 若保留 Flt64 输入，必须命名为 adapter/compat 并转换后进入 V tokenTable。

验收标准：

1. `model/mechanism` 主路径中 `AbstractTokenTableFlt64` 命中为 0。
2. `model/mechanism` 主路径中 `Solution<Flt64>` 命中为 0。
3. `model/mechanism` 主路径中 `Flt64LinearInequality` 命中为 0，compat/adapter 例外需独立文件或清晰命名。
4. `IntoValue.Flt64 as IntoValue<V>` 命中为 0。
5. `MetaModel.kt` 编译不再产生 tokenTable Flt64 unchecked cast 警告。

### 2.5 TokenTable 存在类型安全风险

重点文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenTable.kt`
2. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/Token.kt`

已确认问题：

1. `cachedSolver(cacheKey, solution: List<Flt64>?)` 把 `solution as List<V>?`。
2. `cachedSolver(fixedValues: Map<Symbol, Flt64>)` 把 `fixedValues as Map<Symbol, V>`。
3. `cachedSolverValue`、`cacheSolver`、`cacheSolverIfNotCached` 存在同类强转。
4. 多处将 `IntermediateSymbol<*>` 强转为 `IntermediateSymbolFlt64`。
5. `setSolverSolution(solution: List<Flt64>)` 仍在 tokenTable 主接口上。

设计要求：

1. token cache 主链按 `V` 存储和查询。
2. Flt64 solver 解写入 tokenTable 之前必须通过 converter 转为 `V`。
3. `cachedSolver*` 若保留，应移动到 adapter/compat 或改名明确其边界。
4. 不允许 `List<Flt64> as List<V>`。

验收标准：

1. `TokenTable.kt` 中 `as List<V>` 命中为 0。
2. `TokenTable.kt` 中 `Map<Symbol, Flt64> as Map<Symbol, V>` 命中为 0。
3. `TokenTable.kt` 中 solver cache 主方法以 `Solution<V>` 或 converter 参数为输入。
4. TokenTable 相关泛型回归测试通过。

### 2.6 function 目录仍未全量泛型化

重点文件：

1. `SameAs.kt`
2. `SatisfiedAmount.kt`
3. `SatisfiedAmountInequality.kt`
4. `Slack.kt`
5. `SlackRange.kt`
6. `Product.kt`
7. `Masking.kt`
8. `Max.kt`
9. `MinMax.kt`
10. `BigM.kt`
11. 其他扫描命中的 function 文件

已确认问题：

1. function 目录结构性 Flt64 命中 127。
2. `SameAs.kt` 仍构造 `MutableList<LinearInequality<Flt64>>`。
3. `SatisfiedAmount.kt` 仍有 `List<LinearInequality<Flt64>>`。
4. `SatisfiedAmountInequality.kt` 仍构造 `LinearInequality<Flt64>`。
5. `Slack.kt` / `SlackRange.kt` 的约束生成仍构造 `LinearInequality<Flt64>`。
6. `Product.kt` 主属性已改为 `LinearPolynomial<V>`，但内部仍用 `leftFlt64/rightFlt64`、`AbstractTokenTableFlt64` 强转、`MutableQuadraticPolynomial(emptyList(), Flt64.zero) as MutableQuadraticPolynomial<V>`。
7. 多个文件仍有 Flt64 overload，看起来不全是薄委托，需要逐项确认。

设计要求：

1. function 主构造、主属性、主工厂使用 `V`。
2. Flt64 overload 只能薄委托到泛型主入口。
3. 约束生成若是 core 主逻辑，应生成 `LinearInequality<V>` / `QuadraticInequality<V>` 或等价泛型结构。
4. 如果底层机制暂时只支持 Flt64 inequality，必须先完成 mechanism 泛型化，不能在 function 中继续硬编码。
5. `Product.kt` 的 Flt64 solver view 必须被明确隔离为 adapter 行为，不能污染 `polynomial/asMutable/evaluate` 主实现。

验收标准：

1. function 目录中 `LinearPolynomial<Flt64>` 不出现在泛型类主属性、主构造、主逻辑中。
2. function 目录中 `LinearInequality<Flt64>` 不出现在泛型主逻辑中。
3. function 目录中 `LinearIntermediateSymbolFlt64` 只出现在明确 Flt64 compat overload。
4. function 目录中用于 V/Flt64 强转的 `UNCHECKED_CAST` 命中为 0。
5. `ProductFunction<V>.polynomial` 不通过 `QuadraticPolynomial<Flt64> as QuadraticPolynomial<V>` 实现。

### 2.7 MathInequalityBridge 仍存在

重点文件：

1. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism/MathInequalityBridge.kt`

已确认问题：

1. 文件名仍是 bridge。
2. `ToMathLinearInequality` 明确返回 `Flt64LinearInequality`。
3. `ToMathQuadraticInequality` 明确返回 `QuadraticInequality`，其多项式为 Flt64。
4. `flattenData` 输出 `LinearFlattenDataFlt64` / `QuadraticFlattenDataFlt64`。

设计要求：

1. 若这是 compat/adapter，必须移动到明确的 compat/adapter 命名空间或文件名。
2. 若这是 core 主接口，必须泛型化。
3. 不允许在最终验收里声明 Bridge 类型为 0。

验收标准：

1. `*Bridge*.kt` 文件数为 0，或只存在明确豁免并在 daily.md 中列明理由。
2. `ToMathLinearInequality` 不再以 Flt64-only 作为 core 主接口。
3. flatten 数据类型泛型化或移动到 solver adapter 边界。

---

## 3. 已完成或基本属实的部分

1. `Solution<V> = List<V>` 已存在。
2. `Individual<V>.solution` 已是 `Solution<V>`。
3. PSO 中 `Particle.solution`、`bestPosition` 已是 `Solution<V>`。
4. heuristic 主链扫描中未发现业务 `Solution<Flt64>`、`as List<Flt64>`、用于业务解的 `UNCHECKED_CAST`。
5. `ObjectiveNormalization<V>.invoke` 输入为 `List<V>`，输出为 `List<Flt64>`，符合策略数值层输出权重的方向。
6. `Policy.coerceIn` 已有 V 泛型重载并使用 converter。
7. `FeasibleSolverOutput<V>` 已存在，`FeasibleSolverOutputFlt64.convertTo(converter)` 已存在。
8. `solveV<V>()` 已存在，但只能视为过渡包装，不算主链完成。
9. `IntoValue.Flt64` 使用全限定名访问 `Flt64.zero`，避免 companion 属性名遮蔽递归，方向正确。
10. `import ... as ...` 为 0。
11. `evaluateFlt64/evaluateAsFlt64/constantFlt64` 为 0。
12. 编译和定向测试通过。

---

## 4. 提交级改进计划

## Commit-A：基线冻结与提交状态修正

### 目标

把当前 dirty tree 和真实提交状态整理清楚，防止继续误报完成状态。

### 操作

1. 执行 `git status --short`、`git log --oneline -n 30`、`git diff --stat`。
2. 将当前 dirty 文件按模块分组：core、plugin、framework、example、test、docs。
3. 决定是否先提交当前已通过编译/测试的 WIP，或继续在 dirty tree 中修到验收后再提交。
4. 任何提交必须命名与 daily.md 的 Commit-A/B/C 对齐。

### 验收标准

1. daily.md 中的提交状态与 `git log` 一致。
2. 不再出现“Commit-12 已完成但 git log 没有提交”的情况。
3. 每个提交前后都有扫描结果记录。

---

## Commit-B：callback Flt64 残留清理

### 目标

让 callback 主链彻底以 `Solution<V>` 为输入输出。

### 需改文件

1. `CallBackModel.kt`
2. `CallBackModelInterface.kt`
3. 受影响的 `MetaModel.dump` / `MechanismModel.dump` / framework callback 调用方

### 计划

1. 将 `CallBackModelPolicy.initialSolutions` 改为泛型输出 `List<Solution<V>>`。
2. 将 `initialSolutionsGenerator` 从 `Extractor<Flt64, Pair<UInt64, UInt64>>` 改为可生成 `V`，或改为 Flt64 compat policy。
3. dump callback 构造时保留 `ConstraintImpl<V, *>`，不要强转 `ConstraintImpl<Flt64, *>`。
4. objective dump lambda 全部改为 `Solution<V>`。
5. Flt64 callback 旧入口保留为 `CallBackModelFlt64` 或 companion compat 工厂，内部薄委托。

### 验收标准

1. `Get-ChildItem -Path ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback -Recurse -Include *.kt | Select-String -Pattern 'Solution<Flt64>|List<Flt64>|ConstraintImpl<Flt64|Extractor<.*Solution<Flt64>'` 命中为 0，compat 文件例外需明确说明。
2. callback 测试通过。
3. heuristic 使用 callback 时不需要 Flt64 解向量。

---

## Commit-C：TokenTable 与 MetaModel 类型安全收口

### 目标

移除 `List<Flt64> as List<V>`、`Map<Symbol, Flt64> as Map<Symbol, V>`、`IntoValue.Flt64 as IntoValue<V>` 等主链类型风险。

### 需改文件

1. `TokenTable.kt`
2. `Token.kt`
3. `MetaModel.kt`
4. `Cell.kt`
5. `SubObject.kt`

### 计划

1. `cachedSolver*` 改为接收 `Solution<V>` 或显式 converter。
2. Flt64 solver cache 方法迁移到 adapter/compat 命名区。
3. `setSolverSolution(List<Flt64>)` 改为 adapter 方法，core 主入口使用 `setSolution(Solution<V>)`。
4. 删除 `IntoValue.Flt64 as IntoValue<V>` 默认值，泛型模型必须显式传入 converter。
5. 移除 `flt64Tokens` 强转主路径。
6. 为 Flt64 旧模型提供具体 typealias/工厂，不能让泛型默认值假装 Flt64。

### 验收标准

1. `TokenTable.kt` 中 `as List<V>` 命中为 0。
2. `TokenTable.kt` 中 `Map<Symbol, Flt64> as Map<Symbol, V>` 命中为 0。
3. `MetaModel.kt` 中 `IntoValue.Flt64 as IntoValue<V>` 命中为 0。
4. core 编译不再出现 `MetaModel.kt` tokenTable Flt64 unchecked cast 警告。

---

## Commit-D：mechanism inequality/tokenTable V 化

### 目标

让 mechanism 主链不再以 Flt64 token table 和 Flt64 inequality 为基础。

### 需改文件

1. `MechanismModel.kt`
2. `Constraint.kt`
3. `LinearConstraintInput.kt`
4. `MetaConstraint.kt`
5. `MathInequalityDsl.kt`
6. `MathInequalityBridge.kt`

### 计划

1. 泛型化 `LinearConstraintInput.isTrue` 的 solution 参数。
2. 泛型化 `ConstraintImpl` 内部 relation 和 tokenTable 访问。
3. 将 `Flt64LinearInequality` 主路径替换为 `LinearInequality<V>`。
4. `dualValues` / `farkasDualValues` 若来自外部 solver，应定义为 adapter 结果或转换为 `V`。
5. 将 `MathInequalityBridge.kt` 删除、重命名到 compat/adapter，或泛型化。
6. `MechanismModel<V>` 内部持有 `AbstractTokenTable<V>`，不把它降级为 `AbstractTokenTableFlt64`。

### 验收标准

1. `model/mechanism` 主路径中 `AbstractTokenTableFlt64` 命中为 0。
2. `model/mechanism` 主路径中 `Flt64LinearInequality` 命中为 0。
3. `model/mechanism` 主路径中 `Solution<Flt64>` 命中为 0。
4. `*Bridge*.kt` 文件数为 0，或唯一保留项有明确 adapter 豁免说明。
5. register 双阶段测试仍通过。

---

## Commit-E：core solver 主接口 V 化

### 目标

把 `solveV` 从过渡包装变成主接口，旧 Flt64 solve 明确降为 compat/adapter。

### 需改文件

1. `LinearSolver.kt`
2. `QuadraticSolver.kt`
3. `SolverOutput.kt`
4. `SolverExt.kt`
5. `ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/*.kt`
6. `ospf-kotlin-core-plugin-*` 外部 solver adapter

### 计划

1. core solver interface 增加或切换为 `solve<V>(model, converter): Ret<FeasibleSolverOutput<V>>`。
2. solution pool 主接口返回 `List<Solution<V>>`。
3. Flt64 旧接口改名或下沉到 `Flt64SolverAdapter` 风格接口。
4. plugin 内部仍可使用 Flt64/double，但不得把 `Solution<Flt64>` 暴露回 core 主链。
5. framework solver wrapper 跟随泛型输出。

### 验收标准

1. core solver 主接口不再以 `FeasibleSolverOutputFlt64` 为唯一输出。
2. core solver 主接口不再返回业务 `List<Solution<Flt64>>`。
3. `solveV` 不只是调用 Flt64 `invoke` 后转换。
4. core/framework/example/plugin 编译通过。

---

## Commit-F：function 目录结构性 Flt64 清理

### 目标

清理 function 目录中剩余 127 个结构性 Flt64 命中，区分真正 compat overload 与违规主逻辑。

### 需优先改文件

1. `SameAs.kt`
2. `SatisfiedAmount.kt`
3. `SatisfiedAmountInequality.kt`
4. `Slack.kt`
5. `SlackRange.kt`
6. `Product.kt`
7. `Masking.kt`
8. `Max.kt`
9. `MinMax.kt`
10. `BigM.kt`

### 计划

1. 为每个 function 文件增加审计记录：主构造、主属性、主工厂、Flt64 overload、register、evaluate。
2. `SameAs` / `SatisfiedAmount` / `SatisfiedAmountInequality` 的主结构改为 `LinearInequality<V>`。
3. `Slack` / `SlackRange` 的约束生成跟随 mechanism 泛型 inequality。
4. `Product` 删除 `QuadraticPolynomial<Flt64> as QuadraticPolynomial<V>`、`MutableQuadraticPolynomial(... Flt64.zero) as MutableQuadraticPolynomial<V>`。
5. `Product` 中 Flt64 solver view 只留在 adapter 边界，不参与泛型 `polynomial/asMutable/evaluate` 主实现。
6. Flt64 overload 必须是薄委托，不能复制约束生成逻辑。

### 验收标准

1. function 目录中违规 `LinearPolynomial<Flt64>` 命中为 0。
2. function 目录中违规 `LinearInequality<Flt64>` 命中为 0。
3. function 目录中违规 `LinearIntermediateSymbolFlt64` 命中为 0。
4. function 目录中用于主链的 `UNCHECKED_CAST` 命中为 0。
5. `ProductFunctionTest`、`FunctionSymbolMigrationTest` 通过。

---

## Commit-G：桥接和 DSL 最终收口

### 目标

删除或隔离桥接层，避免旧 Flt64 DSL 继续作为 core 主入口。

### 需改文件

1. `MathInequalityBridge.kt`
2. `MathInequalityDsl.kt`
3. framework/example 中依赖旧 Flt64 DSL 的调用方

### 计划

1. 删除 `MathInequalityBridge.kt`，或移动到明确 compat/adapter 包并从主扫描中白名单化。
2. `ToMathLinearInequality` 泛型化或只作为 Flt64 compat 接口。
3. `flattenData` 泛型化或移到 solver adapter。
4. DSL 主入口返回泛型 inequality。
5. Flt64 DSL overload 保留为薄委托。

### 验收标准

1. `Get-ChildItem -Path ospf-kotlin-core/src/main -Recurse -Include '*Bridge*.kt'` 返回空，或仅返回明确白名单文件。
2. `MathInequalityDsl.kt` 中桥接 typealias 为 0。
3. `import ... as ...` 全仓为 0。
4. framework/example 编译通过。

---

## Commit-H：测试补齐

### 目标

把目前“能编译”升级为“行为被测试锁住”。

### 必测项

1. `CallBackModel<V>` 的 `initialSolutions/objective/constraintSatisfied` 使用 `Solution<V>`。
2. `TokenTable` 不通过 `List<Flt64> as List<V>` 缓存或设置解。
3. `MechanismModel<V>` 构建后 tokenTable 不降级为 Flt64 主路径。
4. `MathFunctionSymbol.register` helper variables 注册到 tokenTable 拷贝，constraints 注册到 MechanismModel。
5. `SameAs/SatisfiedAmount/Slack/Product` 泛型构造和泛型 evaluate。
6. solver adapter 把外部 Flt64 解转换为 `Solution<V>` 后才进入 core。
7. heuristic 保持 `Solution<V>`，策略权重/温度/概率保持 `Flt64`。

### 验收标准

1. core 新增/修改测试通过。
2. 至少一个测试使用非 Flt64 的 `V` 做编译或运行样例。
3. 已有 50 个定向测试继续通过。

---

## Commit-I：最终扫描、编译、提交整理

### 目标

完成最后门禁，确保代码、提交和 daily.md 一致。

### 必跑编译

```powershell
mvn -pl ospf-kotlin-core -DskipTests compile
mvn -pl ospf-kotlin-framework -am -DskipTests compile
mvn -pl ospf-kotlin-example -am -DskipTests compile
```

### 必跑测试

```powershell
mvn -pl ospf-kotlin-core "-Dtest=BendersCutApiTest,GenericTokenTableRegressionTest,SolverExtIISOptionsTest,SolverOutputWithIISTest,ProductFunctionTest,FunctionSymbolMigrationTest,ParticleSwarmHeuristicSolverTest,QuadraticMechanismModelCutTest" test
```

### 必跑扫描

```powershell
Get-ChildItem -Path . -Recurse -Include *.kt | Select-String -Pattern '^import .+ as '
Get-ChildItem -Path ospf-kotlin-core/src/main -Recurse -Include *.kt | Select-String -Pattern 'evaluateFlt64|evaluateAsFlt64|constantFlt64'
Get-ChildItem -Path ospf-kotlin-core/src/main -Recurse -Include *.kt | Select-String -Pattern 'Flt64\.zero as V|Flt64\.one as V|Flt64\([^)]*\) as V|this as Flt64|IntoValue\.Flt64 as IntoValue<V>'
Get-ChildItem -Path ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/callback -Recurse -Include *.kt | Select-String -Pattern 'Solution<Flt64>|List<Flt64>|ConstraintImpl<Flt64|Extractor<.*Solution<Flt64>'
Get-ChildItem -Path ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/solver/heuristic -Recurse -Include *.kt | Select-String -Pattern 'Solution<Flt64>|as List<Flt64>|UNCHECKED_CAST'
Get-ChildItem -Path ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_symbol/function -Recurse -Include *.kt | Select-String -Pattern 'LinearPolynomial<Flt64>|LinearInequality<Flt64>|LinearIntermediateSymbolFlt64|as List<Flt64>|UNCHECKED_CAST'
Get-ChildItem -Path ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/model/mechanism -Recurse -Include *.kt | Select-String -Pattern 'AbstractTokenTableFlt64|Solution<Flt64>|Flt64LinearInequality|sym\.register\(metaModel\)|MathFunctionSymbol<Flt64>|UNCHECKED_CAST'
Get-ChildItem -Path ospf-kotlin-core/src/main -Recurse -Include '*Bridge*.kt'
```

### 验收标准

1. 编译全部 `BUILD SUCCESS`。
2. 测试全部通过。
3. 非白名单扫描命中为 0。
4. heuristic 策略数值层 Flt64 命中有白名单说明。
5. plugin adapter Flt64 命中有边界说明。
6. `git log` 中存在与 Commit-A 到 Commit-I 对应的真实提交，或 daily.md 明确记录仍未提交。

---

## 5. 最终验收口径

1. 公开主链 V 化：对外 API、主构造、主属性、主工厂、evaluate/invoke 使用 `V`。
2. solver 主链 V 化：core solver 主接口、callback、mechanism、tokenTable 使用 `Solution<V>`。
3. heuristic 业务链 V 化：business solution/objective/constraint 使用 `V`。
4. 策略数值层例外：heuristic 的权重、概率、温度、变异率、selection weight、随机数可以是 `Flt64`。
5. 外部 adapter 例外：外部 solver plugin 可以用 Flt64/double，但不得泄漏到 core 主接口。
6. 无桥接回流：`Bridge` 文件、桥接 typealias、旧路径转发壳为 0，或全部明确迁移到 compat/adapter 并从主链断开。
7. 无伪泛型：`Flt64 as V`、`IntoValue.Flt64 as IntoValue<V>`、`List<Flt64> as List<V>`、`Map<Symbol, Flt64> as Map<Symbol, V>` 为 0。
8. register 生命周期正确：helper variables 注册到 tokenTable 拷贝，extra constraints 注册到 MechanismModel，原始 MetaModel 不被污染。
9. 文档与代码一致：daily.md 不能写“扫描全绿”但扫描仍有 callback/function/mechanism 命中。

---

## 6. 不要再接受的说法

1. 不要接受“新增 `solveV` 就等于 solver 主链 V 化”。如果旧 Flt64 `invoke` 仍是主接口，目标未完成。
2. 不要接受“function 目录 Flt64 都是 companion 工厂”。必须逐项区分主逻辑、compat overload、adapter 边界。
3. 不要接受“Bridge 类型为 0”。当前仍存在 `MathInequalityBridge.kt`，除非删除或明确迁移。
4. 不要接受“callback/heuristic 主链无 `Solution<Flt64>`”。callback 当前仍有命中。
5. 不要接受“编译通过即完成”。当前主要问题是类型边界和设计主链，而不是编译。
6. 不要接受“dirty tree 等于已提交 commit”。Commit-9 到 Commit-12 必须真实提交或重新命名为 WIP。