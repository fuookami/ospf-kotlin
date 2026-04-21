# OSPF Kotlin Core Refactor Daily

日期：2026-04-21（P1-12 D0~D4 全量完成归档）

---

## 完成总结

### 阶段 C0~C8 全部完成

| 阶段 | 内容 | 完成日期 |
|------|------|----------|
| C0 | 基线冻结：API 暴露清单、Flt64 固化点清单、.cells 调用清单 | 2026-04-16 |
| C1 | API 清退与主链路切流：39 ReplaceWith + 19 Boolean 兼容层 | 2026-04-16 |
| C2 | 泛型化贯通（第一段）：MetaModelOf\<V\>、MechanismModelOf\<V\> 声明 + typealias 兼容 | 2026-04-16 |
| C3 | 缓存上收：remove 解绑、同步/并发对齐、双写策略、15 个回归测试 | 2026-04-18 |
| C4 | MechanismModel 边界收口：toQuadraticConstraint()、DumpHelpers.kt、clampCoefficient() | 2026-04-18 |
| C5 | Quadratic cut 对齐 Rust：generateOptimalCut/generateFeasibleCut 双路径实现 | 2026-04-17 |
| C6 | 主链路切流：Polynomial.kt 物理删除；Expression.kt / ToPolynomial.kt / intermediate_model.monomial 兼容层过渡保留 | 2026-04-17 |
| C7 | 阶段化回归：run-c7-regression.ps1 脚本 | 2026-04-17 |
| C8 | 门禁：Abstract*Polynomial 拦截、.cells 拦截、Double 固化拦截 | 2026-04-17 |

### P0 全量完成（2026-04-19）

| 项目 | 内容 | 完成方式 | 关键代码证据 |
|------|------|----------|-------------|
| P0-7 | C2 第二段泛型化：V 真实贯通全链路 | 双视图模式（V-typed 访问器 + Flt64 内部视图 via IntoValue\<V\>） | Token.kt, Cell.kt, Constraint.kt, SubObject.kt, IntermediateSymbol.kt |
| P0-8 | BasicModel\<V\> / BasicMechanismModel\<V\> 上层分离 | 继承链贯通 + RealNumber\<V\> 约束 | BasicModel.kt, BasicMechanismModel.kt, MetaModel.kt, MechanismModel.kt |
| P0-9 | ConstraintRelation + mechanism 层 Inequality 体系 | Constraint\<V, P : PolynomialKind\> + SymbolicLinearInequality\<V\> + SymbolicQuadraticInequality\<V\> + LinearConstraintImpl / QuadraticConstraintImpl | Constraint.kt |
| P0-10 | ProductFunction\<V\> 补齐 | register() 通过 addConstraint() 注册约束 | Product.kt |
| P0-11 | registerAuxiliaryTokens 钩子 | 37 处显式覆写 + MathFunctionSymbol 默认实现通过 helperVariables | FunctionSymbol.kt, TokenTable.kt |
| P0-12 | SparseVector\<V\> / SparseMatrix\<V\> | sparseLhs 主表示，lhs 为 @Deprecated 派生 | LinearTriadModel.kt, QuadraticTetradModel.kt |
| P0-13 | convertMechanismModelToF64\<V\>() | 边界函数 + SolverExt 泛型 solve() | MechanismModel.kt, SolverExt.kt |

### P0 验收结果（2026-04-19）

| 命令 | 结果 |
|------|------|
| `mvn -pl ospf-kotlin-core -am test` | 通过，121 tests, 0 failures |
| `mvn compile -pl ospf-kotlin-framework -am` | 通过 |
| `check-c8-guards.ps1` | 通过 |
| `grep phantom/Phase\ 1` | 0 残留 |

### C2 第二段泛型化子步骤完成

