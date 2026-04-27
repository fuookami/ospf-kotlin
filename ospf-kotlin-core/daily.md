# OSPF Kotlin Core Refactor Daily

日期：2026-04-27（P4-2 D1 交接）

状态：P4-2 D1 执行中（接口层泛型优先改造） — `IntermediateSymbol<V>` 接口的 `prepare`/`prepareAndCache`/`evaluateFromTokens`/`evaluateAsV` 等方法签名已从 `LegacyAbstractTokenTable` 改为 `AbstractTokenTable<*>`；`evaluate(tokenTable: AbstractTokenTable<*>)` 默认方法已添加；`Product.kt` 的 `LegacyAbstractTokenTable` evaluate 重载及 companion helpers 已移除。**当前阻塞**：JVM platform declaration clash — `evaluate(tokenTable: AbstractTokenTable<*>)` 与 `evaluate(tokenTable: LegacyAbstractTokenTable)` 在 JVM 字节码层面签名相同（type erasure），需要移除 `LinearExpressionSymbol`/`QuadraticExpressionSymbol` 中的 `LegacyAbstractTokenTable` override 方法，以及 `IfFunction`/`MaskingWithPolyMaskFunction`/`LinearFunctionSymbolAdapter` 中继承的 clash。下一步：系统性移除所有 `evaluate(tokenTable: LegacyAbstractTokenTable)` override（由 `AbstractTokenTable<*>` 默认方法替代），然后验证构建。

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

1. `variable` 与 `token` 已按 Rust 风格物理拆分（P3-3 完成）。
2. `basic / mechanism / intermediate / callback` 四层职责已按 Rust 风格重排到独立分包（P3-4 完成）。
3. `ospf-kotlin-math` 重构已完成，`-am` clean 构建已通过。
4. 当前仍存在未完全泛型化路径，部分 model / token / framework / helper API 仍固化为 `Flt64` 或依赖 `Flt64` 视图。
5. `intermediate_symbol` 包中的实际类（如 `LinearExpressionSymbol`、`QuadraticExpressionSymbol`、`FunctionSymbol`）仍为历史兼容形态，尚未完成彻底迁移与清退。
6. 仍需把上述 `intermediate_symbol` 实际类迁移纳入统一门禁，避免仅靠人工回归。

### 1.3 当前工作重心

1. 启动并完成 `intermediate_symbol` 实际类迁移（P4-1），从“兼容保留”进入“彻底收口”。
2. 继续稳住旧 Kotlin 类型名与接口语义兼容面，优先保证外部调用行为不回退。
3. 把 `intermediate_symbol` 迁移结果纳入全仓 clean test 与门禁，确保可持续守护。

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

1. `mvn -pl ospf-kotlin-core clean test` 通过（140/0/0）
2. `mvn -pl ospf-kotlin-framework clean compile` 通过
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context clean compile` 通过（需先 install core+framework）
4. `mvn -pl ospf-kotlin-core -am clean test` 通过（140/0/0）

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
| P3 | P3-3 | `variable` / `token` 物理拆解 | 新目标 | ✅ 完成 — `core.token` 独立包，5 文件迁入，@Deprecated typealias 过渡，全链路 import 更新 |
| P4 | P3-4 | `basic / mechanism / intermediate / callback` 模型重排 | 新目标 | ✅ 完成 — 四分包建立，@Deprecated typealias 过渡，全链路 import 更新；`-am` clean 构建已通过 |
| P5 | P3-5 | `ospf-kotlin-example` 迁入与 reactor 接线 | 新目标 | ✅ 已完成 — example 迁入、import 迁移、reactor 接线与不 install/deploy 配置已落地 |
| P6 | P3-6 | 门禁增强与全链路验收 | 新目标 + 历史遗留 | ✅ 已完成 — 全仓 `mvn clean test` 已通过 |
| P7 | P4-1 | `intermediate_symbol` 包实际类彻底迁移 | 新目标 | 执行中 — Phase A2 已完成（2026-04-27），A3~A4 推进中 |
| P8 | P4-2 | 主链路完整泛型化收口（与 P4-1 并行衔接） | 新目标 | 计划中 — 待 P4-1A 基线恢复后启动 |
| P9 | P4-3 | `TokenF64` 兼容别名清退与类型统一 | 新目标 | 计划中 — 待 P4-2 完成后启动 |
| P10 | P2-4 | LP 导出能力对齐 Rust | 历史待办 | 待执行 |
| P11 | P2-5 | 结构化错误类型对齐 Rust | 历史待办 | 待执行 |
| P12 | P2-3 | PSO 求解器对齐 Rust | 历史待办 | 待执行 |
| P13 | P2-6 | 非线性残留 TODO 复核 | 历史待办 | 待确认 |

### 4.2 统一执行顺序

```text
P3-0 基线冻结与兼容面清单
  -> P3-1 制定并执行 example import 迁移
  -> P3-2 补齐完全泛型化并清退主链路 Flt64 固化点
  -> P3-3 拆 variable / token
  -> P3-4 拆 basic / mechanism / intermediate / callback
  -> P3-5 迁入 ospf-kotlin-example
  -> P3-6 增强门禁并做全链路验收
  -> P4-1 intermediate_symbol 实际类彻底迁移
  -> P4-2 主链路完整泛型化收口（与 P4-1 并行衔接）
  -> P4-3 TokenF64 兼容别名清退与类型统一
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

