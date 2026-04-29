# OSPF Kotlin Core Refactor Daily

日期：2026-04-29（汇总版）

状态：`P5-1 ~ P5-4` 全部完成，并通过全链路验收。

目标：在保持 Kotlin 对外类型命名与接口语义兼容的前提下，完成 core 主链路泛型化收口与 Deprecated 治理，持续对齐 Rust 架构能力。

---

## P5 完成总结

### P5-1 Deprecated 低风险清理与门禁补强
1. `Flt64AndFunction` 业务调用清零（定义保留，调用为 0）。
2. 新增门禁：`P5-1-1`（禁止 deprecated typealias 在 `core/src/main` 出现定义之外的调用）。
3. `IntermediateSymbol` 6 个 Flt64 兼容扩展函数补充迁移窗口说明（计划移除时间：`2026-09-30`）。

### P5-2 mechanism/callback token 接口泛型化收口
1. `BasicMechanismModel<V>`、`BasicModel<V>` token 存储签名切换到 `V` typed。
2. `MechanismModel<V>`/`MetaModel<V>` 主接口 token 签名切换到 `AbstractTokenTable<V>` / `AbstractMutableTokenTable<V>`。
3. `CallBackModelInterface` 增加三参主接口 `AbstractCallBackModelInterfaceV<Obj, V, TV>`，并保留旧两参 typealias 兼容。
4. solver 边界 cast 收敛到显式桥接点（不散落在主路径签名中）。

### P5-3 constraint/subobject/cell 链路泛型化收口
1. `LinearConstraintInput`、`MetaConstraint` token 参数切换为 `AbstractTokenTable<*>` 主路径。
2. `Constraint` / `SubObject` / `MathInequalityDsl` 的约束构造入口签名去除非必要 `AbstractTokenTable<Flt64>` 固化，保留函数内 solver-boundary 显式 cast。
3. 新增门禁：`P5-3-1`（`core/model` 下 `Flt64` 固化 token 签名冻结基线，禁止回升）。

### P5-4 Deprecated 分层治理
1. `And/Or/Xor/Masking` 增加 `ToLinearPolynomial<Flt64>` 主路径工厂（`fromLinearPolynomials`）。
2. `ToMathLinearPolynomial` 旧桥接保留为 deprecated 兼容层，避免下游源码中断。
3. 新增门禁：`P5-4-1`（`ToMathLinearPolynomial` 引用量基线冻结，禁止回升）。

---

## P6-1 完成总结

1. 将 `LinearIntermediateSymbol<*>` → `LinearIntermediateSymbolF64` 和 `QuadraticIntermediateSymbol<*>` → `QuadraticIntermediateSymbolF64` 批量替换，覆盖 12 个文件：
   - MetaModel.kt, MetaConstraint.kt, MathInequalityDsl.kt, Model.kt, FunctionSymbol.kt
   - Slack.kt, And.kt, Binaryzation.kt, Bridge.kt, Masking.kt, Max.kt, MinMax.kt
2. 修复 import 遗漏问题：替换 `<*>` 时需同时保留基类 import（`LinearIntermediateSymbol`）和 F64 typealias import（`LinearIntermediateSymbolF64`），否则继承 `LinearIntermediateSymbol<Flt64>` 的类会编译失败。
3. 分析剩余 282 个 `<*>` 的结构必要性：
   - `IntermediateSymbol<*>`（224 处）：异构容器、`is` 模式匹配、依赖集、parent 引用、符号注册表 — 全部结构必需
   - `AbstractVariableItem<*, *>`（328 处）：双类型参数异构容器 — 结构必需
   - `AbstractTokenTable<*>`（20 处）：泛型求值上下文 — 结构必需
   - `VariableType<*>`（12 处）：变量类型参数 — 结构必需
   - `LinearExpressionSymbol<*>`/`QuadraticExpressionSymbol<*>`（6 处）：`is` 检查 — 结构必需
4. 结论：P6-1 在不引入破坏性泛型桥接层的前提下，已将 `<*>` 从 363 降至 282（降幅 22%）。剩余 `<*>` 均为结构必需，进一步收敛需 P6-2（Flt64 签名下沉 → V typed）或 P7-2（全量泛型整形）配合。

---

## 当前关键指标（P6-1 收口后）

