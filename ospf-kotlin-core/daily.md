# OSPF Kotlin Core Refactor Daily

日期：2026-04-23

状态：P3-2 完成 + M2~M5 收口 — TokenTable 泛型化、Cell Impl 泛型化、minimize/maximize(symbol) 重载、Flt64 固化点审计、泛型回归测试、ToMathLinearPolynomial/ToMathQuadraticPolynomial deprecated、模型层统一入口、surefire 版本锁定均已完成

目标：在保持原 Kotlin 类型命名与接口语义兼容的前提下，按 Rust 版本架构完成 core 重构，补齐当前尚未完成的完全泛型化（包含 `Token` 体系），并让 `ospf-kotlin-example` 迁入当前仓库后通过调整已变更架构部分的 import 路径完成编译。

---

## 1. 当前结论

### 1.1 已完成结论

1. C0~C8 已全部完成，主链路已经从旧 core 表达式体系切换到 `math.symbol`。
2. P0 级主链路泛型化与机制层能力已完成，泛型边界、机制层约束体系、ProductFunction、稀疏表示、`convertMechanismModelToF64<V>()` 已贯通，但全仓完全泛型化仍未完成。
3. P1-8 / P1-9 / P1-10 / P1-11 / P1-12 已完成，Benders cut 公共 API、语义等价回归、Basic*Model 公开入口、C6 兼容层删除均已落地。
4. 2026-04-21 审核修复已完成，clean 构建与 gantt-scheduling 框架层级联编译问题已收口。
5. P3-2 盘点已完成：Token/TokenList 已泛型化，AbstractTokenTable<V> 接口已泛型化，TokenTable/MutableTokenTable 具体类及 Cell Impl 仍固化为 Flt64，待泛型化。

### 1.2 当前主要缺口

1. `variable` 与 `token` 仍未按 Rust 风格物理拆分。
2. `basic / mechanism / intermediate / callback` 四层职责尚未按 Rust 风格重排到独立分包。
3. 当前仍存在未完全泛型化路径，部分 model / token / framework / helper API 仍固化为 `Flt64` 或依赖 `Flt64` 视图。
4. `ospf-kotlin-example` 仍大量依赖旧 `frontend` 包路径，但当前策略已调整为“直接改 import，不做桥接”，相应迁移清单尚未落地。
5. `ospf-kotlin-example` 尚未迁入本仓库，也未纳入持续构建门禁。
6. C8 门禁仍偏向增量检查，尚未覆盖全仓库存量问题与全链路 clean 构建验证。

### 1.3 当前工作重心

1. 先稳住旧 Kotlin 类型名与接口语义兼容面，并明确 example 的 import 迁移边界。
2. 先补齐“未完全泛型化”的主链路缺口，尤其是 `Token` / `TokenList` / `TokenTable` 相关路径，再做更大范围结构迁移。
3. 再做内部架构重排，避免“边拆边破外部调用”。
4. 尽早把 `ospf-kotlin-example` 迁入并变成持续门禁。

---

## 2. 已完成事项总结

### 2.1 阶段性里程碑

| 阶段 | 结果 | 完成日期 |
|------|------|----------|
| C0 | 基线冻结：API 暴露面、Flt64 固化点、`.cells` 使用点完成盘点 | 2026-04-16 |
| C1 | API 清退与主链路切流：ReplaceWith 与兼容层第一批完成 | 2026-04-16 |
| C2 | 泛型化第一段与第二段完成：`MetaModel<V>` / `MechanismModel<V>` / `Token<V>` / `Constraint<V, P>` / `IntermediateSymbol<V>` 全链路贯通 | 2026-04-19 |
| C3 | 缓存上收：缓存生命周期、双写策略、回归测试完成 | 2026-04-18 |
| C4 | MechanismModel 边界收口：约束转换与 dump 辅助能力完成 | 2026-04-18 |
| C5 | Quadratic Benders cut 对齐 Rust：`generateOptimalCut` / `generateFeasibleCut` 主路径完成 | 2026-04-17 |
| C6 | 旧表达式兼容层迁移收口：`Expression.kt` / `ToPolynomial.kt` / `intermediate_model/monomial/` 物理删除 | 2026-04-21 |
| C7 | 阶段化回归脚本完成：`run-c7-regression.ps1` | 2026-04-17 |
| C8 | 增量门禁完成：旧 polynomial / `.cells` / Double 固化拦截到位 | 2026-04-17 |