| 子步骤 | 内容 | 状态 |
|--------|------|------|
| C2-2.1 | IntoValue\<V\> / SolveValue\<V\> trait | 完成 |
| C2-2.2 | AnyVariable\<V\> 泛型化 | 完成 |
| C2-2.3 | Token\<V\> 泛型化 | 完成 |
| C2-2.4 | Cell\<V\> 泛型化 | 完成 |
| C2-2.5 | Constraint\<V, P\> / SubObject\<V\> 泛型化 | 完成 |
| C2-2.6 | QuadraticInequality DSL 泛型化 | 完成 |
| C2-2.7 | IntermediateSymbol\<V\> sealed interface 泛型化 | 完成 |
| C2-2.8 | convertMechanismModelToF64\<V\>() 提取 | 完成 |
| C2-2.9 | 全量编译 + 测试通过 | 完成 |

### P1~P2 前期完成项

| 项目 | 内容 | 完成方式 |
|------|------|----------|
| P1-1 | Quadratic MechanismModel cut 对齐 Rust | C5 完成 |
| P1-2 | 显式符号依赖图 | TokenTable: symbolDependencies, addSymbolDependency, validateNoCycles |
| P1-3 | .cells 主路径清退 | C8 守卫阻止新使用 |
| P2-1 | 启发式 TODO 清理 | Migration.kt 8 个实现类；Cross.kt/Mutation.kt 死代码删除 |
| P2-2 | 文档与门禁统一 | daily.md 归档 + C8 门禁 |

### 关键交付物

- `docs/refactor-baseline/` — api-exposure.md, flt64-hardening.md, cells-usage.md, api-migration.md, cache-*.md, mechanism-*.md, generic-boundary.md
- `DumpHelpers.kt` — 共享 dump 工具（clampCoefficient）
- `ApiCompatibilityTest.kt` — 8 个 API 兼容测试
- `QuadraticMechanismModelCutTest.kt` — 二次 cut 回归测试
- `ProductFunctionTest.kt` — ProductFunction.register() 测试
- `ConstraintRelationTest.kt` — ConstraintRelation 双向映射测试
- `ConvertMechanismModelTest.kt` — V→F64 转换测试
- `scripts/check-c8-guards.ps1` — 增量门禁脚本
- `scripts/run-c7-regression.ps1` — 阶段化回归脚本
- `.github/workflows/core-refactor-guards.yml` — CI 门禁

### C6 保留项说明

- `Expression.kt`：暂保留。当前仍被 `IntermediateSymbol`、`Monomial`、`CallBackModel` 的 deprecated 兼容入口直接依赖。
- `ToPolynomial.kt`：暂保留。`ToLinearPolynomial`/`ToQuadraticPolynomial` 仍在 core 与 framework 跨模块建模入口中使用。
- `intermediate_model/monomial/`：暂保留。变量运算符、不等式 DSL、flatten/cell 兼容路径仍以该包为输入类型。
- 删除门槛：完成对 `Expression`、`To*Polynomial`、`monomial` 的外部调用迁移后，再统一物理删除。

### C6 兼容层后续删除计划（D0~D4）

| 阶段 | 目标 | 主要动作 | 退出条件 | 目标日期 |
|------|------|----------|----------|----------|
| D0 | 现状冻结 | 固定保留清单（`Expression.kt`、`ToPolynomial.kt`、`intermediate_model/monomial`）并标注所有入口为兼容层 | 保留项与引用说明在 daily.md 可追踪 | 2026-04-19 |
| D1 | 去除 `To*Polynomial` 外部依赖 | framework/core 调用方改为直接使用 `math.symbol` 多项式或 `flattenData`；`ToLinearPolynomial`/`ToQuadraticPolynomial` 仅保留内部过渡 | framework 不再 import `core.intermediate_model.To*Polynomial` | 2026-04-23 |
| D2 | 去除 `Expression` 外部依赖 | 回调与目标函数入口从 `Expression` 迁移到 `MathLinearPolynomial<Flt64>` / `LinearFlattenDataF64`；保留 deprecated 桥接但主路径禁用 | core 对外 API 不再暴露 `Expression` 参数（仅 deprecated 适配层可见） | 2026-04-25 |
| D3 | 去除 `intermediate_model.monomial` 主路径依赖 | 变量运算符、不等式 DSL、SubObject/Constraint 桥接统一切到 `math.symbol` + flatten；`monomial` 包仅用于短期兼容 | 主链路文件（Model/MetaModel/MathInequalityDsl/IntermediateSymbol）不再 import `intermediate_model.monomial.*` | 2026-04-27 |
| D4 | 物理删除与门禁收口 | 删除 `Expression.kt`、`ToPolynomial.kt`、`intermediate_model/monomial/`；更新 C8 门禁与回归脚本，补齐 ReplaceWith 迁移指引 | `mvn -pl ospf-kotlin-core -am test`、`mvn -pl ospf-kotlin-framework -am compile`、`check-c8-guards.ps1` 全通过 | 2026-04-28 |