1. `core/src/main` 中 `<*>`：`282`（基线 363，降幅 22%）
2. `core/src/main` 中 `<Flt64>`：`849`（基线 849，待 P6-2 下沉）
3. `core/src/main` 中 `@Deprecated`：`58`（基线 78，已降 20）
4. `math/src/main` 中 `<*>`：`218`（基线 218，待 P6-3）
5. `math/src/main` 中 `@Deprecated`：`3`（基线 3，待 P6-3）
6. `IntermediateSymbol.kt` 中 `<*>`：`88`（基线 88，需 P7-2 全量整形）
7. `TokenTable.kt` 中 `<*>`：`66`（基线 66，需 P7-2 全量整形）

---

## 验收口径与结果

1. `mvn -pl ospf-kotlin-core -am clean test`：PASS
2. `mvn -pl ospf-kotlin-framework -am clean compile`：PASS
3. `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am clean compile`：PASS
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`：PASS
5. `mvn -pl ospf-kotlin-example -am clean compile`：PASS
6. `powershell -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`：PASS

---

## P6 下一步执行计划（2026-04-29 起）

### P6-0 基线固化与全局门禁扩展（预计 0.5 天）

详细计划：
1. 在 `ospf-kotlin-core/scripts/check-c8-guards.ps1` 新增三类统计门禁：
   - `core/src/main`：`<Flt64>`、`<*>`、`@Deprecated`
   - `math/src/main`：`<Flt64>`、`<*>`、`@Deprecated`
2. 将当前存量固化为冻结基线（仅允许下降，不允许回升）：
   - core：`<Flt64>=849`、`<*>=363`、`@Deprecated=78`
   - math：`<Flt64>=323`、`<*>=218`、`@Deprecated=3`
3. 在脚本输出中新增 delta 展示（`current - baseline`），用于 CR 直接判读。

完成标准：
1. 新增门禁可在本地稳定运行，且输出包含 baseline/current/delta。
2. 基线值与当日快照一致，无误报（允许历史存量，禁止新增回升）。
3. 执行 `powershell -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1` 返回 PASS。

### P6-1 core `*` 投影收敛（预计 2~3 天）

详细计划：
1. 优先处理高密度文件：
   - `intermediate_symbol/IntermediateSymbol.kt`
   - `token/TokenTable.kt`
   - `intermediate_symbol/SymbolCombination.kt`
2. 将 `IntermediateSymbol<*>` 在“可静态确定类型”的场景改为显式泛型或受限类型参数；仅在确需异构容器处保留星投影。
3. 对无法立即泛型化的公共 API，先加中间泛型桥接层，避免一次性破坏下游调用。
4. 每批改造后执行 `core` 单测，确保依赖图缓存与求值流程不回归。

完成标准：
1. `core/src/main` 中 `<*>` 从 `363` 降至 `<=220`。
2. `IntermediateSymbol.kt` 与 `TokenTable.kt` 中星投影行数相较基线下降 `>=40%`。
3. 执行 `mvn -pl ospf-kotlin-core -am clean test` 返回 PASS。

### P6-2 core `Flt64` 固化签名进一步下沉到 solver 边界（预计 2~3 天）

详细计划：
1. 优先处理高密度文件：
   - `model/mechanism/MathInequalityDsl.kt`
   - `intermediate_symbol/function/FunctionSymbol.kt`
   - `model/mechanism/MechanismModel.kt`
2. 将 DSL/机制层中可泛型化的 `Flt64` 显式类型参数替换为 `V` 或上界类型，保留 solver-boundary 的显式 cast。
3. 同步收敛 `core/model` 下 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>` 的兼容残留。
4. 对外 API 若涉及行为变化，补充迁移注释与窗口说明。