#### P3-3 执行完成情况（2026-04-24）

1. ✅ **创建 `core.token` 包** — `fuookami.ospf.kotlin.core.token` 独立包已建立
2. ✅ **物理移动 5 文件** — `Token.kt`/`TokenList.kt` 从 `variable` 迁入，`TokenTable.kt`/`TokenCacheContext.kt`/`TokenCacheKey.kt` 从 `intermediate_model` 迁入
3. ✅ **包声明与 import 更新** — 5 文件包声明改为 `core.token`，跨包引用补齐 import（`AbstractVariableItem`、`ExpressionRange`、`RegistrationStatus` 等）
4. ✅ **旧位置 @Deprecated typealias 过渡** — `variable/Token.kt`、`variable/TokenList.kt`、`intermediate_model/TokenTable.kt`、`intermediate_model/TokenCacheContext.kt` 均已留桥接 typealias，指向 `core.token` 新位置
5. ✅ **全链路 import 更新** — `MechanismModel.kt`、`MetaModel.kt`、`IntermediateSymbol.kt`、`Cell.kt`、`LinearConstraintInput.kt`、`FunctionSymbol.kt`、`If.kt`、`Masking.kt`、`Product.kt`、`MVO.kt` 及 4 个测试文件均补齐 `core.token` import
6. ✅ **clean 构建验证** — core test 143/0/0、framework compile、gantt-scheduling compile 均通过

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

#### P3-4 执行完成情况（2026-04-24）

1. ✅ **创建四个子包** — `model.basic`、`model.mechanism`、`model.intermediate`、`model.callback`（callback 保留原位）
2. ✅ **`model.basic` 迁入 11 文件** — `ModelView.kt`（Variable/ModelCell/ConstraintCell 等）、`Model.kt`（Model/LinearModel/QuadraticModel）、`ObjectCategory.kt`、`ConstraintSign.kt`、`ConstraintPriority.kt`、`RegistrationStatus.kt`、`ExpressionRange.kt`、`ModelFileFormat.kt`、`ModelBuildingStage.kt`、`ModelBuildingStatus.kt`、`MultiObject.kt`
3. ✅ **`model.mechanism` 迁入 12 文件** — `MetaModel.kt`、`MechanismModel.kt`、`BasicModel.kt`、`BasicMechanismModel.kt`、`Constraint.kt`、`MetaConstraint.kt`、`Object.kt`、`SubObject.kt`、`Relation.kt`、`MathInequalityDsl.kt`、`MathInequalityBridge.kt`、`LinearConstraintInput.kt`
4. ✅ **`model.intermediate` 迁入 7 文件** — `LinearTriadModel.kt`、`QuadraticTetradModel.kt`、`SparseMatrix.kt`、`Cell.kt`、`DumpHelpers.kt`、`IntermediateModelDumpingStatus.kt`、`MechanismModelDumpingStatus.kt`
5. ✅ **旧位置 @Deprecated typealias 过渡** — `intermediate_model/` 和 `model/` 旧位置均留桥接 typealias，指向新包
6. ✅ **全链路 import 更新** — core 内部（solver/iis、intermediate_symbol、model/callback）、framework（6 文件）、gantt-scheduling（29 文件）均完成 import 迁移；star import 全部替换为显式 import
7. ✅ **构建验证** — core test 140/0/0、framework compile、gantt-scheduling compile 均通过
8. ✅ **`-am` clean 构建已通过** — math 重构完成后，`MetaDualSolution` typealias 过渡已补齐，`mvn -pl ospf-kotlin-core -am clean test`、framework compile、gantt-scheduling 全子模块 compile 均通过

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