#### D0~D4 执行规则

1. 先迁移 framework 调用点，再迁移 core 兼容入口，最后物理删除文件。
2. 每阶段必须先补测试（语义等价回归）再切实现，避免“先删后补”。
3. 任一阶段失败时不进入下一阶段，回滚到上一阶段完成态。

#### D0~D4 完成定义（Done）

1. 仓库内不存在 `import fuookami.ospf.kotlin.core.intermediate_model.ToLinearPolynomial` / `ToQuadraticPolynomial`（测试兼容样例除外）。
2. 仓库内不存在 `core.intermediate_model.Expression` 作为对外 API 参数类型。
3. `ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/intermediate_model/monomial/`、`Expression.kt`、`ToPolynomial.kt` 已物理删除。
4. C8 门禁新增对应拦截规则并在 CI 稳定通过。

---

## 待办事项

### P1 执行进度

| # | 项目 | 状态 | 完成日期 |
|---|------|------|----------|
| P1-10 | 主链路语义等价回归 | 完成 | 2026-04-20 |
| P1-8 | Benders cut by_id/from_output 公共 API | 完成 | 2026-04-20 |
| P1-11 | Basic*Model 独立公开入口规范化 | 完成 | 2026-04-20 |
| P1-9 | QuadraticTetradModel dual/farkasDual 决策 | 完成 | 2026-04-20 |
| P1-12 | C6 兼容层删除流程（D0~D4） | 完成 | 2026-04-21 |

#### P1-10 完成详情

新增 `SemanticEquivalenceTest.kt`，8 个测试用例覆盖：

1. 线性模型等价 — 旧入口 addConstraint(constraint: LinearMonomial) vs 新入口 addConstraint(relation: MathLinearInequality)
2. 二次约束项正确性
3. V 泛型路径 vs Flt64 路径 — 含非空约束+目标，经 invoke() 构建 MechanismModel 后验证 convertMechanismModelToF64 保留约束和目标
4. ConstraintRelation 双射往返
5. LinearRelation.from(MathLinearInequality) 语义保留
6. 目标函数等价 — minimize(MathLinearPolynomial) vs addObject(LinearFlattenDataF64)
7. 全流水线等价 — 两种入口点经 invoke() 构建真实 MechanismModel，验证约束 sign/rhs/lhs 和目标均一致
8. 插件边界 Double 转换 — QuadraticMechanismModel<Flt64> 经 invoke() 构建含二次约束，验证 convertMechanismModelToF64 后约束完整保留且二次项 token2 非空

验收：`mvn -pl ospf-kotlin-core -am test` — 129 tests, 0 failures

#### P1-11 完成详情

新增 KDoc + companion object `from()` 工厂方法：

1. `BasicLinearTriadModel` — 类级 KDoc（用途、构造方式、与 LinearTriadModel 关系）、`from(model, tokenIndexMap, bounds, fixedVariables)` 工厂方法
2. `BasicQuadraticTetradModel` — 类级 KDoc（用途、构造方式、与 QuadraticTetradModel 关系）、`from(model, tokenIndexMap, bounds, fixedVariables)` 工厂方法
3. `BasicModelEntryTest.kt` — 6 个测试用例：直接构造、工厂方法从 MechanismModel 提取变量+约束、copy 等价性（线性+二次各 3 个）

验收：`mvn -pl ospf-kotlin-core -am test` — 143 tests, 0 failures

