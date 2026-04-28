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

## 当前关键指标（P5 收口后）

1. `intermediate_symbol`：
   - `AbstractTokenTable<*> = 0`
   - `as AbstractTokenTable<Flt64> = 4`（全部位于 `Product.kt` solver-boundary 白名单）
2. deprecated typealias：
   - 定义数：`62`
   - 调用数（定义除外）：`0`
3. `core/model` 中 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>`：
   - 当前：`18`
   - 状态：门禁冻结基线（仅允许边界/兼容层保留）
4. `ToMathLinearPolynomial`（core+framework）：
   - 当前：`23`
   - 状态：门禁冻结基线（迁移窗口内仅减不增）
5. `@Deprecated` 存量：
   - core：`78`
   - math：`3`
   - total：`81`

---

## 验收口径与结果

1. `mvn -pl ospf-kotlin-core -am clean test`：PASS
2. `mvn -pl ospf-kotlin-framework -am clean compile`：PASS
3. `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-domain-layer-assignment-context -am clean compile`：PASS
4. `mvn -pl ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-task-compilation-context -am clean compile`：PASS
5. `mvn -pl ospf-kotlin-example -am clean compile`：PASS
6. `powershell -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`：PASS

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