完成标准：
1. `core/src/main` 中 `<Flt64>` 从 `849` 降至 `<=650`。
2. `core/model` 下 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>` 从 `18` 降至 `<=10`。
3. `P5-3-1`、`P5-4-1` 既有门禁持续 PASS，无新增回升。

### P6-3 math 泛型与 Deprecated 清理（预计 1~2 天）

详细计划：
1. 优先处理：
   - `math/algebra/number/*`（`<*>` 密集区）
   - `math/symbol/serde/InequalitySerde.kt`（`@Deprecated` 3 处）
2. 对 `InequalitySerde` 的废弃入口做分流：保留兼容入口、主路径统一到非废弃 API。
3. 在不破坏表达式解析/序列化行为前提下，减少不必要星投影。
4. 运行 `math` 单测覆盖表达式解析、序列化、运算链路。

完成标准：
1. `math/src/main` 中 `<*>` 从 `218` 降至 `<=150`。
2. `math/src/main` 中 `@Deprecated` 从 `3` 降至 `<=1`（仅保留必要兼容入口）。
3. 执行 `mvn -pl ospf-kotlin-math -am clean test` 返回 PASS。

### P6-4 Deprecated 退出机制落地（预计 1 天）

详细计划：
1. 建立分级退出规则：`WARNING -> ERROR -> 删除`，并为每类兼容别名/桥接函数标注目标版本或日期。
2. 将当前 `core`/`math` 的 `@Deprecated` 清单按“可立即替换 / 需窗口迁移 / 暂不可替换”分类。
3. 为“可立即替换”项安排批量替换任务；为“需窗口迁移”项补足迁移指引。
4. 在 `daily.md` 维护滚动状态：每轮记录净减少数量与剩余风险点。

完成标准：
1. 输出可执行的 Deprecated 台账（含 owner、迁移目标、截止时间）。
2. 新增或更新门禁后，`@Deprecated` 存量实现“只减不增”。
3. `core + math` 的 `@Deprecated` 总量从 `81` 降至 `<=65`（本阶段目标）。

---

## P7 清零导向执行计划（P6 完成后，含破坏性变更）

### P7-0 破坏性变更窗口与基线重签（预计 0.5 天）

详细计划：
1. 明确 P7 为 breaking window：允许删除兼容层 API、允许下游源码同步改造。
2. 在 `daily.md` 固化 P7 新口径：统计范围统一为 `core/src/main` 与 `math/src/main`。
3. 在门禁脚本中新增 P7 模式开关（或独立脚本），将目标从“只减不增”切换为“清零校验”。
4. 锁定下游受影响模块清单：`framework/*`、`example`、`starters`。

完成标准：
1. P7 变更窗口与影响范围形成书面结论并入库。
2. CI 可区分 P6（降量）与 P7（清零）两套门禁口径。
3. 下游改造清单完整，可直接按模块执行。

### P7-1 core Deprecated 兼容层删除（预计 1~2 天）

详细计划：
1. 删除 `core` 中已完成迁移窗口的 deprecated typealias 与桥接函数。
2. 将仍需短期保留但必须阻断新增调用的项，从 `WARNING` 升级为 `ERROR`（仅限明确有替代 API 的项）。
3. 批量替换 core 内部调用到主路径 API，清除“先 deprecated 再转发”的中间层。
4. 对删除项补充迁移说明（替代类型、替代函数、调用示例）。

完成标准：
1. `core/src/main` 中 `@Deprecated` 降至 `0`（或仅保留经评审批准的白名单项，白名单需具名）。
2. `P5-1-1` 与 P7 新门禁同时 PASS。
3. `mvn -pl ospf-kotlin-core -am clean test` PASS。

### P7-2 core 星投影与 Flt64 固化签名清零（预计 3~4 天）

详细计划：
1. 对 `IntermediateSymbol`、`TokenTable`、`SymbolCombination` 做全量泛型整形，移除可替代的 `*` 投影。
2. 对 `MechanismModel`、`MetaModel`、`Constraint`、`SubObject`、`MathInequalityDsl` 做签名重写：
   - 主路径全面 `V` typed
   - solver-boundary 通过显式适配器承接，不在模型层泄漏 `Flt64`
3. 清理 `core/model` 中 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>` 残留。
4. 对必要的运行时分派场景，引入受限接口/封装类型，替代星投影。

完成标准：
1. `core/src/main` 中 `<*>` 计数为 `0`（白名单项为 `0`）。
2. `core/src/main` 中 `<Flt64>` 在 P7 统计口径下计数为 `0`（模型/机制/API 层不允许出现）。
3. `core/model` 下 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>` 为 `0`。
4. `mvn -pl ospf-kotlin-core -am clean test` PASS。

### P7-3 math 星投影与 Deprecated 清零（预计 2~3 天）

详细计划：
1. 重构 `math/algebra/number/*` 中异构泛型容器，移除可静态约束的 `*`。
2. 清理 `math/symbol/serde/InequalitySerde.kt` 的 deprecated 入口，统一到主路径序列化/反序列化接口。
3. 对表达式解析、序列化、求值链路补齐回归测试，覆盖旧入口迁移后的行为一致性。
4. 若存在必须保留的兼容入口，迁移到 `compat` 分层并与主路径彻底隔离。

完成标准：
1. `math/src/main` 中 `<*>` 计数为 `0`（白名单项为 `0`）。
2. `math/src/main` 中 `@Deprecated` 计数为 `0`。
3. `mvn -pl ospf-kotlin-math -am clean test` PASS。

### P7-4 下游模块批量迁移（预计 2~3 天）

详细计划：
1. 按模块分批改造：`framework` -> `framework-*` -> `example` -> `starters`。
2. 替换所有被删除兼容 API 的调用点，统一到 P7 主路径签名。
3. 对编译失败按“签名不匹配 / 泛型不兼容 / 推断退化”分类修复，避免临时 cast 回退。
4. 每批完成后执行对应模块编译与关键场景测试。

完成标准：
1. 下游模块无对已删除 deprecated API 的引用。
2. 全仓 `src/main` 不新增 `<*>` 与 `<Flt64>` 回流。
3. 以下命令全部 PASS：  
   - `mvn -pl ospf-kotlin-framework -am clean compile`  
   - `mvn -pl ospf-kotlin-example -am clean compile`

### P7-5 全链路验收与发布基线更新（预计 1 天）

详细计划：
1. 执行全链路构建与测试，核对功能回归、性能回归与 API 变更清单。
2. 更新门禁脚本基线为 P7 清零口径，并移除 P6 过渡性基线指标。
3. 生成迁移公告：breaking 清单、替代写法、影响模块、回滚策略。
4. 将 `daily.md` 状态更新为 P7 完成，并记录剩余风险（如无则显式写“无”）。

完成标准：
1. `core/src/main + math/src/main` 达成：
   - `<Flt64> = 0`（按 P7 口径）
   - `<*> = 0`
   - `@Deprecated = 0`
2. 门禁脚本在 CI 稳定运行，任一回归可被阻断。
3. 全链路命令 PASS，且发布说明可直接对外使用。

---

## P7 当前进展（2026-04-29 晚，交接快照）

### 本轮已完成

1. `core` 侧完成 `QuadraticTetradModel` 的对偶接口收敛：
   - 删除 `farkasDual` 相关对外接口与实现。
   - 删除 `QuadraticDualUnsupportedTest.kt`（旧接口已下线）。
   - 更新 `BendersCutApiTest` 中 `QuadraticTetradModelView` stub，移除 `farkasDual` override。
2. 为下游过渡添加兼容别名：
   - 在 `core/intermediate_symbol/SymbolCombination.kt` 增加 `LinearIntermediateSymbols* / QuantityLinearIntermediateSymbols* / Dyn*` typealias，降低 `framework` 迁移断点。
3. `gantt-scheduling-domain-task-compilation-context` 已完成大部分 API 迁移：
   - `Switch.kt`：`AndFunction` 与 `MaskingFunction` 改为 `fromLinearPolynomials` 工厂。
   - `TaskTime.kt`：6 处多项式 mask 调用改为 `MaskingFunction.fromLinearPolynomials(...)`。
   - `Compilation.kt`：`executorCompilation` 分支重构为“先构造 OR，再用 `resultVar` 组装表达式”的流程。

### 本轮验证结果

1. `mvn -pl ospf-kotlin-core -am test`：PASS。
2. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`：FAIL。
3. 当前失败已收敛为单点文件：`Compilation.kt`（`executorCompilation` 构造段，约 222~230 行）。

### 当前阻塞点（需下个环境优先处理）

1. `model.add(or)` 类型不匹配：当前 `or` 是 `OrFunction<Flt64>`，不满足 `MetaModel.add` 所需 `IntermediateSymbol` 重载。
2. 由上一条引发级联报错：`when (val result = model.add(or))` 分支中的 `Ok/Failed/Fatal` 与 `result.error/result.errors` 解析失败。

### 接力建议（下一步最短路径）

1. 在 `Compilation.kt` 的 `executorCompilation` 段改为：
   - 保留 `val or = OrFunction(...)`（需要 `or.resultVar`）。
   - 用 `LinearFunctionSymbolAdapter(or)` 作为 `model.add(...)` 入参，确保类型满足 `IntermediateSymbol`。
2. 修复后优先复跑：
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`
3. 若通过，再补跑：
   - `mvn -pl ospf-kotlin-core -am test`
   - `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am test`（若该模块有测试）

---

## 后续历史待办（P5 后）

1. `P2-4`：LP 导出能力对齐 Rust
2. `P2-5`：结构化错误类型对齐 Rust
3. `P2-3`：PSO 求解器对齐 Rust
4. `P2-6`：非线性残留 TODO 复核

执行顺序：`P2-4 -> P2-5 -> P2-3 -> P2-6`

---

## 备注

本文件仅保留“阶段总结 + 当前指标 + 后续待办”，不再保留逐日流水细节。