### P4-1 `intermediate_symbol` 包实际类彻底迁移

目标：完成 `intermediate_symbol` 包中历史兼容实际类（如 `LinearExpressionSymbol`、`QuadraticExpressionSymbol`、`FunctionSymbol`）的迁移收口，减少长期桥接负担。

#### 计划时间

1. 启动日期：2026-04-27（实际）
2. 预计完成：2026-05-02 ~ 2026-05-04

#### Phase A2 执行完成情况（2026-04-27）

1. `SymbolCombination.kt`：清理 `SymbolCombination` / `QuantitySymbolCombination` 初始化分支，移除旧 `LinearFunctionSymbol` / `QuadraticFunctionSymbol` 绑定逻辑，保留 `LinearExpressionSymbol`、`QuadraticExpressionSymbol` 与 `else`。
2. `MechanismModel.kt`：`FunctionSymbol` / `LinearFunctionSymbol` / `QuadraticFunctionSymbol` 导入迁移为 `MathFunctionSymbol`；符号注册统一为 `sym.register(metaModel)`，不再按 `fixedVariables` 走分支。
3. `MetaModel.kt`：`FunctionSymbol` 导入迁移为 `MathFunctionSymbol`；`exportOpm` 场景由 `symbol.register(temp)` 调整为 `symbol.registerAuxiliaryTokens(temp)`。
4. `TokenTable.kt`：移除 `FunctionSymbolRegistrationScope` 与 `add(scope)` 主路径；`register()` 流程统一为在 token 注册阶段只执行 `registerAuxiliaryTokens(tokenTable)`，约束注册由模型层处理。
5. `gantt-scheduling-domain-task-compilation-context/Compilation.kt`：修复 `taskCost.asMutable()` 调用，改为 `(taskCost as LinearExpressionSymbol).asMutable()`，避免接口类型缺少可变视图导致的编译错误。
6. `gantt-scheduling-domain-capacity-scheduling-context`：补齐 `LinearIntermediateSymbol` 导入，并修复 `_cost` 的可变写入为 `(_cost as LinearExpressionSymbol).asMutable()`，收口历史预存编译错误。

#### 最新验证结果（2026-04-27）

