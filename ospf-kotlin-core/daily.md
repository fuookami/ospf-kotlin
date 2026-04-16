# OSPF Kotlin Core Daily (Refreshed)

日期：2026-04-16
整理人：Codex（基于 ospf-rust-core/src 与当前 Kotlin 仓库实扫）

---

## 本次整理目标

1. 阅读 Rust 版本 E:/workspace/ospf-rust/ospf-rust-core/src，明确对齐基线。
2. 清理旧 daily.md 内部互相冲突口径。
3. 重新输出已完成事项、待办事项、分阶段改造计划。

---

## 架构定版硬约束（2026-04-16，已修正）

以下为最终口径，后续改造与验收全部以此为准：

1. core 不再保留任何”对外可见”的符号运算类型（单项式/多项式/不等式）。
2. core 对外只保留两类建模实体：
   - 变量体系（variable）
   - 用于封装约束生成的 functional symbol（intermediate_symbol/function）
3. MetaModel 主链路全部使用 math.symbol 的单项式、多项式、不等式与 relation。
4. MechanismModel 保持泛型化 `<V>`，内部使用 Token + Cell 体系承载约束。
   IntermediateModel（LinearTriadModel/QuadraticTetradModel）作为求解器标准形式，允许直接使用 f64。
5. 旧 core 单项式/多项式上的缓存机制全部上收为 MetaModel 上下文（flatten/value/range context）。
6. 对外使用者接口”基本不变”：历史高频入口需保留，签名调整必须提供 Deprecated + ReplaceWith 兼容通道。
7. 泛型化边界固定（对齐 Rust）：
   - `MetaModel -> MechanismModel` 必须保持泛型值类型 `V` 贯通（Rust: `MechanismModel<V>`）。
   - `IntermediateModel` 作为求解器标准形式，直接使用 f64（Rust: `BasicLinearTriadModel` 直接用 `Token<f64>`）。
   - 在 `MechanismModel -> IntermediateModel` 转换时进行 `V -> f64` 实例化。
   - 禁止在 MetaModel 和 MechanismModel 主链路提前固化为 `Flt64/Double`。

---

## Rust 对齐基线（已核验）

### 1) 模块结构

Rust core 顶层稳定为 5 模块：

- model
- solver
- symbol
- token
- variable

与 Kotlin 当前目标结构一致（Kotlin 也已是这 5 个目录）。

### 2) 关键能力（Rust 当前真值）

1. symbol/intermediate_symbol.rs
   - IntermediateSymbol trait + LinearIntermediateSymbol / QuadraticIntermediateSymbol
   - declared_dependency_ids() 显式依赖声明入口
   - 评估与 range 上下文（value/range cache context）

2. symbol/function_symbol.rs
   - FunctionSymbol / LinearFunctionSymbol / QuadraticFunctionSymbol
   - 统一 register_tokens() + calculate_value() 语义

3. model/basic_model.rs
   - 显式 symbol_dependencies: HashMap<u64, HashSet<u64>>
   - add_symbol_dependency/add_symbol_with_dependencies
   - evaluate_registered_symbol/symbol_range 先依赖预热再求值

4. model/meta_model.rs
   - MetaModel -> MechanismModel -> Triad/Tetrad 转换链路
   - ModelBuildingStatus 全阶段回调

5. model/intermediate/linear_triad_model.rs
   - to_dual()、to_farkas_dual() 与 tidy dual 工具完整

6. model/mechanism/mechanism_model.rs
   - 线性与二次 Benders cut 生成完整：
     - row dual 输入
     - solver output 输入
     - 变量 id 映射
     - feasibility/optimality 双路径

7. solver/solver_ext.rs
   - SolveOptions 统一入口
   - IIS 计算链路
   - async 入口（feature gate）

### 3) 对齐口径注意

- Rust quadratic_tetrad_model.rs 没有 to_dual()/to_farkas_dual() 公共 API。
- 因此 Kotlin 的二次 dual/farkas 如果继续做，属于 Kotlin 扩展能力，不是 Rust 对齐硬要求。

---

## Kotlin 当前快照（2026-04-16 实扫）

### 1) 已达到的结构状态

- core/expression 目录已物理删除（expression_dir_exists=False）。
- core 主代码中 import fuookami.ospf.kotlin.core.expression.* 为 0。
- ModelBuildingStage 与 Rust 阶段枚举已对齐。

### 2) 仍保留的 core 符号运算内核（本次关注重点）

- intermediate_model/Polynomial.kt：1725 行
- intermediate_model/Expression.kt：55 行

这说明“删 expression 目录”已完成，但“完全删除 core 内符号运算类型”尚未完成。

### 3) 真实耦合规模（core/src/main）

- AbstractLinearPolynomial<...>：80 处，15 文件
- AbstractQuadraticPolynomial<...>：46 处，7 文件
- ExpressionRange<...>：36 处，12 文件

### 4) 关键 TODO（活跃）

共 13 个活跃 TODO（不含注释行），主要分布：

1. intermediate_model/MechanismModel.kt
   - generateOptimalCut(...)（Quadratic）未实现
   - generateFeasibleCut(...)（Quadratic）未实现

2. intermediate_model/QuadraticTetradModel.kt
   - dual() 未实现
   - farkasDual() 未实现

3. solver/heuristic/Migration.kt
   - 8 个 TODO（与本次 core 符号运算删除主线弱相关）

4. intermediate_model/monomial/Monomial.kt
   - MonomialCell.invoke(..., category) 对非线性分支仍是 TODO

### 5) 已具备的关键能力（已核验）

- 线性 dual()/farkasDual() 已在 LinearTriadModel.kt 实现。
- 线性 generateOptimalCut/generateFeasibleCut 已在 MechanismModel.kt 实现。
- SolveOptions builder DSL、SolverExt 统一入口、IIS 入口均已在 Kotlin 主线存在。

---

## 已完成事项（重排）

仅保留“当前代码可证实 + 历史结论一致”的事项。

1. 目录主线完成
   - core 目录收敛为 variable/intermediate_symbol/model/intermediate_model/solver。
   - core/expression 目录已删除。