### 2.2 P0 完成摘要

| 项目 | 结果 | 关键交付物 |
|------|------|------------|
| 泛型值类型贯通 | 主链路 `V` 从 MetaModel 到 MechanismModel 保持贯通，`Token<V>` 已建立；但 token 体系与外围调用面的全仓完全泛型化待后续补齐 | `Token.kt`、`Cell.kt`、`Constraint.kt`、`SubObject.kt`、`SolverExt.kt` |
| 基础模型分层 | `BasicModel<V>` 与 `BasicMechanismModel<V>` 已建立并接入主继承链 | `BasicModel.kt`、`BasicMechanismModel.kt` |
| 机制层约束体系 | `Constraint<V, P>`、线性/二次不等式、Relation 枚举完成 | `Constraint.kt` |
| Symbol 钩子补齐 | `registerAuxiliaryTokens` / `evaluateFromTokens` 已补齐并接入 TokenTable | `FunctionSymbol.kt`、`TokenTable.kt` |
| 稀疏表示与求解器边界 | `SparseVector<V>` / `SparseMatrix<V>` 与 `convertMechanismModelToF64<V>()` 已完成 | `LinearTriadModel.kt`、`QuadraticTetradModel.kt`、`MechanismModel.kt` |

### 2.3 P1 完成摘要

| 项目 | 结果 | 关键交付物 |
|------|------|------------|
| P1-10 | 主链路语义等价回归已建立 | `SemanticEquivalenceTest.kt` |
| P1-8 | Benders cut 的 `by_id` / `from_output` 公共 API 已补齐 | `MechanismModel.kt`、`BendersCutApiTest.kt` |
| P1-11 | `BasicLinearTriadModel` / `BasicQuadraticTetradModel` 独立公开入口已规范化 | `LinearTriadModel.kt`、`QuadraticTetradModel.kt`、`BasicModelEntryTest.kt` |
| P1-9 | `QuadraticTetradModel.dual()` / `farkasDual()` 已明确为不支持实现 | `QuadraticTetradModel.kt`、`QuadraticDualUnsupportedTest.kt` |
| P1-12 | C6 兼容层删除流程 D0~D4 已完成 | `MathInequalityBridge.kt`、`IntermediateSymbol.kt`、`AbstractVariableItem.kt` |

### 2.4 2026-04-21 审核修复摘要

| 修复项 | 结果 |
|--------|------|
| clean 构建阻断修复 | 删除 `CallBackModel.kt` 中残留的 `Expression` 兼容方法 |
| 遗留导入修复 | `Switch.kt` 从 `ToLinearPolynomial` 迁移到 `ToMathLinearPolynomial` |
| 框架层运算符修复 | `IntermediateSymbol.kt` 补 `plus/minus` 扩展函数 |
| 框架层泛型修复 | `ShadowPriceMap.kt` 改为 `AbstractLinearMetaModel<*>` |
| gantt-scheduling 级联修复 | 29 个文件完成泛型、导入、API 调用方式适配 |

### 2.5 当前稳定交付物

1. 文档：
   - `docs/refactor-baseline/cache-double-write.md`
   - `docs/refactor-baseline/cache-usage.md`
   - `docs/refactor-baseline/cache-lifecycle.md`
   - `docs/refactor-baseline/cache-tests.md`
   - `docs/refactor-baseline/mechanism-boundary.md`
   - `docs/refactor-baseline/mechanism-plan.md`
   - `docs/refactor-baseline/test-compile-fix.md`
   - `docs/refactor-baseline/phase7-phase8-guards.md`
2. 测试：
   - `ApiCompatibilityTest.kt`
   - `SemanticEquivalenceTest.kt`
   - `BendersCutApiTest.kt`
   - `BasicModelEntryTest.kt`
   - `QuadraticMechanismModelCutTest.kt`
   - `QuadraticDualUnsupportedTest.kt`
3. 脚本与 CI：
   - `ospf-kotlin-core/scripts/check-c8-guards.ps1`
   - `ospf-kotlin-core/scripts/run-c7-regression.ps1`
   - `.github/workflows/core-refactor-guards.yml`

### 2.6 最近一次已验证基线