1. `mvn -pl ospf-kotlin-core test` 通过（135/0/0）。
2. `mvn -pl ospf-kotlin-framework compile` 通过。
3. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-capacity-scheduling-context -am compile` 通过。

#### 执行步骤

1. 盘点 `intermediate_symbol` 包内“实际类 vs typealias/桥接”清单，按调用热度排序。
2. 分模块迁移调用方到 `math.symbol` 与 `core.intermediate_symbol.function` 主路径，禁止新增转接。
3. 对缺失的快捷接口，优先在 `math.symbol` 层补齐等价快捷接口，再回填调用方。
4. 清理可删除的 `@Deprecated` 过渡代码，并补齐回归测试。
5. 完成全仓 `clean test` 验收并更新门禁脚本检查项。

#### 验收标准

1. `intermediate_symbol` 包中目标实际类完成迁移或明确保留原因，不存在“未说明保留”。
2. 调用方主路径不再依赖历史桥接类型。
3. 全仓 `mvn clean test` 通过。

### P4-2 主链路完整泛型化收口（与 P4-1 并行衔接）

目标：在 P4-1 迁移过程中同步完成 `intermediate_symbol -> token -> model` 的泛型主路径收口，确保除求解器/插件边界外不再新增主链路 `Flt64` 提前固化点。

#### 前置条件

1. `P4-1A` 完成并恢复 core 编译基线：`mvn -pl ospf-kotlin-core -DskipTests clean compile` 通过。
2. 旧 `FunctionSymbol` 族残留引用完成一次性收口，以下文件无 `Unresolved reference`：
   - `core/intermediate_symbol/SymbolCombination.kt`
   - `core/token/TokenTable.kt`
   - `core/model/mechanism/MetaModel.kt`
   - `core/model/mechanism/MechanismModel.kt`
3. `P3-6` 门禁入口可执行；若历史脚本缺失，先补最小可运行脚本再进入 P4-2 正式改造。
4. 冻结当前基线数字并写入 D0 文档（至少包含）：
   - `TokenF64` 在 `src/main` 的分布与总数；
   - `LegacyAbstractTokenTable` / `LegacyAbstractMutableTokenTable` 在 `src/main` 的分布与总数；
   - `@Deprecated` typealias 在 `core/src/main` 的声明数量与仍被引用清单。

#### 详细执行计划（D0~D7）

1. D0（基线冻结，0.5d）：冻结泛型化改造清单与边界清单。
   交付物：`docs/refactor-baseline/p4-2-generic-baseline.md`（记录当前 `Flt64` 存量、边界分类、待迁移文件清单、禁止新增规则）。
2. D0.5（门禁引导，0.5d）：补齐并接线 `ospf-kotlin-core/scripts/check-c8-guards.ps1`。
   处理内容：先实现最小可运行规则，再逐步增强；至少包含“禁止新增旧 `FunctionSymbol` 命名”“禁止新增 `TokenF64` 非豁免使用”“禁止新增 `LegacyAbstractTokenTable*` 非豁免使用”三条检查。
3. D1（接口层，1d）：提升 `IntermediateSymbol<V>` 主接口为“泛型优先，Flt64 兼容次级”。
   处理内容：统一 `range/evaluate/prepare` 对外泛型语义，保留 `Flt64` 兼容 typealias 与过渡入口，不新增新桥接类型。
4. D2（实现层，1d）：完成 `LinearExpressionSymbol` / `QuadraticExpressionSymbol` 的完整泛型化改造。
   处理内容：构造器、`flattenedMonomials`、`toMath*` 转换和 `SymbolCombination` 的类型约束同步改造，保证旧命名兼容但主实现不再绑定 `Flt64`。
5. D3（函数符号链路，1~1.5d）：重构 `function` 与 `TokenTable.register` 的注册流程。
   处理内容：移除对旧 `FunctionSymbolRegistrationScope` 的依赖，统一为“辅助 token 注册 + 约束注册”双阶段流程，消除 `add(scope)` 重载歧义。
6. D4（别名清扫前置，1d）：优先清理 `LegacyAbstractTokenTable*` 在 core 主路径的直接引用。
   处理内容：按 `model.mechanism -> model.callback -> intermediate_symbol` 顺序替换为 `AbstractTokenTable<Flt64>` / `AbstractMutableTokenTable<Flt64>`；仅保留 typealias 定义与兼容测试使用点。
7. D5（模型层联动，1d）：迁移 `MetaModel/MechanismModel/CallBackModel/BasicModel` 的函数符号注册与导出逻辑。
   处理内容：更新 `exportOpm` 与 dump 过程中的符号注册入口，确保泛型路径和 `Flt64` 边界路径职责清晰。
8. D6（调用方迁移，1d）：修复 framework、gantt-scheduling、example 的泛型调用与 import。
   处理内容：仅允许类型路径与泛型参数调整，不改业务建模语义。
9. D7（门禁与回归，0.5~1d）：补齐回归测试与静态门禁。
   处理内容：新增/扩展泛型回归测试，门禁增加“禁止新增非边界 `Flt64` 固化点”“禁止回流旧 `FunctionSymbol` 命名”“禁止新增 `LegacyAbstractTokenTable*` / `TokenF64` 非豁免引用”检查。

#### 验收标准

1. 构建验证通过：
   - `mvn -pl ospf-kotlin-core -am clean test`
   - `mvn -pl ospf-kotlin-framework -am clean compile`
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`
   - `mvn -pl ospf-kotlin-example -am clean compile`
   - `pwsh.exe -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`