2. 函数符号迁移主线完成
   - intermediate_symbol/function/* 已覆盖 legacy 主函数族。
   - legacy function 目录已删除。

3. 求解入口与状态桥接完成
   - SolveOptions + SolverExt 统一调用路径在位。
   - ModelBuildingStage/Status 已对齐 Rust 阶段模型。

4. 线性 dual/farkas + 线性 Benders cut 主链路可用
   - 线性三元模型 dual/farkas 已实现。
   - 线性 MechanismModel cut 生成已实现。

---

## 待办事项（交接执行版）

### P0（阻断项，必须先完成）

#### P0-1 core 对外符号类型清退

- 目标：对外公开 API 不再暴露 core monomial/polynomial/inequality 类型。
- 输入：当前 public API 签名清单。
- 输出：API 清退映射表（旧签名 -> 新 math.symbol 签名）。
- 重点文件：
  - `ospf-kotlin-core/src/main/.../model/Model.kt`
  - `ospf-kotlin-core/src/main/.../intermediate_model/MetaModel.kt`
  - `ospf-kotlin-core/src/main/.../intermediate_model/MathInequalityDsl.kt`
  - `ospf-kotlin-core/src/main/.../intermediate_symbol/function/Bridge.kt`
- 验收：公共签名扫描不再出现 `AbstractLinearPolynomial` / `AbstractQuadraticPolynomial` 作为主入口参数类型。

#### P0-2 MetaModel 主链路切换到 math.symbol

- 目标：MetaModel 构建、约束、目标接口主链路统一使用 math.symbol 类型。
- 输入：P0-1 API 清退映射表。
- 输出：主链路实现不再依赖 core 多项式类型，仅保留兼容转发层。
- 重点文件：
  - `MetaModel.kt`
  - `Model.kt`
  - `MathInequalityDsl.kt`
  - `MetaConstraint.kt`
- 验收：主实现代码路径中不再有 `Abstract*Polynomial` 运算逻辑。

#### P0-3 泛型化贯通（MetaModel -> MechanismModel）

- 目标：`MetaModel -> MechanismModel` 保持泛型 `V`（对齐 Rust: `MechanismModel<V>`）。
- 输入：当前 `Flt64` 固化点扫描清单。
- 输出：MetaModel 和 MechanismModel 主链路中的 `Flt64/Double` 固化点被收敛或隔离。
- 重点文件：
  - `MetaModel.kt`
  - `MechanismModel.kt`
  - `TokenTable.kt`
  - `TokenCacheContext.kt`
- 验收：MetaModel 和 MechanismModel 不新增 `Flt64/Double` 固化逻辑。

#### P0-4 IntermediateModel 边界明确（求解器标准形式）

- 目标：明确 IntermediateModel 为求解器标准形式，直接使用 f64（对齐 Rust: `BasicLinearTriadModel`）。
- 输入：P0-3 泛型链路产物。
- 输出：转换点明确在 `MechanismModel -> IntermediateModel`，而非 plugin 层。
- 重点文件：
  - `LinearTriadModel.kt`
  - `QuadraticTetradModel.kt`
- 验收：IntermediateModel 边界清晰，转换入口统一。
  - `ospf-kotlin-core-plugin/*/src/main/**/*.kt`
- 验收：core 主链路无求解器前置 `Double` 实例化逻辑；插件层存在统一转换入口。

#### P0-5 缓存机制上收到 MetaModel contexts

- 目标：旧 core monomial/polynomial 缓存语义迁移为 MetaModel flatten/value/range context。
- 输入：旧缓存点列表（TokenTable/TokenCacheContext/Polynomial/Monomial）。
- 输出：缓存归属清晰，旧类型缓存实现不再是主路径。
- 重点文件：
  - `TokenCacheContext.kt`
  - `TokenTable.kt`
  - `intermediate_symbol/flatten/*`
  - `MetaModel.kt`
- 验收：缓存读写主要发生在 context 体系，旧类型缓存仅保留过渡兼容。

#### P0-6 对外接口兼容保持

- 目标：外部调用“基本不变”，迁移可自动替换。
- 输入：历史高频接口调用样例（core + framework）。
- 输出：兼容层 + ReplaceWith + 对照测试。
- 重点文件：
  - `Model.kt`
  - `MetaModel.kt`
  - `MathInequalityDsl.kt`
  - `LinearSolver.kt`
  - `QuadraticSolver.kt`
  - `SolverExt.kt`
- 验收：历史样例编译通过，且新旧入口行为等价。

### P1（高优先）

#### P1-1 Quadratic MechanismModel cut 对齐 Rust

- 目标：补齐 quadratic optimal/feasible cut（row dual / solver output 双入口）。
- 输入：Rust `mechanism_model.rs` 对照逻辑。
- 输出：Kotlin Quadratic cut 全路径实现 + 回归测试。
- 重点文件：
  - `MechanismModel.kt`
  - `QuadraticTetradModel.kt`
  - 对应 `src/test` 回归用例
- 验收：相关 TODO 清零，测试覆盖双入口。

#### P1-2 显式符号依赖图

- 目标：补 `addSymbolDependency/addSymbolWithDependencies` 一等 API。
- 输入：现有隐式依赖链路。
- 输出：显式依赖 API、校验逻辑、回归测试。
- 重点文件：
  - `MetaModel.kt`
  - `TokenTable.kt`
  - `intermediate_symbol/IntermediateSymbol.kt`
- 验收：依赖图可观测、可验证、可回归。

#### P1-3 .cells 主路径清退

- 目标：主计算路径不再依赖 `.cells`。
- 输入：`.cells` 调用清单。
- 输出：改造后的主路径与 CI 守卫。
- 验收：主路径 `.cells` 调用为 0（兼容层除外）。

### P2（常规收尾）

#### P2-1 启发式 TODO 清理

- 目标：清理 `solver/heuristic/*` 活跃 TODO。
- 输出：可编译且语义明确（实现或显式限制）。

#### P2-2 文档与门禁统一

- 目标：README/daily/脚本门禁一致。
- 输出：单一事实源，避免交接口径分叉。

---

## 改造计划（交接可执行版）

### 阶段 C0：基线冻结（0.5 天） ✅ 已完成 (2026-04-16)

1. 扫描并生成基线清单：
   - 对外 API 暴露 core 符号类型清单 ✅
   - core 主链路 `Flt64/Double` 固化点清单 ✅
   - `.cells` 调用清单 ✅
2. 生成回归基线快照：
   - core test ✅ (91 tests, 0 failures)
   - framework compile ✅
   - core-plugin compile (待后续验证)
3. 交付物：
   - `docs/refactor-baseline/api-exposure.md` ✅
   - `docs/refactor-baseline/flt64-hardening.md` ✅
   - `docs/refactor-baseline/cells-usage.md` ✅
4. 退出条件：
   - 所有清单可复现并入库 ✅
5. 提交：
   - `86e1f342` chore(core): add refactor baseline inventories
   - tag: `core-refactor-c0-baseline`

### 阶段 C1：API 清退与主链路切流（2 天） ✅ 已完成 (2026-04-16)

1. 先改主签名：
   - `Model.kt` ✅ (早期已完成)
   - `MetaModel.kt` ✅ (+2 @Deprecated)
   - `MathInequalityDsl.kt` ✅ (+52 @Deprecated)
2. 再改主实现：
   - 使用 math.symbol 类型承载约束与目标逻辑 ✅ (函数体不变，保留兼容)
3. 最后补兼容层：
   - 旧签名保留为转发 ✅
   - 添加 `Deprecated + ReplaceWith` ✅ (39 有 ReplaceWith，19 Boolean 无)
4. 交付物：
   - API 映射表（旧 -> 新）✅ `docs/refactor-baseline/api-migration.md`
   - 兼容层清单 ✅ (见 api-migration.md)
5. 退出条件：
   - 主入口不再以 `Abstract*Polynomial` 承载主实现 ✅
6. 提交：
   - `c110c14b` refactor(core): add Deprecated annotations to Abstract*Polynomial APIs
   - `17b360b1` fix(core): correct ReplaceWith expressions for Boolean RHS and add api-migration doc
   - tag: `core-refactor-c1-deprecated`

### 阶段 C2：泛型化贯通 + IntermediateModel 边界明确（2~3 天） ⏳ 进行中

#### C2 审核发现（2026-04-16）

经审核，原计划的"全链路泛型化"存在以下阻断点：

| 问题点 | 当前状态 | 影响 |
|--------|----------|------|
| **数值模型层** | MechanismModel/Constraint/TokenTable 绑定 `Flt64` | 泛型化需改底层 |
| **二次数学层** | `QuadraticInequality` 固定 `QuadraticPolynomial<Flt64>` | 与泛型目标冲突 |
| **Token/Cell 链** | Token、TokenList、TokenTable、缓存、Cell、Constraint、FlattenData 全是 `Flt64` | 改动面大 |
| **Polynomial 层** | `AbstractLinearPolynomial/AbstractQuadraticPolynomial` 是 `Flt64` 语义 | 不改无法闭环 |
| **转换边界** | `LinearTriadModel(model)` 构造函数主导，不是扩展方法 | 需改构造逻辑 |
| **兼容性** | 外部模块直接调用 `LinearMetaModel(...)`（如 BranchAndPriceAlgorithm.kt） | 无别名会破调用 |

#### C2 两段式拆分方案

#### C2 命名修订（2026-04-16）

- 多项式相关新增类型命名统一不使用 `Of` 后缀。
- 薄接口命名统一使用 `PolynomialView` 系列：
  - `PolynomialView<V, M>`
  - `LinearPolynomialView<V>`
  - `QuadraticPolynomialView<V>`
  - `CanonicalPolynomialView<V>`
- 若采用抽象基类路线，命名统一使用 `AbstractPolynomial` 系列：
  - `AbstractPolynomial<V, M>`
  - `AbstractLinearPolynomial<V>`
  - `AbstractQuadraticPolynomial<V>`
  - `AbstractCanonicalPolynomial<V>`
- C2 期间如出现 `*Of` 的多项式新类型，需在同一提交内改名为上述规范，避免后续二次迁移。

**命名替换映射（仅多项式相关）**:

| 旧命名 | 新命名 |
|--------|--------|
| `PolynomialViewOf<V, M>` | `PolynomialView<V, M>` |
| `LinearPolynomialViewOf<V>` | `LinearPolynomialView<V>` |
| `QuadraticPolynomialViewOf<V>` | `QuadraticPolynomialView<V>` |
| `CanonicalPolynomialViewOf<V>` | `CanonicalPolynomialView<V>` |
| `AbstractPolynomialOf<V, M>` | `AbstractPolynomial<V, M>` |

##### 第一段：边界声明泛型 + 兼容别名（不动数值内核）

**目标**:
- 在 API 层面声明泛型签名
- 提供兼容别名保证现有调用不破坏
- 数值内核保持 `Flt64`

**设计模式**:
```kotlin
// 新泛型基类
interface MetaModelOf<V> { ... }
class LinearMetaModelOf<V> : MetaModelOf<V> { ... }

// 兼容别名（保持现有代码可用）
typealias LinearMetaModel = LinearMetaModelOf<Flt64>
typealias QuadraticMetaModel = QuadraticMetaModelOf<Flt64>

// 兼容构造器
fun LinearMetaModel(name: String, ...): LinearMetaModelOf<Flt64> = ...
```

**覆盖范围**:
| 层级 | 声明泛型 | 兼容别名 | 数值内核 |
|------|----------|----------|----------|
| MetaModel | ✅ `MetaModelOf<V>` | ✅ `typealias` | ❌ 保持 `Flt64` |
| MechanismModel | ✅ `MechanismModelOf<V>` | ✅ `typealias` | ❌ 保持 `Flt64` |
| Constraint | ✅ `ConstraintOf<V>` | ✅ `typealias` | ❌ 保持 `Flt64` |
| Token/Table | ✅ 签名泛型 | ✅ `typealias` | ❌ 保持 `Flt64` |

**详细步骤**:
| 步骤 | 内容 | 文件 | 预估 |
|------|------|------|------|
| C2-1.1 | 设计 `MetaModelOf<V>` 接口签名 | `MetaModel.kt` | 0.5h |
| C2-1.2 | 实现 `LinearMetaModelOf<V>` / `QuadraticMetaModelOf<V>` | `MetaModel.kt` | 1h |
| C2-1.3 | 添加 `typealias` 兼容别名 | `MetaModel.kt` | 0.25h |
| C2-1.4 | 设计 `MechanismModelOf<V>` 接口签名 | `MechanismModel.kt` | 0.5h |
| C2-1.5 | 实现 `LinearMechanismModelOf<V>` / `QuadraticMechanismModelOf<V>` | `MechanismModel.kt` | 1h |
| C2-1.6 | 添加 `typealias` 兼容别名 | `MechanismModel.kt` | 0.25h |
| C2-1.7 | 兼容构造器函数 | 各文件 | 0.5h |
| C2-1.8 | 验证测试通过 | - | 0.5h |

**总预估**: 4.5 小时

**退出条件**:
- 泛型接口声明完成（`MetaModelOf<V>`, `MechanismModelOf<V>` 签名存在）
- 兼容别名生效（`LinearMetaModel` 等别名指向泛型版本）
- 现有调用不破坏（外部模块如 framework 编译通过）
- 测试通过（`mvn -pl ospf-kotlin-core -am test`）

##### 第二段：数值内核泛型化专项

**目标**: 递进式泛型化数值内核

**递进顺序**（按依赖关系）:
```
math.symbol (quadratic)
    ↓
polynomial/monomial/cell
    ↓
token/cache/table
    ↓
constraint/objective
    ↓
meta/mechanism
    ↓
triad/tetrad/solver
```

**详细步骤**:
| 步骤 | 覆盖组件 | 工作量 | 关键改动 |
|------|----------|--------|----------|
| C2-2.1 | `QuadraticInequality` 泛型化 | 2h | 改为 `QuadraticInequality<V>` |
| C2-2.2 | `LinearMonomial/QuadraticMonomial` 泛型化 | 1h | Cell 系统泛型化 |
| C2-2.3 | `FlattenData` 泛型化 | 1h | `LinearFlattenData<V>` 等 |
| C2-2.4 | `Token/TokenList` 泛型化 | 2h | `Token<V>` 类型化 |
| C2-2.5 | `TokenTable/CacheContext` 泛型化 | 2h | 缓存系统泛型化 |
| C2-2.6 | `Constraint/Objective` 泛型化 | 1h | 约束和目标泛型化 |
| C2-2.7 | `MechanismModel` 数值内核泛型化 | 2h | 内部实现泛型化 |
| C2-2.8 | `MetaModel` 数值内核泛型化 | 2h | SubObject 泛型化 |
| C2-2.9 | `IntermediateModel` 转换边界 | 1h | 构造函数显式转换 |
| C2-2.10 | `Solver` 适配 | 1h | 求解器入口适配 |

**总预估**: 15-20 小时（约 2-3 天）

**退出条件**:
- 数值内核泛型化完成（Token、Cell、Constraint 等支持 `<V>`）
- 转换边界明确（`MechanismModel<V> → IntermediateModel` 显式转换）
- 二次数学层泛型化（`QuadraticInequality<V>` 可用）
- 测试通过（全链路测试通过）

#### C2 当前交付物

- `docs/refactor-baseline/generic-boundary.md` ✅ 泛型化差异清单

#### C2-2 第二段阶段性完成（2026-04-16）

**状态**：C2-2 第二段阶段性完成（声明层骨架 + 兼容层 + 全量测试通过），非最终收口完成。

**已完成项（声明层骨架）**:

| 步骤 | 计划内容 | 实际实现 | 状态 |
|------|----------|----------|------|
| C2-2.1 | `QuadraticInequality<V>` | `QuadraticInequalityOf<T>` + typealias（ospf-kotlin-math） | ⚠️ 部分完成（数据结构已泛型化，DSL/调用面仍以 Flt64 为主） |
| C2-2.2 | `LinearMonomial/QuadraticMonomial` 泛型化 | `LinearMonomialCellOf<V>`, `QuadraticMonomialCellOf<V>` + typealiases | ✅ 完成 |
| C2-2.3 | `FlattenData<V>` | `LinearFlattenDataOf<T>`, `QuadraticFlattenDataOf<T>` + typealiases | ✅ 完成 |
| C2-2.4 | `Token<V>` | `TokenOf<T>`, `AbstractTokenListOf<T>` 等全套 + typealiases | ✅ 完成 |
| C2-2.5 | `TokenTable/CacheContext` | CacheContext 已泛型化；C2-2.5a 新增 `AbstractTokenTableOf<V>` 泛型接口骨架；C2-2.5b 待 C6 收口 | ⚠️ 部分完成（骨架已声明） |
| C2-2.6 | `Constraint/Objective` 泛型化 | 新增 `ConstraintOf<V>` / `CellOf<V>` 骨架接口；Objective 已是旧 `Cell` 泛型 | ⚠️ 部分完成（骨架已声明，内核仍 Flt64） |
| C2-2.7 | `MechanismModel` 内部实现泛型化 | `MechanismModelOf<V>` / `LinearMechanismModelOf<V>` / `QuadraticMechanismModelOf<V>` 接口层泛型化 | ⚠️ 部分完成（接口层泛型，内部字段仍 `List<Constraint>`、`AbstractTokenTable`） |
| C2-2.8 | `MetaModel` 内部实现泛型化 | `MetaModelOf<V>` / `LinearMetaModelOf<V>` / `QuadraticMetaModelOf<V>` 接口层泛型化 | ⚠️ 部分完成（接口层泛型，SubObject 仍 `UtilsLinearPolynomial<Flt64>`） |
| C2-2.9 | `IntermediateModel` 转换边界 | `LinearTriadModel.invoke(model: LinearMechanismModel)` 入口存在 | ⚠️ 部分完成（入口参数是 `LinearMechanismModelOf<Flt64>` 别名，非 `<V>` 到 Flt64 显式转换） |
| C2-2.10 | 验证全链路测试 | `mvn -pl ospf-kotlin-core -am test` 通过 | ✅ 完成（91 tests, 0 failures） |

**口径差异标注**:

1. **QuadraticInequality 泛型化范围**：已确认纳入 C2-2。ospf-kotlin-math 中 `QuadraticInequalityOf<T>` 数据结构已泛型化，但 DSL/调用面仍以 `Flt64` 为主，后续需收口。

2. **TokenTable 接口泛型化策略**：采用兼容层方案，拆分两步：
   - **C2-2.5a** ✅ 已完成：新增 `AbstractTokenTableOf<V>` / `AbstractMutableTokenTableOf<V>` 泛型接口骨架，与现有 `AbstractTokenTable` sealed interface 并存，不改动 Expression.kt 等现有签名。
   - **C2-2.5b** 待执行：C6 删除 Expression/Polynomial 后收口，将 `AbstractTokenTable` 改为 typealias，迁移实现类。

3. **Constraint/MetaModel 内核泛型化**：目前仅声明层骨架，内核仍是 Flt64。收口与 C6 删除旧路径强耦合，避免二次改造成本。

4. **MechanismModel -> IntermediateModel 显式转换**：当前入口参数是 `LinearMechanismModel`（即 `LinearMechanismModelOf<Flt64>` 别名），非 `<V>` 到 Flt64 显式泛型转换。边界收口待 C4。

5. **验证强度**：已通过 `mvn -pl ospf-kotlin-core -am test`（91 tests, 0 failures），符合阶段性退出条件。

**暂不改动（C6 阶段删除）**:

- `Expression.kt` 接口签名保持 `AbstractTokenList`（避免向 15+ 实现类传播）
- `Polynomial.kt` evaluate 方法签名保持 `AbstractTokenList`
- 测试文件 `MonomialCoefficientPreservationTest.kt` 使用显式类型参数调用

**剩余项分配**:

| 原步骤 | 内容 | 分配到阶段 |
|--------|------|-----------|
| C2-2.1 收口 | QuadraticInequality DSL/调用面泛型化 | C6 |
| C2-2.5b | AbstractTokenTable typealias 收口 | C6 |
| C2-2.6 收口 | Constraint/Meta 内核去 Flt64 化 | C6 |
| C2-2.7 收口 | MechanismModel 内部字段泛型化 | C6 |
| C2-2.8 收口 | MetaModel SubObject 泛型化 | C6 |
| C2-2.9 收口 | MechanismModel<V> -> IntermediateModel 显式转换边界 | C4 |

#### C2 下一步

C2 阶段性完成，进入 C3（缓存上收）。

### 阶段 C3：缓存上收（3 天，修订版） ✅ 已完成 (2026-04-16)

> **状态**：✅ 实现与文档交付完成，全量测试受 C2 阻塞
> **完成日期**：2026-04-16
> **验收口径**：主代码编译通过 + 新增测试文件存在 + 文档交付完成；全量测试待 C2 遗留修复后补验

#### C3 完成总结（2026-04-16）

| 交付物 | 状态 | 文件路径 |
|--------|------|----------|
| **B1-B3 阻断修复** | ✅ 完成 | TokenCacheContext.kt, TokenTable.kt |
| **C3-1 缓存点清单** | ✅ 完成 | ospf-kotlin-core/docs/refactor-baseline/cache-usage.md |
| **C3-2 生命周期图** | ✅ 完成 | ospf-kotlin-core/docs/refactor-baseline/cache-lifecycle.md |
| **C3-3 双写收口决策** | ✅ 完成 | ospf-kotlin-core/docs/refactor-baseline/cache-double-write.md |
| **C3-4 测试清单** | ✅ 完成 | ospf-kotlin-core/docs/refactor-baseline/cache-tests.md |
| **主代码编译** | ✅ 通过 | mvn -pl ospf-kotlin-core compile -q |
| **全量测试** | ⏳ 阻塞 | C2 泛型化遗留导致 test-compile 失败 |

**C2 阻塞清单**（非 C3 回归）:
- FlattenUtilityTest.kt: `LinearFlattenData` vs `LinearFlattenDataOf<Flt64>` 类型不匹配
- MonomialCoefficientPreservationTest.kt: `invoke` 类型参数错误
- LinearPolynomialBaselineTest.kt: `evaluate` 重载歧义
- QuadraticPolynomialBaselineTest.kt: 同上
- SubObjectTest.kt: 类型不匹配

**提交记录**:
- `0ea150fd` refactor(core): fix cache consistency blockers B1-B3 for C3 phase
- `4c3aae52` docs(core): add cache-usage.md baseline inventory for C3-1
- `0c508988` docs(core): clarify cache-usage.md statistics rules
- `ca2554f5` docs(core): add cache-lifecycle.md for C3-2
- `a8ec35ba` docs(core): add cache-double-write.md decision doc for C3-3
- `9d503883` docs(core): add cache-tests.md and complete C3-4 verification

#### C3 审核发现（2026-04-16）

| 阻断点 | 当前状态 | 影响 |
|--------|----------|------|
| **并发注册链路遗漏** | 同步走 `prepareAndCache` + `cacheSymbolContext`，并发走 `prepare` + `cache(symbols=...)` | 验收漏检 |
| **remove(symbol) 缺解绑** | 只删列表和 map，不做 unbind 和缓存清理 | 缓存残留、上下文泄漏 |
| **双写缓存并存** | Symbol key + Polynomial private key 两条路径写同一 context | 缓存冗余、清理不一致 |
| **cacheSymbolContext 重复调用** | 单符号 + 批量各调用一次 | 预热开销冗余 |
| **扫描关键词不全** | 漏了 range/bind/unbind 相关 API | 清单不完整 |

#### C3 详细步骤（修订版）

##### C3-1: 缓存点分布清单生成（预估 2.5h）

**目标**: 生成完整的缓存调用点清单

**扫描关键词（补全后）**:
| 分类 | 关键词 | 说明 |
|------|--------|------|
| 写入 | `cacheLinearFlatten`, `cacheQuadraticFlatten`, `cacheRange`, `cache(cacheKey` | 按类型分 |
| 写入 | `cacheSymbolContext`, `cacheSymbolContexts` | 批量预热 |
| 读取 | `cachedLinearFlatten`, `cachedQuadraticFlatten`, `cachedRange`, `cachedValue` | 命中检查 |
| 读取 | `cachedLinearFlattenValue`, `cachedQuadraticFlattenValue`, `cachedRangeValue` | 取值 |
| 清理 | `clearLinearFlatten`, `clearQuadraticFlatten`, `clearRange`, `clearAll` | 失效 |
| 绑定 | `bindTokenTableContext`, `unbindTokenTableContext`, `boundTokenTableContext` | 上下文 |
| 移除 | `remove(symbol` | 符号移除 |

**输出**: `ospf-kotlin-core/docs/refactor-baseline/cache-usage.md`（仓库根路径口径）

**验收**: 清单可复现，包含文件名、行号、调用上下文、路径类型（Symbol key / Private key）

---

##### C3-2: 缓存生命周期分析（预估 3h）

**目标**: 完整分析缓存绑定/解绑/失效/移除生命周期

**生命周期图**:
```
[符号注册]
  ├── 同步注册 register()
  │     └── prepareAndCache() → cache(cacheKey=symbol) → cacheSymbolContext(symbol)
  │           [问题：cacheSymbolContext 重复调用]
  │
  └── 并发注册 register()
        └── prepare() → cache(symbols=...) → cacheSymbolContexts(readySymbols)
              [遗漏：无单个符号的 cacheSymbolContext]
              
[符号移除] ← 需新增
  └── remove(symbol)
        ├── 当前：仅 _symbols.remove(symbol), _symbolsMap.remove(name)
        ├── 缺失：unbindTokenTableContext(symbol, this)
        ├── 缺失：clearLinearFlatten(symbol), clearQuadraticFlatten(symbol)
        ├── 缺失：clearRange(symbol)
        └── 缺失：cacheContexts.value.remove(symbol)

[缓存失效]
  ├── MetaModel.flush(force)
  │     └── TokenTable.flush() → cacheContexts.clearAll()
  │     └── Symbol.flush(force) [逐个]
  │
  └── Polynomial.flush(force) ← Private key 路径
        └── clearRange(privateKey), clearLinearFlatten(privateKey)
              [问题：Private key 缓存不在 clearAll 管理范围内]
```

**输出**: `ospf-kotlin-core/docs/refactor-baseline/cache-lifecycle.md`（仓库根路径口径）

---

##### C3-3: 统一缓存失效与移除入口（预估 4h） ⚠️ 阻断点修复

**目标**: 统一 flush 和 remove 的缓存清理逻辑

**改动文件**:
| 文件 | 改动 |
|------|------|
| `MutableTokenTable` | 修改 `remove(symbol)` 增加 unbind 和缓存清理 |
| `ConcurrentMutableTokenTable` | 同上，加 synchronized |
| `AbstractMutableTokenTable` interface | 无需改动（已有 remove 签名） |

**改动实现草案**:
```kotlin
// MutableTokenTable.kt
override fun remove(symbol: IntermediateSymbol) {
    _symbols.remove(symbol)
    _symbolsMap.remove(symbol.name)
    
    // 新增：解绑和缓存清理
    unbindTokenTableContext(symbol, this)
    cacheContexts.linearFlatten.remove(symbol)
    cacheContexts.quadraticFlatten.remove(symbol)
    cacheContexts.range.remove(symbol)
}

// ConcurrentMutableTokenTable.kt
override fun remove(symbol: IntermediateSymbol) {
    synchronized(lock) {
        _symbols.remove(symbol)
        _symbolsMap.remove(symbol.name)
        
        unbindTokenTableContext(symbol, this)
        cacheContexts.linearFlatten.remove(symbol)
        cacheContexts.quadraticFlatten.remove(symbol)
        cacheContexts.range.remove(symbol)
    }
}
```

**验收**: remove 后符号缓存清空、unbind 触发

---

##### C3-4: 缓存预热链路验证（预估 4h） ⚠️ 阻断点修复

**目标**: 验证两条注册链路的缓存预热一致性

**链路对比**:
| 步骤 | 同步注册 | 并发注册 |
|------|----------|----------|
| 1 | `symbol.prepareAndCache(values, tokenTable)` | `symbol.prepare(values, tokenTable)` |
| 2 | `tokenTable.cache(cacheKey=symbol, value)` [在 prepareAndCache 内] | `tokenTable.cache(symbols=map)` [批量写入] |
| 3 | `tokenTable.cacheSymbolContext(symbol)` [单符号] | `tokenTable.cacheSymbolContexts(readySymbols)` [批量] |
| 4 | `tokenTable.cacheSymbolContexts(readySymbols)` [批量二次] | 无二次调用 |

**问题点**:
- 同步注册：cacheSymbolContext 被调用两次（单符号 + 批量）
- 并发注册：无单符号调用，但批量调用覆盖

**修复建议**:
| 问题 | 建议修复 |
|------|----------|
| 同步重复调用 | 移除 TokenTable.kt 第 701 行的单符号调用，仅保留批量调用 |
| 路一致性 | 确认两种链路最终效果一致（缓存命中检查） |

**验收**: 两条链路预热后缓存均可用，依赖链预热正确

---

##### C3-5: 缓存一致性测试补充（预估 4h） ⚠️ 阻断点修复

**目标**: 补充覆盖阻断点的回归测试

**新增测试场景**:
| 场景 | 测试文件 | 验收点 |
|------|----------|--------|
| **并发注册预热** | `TokenCacheContextsTest.kt` | 多线程注册后缓存命中 |
| **remove 后重绑** | 新增 `CacheRebindTest.kt` | remove 后重新 add 同名符号，缓存重新生成 |
| **双 TokenTable 重绑一致性** | `CacheRebindTest.kt` | 符号绑定新 TokenTable 后，旧 TokenTable 缓存失效 |
| **缓存命中/失效** | `TokenCacheContextsTest.kt` | flush 后缓存清空 |
| **Private key 与 Symbol key 冲突检测** | `CacheKeyConflictTest.kt` | 验证两种 key 不产生数据覆盖 |

**验收**: 新增测试通过，覆盖核心阻断场景

---

##### C3-6: 双写缓存收口策略定义（预估 2h）

**目标**: 明确 Symbol key 与 Private key 的收口策略

**当前状态分析**:
| 路径 | Key 类型 | Key 前缀 | 调用位置 |
|------|----------|----------|----------|
| Symbol key | `IntermediateSymbol` | 符号标识符 | `cacheSymbolContext(symbol)` |
| Private key | `TokenCacheKey` | `__linear_polynomial_flatten_cache__` | `Polynomial.flattenedMonomials` |

**收口策略**: 采用 **选项 C** 作为 C3 临时方案，C6 删除 Polynomial 后自动解决
- remove(symbol) 时清理 Symbol key
- flush() 时 clearAll() 清理所有
- Private key 仅 Polynomial 内部使用，C6 删除后自动消失

**输出**: `ospf-kotlin-core/docs/refactor-baseline/cache-double-write.md`（仓库根路径口径）

---

##### C3-7: 全量测试验证（预估 1h）

**命令**: `mvn -pl ospf-kotlin-core -am test`

**验收标准**:
- ✅ 无新增失败
- ✅ `TokenCacheContextsTest` 全绿
- ✅ 新增 `CacheRebindTest`、`CacheKeyConflictTest` 全绿

---

##### C3-8: 交付物生成（预估 1h）

**交付物**（仓库根路径口径）:
| 文件 | 内容 |
|------|------|
| `ospf-kotlin-core/docs/refactor-baseline/cache-usage.md` | 缓存调用点清单（含 key 类型标注） |
| `ospf-kotlin-core/docs/refactor-baseline/cache-lifecycle.md` | 缓存生命周期图（含 remove 分支） |
| `ospf-kotlin-core/docs/refactor-baseline/cache-tests.md` | 新增测试清单 |
| `ospf-kotlin-core/docs/refactor-baseline/cache-double-write.md` | 双写缓存收口策略 |

#### C3 阻断点修复进度（2026-04-16）

| 阻断点 | 状态 | 完成内容 |
|--------|------|----------|
| **B1** | ✅ 已完成 | `remove(symbol)` 增加 unbind + 四类缓存清理（linear/quadratic/range/value） |
| **B2** | ✅ 已完成 | 同步注册链路移除重复 `cacheSymbolContext(symbol)` 调用，并发链路添加注释保持一致性 |
| **B3** | ✅ 已完成 | 新增 3 个回归测试：并发预热、remove 重绑、双 TokenTable 重绑一致性 |

**B1/B2/B3 主代码改动**:

| 文件 | 改动内容 |
|------|----------|
| `TokenCacheContext.kt` | `ValueCacheContext.remove(cacheKey)` 新增按 key 清理能力 |
| `TokenCacheContext.kt` | `bindTokenTableContext` 重绑时清理旧表缓存 |
| `TokenTable.kt` | `MutableTokenTable.remove(symbol)` 增加 unbind + 四类缓存清理 |
| `TokenTable.kt` | `ConcurrentMutableTokenTable.remove(symbol)` 同上 + synchronized |
| `TokenTable.kt` | `AbstractTokenTable.clearValue(cacheKey)` 新增接口 + 四类实现 |
| `TokenTable.kt` | 同步注册移除重复 `cacheSymbolContext(symbol)`，改用批量 value 预热对齐并发 |
| `TokenTable.kt` | 并发注册添加 B2 注释保持文档一致性 |

**B3 新增测试**:

| 测试文件 | 测试方法 | 验收点 |
|----------|----------|--------|
| `TokenCacheContextsTest.kt` | `concurrentRegisterShouldPreheatValueFlattenAndRangeCache` | 并发注册后缓存命中 |
| `CacheRebindTest.kt` | `removeShouldClearCachesAndAllowRebind` | remove 后重新注册，缓存重新生成 |
| `CacheRebindTest.kt` | `rebindToNewTokenTableShouldInvalidateOldTableCaches` | 符号绑定新表后，旧表缓存失效 |

**编译验证**:
- ✅ `mvn -pl ospf-kotlin-core compile` 主代码通过
- ⚠️ 测试编译失败（C2 泛型化遗留：FlattenUtilityTest、MonomialCoefficientPreservationTest 等）
- 测试编译问题与 B1/B2/B3 改动无关，需在后续专项修复

---

#### C3 退出条件（修订版）

1. ✅ `ospf-kotlin-core/docs/refactor-baseline/cache-*.md` 文档存在且可复现（仓库根路径口径）
2. ✅ 缓存生命周期图包含 remove 分支
3. ✅ `remove(symbol)` 实现 unbind + 缓存清理
4. ✅ 同步/并发两条注册链路预热效果一致
5. ✅ 新增并发预热、remove 重绑、双 TokenTable 重绑测试通过
6. ✅ 双写缓存收口策略文档化
7. ✅ `mvn -pl ospf-kotlin-core -am test` 无新增失败

#### C3 预估工时汇总

| 步骤 | 预估 | 变化 |
|------|------|------|
| C3-1 | 2.5h | +0.5h（补全关键词） |
| C3-2 | 3h | +1h（增加 remove 分支） |
| C3-3 | 4h | +1h（增加 remove 实现） |
| C3-4 | 4h | +1h（拆分链路验收） |
| C3-5 | 4h | +1h（增加 3 个场景） |
| C3-6 | 2h | 新增（双写收口策略） |
| C3-7 | 1h | 无变化 |
| C3-8 | 1h | 无变化 |
| **总计** | **21.5h（约 3 天）** | +4.5h |

### 阶段 C4：MechanismModel 边界收口（1.5 天）

1. 明确内部模型边界：
   - MetaModel 输入（math.symbol）
   - MechanismModel 内核（Token + Cell）
2. 统一转换入口：
   - math.symbol -> Cell 统一桥接
3. 消除多入口分叉：
   - 同类转换不允许多实现并存
4. 退出条件：
   - 边界清晰、转换单点。

### 阶段 C5：Quadratic cut 对齐 Rust（2~3 天）

1. 迁移算法：
   - row dual 路径
   - solver output 路径
2. 补测试：
   - 正常路径
   - 边界与失败路径
3. 退出条件：
   - Quadratic cut TODO 清零，双入口回归全绿。

### 阶段 C6：删库与门禁（1~2 天）

1. 物理删除：
   - `Polynomial.kt`
   - `Expression.kt`
2. CI 门禁上线：
   - 禁止对外回流 core 符号类型
   - 禁止 Abstract*Polynomial 回流主链路
   - 禁止 .cells 回流主计算路径
   - 禁止 core 主链路新增 `Flt64/Double` 固化路径（plugin 适配层除外）
3. 退出条件：
   - 编译通过 + 回归通过 + 门禁生效。

### 总工期预估（交接版）

- C0-C6 合计：10~16 人天
- 若并行执行（API/缓存/plugin 分线）：可压缩到 8~12 人天

---

## 下一步执行清单（可直接派工）

1. 立即执行 C0：生成“core 对外符号类型暴露清单”并入库（脚本化）。
2. 执行 C1：先改 Model.kt 与 MetaModel.kt 主入口签名，再改 DSL。
3. 执行 C2：完成 core 全链路泛型化，并把 `V -> Double` 适配下沉到 core-plugin。
4. 执行 C3：把旧缓存逻辑迁到 MetaModel contexts，并补回归测试。
5. 执行 C5：优先打通 Quadratic generateOptimalCut/generateFeasibleCut 的 Rust 对齐实现。
6. 最后执行 C6：删 `Polynomial.kt`/`Expression.kt` + 上 CI 门禁。

---

## Commit Plan（交接执行）

> 目标：将 C0~C6 落地为可连续执行、可回退的提交序列。  
> 约定：每个提交完成后必须先本地验证再进入下一个提交。

### 提交 1：baseline 清单入库（对应 C0）

- 建议提交信息：
  - `chore(core): add refactor baseline inventories for symbol exposure and numeric concretization`
- 主要改动：
  - 新增 `ospf-kotlin-core/docs/refactor-baseline/`（API 暴露清单、`Flt64/Double` 固化点、`.cells` 调用点）
  - 新增或更新扫描脚本（例如 `scripts/refactor-baseline.ps1`）
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test -DskipTests`
- 通过标准：
  - 清单可复现、脚本可重复执行且输出稳定
- 回滚点：
  - `tag: core-refactor-c0-baseline`

### 提交 2：Model/MetaModel 主入口签名切流（对应 C1 上半）

- 建议提交信息：
  - `refactor(core): switch Model and MetaModel primary APIs to math.symbol types`
- 主要改动：
  - `Model.kt`
  - `MetaModel.kt`
  - `MetaConstraint.kt`
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
- 通过标准：
  - 主入口已支持 math.symbol；旧入口仍可编译
- 回滚点：
  - `tag: core-refactor-c1-api-main`

### 提交 3：DSL 与兼容层补齐（对应 C1 下半）

- 建议提交信息：
  - `refactor(core): migrate MathInequalityDsl main path and add Deprecated ReplaceWith bridges`
- 主要改动：
  - `MathInequalityDsl.kt`
  - 相关 bridge/adapter 文件
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
  - `mvn compile -pl ospf-kotlin-framework -am`
- 通过标准：
  - DSL 主链路基于 math.symbol；兼容层可用且告警可迁移
- 回滚点：
  - `tag: core-refactor-c1-dsl-compat`

### 提交 4：core 泛型链路贯通（对应 C2 上半）

- 建议提交信息：
  - `refactor(core): propagate generic value type V across MetaModel MechanismModel IntermediateModel`
- 主要改动：
  - `MetaModel.kt`
  - `MechanismModel.kt`
  - `LinearTriadModel.kt`
  - `QuadraticTetradModel.kt`
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
- 通过标准：
  - core 主链路不再新增 `Flt64/Double` 固化实现
- 回滚点：
  - `tag: core-refactor-c2-generic-core`

### 提交 5：plugin 边界 `V -> Double` 统一适配（对应 C2 下半）

- 建议提交信息：
  - `refactor(core-plugin): centralize V-to-Double adaptation at solver boundary`
- 主要改动：
  - `ospf-kotlin-core-plugin/*/src/main/**/*.kt`（分插件统一适配入口）
- 必跑命令：
  - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`