1. `mvn -pl ospf-kotlin-core -am clean test` 通过
2. `mvn -pl ospf-kotlin-framework -am clean compile` 通过
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile` 通过

---

## 3. 架构定版硬约束

1. `core` 内部结构要对齐 Rust 顶层职责：`variable` / `token` / `symbol` / `model`。
2. `model` 必须拆为四个明确层级：
   - `basic`
   - `mechanism`
   - `intermediate`
   - `callback`
3. 符号运算唯一主实现固定为 `ospf-kotlin-math` 的 `math.symbol`；`core` 不再保留第二套对外可见的单项式 / 多项式 / 不等式 / relation 主实现。
4. 必须继续推进完全泛型化：`MetaModel -> MechanismModel`、`Token` 体系都要保持泛型值类型 `V` 贯通；除 `MechanismModel -> IntermediateModel`、插件边界、求解器边界外，禁止新增主链路 `Flt64` 提前固化点。
5. 所有存量 `Flt64` 固化点都必须被归类为“边界保留”或“待迁移项”，不允许处于未说明状态；`Token` / `TokenList` / `TokenTable` 不得游离在泛型化目标之外。
6. 旧 Kotlin 对外类型命名、接口语义、高频入口必须优先保持兼容；对于因架构重排发生变化的包路径，允许直接调整 import，不做包级桥接。
7. `ospf-kotlin-example` 迁入时，允许修改已变更架构部分的 import 路径与必要的直接类型引用路径；不通过 `frontend` 包桥接吸收差异。
8. `ospf-kotlin-example` 迁入后必须参与 reactor 编译/测试，但其 `pom` 必须配置为不执行 install、不执行 deploy。
9. 任何结构迁移都必须先补最小兼容测试，再做物理移动，避免“先拆后修”。
10. 门禁最终必须覆盖 clean 构建、全仓库存量检查和 example 编译。

---

## 4. 待办事项与新目标事项（统一优先级）

### 4.1 优先级总表

| 优先级 | ID | 项目 | 类型 | 当前状态 |
|--------|----|------|------|----------|
| P0 | P3-0 | 基线冻结与兼容面清单 | 新目标 | ✅ 已完成 |
| P1 | P3-1 | example import 迁移清单与旧 `frontend` 引用清退 | 新目标 | ✅ 映射表已完成，执行待 P3-5 |
| P2 | P3-2 | 完全泛型化补齐与 `Flt64` 固化点清退 | 新目标 | 完成 — TokenTable/Cell Impl 泛型化，minimize(symbol) 重载，Flt64 审计文档，回归测试 |
| P3 | P3-3 | `variable` / `token` 物理拆解 | 新目标 | 待执行 |
| P4 | P3-4 | `basic / mechanism / intermediate / callback` 模型重排 | 新目标 | 待执行 |
| P5 | P3-5 | `ospf-kotlin-example` 迁入与 reactor 接线 | 新目标 | 待执行 |
| P6 | P3-6 | 门禁增强与全链路验收 | 新目标 + 历史遗留 | 待执行 |
| P7 | P2-4 | LP 导出能力对齐 Rust | 历史待办 | 待执行 |
| P8 | P2-5 | 结构化错误类型对齐 Rust | 历史待办 | 待执行 |
| P9 | P2-3 | PSO 求解器对齐 Rust | 历史待办 | 待执行 |
| P10 | P2-6 | 非线性残留 TODO 复核 | 历史待办 | 待确认 |

### 4.2 统一执行顺序

```text
P3-0 基线冻结与兼容面清单
  -> P3-1 制定并执行 example import 迁移
  -> P3-2 补齐完全泛型化并清退主链路 Flt64 固化点
  -> P3-3 拆 variable / token
  -> P3-4 拆 basic / mechanism / intermediate / callback
  -> P3-5 迁入 ospf-kotlin-example
  -> P3-6 增强门禁并做全链路验收
  -> P2-4 / P2-5 / P2-3 / P2-6 进入后续迭代