2. `core/intermediate_symbol`、`core/token`、`core/model` 三条主链路中，不存在未归类的 `Flt64` 提前固化点。
3. 全仓不再依赖旧 `FunctionSymbol` / `LinearFunctionSymbol` / `QuadraticFunctionSymbol` / `FunctionSymbolRegistrationScope` 作为主路径类型。
4. `core/src/main` 中 `LegacyAbstractTokenTable*` 仅允许保留在别名定义与历史兼容入口，主实现路径不再直接使用。
5. `ApiCompatibilityTest.kt` 与泛型回归测试通过，且兼容层仅保留可追踪、可删除的最小集合。

### P4-3 `TokenF64` 兼容别名清退与类型统一

目标：将主链路中对 `TokenF64` 的直接依赖收口到 `Token<Flt64>`，并通过“先弃用、后移除”两阶段策略完成兼容平滑过渡。

#### 前置条件

1. `P4-2` 完成并通过 core/framework/example 构建验收。
2. 外部兼容策略明确：当前大版本是否允许源兼容破坏（若不允许，仅执行 P4-3A）。
3. `ApiCompatibilityTest.kt` 已覆盖 token 相关高频入口，能够识别兼容回退。
4. `TokenF64` 当前使用基线已冻结（按模块分组，并写入 D0 文档），后续每个阶段对照该基线做净减少校验。

#### 详细执行计划（A/B 两阶段）

1. P4-3A（当前大版本，兼容保留）：
   - A1 盘点：冻结 `TokenF64` 使用清单（core/core-plugin/framework/example）并区分“公开签名/内部实现”，输出 Top 文件与风险等级。
   - A2 内部替换（分批）：`core.model.intermediate` -> `core.model.mechanism` -> `core-plugin` -> `framework/example`，每一批完成后单独编译验证。
   - A3 软弃用：对 `typealias TokenF64 = Token<Flt64>` 增加 `@Deprecated(level = WARNING, ReplaceWith("Token<Flt64>"))`。
   - A4 兼容测试：补充 `TokenF64` 到 `Token<Flt64>` 的语义等价回归（构造、查找、solverIndex、copy 路径）。
   - A5 门禁：新增检查，禁止新增 `TokenF64` 使用点（豁免别名定义与历史兼容测试），并输出“本次相对基线净减少数量”。
2. P4-3B（下一大版本，兼容清退）：
   - B1 破坏窗口：删除 `TokenF64` 别名，统一为 `Token<Flt64>`。
   - B2 调用方收口：同步修复 core-plugin/framework/example 的类型签名与导入。
   - B3 兼容测试更新：移除旧别名断言，新增“别名已删除”静态检查。
   - B4 发布治理：在 CHANGELOG/迁移文档中明确替换规则、影响面与一键检索命令。

#### 验收标准

1. P4-3A 验收：
   - `src/main` 新增代码中不允许新增 `TokenF64` 引用；
   - 现有主链路实现以 `Token<Flt64>` 为主；
   - `TokenF64` 仅作为 `@Deprecated(WARNING)` 兼容别名存在；
   - `TokenF64` 使用总量相对冻结基线净减少，且减少量可追踪到模块级清单。
2. P4-3B 验收（仅在允许破坏兼容时执行）：
   - 全仓不存在 `TokenF64` 类型引用与别名定义；
   - core/framework/example 构建与测试通过；
   - 发布说明明确记录该破坏性变更。

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

## 7. P4-2 D1 交接记录（2026-04-27）

### 7.1 已完成的修改