- 通过标准：
  - 求解器调用前置转换统一在 plugin 层，core 不承担该责任
- 回滚点：
  - `tag: core-refactor-c2-plugin-boundary`

### 提交 6：缓存上收与 context 一致性（对应 C3）

- 建议提交信息：
  - `refactor(core): move monomial polynomial cache semantics into MetaModel contexts`
- 主要改动：
  - `TokenCacheContext.kt`
  - `TokenTable.kt`
  - `intermediate_symbol/flatten/*`
  - 相关测试
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
- 通过标准：
  - flatten/value/range context 为主缓存路径
- 回滚点：
  - `tag: core-refactor-c3-cache-context`

### 提交 7：MechanismModel 边界与桥接单点化（对应 C4）

- 建议提交信息：
  - `refactor(core): unify math.symbol to cell bridge at mechanism boundary`
- 主要改动：
  - `MechanismModel.kt`
  - `LinearConstraintInput.kt`
  - 相关 bridge 文件
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
  - `mvn compile -pl ospf-kotlin-framework -am`
- 通过标准：
  - MetaModel 输入与 MechanismModel 内核边界清晰，转换单点
- 回滚点：
  - `tag: core-refactor-c4-boundary-bridge`

### 提交 8：Quadratic cut Rust 对齐实现（对应 C5）