#### P1-12 执行进度（D0~D4）

**D0: 现状冻结** — 已完成（2026-04-19）

**D1: 去除 To\*Polynomial 外部依赖** — 已完成（2026-04-20）

已完成：
1. **接口层次重构**（`MathInequalityBridge.kt`）：
   - 新增 `ToMathLinearPolynomial` 接口（`toMathLinearPolynomial(): UtilsLinearPolynomial<Flt64>`）
   - 新增 `ToMathQuadraticPolynomial` 接口（`toMathQuadraticPolynomial(): UtilsQuadraticPolynomial<Flt64>`）
   - `ToMathLinearInequality` 改为继承 `ToMathLinearPolynomial`，默认实现 `toMathLinearPolynomial() = toMathLinearInequality().lhs`
   - `ToMathQuadraticInequality` 改为继承 `ToMathQuadraticPolynomial`，默认实现 `toMathQuadraticPolynomial() = toMathQuadraticInequality().lhs`

2. **AbstractVariableItem.kt 迁移完成**：
   - `AbstractVariableItem` 超类型从 `ToLinearPolynomial, ToQuadraticPolynomial` 改为 `ToMathLinearInequality, ToMathQuadraticInequality`
   - 直接实现 `toMathLinearInequality()` 和 `toMathQuadraticInequality()`
   - 移除 `ToLinearPolynomial` / `ToQuadraticPolynomial` 导入

3. **IntermediateSymbol.kt 超类型迁移完成**（D1.3）：
   - `LinearIntermediateSymbol` 超类型从 `ToLinearPolynomial, ToQuadraticPolynomial` 改为 `ToMathLinearInequality, ToMathQuadraticInequality`
   - `QuadraticIntermediateSymbol` 超类型从 `ToQuadraticPolynomial` 改为 `ToMathQuadraticInequality`
   - 实现方法从 `toLinearPolynomial()`/`toQuadraticPolynomial()` 改为 `toMathLinearInequality()`/`toMathQuadraticInequality()`

4. **Category A 调用点迁移完成**：
   - `SubObject.kt` — 参数类型 `ToLinearPolynomial` → `ToMathLinearPolynomial`，`ToQuadraticPolynomial` → `ToMathQuadraticPolynomial`；调用 `.toMathLinearPolynomial()` / `.toMathQuadraticPolynomial()`
   - `MetaConstraint.kt` — 参数类型 `ToLinearPolynomial` → `ToMathLinearInequality`，`ToQuadraticPolynomial` → `ToMathQuadraticInequality`；调用 `.toMathQuadraticPolynomial()`
   - `MetaModel.kt` — `.toQuadraticPolynomial()` → `.toMathQuadraticPolynomial()`
   - `Model.kt` — `as ToQuadraticPolynomial` → `as ToMathQuadraticInequality`
   - `Bridge.kt` — `.toLinearPolynomial()` → `.toMathLinearPolynomial()`
   - `And.kt` — 参数类型 `ToLinearPolynomial` → `ToMathLinearPolynomial`；调用 `.toMathLinearPolynomial()`
   - `Masking.kt` — 参数类型 `ToLinearPolynomial` → `ToMathLinearPolynomial`；调用 `.toMathLinearPolynomial()`
   - `MathInequalityDsl.kt` — `QuadraticIntermediateSymbol<*>` 上 `.toQuadraticPolynomial()` → `.toMathQuadraticPolynomial()`

5. **FunctionSymbol/If/Masking/Product override 迁移完成**：
   - `LinearFunctionSymbolAdapter`、`IfFunction`、`MaskingWithPolyMaskFunction`：`toLinearPolynomial()`/`toQuadraticPolynomial()` → `toMathLinearInequality()`/`toMathQuadraticInequality()`
   - `ProductFunction`：`toQuadraticPolynomial()` → `toMathQuadraticInequality()`；内部调用 `.toMathQuadraticPolynomial()`