1. **`IntermediateSymbol.kt` 接口层**：
   - `prepare(values, tokenTable: AbstractTokenTable<*>)` — 签名从 `LegacyAbstractTokenTable` 改为 `AbstractTokenTable<*>`
   - `prepareAndCache(values, tokenTable: AbstractTokenTable<*>)` — 同上，内部 `@Suppress("UNCHECKED_CAST") val tt = tokenTable as AbstractTokenTable<Flt64>` 后调用 `tt.cache()`
   - `evaluateFromTokens(tokenTable: AbstractTokenTable<*>)` — 签名改为 `AbstractTokenTable<*>`
   - `prepareAsV`/`evaluateAsV`/`evaluateFromTokensAsV` — 签名改为 `AbstractTokenTable<*>`
   - 新增 `evaluate(tokenTable: AbstractTokenTable<*>, zeroIfNone)` 默认方法（3 个重载），内部通过 `@Suppress("UNCHECKED_CAST")` 转型后委托给 `evaluate(tokenList)`
   - **已移除** `evaluate(tokenTable: LegacyAbstractTokenTable)` 默认方法（3 个重载），因为与 `AbstractTokenTable<*>` 版本 JVM 签名冲突
   - `evaluateSymbol` 私有方法改为接受 `AbstractTokenTable<*>`，内部 `@Suppress("UNCHECKED_CAST")` 转型后调用 `tt.find(symbol)?.result`
   - `LinearExpressionSymbol.prepare` override 改为 `AbstractTokenTable<*>`
   - `QuadraticExpressionSymbol.prepare` override 改为 `AbstractTokenTable<*>`

2. **`FunctionSymbol.kt`**：
   - `LinearFunctionSymbolAdapter.prepare` override 改为 `AbstractTokenTable<*>`

3. **`If.kt`**：
   - `IfFunction.prepare` override 改为 `AbstractTokenTable<*>`

4. **`Masking.kt`**：
   - `MaskingWithPolyMaskFunction.prepare` override 改为 `AbstractTokenTable<*>`

5. **`Product.kt`**：
   - `prepare` override 改为 `AbstractTokenTable<*>`，内部 `@Suppress("UNCHECKED_CAST")` 后用 `tt.tokenList`
   - 移除 `evaluate(tokenTable: LegacyAbstractTokenTable)` override（3 个重载），由接口默认方法替代
   - 移除 companion object 中 `evaluateLinear(LegacyAbstractTokenTable)`/`evaluateLinearFromResults(LegacyAbstractTokenTable)`/`evaluateLinearFromValues(LegacyAbstractTokenTable?)` 辅助方法

### 7.2 当前阻塞：JVM Platform Declaration Clash

**根因**：`AbstractTokenTable<*>` 和 `AbstractTokenTable<Flt64>`（即 `LegacyAbstractTokenTable`）在 JVM 字节码层面 type erasure 后签名相同，因此 Kotlin 不允许在同一接口/类中同时存在这两个重载。

**受影响位置**（构建错误列表）：

| 文件 | 类 | 冲突方法 |
|------|-----|----------|
| `IntermediateSymbol.kt` | `LinearExpressionSymbol` | `evaluate(tokenTable)`, `evaluate(results, tokenTable)`, `evaluate(values, tokenTable)` |
| `IntermediateSymbol.kt` | `QuadraticExpressionSymbol` | 同上 |
| `FunctionSymbol.kt` | `LinearFunctionSymbolAdapter` | 继承 clash |
| `If.kt` | `IfFunction` | 继承 clash |
| `Masking.kt` | `MaskingWithPolyMaskFunction` | 继承 clash |
| `Product.kt` | `ProductFunction` | 继承 clash |

**修复方案**：系统性移除所有 `evaluate(tokenTable: LegacyAbstractTokenTable)` override 方法。这些 override 不再需要，因为接口的 `evaluate(tokenTable: AbstractTokenTable<*>)` 默认方法已覆盖相同逻辑（通过 unchecked cast 委托到 `evaluate(tokenList)`）。

### 7.3 仍需保留的 `LegacyAbstractTokenTable` 引用（D2/D4 再处理）