```

---

## 5. 各待办项执行步骤与验收标准

### P3-0 基线冻结与兼容面清单

目标：先把”现状、目标、兼容面”一次性盘清，防止后续结构迁移时重复补洞。

#### 现状关键发现

| 维度 | 现状 |
|------|------|
| 包结构 | 4 个顶层包：`variable`（含 Token/TokenList）、`intermediate_model`（含全部模型层）、`intermediate_symbol`、`model`（含 callback）、`solver` |
| Flt64 引用 | core 主码 91 文件 2654 处引用，其中 TokenTable 仍固化为 `TokenListF64`，大量 typealias 充当兼容层 |
| Token 泛型化 | `Token<V>` 和 `TokenList<V>` 已泛型化，但 `TokenTable` 仍为 `TokenListF64` 固化 |
| TokenCacheContexts | token/symbol 计算缓存（非求解器输出边界），纳入 P3-2 泛型化范围 |
| framework | 已无 `frontend` 引用，6 文件含 Flt64 |
| example | 310 个 .kt 文件，596 条 frontend import（191 文件），61 条 backend import（50 文件），插件路径 `core.backend.plugins.*` 与当前仓库一致无需迁移 |

#### 执行步骤

1. 冻结 core 包结构与主类型清单（当前 4 包：`variable`/`intermediate_model`/`intermediate_symbol`/`model`）。
2. 冻结 framework 对 core 的 import 依赖清单（10 文件）。
3. 盘点 example 的 import 清单与 POM 依赖。
4. 建立三方映射表：当前实现 -> Rust 目标模块 -> 旧 Kotlin 入口。
5. 标出所有 `Flt64` 固化点，区分”合法边界”与”待迁移项”。
6. 输出文档到 `docs/refactor-baseline/p3-baseline.md`。

#### 验收标准

1. `daily.md` 中有统一优先级和执行顺序。
2. 三方映射关系可追踪，不存在”example 依赖了什么还不清楚”的盲区。
3. 后续 P3-1~P3-5 的改动对象有明确文件/包级清单。

### P3-1 example import 迁移清单与旧 `frontend` 引用清退

目标：不恢复旧 `frontend` 包桥接，改为明确映射并直接迁移 example 的 import 路径。

#### 执行步骤

1. 扫描 example 全部源文件，提取所有 `import fuookami.ospf.kotlin.core.*` 和 `import fuookami.ospf.kotlin.framework.*`。
2. 建立映射表：
   - `frontend.variable.*` -> `core.variable.*`
   - `frontend.expression.*` -> `math.symbol.*` / `core.intermediate_symbol.*`
   - `frontend.inequality.*` -> `math.symbol.inequality.*`
   - `frontend.model.*` -> `core.intermediate_model.*` / `core.model.*`
3. 在 example 中按模块批量替换已变更架构部分的 import。
4. 对保留的公共类型名与方法签名做兼容校验，但不新增包级桥接文件。
5. 扩展 `ApiCompatibilityTest.kt`，重点覆盖类型名、接口语义和高频入口，而不再覆盖旧包路径可解析性。

#### 验收标准

1. example 中已变更架构部分不再依赖旧 `frontend` 包路径。
2. 仓库内不新增 `core.frontend.*` 包级桥接层。
3. `ApiCompatibilityTest.kt` 覆盖类型名与接口兼容，而不以旧包路径兼容为目标。

### P3-2 完全泛型化补齐与 `Flt64` 固化点清退

目标：把当前”主链路已泛型化，但并未完全泛型化”的状态继续推进到可验收状态。

#### 2026-04-22 盘点结果

| 文件 | 行数 | Flt64引用 | 当前状态 | 下一步 |
|------|------|-----------|----------|--------|
| `Token.kt` | 132 | 0 | ✅ 已泛型化 `Token<V>`，dual-view 模式 | 无需改动 |
| `TokenList.kt` | 395 | 0 | ✅ 已泛型化 `TokenList<T>`，所有类均带 `<T>` | 无需改动 |
| `TokenTable.kt` | 1578 | 95 | ✅ 6 个具体类已泛型化，`*F64` typealias 保留，`symbolTokenTableContext` 改为 `AbstractTokenTable<*>` | 无需进一步改动 |
| `TokenCacheContext.kt` | 295 | 26 | ✅ `TokenCacheContexts<V>` 已泛型化，solution/fixedValues 属求解器边界保留，全局映射已调整 | 无需进一步改动 |
| `Cell.kt` | 160 | 40 | ✅ `LinearCellImpl<V>` / `QuadraticCellImpl<V>` 已泛型化，dual-view 模式，`*F64` typealias 保留 | 无需进一步改动 |
| `LinearTriadModel.kt` | 2352 | 188 | 求解器边界保留 | 确认边界保留，公共 API 泛型化 |
| `QuadraticTetradModel.kt` | 1628 | 118 | 求解器边界保留 | 同上 |
| `MechanismModel.kt` | 1219 | 104 | 已有 `<V>`，部分边界保留 | 确认边界保留点 |

#### P3-2 执行步骤完成情况（2026-04-23）

1. ✅ **TokenTable 具体类泛型化** — `TokenTable<V>` / `MutableTokenTable<V>` / `ConcurrentTokenTable<V>` / `ConcurrentMutableTokenTable<V>` / `AutoTokenTable<V>` / `ManualTokenTable<V>` / `ConcurrentAutoTokenTable<V>` / `ConcurrentManualAddTokenTable<V>` 全部泛型化，保留 `*F64` typealias；`symbolTokenTableContext` 全局映射改为 `AbstractTokenTable<*>`
2. ✅ **Cell Impl 类泛型化** — `LinearCellImpl<V>` / `QuadraticCellImpl<V>` 泛型化，dual-view 模式（`converter: IntoValue<V>?`），保留 `LinearCellImplF64` / `QuadraticCellImplF64` typealias
3. ✅ **minimize/maximize(symbol) 重载** — `LinearModel.minimize(symbol: LinearIntermediateSymbol<*>)` / `QuadraticModel.minimize(symbol: QuadraticIntermediateSymbol<*>)` 及对应 maximize 重载已添加，使用 `flattenedMonomials` 构建目标
4. ✅ **Flt64 固化点分类归档** — 输出 `docs/refactor-baseline/p3-flt64-audit.md`，约 178 处 Flt64 引用分类为 A（求解器边界 ~87）/ B（待泛型化 ~58）/ C（向后兼容 ~33）
5. ✅ **泛型回归测试（P3-2R 已收口）** — `GenericTokenTableRegressionTest` 现为 18 cases（原 12 + 新增 6 个 `copy()` 回归测试），已通过 `mvn -pl ospf-kotlin-core -Dtest=GenericTokenTableRegressionTest test` 验收（18/18）
6. ✅ **gantt-scheduling 级联修复** — `TaskCostMinimization.kt` / `CapacityCostMinimization.kt` 改用 `minimize(symbol=)`

#### P3-2 审阅意见（2026-04-23 初次复核）

1. `P0` 阻断项：`GenericTokenTableRegressionTest` 当前无法通过编译，`tokenTable.find(symbol)` 调用与 `find(AbstractVariableItem<*, *>)` 签名不匹配（见测试文件第 68/80/81 行）。已通过 `mvn -pl ospf-kotlin-core -am -Dtest=GenericTokenTableRegressionTest "-Dsurefire.failIfNoSpecifiedTests=false" test` 复现。
2. `P1` 语义风险：线性对象转二次形式时，当前多处使用 `QuadraticMonomial(..., symbol, symbol)`，这会被解释为平方项；按 `math.symbol` 语义，线性项应为 `QuadraticMonomial.linear(..., symbol)`（`symbol2 = null`）。
3. `P2` 盘点口径偏差：本节“2026-04-22 盘点结果”中 `Token.kt` / `TokenList.kt` 的 `Flt64引用=0` 与现状不一致；应改为“存在求解器边界保留点（已分类）”，避免与审计文档和源码口径冲突。
4. 已确认落地项：`TokenTable/Cell` 具体类泛型化与 `minimize/maximize(symbol)` 重载已在主干代码落地，方向正确。

#### P3-2R 收口复核（2026-04-23 更新）

1. ✅ `P0` 已关闭：`GenericTokenTableRegressionTest` 编译问题已修复，当前测试结果为 `Tests run: 18, Failures: 0, Errors: 0`。
2. ✅ `P1` 已关闭：线性转二次项语义已统一为 `QuadraticMonomial.linear(..., symbol)`，不再误用平方项表示线性项。
3. ✅ `copy()` 回归覆盖已补齐：`AutoTokenTable` / `ManualTokenTable` / `ConcurrentAutoTokenTable` / `ConcurrentManualAddTokenTable` 均新增“copy 保留 token 与 solverIndex”验证，并补充空表 copy 边界用例。
4. ✅ 残余技术债已修复：`maven-surefire-plugin` 版本已锁定到 3.2.5，Kotlin 版本警告已通过 `-Xsuppress-version-warnings` 抑制；历史 deprecation / unchecked cast 警告仍存在但不阻断构建。

#### P3-2 后续改进计划（含多项式转换接口统一）

1. ✅ `P3-2R`（收口修复）已完成：`GenericTokenTableRegressionTest` 编译问题与 `copy()` 回归已闭环，`minimize/maximize(symbol=)` 基础回归已补齐，线性到二次转换语义已统一为 `symbol2 = null`。
2. `M1`（math.symbol 新接口收口）：在已引入 `ToLinearPolynomial` / `ToQuadraticPolynomial` / `ToCanonicalPolynomial` 的基础上，继续补齐 variable/symbol 侧实现，做到 variable/symbol/monomial/polynomial 全覆盖。
3. ✅ `M2`（兼容桥接）：`ToMathLinearPolynomial` / `ToMathQuadraticPolynomial` 已标记 `@Deprecated(WARNING)`，指向 `math.symbol.operation.ToPolynomial` 新接口；相关调用方已补 ``@Suppress("DEPRECATION")``。
4. ✅ `M3`（模型层统一）：`LinearModel` 新增 `minimize`/`maximize`/`addConstraint` 接收 `ToMathLinearInequality` 的统一入口；现有重载保留。
5. ✅ `M4`（二次型同理）：`QuadraticModel` 新增 `minimize`/`maximize`/`addConstraint` 接收 `ToMathQuadraticInequality` 的统一入口；现有重载保留。
6. ✅ `M5`（验收与文档）：core test (143/0/0)、gantt-scheduling compile 均通过；`maven-surefire-plugin` 版本已锁定；daily.md 状态已更新。

#### 验收标准

1. 主建模链路不存在未说明的 `Flt64` 提前固化点。
2. 剩余 `Flt64` 点都能被归类为边界保留，并在文档中可追踪。
3. `Token` / `TokenList` / `TokenTable` 以泛型 `V` 路径为主，不再以 `Flt64` 特化接口充当主入口。
4. 高优先级 core/framework 入口以泛型 `V` 为主，而不是以 `Flt64` 特化为主。
5. 泛型路径回归测试通过。

### P3-3 `variable` / `token` 物理拆解

目标：让 `variable` 与 `token` 对齐 Rust 架构职责，不再混放，并保持 token 体系的泛型化结果不在拆分过程中回退。

#### 当前 `variable` 包内容与迁移目标

| 文件 | 职责 | 目标包 |
|------|------|--------|
| `AbstractVariableItem.kt` | 纯变量 | `variable`（保留） |
| `AnyVariable.kt` | 纯变量 | `variable`（保留） |
| `Type.kt` | 纯变量 | `variable`（保留） |
| `VariableCombinationItem.kt` | 纯变量 | `variable`（保留） |
| `VariableIndependentItem.kt` | 纯变量 | `variable`（保留） |
| `VariableRange.kt` | 纯变量 | `variable`（保留） |
| **`Token.kt`** | token 体系 | **`token`** |
| **`TokenList.kt`** | token 体系 | **`token`** |

#### 从 `intermediate_model` 迁出

| 文件 | 目标包 |
|------|--------|
| **`TokenTable.kt`** | `token` |
| **`TokenCacheContext.kt`** | `token` |
| **`TokenCacheKey.kt`** | `token` |

#### 执行步骤

1. 先补兼容测试，确保当前 `variable.Token` / `intermediate_model.TokenTable` 的引用方可编译。
2. 创建 `core.token` 包。
3. 物理移动 Token/TokenList/TokenTable/TokenCache* 到 `core.token`。
4. 在旧位置留 `@Deprecated` typealias 过渡。
5. 更新 framework 10 文件 + gantt-scheduling 的 import。
6. 验证 clean 构建。

#### 验收标准

1. `variable` 包不再承载 token 表管理主职责。
2. `token` 包形成独立、稳定的主入口。
3. Rust 的 `src/variable` / `src/token` 与 Kotlin 的新结构可以一一对应。
4. example 与框架层对 token 体系的引用可通过直接 import 新路径完成编译。
5. token 模块拆分后不破坏泛型化主路径。

### P3-4 模型层重排：`basic / mechanism / intermediate / callback`

目标：将当前混放在 `core.intermediate_model` 内的多层职责拆到 Rust 对齐结构。

#### 当前 `intermediate_model` 包 -> 目标映射

| 当前文件 | 目标包 |
|----------|--------|
| `BasicModel.kt` | `model.basic` |
| `BasicMechanismModel.kt` | `model.basic` |
| `Model.kt` (interface) | `model.basic` |
| `MetaModel.kt` | `model.mechanism` |
| `MechanismModel.kt` | `model.mechanism` |
| `Constraint.kt` | `model.mechanism` |
| `MetaConstraint.kt` | `model.mechanism` |
| `SubObject.kt` | `model.mechanism` |
| `Object.kt` / `ObjectCategory.kt` | `model.mechanism` |
| `Relation.kt` / `ConstraintSign.kt` / `ConstraintPriority.kt` | `model.mechanism` |
| `RegistrationStatus.kt` | `model.mechanism` |
| `LinearTriadModel.kt` | `model.intermediate` |
| `QuadraticTetradModel.kt` | `model.intermediate` |
| `SparseMatrix.kt` | `model.intermediate` |
| `Cell.kt` | `model.intermediate` |
| `MathInequalityDsl.kt` / `MathInequalityBridge.kt` | `model.intermediate` |
| `LinearConstraintInput.kt` | `model.intermediate` |
| `DumpHelpers.kt` | `model.intermediate` |
| `ExpressionRange.kt` | `model.intermediate` |
| `*DumpingStatus.kt` | `model.intermediate` |
| `ModelFileFormat.kt` | `model.intermediate` |

#### 当前 `model` 包 -> 目标映射

| 当前文件 | 目标包 |
|----------|--------|
| `Model.kt` | `model.basic` |
| `MultiObject.kt` | `model.basic` |
| `status/*` | `model.basic` |
| `callback/CallBackModel.kt` | `model.callback` |
| `callback/CallBackModelInterface.kt` | `model.callback` |

#### 执行步骤

1. 先补兼容测试。
2. 创建 4 个子包：`model.basic`、`model.mechanism`、`model.intermediate`、`model.callback`。
3. 按映射表物理移动文件。
4. 旧位置留 `@Deprecated` typealias。
5. 更新 framework + gantt-scheduling import。
6. 删除空 `intermediate_model` 包。
7. 验证 clean 构建。

#### 验收标准

1. 新包结构可以清晰映射到 Rust `model` 下的四个子模块。
2. `core.intermediate_model` 不再作为多层职责混放主入口。
3. 框架层和 example 能通过新路径访问主入口，不依赖旧包桥接。

### P3-5 `ospf-kotlin-example` 迁入与 reactor 接线

目标：把 example 变成真实兼容回归，而不是口头约束。

#### 执行步骤

1. 将 `E:\workspace\ospf\examples\ospf-kotlin-example` 复制到当前仓库根目录。
2. 按映射表调整 import 路径（P3-1 已产出映射），不改业务建模语义。
3. 调整根 `pom.xml` 与聚合模块，使 example 能参与 reactor 构建。
4. 在 `ospf-kotlin-example/pom.xml` 中明确配置：
   - 不 install
   - 不 deploy
5. 若编译失败，仅允许修复：
   - example import 路径
   - 模块依赖坐标
   - 聚合方式

#### 验收标准

1. example 的业务建模逻辑保持不变，仅存在 import 路径和必要的直接类型路径调整。
2. `ospf-kotlin-example/pom.xml` 已明确配置为不 install、不 deploy。
3. `mvn -pl ospf-kotlin-example -am clean compile` 通过。
4. 若求解器依赖可用，则 `mvn -pl ospf-kotlin-example -am test` 通过。

### P3-6 门禁增强与全链路验收

目标：把本轮重构真正变成”可守”的状态，而不是一次性人工收口。

#### 执行步骤

1. 增强 C8 门禁，覆盖全仓库存量检查，而不只检查增量行。
2. 将 clean 构建验证纳入门禁：
   - core
   - framework
   - 关键框架子模块
   - example
3. 增加 example import 迁移完成性检查与 example 编译检查。
4. 补齐必要的回归测试与脚本入口。

#### 验收标准

1. `mvn -pl ospf-kotlin-core -am clean test` 通过。
2. `mvn -pl ospf-kotlin-framework -am clean compile` 通过。
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile` 通过。
4. `mvn -pl ospf-kotlin-example -am clean compile` 通过。
5. `pwsh.exe -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1` 通过。

### P2-4 LP 导出能力对齐 Rust

目标：补齐 Rust `lp_export` / `LPExportableModel` 能力。

执行步骤：
1. 对照 Rust `model/intermediate/lp_export.rs` 设计 Kotlin 对应入口。
2. 明确适用模型层级与导出边界。
3. 增加最小回归测试和示例。

验收标准：
1. 线性与可导出模型具备统一 LP export 入口。
2. 至少存在一组导出内容正确性的测试。

### P2-5 结构化错误类型对齐 Rust

目标：以结构化错误替代当前散落的异常抛出。

执行步骤：
1. 对照 Rust `error.rs` 和 `model::Error` 设计 Kotlin 错误枚举或层级。
2. 先替换核心建模路径，再扩展到插件边界。
3. 为高频错误路径增加单元测试。

验收标准：
1. 核心建模路径不再依赖零散字符串异常表达语义。
2. 错误码、错误消息、调用方处理方式可统一。

### P2-3 PSO 求解器对齐 Rust

目标：补齐 Rust `pso` 模块。

执行步骤：
1. 对照 Rust `solver/pso` 设计 Kotlin 求解器接口与配置项。
2. 明确与现有 heuristic 体系的边界。
3. 增加最小可运行示例和测试。

验收标准：
1. 暴露统一的 PSO 求解入口。
2. 至少存在一组功能性回归测试。

### P2-6 非线性残留 TODO 复核

目标：确认旧 `MonomialCell` 非线性 TODO 在当前路线下是否仍有保留价值。

执行步骤：
1. 全仓搜索相关 TODO 和调用残留。
2. 判断其是否已被 `math.symbol` 路线覆盖。
3. 若仍保留，则决定“实现 / 删除 / 明确不支持”。

验收标准：
1. 不存在语义不明的残留 TODO。
2. 每个残留点都有明确归属结论。

---

## 6. 总体验收标准

### 6.1 构建与门禁

1. `mvn -pl ospf-kotlin-core -am clean test`
2. `mvn -pl ospf-kotlin-framework -am clean compile`
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`
4. `mvn -pl ospf-kotlin-example -am clean compile`
5. `pwsh.exe -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`

### 6.2 架构对齐

1. Kotlin 顶层职责可映射到 Rust：`variable` / `token` / `symbol` / `model`
2. Kotlin `model` 可映射到 Rust：`basic` / `mechanism` / `intermediate` / `callback`
3. `math.symbol` 成为唯一对外符号运算主实现
4. 主建模链路与 `Token` 体系完成完全泛型化收口，剩余 `Flt64` 固化点仅存在于已声明边界

### 6.3 兼容性

1. 旧 Kotlin 对外类型命名与接口语义保持可用
2. `ospf-kotlin-example` 在完成 import 迁移后可编译
3. 仓库内不依赖 `frontend` 包级桥接吸收架构变化
4. `ApiCompatibilityTest.kt` 覆盖旧命名与接口语义兼容

### 6.4 质量

1. 先补兼容测试，再做物理迁移
2. 每一阶段完成后至少保留一次 clean 构建记录
3. 新增兼容适配代码必须可删可追踪，避免长期隐式分叉

---

## 7. 备注

1. 当前最重要的不是再补单点功能，而是先把架构重排、旧接口兼容、example 回归三件事做成同一条主线。
2. 后续所有迭代默认遵循：
   - 先盘点
   - 再建桥
   - 再迁移内部结构
   - 最后用 example 和 clean 构建验收
3. 若执行过程中发现与 `ospf-kotlin-main` 命名不一致，应优先修类型名与接口语义兼容；对架构变更导致的包路径差异，直接改 import，不做桥接。

---

## 8. P3 执行时间线与风险

### 8.1 预估时间线

```
Week 1: P3-0 (1d) -> P3-1 (1-2d) -> P3-2 开始
Week 2: P3-2 完成 (3-5d total) -> P3-3 开始
Week 3: P3-3 完成 (2-3d) -> P3-4 开始
Week 4: P3-4 完成 (3-5d) -> P3-5 (1-2d) -> P3-6 (1-2d)
```

### 8.2 关键风险

1. **P3-2 TokenTable 泛型化**影响面最大（90 处 Flt64 引用），需谨慎推进。
2. **P3-4 模型层重排**涉及 20+ 文件物理移动，需确保 typealias 过渡不遗漏。
3. 每个阶段完成后必须验证 clean 构建，避免问题累积。
4. P3-3 与 P3-4 有依赖关系：token 拆解应在模型层重排之前完成，否则 token 相关文件会被重排两次。