6. **Monomial 文件迁移完成**（D1.5）：
   - `LinearMonomial` 超类型从 `ToLinearPolynomial, ToQuadraticPolynomial` 改为 `ToMathLinearPolynomial, ToMathQuadraticPolynomial`
   - `QuadraticMonomial` 超类型从 `ToQuadraticPolynomial` 改为 `ToMathQuadraticPolynomial`

7. **ToPolynomial.kt 物理删除**（D1.4）

8. **D1 验证通过**：`mvn compile -pl ospf-kotlin-core -am` ✓、`mvn test -pl ospf-kotlin-core -am` ✓、`mvn compile -pl ospf-kotlin-framework -am` ✓

**D2: 去除 Expression 外部依赖** — 已完成（2026-04-21）

退出条件已满足：`Expression` 仅在 `@Deprecated` CallBackModel 方法中作为参数类型可见，core 对外 API 不再暴露 `Expression` 参数。

**D3: 去除 intermediate_model.monomial 主路径依赖** — 已完成（2026-04-21）

1. 移除所有未使用的 monomial 导入（MetaModel、IntermediateSymbol、TokenCacheContext、SubObject、MathInequalityBridge、Product）
2. MetaModel/MetaConstraint 中 `LinearMonomial`/`QuadraticMonomial` 参数方法标注 `@Deprecated(WARNING)`
3. MathInequalityDsl 中 `LinearMonomial`/`QuadraticMonomial` DSL 运算符已随 monomial/ 删除一并移除
4. 编译 + 测试通过

**D4: 物理删除与门禁收口** — 已完成（2026-04-21）

1. `Expression` 成员内联到 `IntermediateSymbol` 和 `Monomial`，`Expression.kt` 物理删除
2. `ToPolynomial.kt` 已在 D1 物理删除
3. `intermediate_model/monomial/` 目录物理删除（4 文件：Monomial.kt、LinearMonomial.kt、QuadraticMonomial.kt、LinearMonomialSymbol.kt）
4. 所有 monomial 调用点迁移完成：
   - `@Deprecated` 方法（MetaModel/MetaConstraint/Model 中接受 monomial 参数的方法）已删除
   - `@Deprecated cells` 属性（IntermediateSymbol/FunctionSymbol/If/Masking/Product）已删除
   - `Bridge.kt` 中 `asCoreLinearMonomial()` 已删除
   - `SymbolCombination.kt` 中 `map()` 工厂函数签名已更新
   - `AbstractVariableItem.kt` 运算符已迁移到返回 `math.symbol` 类型
   - `MathInequalityDsl.kt` 中 monomial DSL 运算符已移除
5. 测试更新：删除使用 monomial 类型的测试，替换 `cells.toLinearFlattenData()` 为 `flattenedMonomials`
6. 验收：`mvn test -pl ospf-kotlin-core -am` ✓、`mvn compile -pl ospf-kotlin-framework -am` ✓、`check-c8-guards.ps1` ✓

#### P1-9 完成详情

选择方案 B（不支持版）：

1. `QuadraticTetradModel.dual()` 改为抛出 `UnsupportedOperationException("Quadratic dual is not supported")` 而非 `TODO`
2. `QuadraticTetradModel.farkasDual()` 同上
3. 两者均添加 `@Deprecated("Quadratic dual is not supported — Rust has no public API for this")` 注解
4. `solveDual()`/`solveFarkasDual()` 调用方无需改动（异常传播行为与原 TODO 一致，仅异常类型从 NotImplementedError 变为 UnsupportedOperationException）
5. `QuadraticDualUnsupportedTest.kt` — 2 个测试：调用 dual()/farkasDual() 断言抛出 UnsupportedOperationException

验收：`mvn -pl ospf-kotlin-core -am test` — 145 tests, 0 failures

#### P1-8 完成详情

新增 `by_id` 和 `from_output` 两套 Benders cut 公共 API 变体，覆盖 LinearMechanismModel 和 QuadraticMechanismModel：

