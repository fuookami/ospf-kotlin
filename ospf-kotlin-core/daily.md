# OSPF Kotlin Core Refactor Daily

日期：2026-04-28（汇总版）

状态：P4-5 已完成并通过全链路验收；当前进入 P5 收口阶段。

目标：在保持 Kotlin 对外类型命名与接口语义兼容的前提下，完成 core 主链路泛型化收口和 Deprecated 治理，并持续对齐 Rust 架构能力。

---

## 已完成事项总结（截至 2026-04-28）

1. 阶段完成：`C0~C8`、`P3-0~P3-6`、`P4-1~P4-5` 全部完成。
2. 主链路收口：`IntermediateSymbol<V>` 的 `prepare/evaluate/evaluateFromTokens` 已全部切换到 `V` typed primary path（`AbstractTokenTable<V>` + `IntoValue<V>`）。
3. 关键指标收敛：
   - `intermediate_symbol` 包 `AbstractTokenTable<*>`：`21 -> 0`
   - `intermediate_symbol` 包 `as AbstractTokenTable<Flt64>`：`10 -> 0`（`Product.kt` 4 处 solver-boundary 豁免）
4. 兼容层处理：保留 6 个 `@Deprecated` 的 Flt64 扩展函数作为过渡兼容层。
5. 门禁升级：`P4-4-1/P4-4-2` 零容忍策略已启用，并新增 `P4-5-1` 门禁。
6. 全链路验收：core test、framework compile、bpp3d compile、gantt compile、example compile、guard script 全部 PASS。

---

## 未完成事项（当前）

1. `model.mechanism` / `model.callback` 仍有 token 接口固化为 `Flt64`（`AbstractTokenTable<Flt64>` / `AbstractMutableTokenTable<Flt64>`）。
2. `Constraint` / `SubObject` / `Cell` / `LinearConstraintInput` 链路仍存在 `Flt64` 早固化和 cast 存量。
3. Deprecated 存量仍高：全仓 `@Deprecated` 77 处（core 74 / math 3）。
4. Deprecated typealias 定义 61 处；现存调用点 1 个（`Flt64AndFunction`，2 处调用）。
5. `ToMathLinearPolynomial` 现存调用约 17 处（core/framework）。
6. `Product.kt` 4 处 solver-boundary cast 仍需白名单门禁持续守护。

---

## P5 计划（新增）

### P5-1 Deprecated 低风险清理与门禁补强（P0）
- 目标：先清理可机械替换的低风险兼容层调用。
- 范围：替换 `Flt64AndFunction` 调用；新增“deprecated typealias 禁止新增调用”门禁；补充 `IntermediateSymbol` 6 个 deprecated 扩展函数迁移窗口说明。
- 完成标准：deprecated typealias 调用点降到 0（定义可暂保留），门禁与编译全 PASS，语义不变。

### P5-2 mechanism/callback token 接口泛型化收口（P1）
- 目标：把 `model.mechanism` / `model.callback` token 接口从 `Flt64` 固化迁移到 `V` 主路径。
- 范围：迁移 `tokens` 签名到 `AbstractTokenTable<V>` / `AbstractMutableTokenTable<V>`，并保留可追踪的 `Flt64` 边界桥接。
- 完成标准：主接口不再固化 `Flt64`，全链路编译与回归通过。

### P5-3 constraint/subobject/cell 链路泛型化收口（P2）
- 目标：减少非边界 `Flt64` 固化点和非必要 cast。
- 范围：主评估路径迁移到 `V` typed primary；solver-boundary `Flt64` 显式注释；复核 `Product.kt` 4 处豁免 cast 的内聚可能。
- 完成标准：非边界固化点显著下降，`UNCHECKED_CAST`/边界 cast 收敛到白名单，约束求值回归通过。

### P5-4 Deprecated 分层治理（P3）
- 目标：将 Deprecated 库存转为可发布治理状态。
- 范围：按“长期保留 / 下版本升级 ERROR / 可删除”分层；处理 `FunctionSymbol.kt`、`SymbolCombination.kt` 的 deprecated typealias；收敛 `ToMathLinearPolynomial` 存量调用。
- 完成标准：每条 Deprecated 有归属与时间点，调用面可量化下降，不破坏公开 API 兼容承诺。

---

## 后续历史待办（P5 后执行）

1. `P2-4`：LP 导出能力对齐 Rust。  
2. `P2-5`：结构化错误类型对齐 Rust。  
3. `P2-3`：PSO 求解器对齐 Rust。  
4. `P2-6`：非线性残留 TODO 复核。  

执行顺序：`P5-1 -> P5-2 -> P5-3 -> P5-4 -> P2-4/P2-5/P2-3/P2-6`

---

## 验收口径（保持不变）

1. `mvn -pl ospf-kotlin-core -am clean test`
2. `mvn -pl ospf-kotlin-framework -am clean compile`
3. `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am clean compile`
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`
5. `mvn -pl ospf-kotlin-example -am clean compile`
6. `pwsh.exe -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`

---

## 备注

本文件仅保留“阶段总结 + 下一步计划”；原逐日细节与过程性流水已删除，不再展开。