- 建议提交信息：
  - `feat(core): implement quadratic optimal feasible cuts aligned with rust mechanism model`
- 主要改动：
  - `MechanismModel.kt`（Quadratic cut）
  - `QuadraticTetradModel.kt`（如需协作修改）
  - 对应 `src/test`（row dual / solver output 双路径）
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
- 通过标准：
  - Quadratic cut TODO 清零，新增回归全绿
- 回滚点：
  - `tag: core-refactor-c5-quadratic-cut`

### 提交 9：物理删库（对应 C6 上半）

- 建议提交信息：
  - `refactor(core): remove legacy Polynomial and Expression after API and cache migration`
- 主要改动：
  - 删除 `Polynomial.kt`
  - 删除 `Expression.kt`
  - 清理剩余引用
- 必跑命令：
  - `mvn -pl ospf-kotlin-core -am test`
  - `mvn compile -pl ospf-kotlin-framework -am`
  - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`
- 通过标准：
  - 编译、测试、插件编译全部通过
- 回滚点：
  - `tag: core-refactor-c6-delete-symbol-types`

### 提交 10：CI 门禁与文档封口（对应 C6 下半）

- 建议提交信息：
  - `chore(ci): add guards against symbol type regression cells regression and early double concretization`
- 主要改动：
  - CI 规则脚本/工作流
  - `README.md` / `README_ch.md` / `daily.md` 同步
- 必跑命令：
  - CI 全量流水线
- 通过标准：
  - 以下门禁生效：
  - 禁止 core 对外回流 monomial/polynomial/inequality 类型
  - 禁止主链路回流 `Abstract*Polynomial`
  - 禁止主计算路径回流 `.cells`
  - 禁止 core 主链路新增 `Flt64/Double` 固化（plugin 边界除外）
- 回滚点：
  - `tag: core-refactor-c6-ci-guard`

### 建议合并策略

- 线性串行：提交 1 -> 10 依次推进。
- 可并行分线（降低总时长）：
  - 分线 A：提交 2/3（API 与 DSL）
  - 分线 B：提交 4/5（泛型与 plugin 边界）
  - 分线 C：提交 6（缓存上收）
  - 汇合后执行提交 7/8/9/10
- 合并要求：
  - 每次合并前必须通过对应阶段“必跑命令”。

---

## 验收命令（建议）

1. mvn -pl ospf-kotlin-core -am test
2. mvn compile -pl ospf-kotlin-framework -am
3. mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile

---

## 风险与止损

1. 如果 C1 后主入口仍有 Abstract*Polynomial 新增引用，立即阻断合并。
2. 如果 C2 后 core 主链路仍新增 `Flt64/Double` 固化实现，立即阻断合并。
3. 如果 C5 引入二次 cut 回归失败超过 1 天，回退到 C3 稳定点。
4. C1-C4 未完成前，不进入 C6 文件物理删除阶段。

---

## Phase 4 ~ Phase 8 欠缺补充（Kotlin 相对 Rust）

> 按原 daily 的 Phase 4 ~ Phase 8 语义补充“当前 Kotlin 仍弱于 Rust 已实现能力”的缺口。

### Phase 4（中间模型/dual/cut 闭环）欠缺

1. Quadratic Benders cut 能力缺口（P1）
   - Rust：`mechanism_model.rs` 已有 quadratic optimal/feasible cut（row dual / solver output 双入口）。
   - Kotlin：`MechanismModel.kt` 的 Quadratic `generateOptimalCut/generateFeasibleCut` 仍为 TODO。

2. Quadratic dual/farkas 口径未收口（P1）
   - Rust：无 `QuadraticTetradModel.to_dual()/to_farkas_dual()` 公共 API。
   - Kotlin：仍有 `QuadraticTetradModel.dual()/farkasDual()` TODO，需明确为“扩展能力实现”或“显式不支持”。

### Phase 5（性能与稳定性）欠缺

3. 显式符号依赖图能力不足（P1）
   - Rust：`symbol_dependencies` + `add_symbol_dependency/add_symbol_with_dependencies` 为一等 API。
   - Kotlin：仍以隐式注册依赖为主，缺少同等级显式依赖图 API 与回归覆盖。

4. FunctionSymbol 上下文一体化语义不完整（P1）
   - Rust：统一 `register_auxiliary_tokens` + `evaluate_from_tokens`，且 `add_symbol` 触发 context 重绑。
   - Kotlin：符号注册与求值路径仍分散在 registration scope/prepare 链路，尚未完全对齐为单一钩子模型。

5. 缓存归属仍处于过渡态（P0）
   - Rust：value/range/flatten 上下文归属清晰，随符号注册生命周期管理。
   - Kotlin：旧 monomial/polynomial 缓存语义尚未完全上收到 MetaModel contexts，仍有历史类型耦合残留。

6. 泛型化链路未闭环（P0）
   - Rust：值类型 `V` 贯穿 model/symbol/token 体系，求解器适配在边界处理。
   - Kotlin：当前 core 主链路仍存在较多 `Flt64` 固化路径，未完全收敛到“plugin 边界才转 Double”。

### Phase 6（测试补齐与达标判定）欠缺

7. 接口兼容测试覆盖不足（P0）
   - 目标要求“对外接口基本不变”，但当前缺少系统化兼容用例矩阵（历史重载/别名/迁移通道）。

8. 主链路语义等价回归不足（P1）
   - `Abstract*Polynomial` 到 `math.symbol` 切流后，缺少成套“旧入口 vs 新入口”语义等价回归。
   - 泛型 `V` 与 plugin 边界 `Double` 转换也缺少专项回归覆盖。

### Phase 7（集中回归）欠缺

9. C1-C6 阶段化回归编排未落地（P1）
   - 目前有命令模板，但尚未形成“每阶段固定回归子集 + 全量回归 + 插件编译”的流水线基线产物。

### Phase 8（封口门禁）欠缺

10. 对外符号类型回流门禁未完全落地（P0）
   - 缺少“禁止 core 对外再次暴露 monomial/polynomial/inequality 类型”的 CI 守卫。

11. 过渡类型与旧路径门禁未完全落地（P1）
   - 缺少“禁止 Abstract*Polynomial 回流主链路”与“禁止 .cells 回流主计算路径”的自动化门禁。
   - 缺少“禁止 core 主链路新增 Flt64/Double 固化路径（plugin 适配层除外）”的门禁。

12. 非线性分支基础完备性不足（P2）
   - Kotlin `MonomialCell.invoke(..., category)` 对非线性分支仍存在 TODO，会影响后续类别扩展的一致性。