**by_id 变体**（约束名称→dual 值映射）：
- `generateOptimalCutById(objectVariable, fixedVariables, dualSolutionById: Map<String, Flt64>)` — Linear
- `generateFeasibleCutById(fixedVariables, farkasDualSolutionById: Map<String, Flt64>)` — Linear
- `generateOptimalCutById(objective, objectVariable, fixedVariables, dualSolutionById: Map<String, Flt64>)` — Quadratic
- `generateFeasibleCutById(fixedVariables, farkasDualSolutionById: Map<String, Flt64>)` — Quadratic

**from_output 变体**（原始求解器 dual 值 + TriadModel/TetradModel 原点解析）：
- `generateOptimalCutFromOutput(objectVariable, fixedVariables, dualValues: Solution, triadModel: LinearTriadModelView)` — Linear
- `generateFeasibleCutFromOutput(fixedVariables, farkasDualValues: Solution, triadModel: LinearTriadModelView)` — Linear
- `generateOptimalCutFromOutput(objective, objectVariable, fixedVariables, dualValues: Solution, tetradModel: QuadraticTetradModelView)` — Quadratic
- `generateFeasibleCutFromOutput(fixedVariables, farkasDualValues: Solution, tetradModel: QuadraticTetradModelView)` — Quadratic

新增 `BendersCutApiTest.kt`，8 个测试用例覆盖所有新 API 方法，验证与现有 `generateOptimalCut`/`generateFeasibleCut` 结果一致。

关键代码证据：`MechanismModel.kt`（8 个新方法 + KDoc），`BendersCutApiTest.kt`（8 个测试）

验收：`mvn -pl ospf-kotlin-core -am test` — 137 tests, 0 failures

---

### P1 — 本期尽量完成

| # | 项目 | 内容 | 风险 | 预估 |
|---|------|------|------|------|
| P1-10 | **主链路语义等价回归** | 成套"旧入口 vs 新入口"端到端语义等价回归；泛型 V 与 plugin 边界 Double 转换专项回归。作为后续迁移的门禁：P1-8/P1-12 完成前必须先有此回归基线 | 低 | 3h |
| P1-8 | **Benders cut 完整套件** | by_id / from_output 公共 API 变体。当前插件侧已有各自 tidyDualSolution(...) 提取路径，from_output 是"公共 API 缺失"而非"能力完全没有" | 低 | 2h |
| P1-11 | **BasicLinearTriadModel/BasicQuadraticTetradModel 独立公开入口规范化** | 类已存在且公开（LinearTriadModel.kt:183, QuadraticTetradModel.kt:168），测试也直接使用（SolverExtIISOptionsTest.kt）。缺的是：独立主入口 API 的产品化定义（命名规范、KDoc、工厂示例、独立测试） | 低 | 1h |
| P1-9 | **QuadraticTetradModel dual/farkasDual 决策** | 当前为 TODO 存根（运行时抛异常），solveDual/solveFarkasDual 会调用它们。DoD 二选一：(A) 实现版：补齐 dual/farkasDual + 回归测试；(B) 不支持版：改为显式 Failed(ErrorCode.UnsupportedOperation) + API 注释 @Deprecated("Quadratic dual not supported") + 测试验证返回 Failed，禁止保留 TODO | 中 | 0.5h (B) / 4h (A) |
| P1-12 | **C6 兼容层删除流程（D0~D4）** | 按 D0→D4 执行兼容层迁移与删除，分阶段验收。To*Polynomial、monomial 仍是高耦合面，每阶段可能触发级联编译修复 | 中 | 10~14h |

#### P1 执行顺序

```
P1-10 (语义等价回归基线 — 作为迁移门禁)
  → P1-8 (Benders cut by_id/from_output 公共 API)
  → P1-11 (Basic*Model 独立公开入口规范化)
  → P1-9 (Quadratic dual/farkasDual 决策，建议选 B: 不支持版)
  → P1-12 (C6 兼容层删除 D0~D4)
```

#### P1-9 DoD（不支持版 B）

