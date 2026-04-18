# OSPF Kotlin Core Refactor Daily

日期：2026-04-18（整理归档）

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
| C6 | 删库：Polynomial.kt、Expression.kt 物理删除，引用迁移至 math.symbol | 2026-04-17 |
| C7 | 阶段化回归：run-c7-regression.ps1 脚本 | 2026-04-17 |
| C8 | 门禁：Abstract*Polynomial 拦截、.cells 拦截、Double 固化拦截 | 2026-04-17 |

### P0~P2 待办项完成

| 项目 | 内容 | 完成方式 |
|------|------|----------|
| P0-1 | core 对外符号类型清退 | C1 完成 |
| P0-2 | MetaModel 主链路切换到 math.symbol | C1 完成 |
| P0-5 | 缓存机制上收到 MetaModel contexts | C3 + C6 完成 |
| P0-6 | 对外接口兼容保持 | ApiCompatibilityTest.kt（8 个测试） |
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
- `scripts/check-c8-guards.ps1` — 增量门禁脚本
- `scripts/run-c7-regression.ps1` — 阶段化回归脚本
- `.github/workflows/core-refactor-guards.yml` — CI 门禁

### 验证状态

- `mvn -pl ospf-kotlin-core -am test`：111 tests, 0 failures
- `check-c8-guards.ps1`：全部通过
- C2 第二段延期（见下方）

---

## 延期项

### C2 第二段：数值内核泛型化专项

**状态**：设计完成，实现延期

**理由**：V 在所有 `*Of<V>` 类型中为 phantom（从未实际使用，内部均为 Flt64）。真正泛型化需改 math 库、sealed interface、Token/Cell/Constraint 内部字段，预估 15-20h 高风险工作，无当前使用场景。

**何时恢复**：出现需要 V ≠ Flt64 的具体使用场景时重新评估。

**已完成设计产物**：
- `docs/refactor-baseline/generic-boundary.md`
- C2-2.1~C2-2.9 声明层骨架（接口泛型、typealias 兼容）
- C2-2.10 全量测试通过

---

## 未完成项

### 1. QuadraticTetradModel dual/farkasDual（P1）

- `QuadraticTetradModel.kt` 的 `dual()` 和 `farkasDual()` 仍为 `TODO("not implemented yet")`
- Rust 无此公共 API，属于 Kotlin 扩展能力
- 需决策：实现为扩展能力，或显式标注为不支持

### 2. MonomialCell 非线性分支（P2）

- `Monomial.kt` 的 `MonomialCell.invoke(..., category)` 对非线性分支仍为 `TODO("NOT IMPLEMENT YET")`
- 影响后续类别扩展的一致性

### 3. FunctionSymbol 上下文一体化（P1）

- Rust 统一 `register_auxiliary_tokens` + `evaluate_from_tokens`，且 `add_symbol` 触发 context 重绑
- Kotlin 符号注册与求值路径仍分散在 registration scope/prepare 链路，尚未完全对齐为单一钩子模型

### 4. 主链路语义等价回归（P1）

- `Abstract*Polynomial` 到 `math.symbol` 切流后，缺少成套"旧入口 vs 新入口"语义等价回归
- 泛型 V 与 plugin 边界 Double 转换缺少专项回归覆盖

### 5. 泛型化链路闭环（P0，与 C2 第二段同）

- core 主链路仍存在 Flt64 固化路径，未完全收敛到"plugin 边界才转 Double"
- 与 C2 第二段延期项合并，恢复时一并处理

---

## 转换边界标记

- `LinearTriadModel.invoke()` — MechanismModel\<V\> 到 IntermediateModel 的 V→Flt64 实例化入口
- `QuadraticTetradModel.invoke()` — 同上，二次路径的 V→Flt64 实例化入口

---

## 架构定版硬约束

1. core 不再保留任何"对外可见"的符号运算类型（单项式/多项式/不等式）。
2. core 对外只保留两类建模实体：变量体系（variable）、用于封装约束生成的 functional symbol。
3. MetaModel 主链路全部使用 math.symbol 的单项式、多项式、不等式与 relation。
4. MechanismModel 保持泛型化 \<V\>，内部使用 Token + Cell 体系承载约束。IntermediateModel 作为求解器标准形式，允许直接使用 f64。
5. 旧 core 单项式/多项式上的缓存机制全部上收为 MetaModel 上下文（flatten/value/range context）。
6. 对外使用者接口"基本不变"：历史高频入口需保留，签名调整必须提供 Deprecated + ReplaceWith 兼容通道。
7. 泛型化边界固定（对齐 Rust）：
   - `MetaModel -> MechanismModel` 必须保持泛型值类型 `V` 贯通。
   - `IntermediateModel` 作为求解器标准形式，直接使用 f64。
   - 在 `MechanismModel -> IntermediateModel` 转换时进行 `V -> f64` 实例化。
   - 禁止在 MetaModel 和 MechanismModel 主链路提前固化为 `Flt64/Double`。

---

## Rust 对齐基线

- 模块结构：model / solver / symbol / token / variable（与 Kotlin 一致）
- 关键能力：IntermediateSymbol trait、FunctionSymbol、symbol_dependencies、MetaModel→MechanismModel→Triad/Tetrad 链路、线性 dual/farkas、线性+二次 Benders cut、SolveOptions + IIS
- 口径注意：Rust quadratic_tetrad_model.rs 无 to_dual()/to_farkas_dual() 公共 API

---

## 验收命令

1. `mvn -pl ospf-kotlin-core -am test`
2. `mvn compile -pl ospf-kotlin-framework -am`
3. `powershell -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1`