1. `shouldPrepare`/`prepareIfNotCached`/`evaluateWithCachedTokenTable` 辅助方法仍使用 `LegacyAbstractTokenTable`
2. `cacheTokenTable()` 返回 `LegacyAbstractTokenTable?`
3. `evaluateSymbol(results, tokenTable: LegacyAbstractTokenTable)` 私有方法（`LinearExpressionSymbol` 和 `QuadraticExpressionSymbol` 各一个）
4. `LinearIntermediateSymbol`/`QuadraticIntermediateSymbol` 子接口中仍可能有 `LegacyAbstractTokenTable` 签名方法
5. `Binaryzation.kt` 中 `import LegacyAbstractTokenTable`

### 7.4 下一步操作清单

1. **移除 `LinearExpressionSymbol` 的 3 个 `evaluate(tokenTable: LegacyAbstractTokenTable)` override**（约第 870/892/915 行）
2. **移除 `QuadraticExpressionSymbol` 的 3 个 `evaluate(tokenTable: LegacyAbstractTokenTable)` override**（约第 1449/1481/1514 行）
3. **验证构建通过** — 移除后 `AbstractTokenTable<*>` 默认方法会自动接管
4. **检查外部调用方** — 搜索 framework/gantt-scheduling/example 中是否有显式调用 `evaluate(tokenTable: LegacyAbstractTokenTable)` 的代码，如有需改为 `evaluate(tokenTable: AbstractTokenTable<*>)` 或依赖自动分发
5. **继续 D2** — `LinearExpressionSymbol`/`QuadraticExpressionSymbol` 完整泛型化改造

### 7.5 关键设计决策记录

1. **为什么用 `AbstractTokenTable<*>` 而不是 `AbstractTokenTable<V>`？**
   - `IntermediateSymbol<V>` 的 `V` 约束为 `V : RealNumber<V>`，但 `AbstractTokenTable<V>` 要求 `V : RealNumber<V>, V : NumberField<V>`（更严格）。`IntermediateSymbol<V>` 的 V 无法满足 `NumberField<V>` 约束，因此不能用 `AbstractTokenTable<V>`。Star projection `AbstractTokenTable<*>` 是安全的，因为 `prepare` 返回 `Flt64?`（不依赖 V），且运行时 token table 始终是 `AbstractTokenTable<Flt64>`。

2. **为什么移除 `LegacyAbstractTokenTable` 默认方法而不是移除 `AbstractTokenTable<*>` 版本？**
   - JVM type erasure 导致两者签名冲突，必须只保留一个。`AbstractTokenTable<*>` 是泛型优先的主路径，`LegacyAbstractTokenTable` 是向后兼容桥接。保留主路径、移除桥接符合 P4-2 "泛型优先" 目标。调用方传入 `LegacyAbstractTokenTable`（= `AbstractTokenTable<Flt64>`）时会自动匹配 `AbstractTokenTable<*>` 参数。

3. **`@Suppress("UNCHECKED_CAST")` 安全性**：运行时所有 `AbstractTokenTable<*>` 实例实际都是 `AbstractTokenTable<Flt64>`，unchecked cast 不会失败。这是从 Flt64 固化体系向泛型体系过渡的必要桥接模式。

---

## 8. 备注

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
Week 4: P3-4 完成 (3-5d) -> P3-5 (1-2d) -> P3-6 (1-2d)（已完成）
Week 5: P4-1（2026-04-27 已启动，Phase A2 已完成，预计 2026-05-02 ~ 2026-05-04 完成）
Week 6: P4-2（满足前置条件后启动，预计 2026-05-06 ~ 2026-05-09 完成）
Week 7: P4-3A（2026-05-12 ~ 2026-05-14，兼容保留）-> P4-3B（下一大版本窗口）
```

### 8.2 关键风险

1. **P3-2 TokenTable 泛型化**影响面最大（90 处 Flt64 引用），需谨慎推进。
2. **P3-4 模型层重排**涉及 20+ 文件物理移动，需确保 typealias 过渡不遗漏。
3. 每个阶段完成后必须验证 clean 构建，避免问题累积。
4. P3-3 与 P3-4 有依赖关系：token 拆解应在模型层重排之前完成，否则 token 相关文件会被重排两次。