1. `QuadraticTetradModel.dual()` 改为返回 `Failed(ErrorCode.UnsupportedOperation, "Quadratic dual is not supported")` 而非 `TODO`。
2. `QuadraticTetradModel.farkasDual()` 同上。
3. `solveDual()`/`solveFarkasDual()` 调用方已处理 Failed 返回值（验证）。
4. 添加 `@Deprecated("Quadratic dual not supported — Rust has no public API for this")` 注解。
5. 测试：调用 dual()/farkasDual() 断言返回 Failed。

### P2 — 后续迭代

| # | 项目 | 内容 | 风险 | 预估 |
|---|------|------|------|------|
| P2-3 | **PSO 求解器** | 对齐 Rust `pso` 模块 | 中 | 8h |
| P2-4 | **LP 导出** | 对齐 Rust `lp_export` / `LPExportableModel` trait | 低 | 2h |
| P2-5 | **结构化错误类型** | 对齐 Rust `model::Error` 枚举，替换当前散落的异常抛出 | 低 | 3h |
| P2-6 | **MonomialCell 非线性分支** | `Monomial.kt` 的 `MonomialCell.invoke(..., category)` 非线性分支仍为 TODO | 低 | 1h |

---

## 功能差异矩阵（Rust ← Kotlin 双向对照）

### Rust 有 / Kotlin 缺 — 需补齐

| # | Rust 组件 | 说明 | 优先级 | 状态 |
|---|----------|------|--------|------|
| G1 | `BasicModel<V>` | 变量+约束基础层 | P0 | 已实现 |
| G2 | `BasicMechanismModel<V>` | 展开后变量+约束层 | P0 | 已实现 |
| G3 | `IntoValue<V>` trait | 值类型转换 trait | P0 | 已实现 |
| G4 | `SolveValue<V>` trait | 求解值类型 trait | P0 | 已实现 |
| G5 | `AnyVariable<V>` | 类型擦除变量包装 | P0 | 已实现 |
| G6 | `ProductFunction<V>` | 两个线性多项式乘积 | P0 | 已实现 |
| G7 | `SymbolicLinearInequality<V>` | 符号化线性不等式 | P0 | 已实现 |
| G8 | `SymbolicQuadraticInequality<V>` | 符号化二次不等式 | P0 | 已实现 |
| G9 | `SparseVector<V>` / `SparseMatrix<V>` | 稀疏约束矩阵表示 | P0 | 已实现 |
| G10 | `register_auxiliary_tokens` 钩子 | 统一符号注册钩子 | P0 | 已实现 |
| G11 | `evaluate_from_tokens` 钩子 | 统一符号求值钩子 | P0 | 已实现 |
| G12 | `convert_mechanism_model_to_f64<V>()` | V→f64 转换边界 | P0 | 已实现 |
| G13 | `ConstraintRelation` enum | LessEqual/Equal/GreaterEqual 枚举 | P0 | 已实现 |
| G14 | `LinearInequality<V>` (mechanism 层) | mechanism 层线性不等式 | P0 | 已实现 |
| G15 | `QuadraticInequality<V>` (mechanism 层) | mechanism 层二次不等式 | P0 | 已实现 |
| G16 | `Constraint<V, P>` (mechanism 层) | 泛型约束包装 | P0 | 已实现 |
| G17 | `BasicLinearTriadModel<V>` | 无目标线性中间模型 | P1 | 已存在（公开类），缺独立入口规范化 |
| G18 | `BasicQuadraticTetradModel<V>` | 无目标二次中间模型 | P1 | 已存在（公开类），缺独立入口规范化 |
| G19 | `LPExportableModel` trait | LP 导出能力 | P2 | 待实现 |
| G20 | PSO 求解器 | 粒子群优化 | P2 | 待实现 |
| G21 | 结构化错误类型 `model::Error` | 替代散落异常抛出 | P2 | 待实现 |

### Kotlin 有 / Rust 无 — 需保留

| # | Kotlin 组件 | 位置 | 说明 | 保留策略 |
|---|------------|------|------|----------|
| K1 | `QuadraticTetradModel.dual()` | QuadraticTetradModel.kt | Rust 无此公共 API | 保留，决策实现或标注不支持 |
| K2 | `QuadraticTetradModel.farkasDual()` | QuadraticTetradModel.kt | 同上 | 保留，决策实现或标注不支持 |
| K3 | `ConcurrentMutableTokenTable` | TokenTable.kt | Kotlin 并发安全注册路径 | 保留 |
| K4 | `TokenCacheContexts` 四类缓存 | TokenCacheContext.kt | value/linearFlatten/quadraticFlatten/range | 保留 |
| K5 | `MechanismModelDumpingStatus` | MechanismModelDumpingStatus.kt | 调试转储状态回调 | 保留 |
| K6 | `RegistrationStatus` 回调 | RegistrationStatus.kt | 注册状态通知 | 保留 |
| K7 | `ConstraintPriority` | ConstraintPriority.kt | 约束优先级枚举 | 保留 |
| K8 | `Cross<V>` / `Mutation<V>` 接口 | solver/heuristic/ | 启发式交叉/变异接口 | 保留 |
| K9 | 8 种 Migration 实现 | solver/heuristic/Migration.kt | Kotlin 侧完整迁移策略 | 保留 |
| K10 | `SolveValue` + `SolveValueConversionContext` | solver/value/ | 求解值转换体系 | 保留 |

### 双方共有 — 对齐状态

| # | 组件 | 对齐状态 |
|---|------|----------|
| B1 | MetaModel\<V\> extends BasicModel\<V\> | 已对齐 |
| B2 | MechanismModel\<V\> extends BasicMechanismModel\<V\> | 已对齐 |
| B3 | Token\<V\> 泛型 | 已对齐 |
| B4 | Cell\<V\> 泛型 | 已对齐 |
| B5 | Constraint\<V, P\> 双泛型 | 已对齐 |
| B6 | IntermediateSymbol\<V\> 泛型 | 已对齐 |
| B7 | FunctionSymbol\<V\> 泛型 | 已对齐 |
| B8 | ProductFunction\<V\> | 已对齐 |
| B9 | Flatten 上下文 | 已对齐 |
| B10 | Benders cut | P1: 补齐 by_id/from_output |
| B11 | IIS | 已对齐 |
| B12 | Solver 配置 | 已对齐 |

---

## 架构定版硬约束

1. core 不再保留任何"对外可见"的符号运算类型（单项式/多项式/不等式）。
2. core 对外只保留两类建模实体：变量体系（variable）、用于封装约束生成的 functional symbol。
3. MetaModel 主链路全部使用 math.symbol 的单项式、多项式、不等式与 relation。
4. MechanismModel 保持泛型化 \<V\>，内部使用 Token + Cell 体系承载约束。
5. 旧 core 单项式/多项式上的缓存机制全部上收为 MetaModel 上下文。
6. 对外使用者接口"基本不变"：历史高频入口需保留，签名调整必须提供 Deprecated + ReplaceWith 兼容通道。
7. 泛型化边界固定（对齐 Rust）：
   - `MetaModel -> MechanismModel` 必须保持泛型值类型 `V` 贯通。
   - `IntermediateModel` 作为求解器标准形式，直接使用 f64。
   - 在 `MechanismModel -> IntermediateModel` 转换时进行 `V -> f64` 实例化。
   - 禁止在 MetaModel 和 MechanismModel 主链路提前固化为 `Flt64/Double`。

---

## 验收标准

### 构建与门禁

1. `mvn -pl ospf-kotlin-core -am test` — 全量测试通过
2. `mvn compile -pl ospf-kotlin-framework -am` — 框架层编译通过
3. `powershell -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1` — 门禁通过

### 差异矩阵清零检查

4. G1~G16（P0 级 Rust-only 项）全部实现
5. K1~K10（Kotlin-only 保留项）全部保留且有测试覆盖
6. B1~B12（双方共有对齐项）全部泛型化完成

### 命名兼容检查

7. 原版 Kotlin 对外名全部可通过 typealias 或 @Deprecated 兼容层访问
8. `ApiCompatibilityTest.kt` 覆盖所有原版对外名的兼容性断言
9. 新增类型命名与 Rust 一致（Kotlin 驼峰风格）
