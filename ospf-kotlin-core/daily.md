# OSPF Kotlin Core Daily

日期：2026-04-14
交接目标：下一个执行环境
Rust 对齐参考：`E:\workspace\ospf-rust\ospf-rust-core\src`

---

## 路线变更（2026-04-13）

**从增量迁移路线（daily.md）转入 Big-Bang 重写路线（rewrite.md）**。

历史 daily.md 记录了大量渐进式迁移决策，但经过多轮实践后得出结论：双轨并行（旧 `frontend.expression` + 新 `math.symbol`）的维护成本高于一次性切换。现在采用 `rewrite.md` 定义的一次性重写策略。

**核心判断变更**：
- ~~`frontend.expression.monomial/polynomial` 是前端 DSL 核心，不应删除~~ → **保留为建模 DSL，但函数符号层统一切到 `math.symbol`**
- ~~框架文件无法迁移，`@Suppress("DEPRECATION")` 为最终态~~ → **框架文件仍保留旧 DSL，但函数符号层不再维持双轨**
- ~~渐进迁移，逐阶段落地~~ → **Big-Bang 一次切流，不维持长期双轨**

---

## 重构前提

**符号运算职责边界**：`math.symbol` 是所有数学符号运算的唯一归属地。
- 如果发现某个操作符重载（如 `+`, `-`, `*`, `abs()`, 比较操作符等）在 `math.symbol` 中**不完备**（例如缺少特定参数类型的重载、缺少对称版本、缺少常量一侧的重载），**应当直接添加到 `math.symbol` 模块中**，而非在 core 或 function 层做变通。
- `core/` 层仅负责**使用** `math.symbol` 提供的运算能力，不应自行补全数学运算的缺失重载。
- 原因：数学运算是通用基础能力，分散补全会导致多处重复、版本不一致、且后续迁移成本成倍放大。

---

## 已完成事项

### 代数内核（Phase 1-4, M1-M4）
- `LinearInequality<T : Ring<T>>` 泛型化（math 模块），`Flt64LinearInequality` 类型别名兼容
- `LinearConstraint` / `QuadraticConstraint` 的 `MathConstraint` 接口
- `MetaModel` / `MechanismModel` 的 relation API（`addConstraint(relation: ...)`）
- 删除 `frontend/inequality` 全目录（含 adapter），零残留引用

### 模型层（M5）
- `MetaConstraint` 新增 `LinearRelationConstraint` / `QuadraticRelationConstraint`
- `addConstraint(relation: LinearRelation/QuadraticRelation)` / `addObject(flattenData: ...)`
- Benders cut 生成迁移到 `math.symbol.LinearPolynomial<Flt64>` 类型

### 函数符号层 — 新系统（M8/Phase 3，24 文件，22 个函数符号类）

`core/function/` 目录，基于 `MathFunctionSymbol<T : Field<T>>` 泛型接口：

| 类别 | 文件 | 函数符号 | 状态 |
|------|------|---------|------|
| 基础设施 | `FunctionSymbol.kt` | 接口 + 辅助函数 + 25+ 类型别名 | ✅ |
| 基础设施 | `BigM.kt` | `nonzeroIndicatorConstraints`, `simpleIndicatorConstraints` | ✅ |
| 逻辑函数 | `And.kt` | `AndFunction`, `OrFunction`, `NotFunction`, `XorFunction` | ✅ |
| 逻辑函数 | `If.kt` | `IfFunction` (premise => consequence) | ✅ |
| 逻辑函数 | `IfThen.kt` | `IfThenFunction` (蕴含 pu <= qu) | ✅ |
| 逻辑函数 | `IfIn.kt` | `IfInFunction` (区间包含) | ✅ |
| 逻辑函数 | `OneOf.kt` | `OneOfFunction` (单选分支) | ✅ |
| 逻辑函数 | `First.kt` | `FirstFunction` (首个非零索引) | ✅ |
| 极值函数 | `Max.kt` | `MaxFunction`, `MinFunction` | ✅ |
| 极值函数 | `MinMax.kt` | `MinMaxFunction`, `MaxMinFunction` | ✅ |
| 差分函数 | `Slack.kt` | `SlackFunction` (x-y=neg-pos) | ✅ |
| 差分函数 | `Abs.kt` | `AbsFunction` (pos/neg 分解) | ✅ |
| 差分函数 | `Semi.kt` | `SemiFunction` (半连续, BigM) | ✅ |
| 取整函数 | `Floor.kt` | `FloorFunction` (x=d*q+r) | ✅ |
| 取整函数 | `Ceiling.kt` | `CeilingFunction` (x=d*q-r) | ✅ |
| 取整函数 | `Rounding.kt` | `RoundingFunction` | ✅ |
| 取整函数 | `Mod.kt` | `ModFunction` (返回 r) | ✅ |
| 分段函数 | `UnivariateLinearPiecewise.kt` | 一元分段线性 (SOS2) | ✅ |
| 分段函数 | `BivariateLinearPiecewise.kt` | 二元分段线性 (三角形插值) | ✅ |
| 分段函数 | `Sigmoid.kt` | `SigmoidFunction` (委托一元分段) | ✅ |
| 二元化 | `Binaryzation.kt` | `BinaryzationFunction` (BigM/Threshold/Indicator/SOS1) | ✅ |
| 掩码函数 | `Masking.kt` | `MaskingFunction`, `MaskingRangeFunction` | ✅ |
| 满足量 | `SatisfiedAmount.kt` | `SatisfiedAmountFunction` | ✅ |
| 相等判断 | `SameAs.kt` | `SameAsFunction` | ✅ |

**架构决策**：
- `register()` 内部调用 `asFlt64Poly()` 转换为 Flt64 后生成约束（临时方案，模型层尚未泛型化）
- 目标态：转换点推迟到求解器接口层（Phase 6，暂缓）

### 编译与测试
- 全模块编译：BUILD SUCCESS ✅
- Core 回归：91 tests, 0 failures ✅
- Framework 模块（gantt-scheduling, bpp3d）：BUILD SUCCESS ✅
- Solver plugins（copt, gurobi, scip）：BUILD SUCCESS ✅

---

## 未完成事项

### 新系统缺失函数符号（7 个，需创建）

| 文件（旧位置） | 行数 | 功能 | 优先级 |
|---------------|------|------|--------|
| `linear_function/BalanceTernaryzation.kt` | 1063 | 平衡三值化 | P1 |
| `linear_function/InStepRangeFunction.kt` | 337 | 区间内阶跃函数 | P1 |
| `linear_function/Inequality.kt` | 388 | 不等式满足量 | P2 |
| `linear_function/SlackRange.kt` | 847 | SlackRange (quadratic 变体) | P2 |
| `linear_function/SatisfiedAmountInequality.kt` | 840 | 满足量不等式（复杂组合函数） | P2 |
| `linear_function/Sin.kt` | 3 | 正弦（旧版为 stub，委托 UnivariateLinearPiecewise） | P3 |
| `linear_function/Cos.kt` | 3 | 余弦（旧版为 stub，委托 UnivariateLinearPiecewise） | P3 |

### 旧系统待删除（rewrite.md B6 范围）

| 目录/文件 | 外部引用数 | 状态 |
|-----------|-----------|------|
| `frontend/expression/symbol/linear_function/` | 51 文件引用 | 保留（框架使用），标注 deprecated |
| `frontend/expression/symbol/quadratic_function/` | 部分文件引用 | 保留（框架使用），标注 deprecated |
| `frontend/expression/Expression.kt` | 待扫描 | 待评估 |

**注意**：`frontend.expression.monomial/polynomial` 已确认**不应删除**（前端 DSL 核心），`@file:Suppress("DEPRECATION")` 为最终态。

### 阶段计划未完成（rewrite.md B0-B9 进度）

| 阶段 | 状态 | 说明 |
|------|------|------|
| B0 冻结与分支 | 未执行 | 需在主线创建 `rewrite-bigbang` 分支 |
| B1 边界 API 定稿 | 已完成 | relation API 已冻结 |
| B2 桥接层重建 | 部分完成 | `asFlt64Poly()` 已实现，`flattenedMonomials` 已迁移 |
| B3 函数符号改造 | 进行中 | 22/29 个函数符号已迁移到新系统 |
| B4 模型层收口 | 已完成 | relation API 为主入口 |
| B5 一次性切流 | 未执行 | 需框架代码从旧函数符号迁移到新函数符号 |
| B6 删除旧目录 | 未执行 | 删除旧 `expression/symbol/linear_function/` 等 |
| B7 集中回归 | 未执行 | 待切流后执行 |
| B8 插件编译校验 | 未执行 | 待切流后执行 |
| B9 封口门禁 | 未执行 | CI 守卫 + 文档 |

---

## Rust 架构对齐参考

Rust 版本 (`E:\workspace\ospf-rust\ospf-rust-core\src`) 的核心架构要点：

### 符号系统
- **`IntermediateSymbol<V>`**：所有符号的基础 trait，含 `category()`、`evaluate_from_tokens()`、`prepare()`、`mechanism_constraints()`
- **`FunctionSymbol<V>`**：扩展 `IntermediateSymbol`，支持辅助变量注册（`register_tokens()`）和值计算（`calculate_value()`）
- **专门化 trait**：`LinearIntermediateSymbol<V>`（转 `Linear<V>` / `Quadratic<V>` 多项式）、`QuadraticFunctionSymbol<V>`

### 模型层
- **`BasicModel<V>`**：基础层，管理 `Token`s、`IntermediateSymbol`s、约束、符号依赖图
- **Mechanism 层**：`BasicMechanismModel<V>` + `MechanismModel<V>`，支持约束组和目标函数
- **Flatten 层**：`Linear<V>` / `Quadratic<V>` 多项式内盒类型，`Flattenable<C, V>` trait 展开符号
- **中间层**：`LinearTriadModel` / `QuadraticTetradModel` 稀疏矩阵表示

### Token 系统
- **`Token<V>`**：求解器侧变量表示，含 `solver_index` 和 `result: RwLock<Option<V>>`
- **`AnyVariable<V>`**：类型擦除的变量包装器

### 关键差异（Kotlin 应对）
| 维度 | Rust | Kotlin 当前 | 对齐方向 |
|------|------|------------|---------|
| 泛型策略 | `V` 作为类型参数贯穿全栈 | 临时 `asFlt64()` 转换 | 最终需泛型化模型层 |
| 缓存 | `Box<Inner>` 地址作为缓存键 | `hashCode` 缓存 | 保持一致 |
| 变量分配 | `VariableArena` + `typed_arena` | `AddableTokenCollection` | 已对齐 |
| 错误处理 | `Result<T>` | `Try` (Ok/Failed/Fatal) | 已对齐 |
| 符号依赖追踪 | 显式 `HashMap<u64, HashSet<u64>>` | 隐式 `FunctionSymbolRegistrationScope` | **需改进**：添加显式追踪 |

### Kotlin 独有功能（Rust 缺失 — 重写时必须保留）

| 功能 | 说明 | 重要性 |
|------|------|--------|
| **物理量系统** | `ospf-kotlin-quantities` 模块，47 文件，量纲检查 + SI 单位 | 核心优势 |
| **可变多项式** | `MutableLinearPolynomial` / `MutableQuadraticPolynomial`，原地修改 | 前端 DSL 核心 |
| **Benders 分解** | 6 个求解器插件（Gurobi/CPLEX/COPT/SCIP/MindOPT/二次） | 核心优势 |
| **多维变量** | `VariableCombination`，支持 Shape1/2/3 多维变量数组 | 核心优势 |
| **物理量变量** | `QuantityBinVar` 等，变量带单位 | 核心优势 |
| **数学符号计算** | 表达式解析器、微分、积分、LaTeX 输出 | math 模块优势 |
| **回调模型** | Policy-based + Extractor，多目标支持更完整 | 已对齐 |
| **Coroutine 导出** | `suspend fun export()` | 独有便利性 |

---

## 模块级架构对齐详情

### Intermediate Symbol 模块

| 子模块 | 文件数 | 行数 | 新系统对应 | 状态 |
|--------|--------|------|-----------|------|
| `IntermediateSymbol.kt` | 1 | 1169 | `MathFunctionSymbol<T>` | 部分对齐 |
| `SymbolCombination.kt` | 1 | ~520 | 保留（DSL 核心） | 不需重写 |
| `legacy/linear_function/` | 33 | 19,667 | `function/` (24 文件) | 9 个待创建/废弃 |
| `legacy/quadratic_function/` | 21 | 10,453 | 合并到线性版 | 需标记废弃 |
| **总计** | **56** | **~31,800** | **24** | **29/56 已对齐** |

### Variable 模块（含 Token）

| 文件 | 行数 | 功能 | Rust 对应 | 差异 |
|------|------|------|-----------|------|
| `AbstractVariableItem.kt` | 145 | 基类 | `variable_item.rs` | Kotlin 双泛型更强 |
| `Type.kt` | 119 | 变量类型定义 | `variable_type.rs` | 基本对齐 |
| `VariableIndependentItem.kt` | 46 | BinVar/IntVar 等 | 类型别名 | 基本对齐 |
| `VariableCombinationItem.kt` | 306 | 多维变量数组 | **无** | **Kotlin 独有** |
| `VariableRange.kt` | 182 | 值域 | `variable_range.rs` | 基本对齐 |
| `Token.kt` | 90 | Token 包装 | `token.rs` | Kotlin 回调更强 |
| `TokenList.kt` | 360 | Token 列表 | `token_list.rs` | 需优化索引 |

### Intermediate Model 模块（含 TokenTable）

| 文件 | 行数 | 功能 | Rust 对应 | 差异 |
|------|------|------|-----------|------|
| `TokenTable.kt` | 1,522 | Token 表 + 缓存上下文 | `token_table.rs` | Kotlin 更丰富 |
| `TokenCacheContexts` | — | 4 种缓存 | Lazy 上下文 | Kotlin 更丰富 |
| Token 查找 | — | `Map<VariableItemKey, Token>` | `HashMap<VariableId, usize>` | 需优化 |
| 按类型分组 | 无 | — | `tokens_by_type()` | **需补充** |

### Model 模块

| 文件 | 行数 | 功能 | Rust 对应 | 差异 |
|------|------|------|-----------|------|
| `MetaModel.kt` | 1,219 | 元模型 | `basic_model.rs` + `meta_model.rs` | Kotlin 接口更强 |
| `CallBackModel.kt` | 658 | 回调模型 | `callback/` 目录 | Kotlin 多目标更强 |
| `CallBackModelInterface.kt` | 141 | 回调接口 | `callback_model_trait.rs` | 基本对齐 |
| `MechanismModel.kt` | — | Benders cut 生成 | 仅基础结构 | **Kotlin 核心优势** |

---

## 已完成事项（摘要）

### A. 求解入口与状态/输出链路
1. 已落地 `SolveOptions` + `SolverExt.solveWithOptions(...)` 统一入口，覆盖 LP/QP 主路径。
2. 旧 `LinearSolver` / `QuadraticSolver` 重载入口保留兼容并转发到统一入口。
3. 已完成 `ModelBuildingStage` / `ModelBuildingStatus` 统一状态桥接。
4. 统一输出字段已补齐到可行与不可行分支：`iterations/nodeCount/bestBound/mipGap/solveTime`。

### B. IIS 与中间模型补完
1. `QuadraticTetradModel.elastic()` 已实现。
2. 线性 IIS 删除过滤已实现。
3. 二次 IIS 已实现 elastic filtering + deletion filtering + snapshot fallback。
4. 对应回归测试已补齐（含 IIS 选项转发、统一字段回填、elastic/deletion 路径）。

### C. Token 缓存上下文改造
1. 已接入 `TokenCacheContexts`：`LinearFlattenContext` / `QuadraticFlattenContext` / `ValueCacheContext` / `RangeCacheContext`。
2. `TokenTable` flatten 缓存 API 已按线性/二次拆分。
3. flatten 载荷改为：
   - `LinearFlattenData(monomials, constant)`
   - `QuadraticFlattenData(monomials, constant)`
4. `cacheKey` 统一为 `Any`，并引入 `TokenCacheKey/newTokenCacheKey`。

### D. math.symbol 对齐与桥接能力补齐
1. 已对齐并接入 `math.symbol` 的线性/二次符号运算能力。
2. `TokenCache` / `flatten` 载荷完成与 `math.symbol` 表达结果的桥接。
3. 关系层（`LinearRelation` / `QuadraticRelation`）已承接模型约束主路径。
4. canonical 类型补回（`CanonicalMonomial` / `CanonicalPolynomial`）用于兼容历史行为。

### E. Phase 1（正确性热修）
1. 修复 `LinearMonomial.evaluate(values, ...)` 系数丢失。
2. 修复 `QuadraticMonomial.evaluate(results|values, ...)` 多处分支系数问题。
3. 修复 `QuadraticMonomialCell.equals` 类型判断错误。
4. 新增 `MonomialCoefficientPreservationTest`（12 tests）。

### F. Phase 2（Context 架构对齐）
1. `AbstractMutableTokenTable` 增加生命周期方法：
   - `ensureFlattenContext()` / `ensureValueCacheContext()` / `ensureRangeCacheContext()`
   - `rebindContexts()` / `invalidateAllCaches()` / `invalidateSolutionCaches()`
2. `MutableTokenTable` 已实现上述方法。
3. `add(symbol)` 自动绑定、`remove(symbol)` 自动解绑已接入。

### G. Phase 3（已完成部分）
1. `flattenedMonomials` 已在 monomial/polynomial/inequality/symbol 主类型接入。
2. `Constraint` / `SubObject` / `TokenTable` 已切到 `flattenedMonomials` 主路径。
3. `cells` 已在核心类型标注 `@Deprecated(level = WARNING)`，作为过渡兼容层。
4. 已新增 `FlattenMigrationGuardTest`（12 tests）用于迁移守卫。

### H. M8/Phase 3 全量完成（2026-04-13）
1. **25 个函数符号类**（`core/function/`）：全部 `MathFunctionSymbol<T>` 泛型化
2. **全模块编译通过**：`mvn compile -DskipTests` BUILD SUCCESS ✅
3. **全量回归通过**：`mvn -pl ospf-kotlin-core -am test` — **Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅
4. **Framework 模块**：gantt-scheduling / bpp3d BUILD SUCCESS ✅
5. **Solver plugins**：copt / gurobi / scip BUILD SUCCESS ✅
6. `LinearInequality.kt`（math）修复：Flt64 infix 类型推断错误（4 行改为直接构造）

---

## 历史实施清单（P0-M9，仅供参考）

### P0：Phase 3 正确性阻断 ✅
- `[x]` 修复 `QuadraticMonomialSymbol.flattenedMonomials` 的 `constant * monomial` 分支
- `[x]` 重写 `QuadraticPolynomial` 对称项归并 key 为确定性规范化策略
- `[x]` 新增 3 组守卫测试（`FlattenMigrationGuardTest` 15 tests）
- 回归结果：`Tests run: 56, Failures: 0, Errors: 0, Skipped: 0`

### M1：统一代数内核 ✅
- `[x]` 新建 `FlattenUtility.kt`：merge/multiply/normalize 单点实现
- `[x]` Linear/QuadraticPolynomial 改用 utility
- 回归结果：`Tests run: 114, Failures: 0, Errors: 0, Skipped: 0`

### M2：表达层收口 ✅
- `[x]` 主链路无 `.cells` 计算依赖
- `[x]` `cells` 降级为兼容视图（`@Deprecated(level = WARNING)`）
- 回归结果：`Tests run: 118, Failures: 0, Errors: 0, Skipped: 0`

### M3：引入新关系类型 ✅
- `[x]` `Relation.kt`：`LinearRelation`/`QuadraticRelation` 接口及实现
- `[x]` `Constraint.kt` 新增关系对象构造器
- 回归结果：126 tests

### M4：机制层去旧泛型 ✅
- `[x]` `SubObject.kt` 新增 FlattenData 构造器
- `[x]` `SubObjectTest.kt`：4 tests

### M5：模型 API 迁移 ✅
- `[x]` `MetaModel.kt` 新增 `addConstraint(relation: ...)` 和 `addObject(flattenData: ...)`
- `[x]` `MechanismModel.kt` 新增 `addConstraint(relation: ...)`
- 回归结果：130 tests

### M6：函数符号迁移 ✅
- `[x]` R0 冻结扫描基线
- `[x]` R1 补齐 relation DSL 壳
- `[x]` R2 迁移 7 个显式 inequality 类型文件
- `[x]` R3 分批迁移 operator-only 函数符号
- `[x]` R4 清理 `.cells` 缓存预热
- `[x]` R5 删除 `frontend/inequality/adapter` + `frontend/inequality` 主目录
- 回归结果：91 tests

### M7：删除 adapter ✅
- `[x]` `frontend/inequality/adapter/` 已删除（3 文件）
- `[x]` `frontend/symbol_migration/adapter/` 测试已删除（2 文件）

### M8：最终目录删除 → **已重新定义为重写路线**
- `[x]` R7 分析结论：`monomial/polynomial` 为前端 DSL 核心，保留不删
- `[x]` R8/R9 编译修复完成
- `[-]` 剩余工作转入 Big-Bang 重写路线

### M9：封口门禁 → **已重新定义**
- `[ ]` 禁止旧路径 import 回流
- `[ ]` 更新迁移文档

### Phase 4：二次 dual/Farkas/cut + solver dual 输出闭环

**当前状态**：
- `QuadraticTetradModel.elastic()` — 已实现（1072-1394 行）✅
- `QuadraticTetradModel.dual()` — `TODO("not implemented yet")` ❌
- `QuadraticTetradModel.farkasDual()` — `TODO("not implemented yet")` ❌
- `LinearMechanismModel.generateOptimalCut()/generateFeasibleCut()` — 已实现 ✅
- `QuadraticMechanismModel.generateOptimalCut()` — `TODO("not implemented yet")` ❌
- `QuadraticMechanismModel.generateFeasibleCut()` — `TODO("not implemented yet")` ❌
- Solver 输出层（`FeasibleSolverOutput`）— **不包含 dual 值** ❌
- 插件 dual 读取：Gurobi/SCIP/CPLEX 已在 Benders/ColumnGeneration 中使用，COPT/MindOPT 缺失 ❌

**文件引用**：
| 文件 | 行号 | 状态 |
|------|------|------|
| `QuadraticTetradModel.kt` | 901-903 | `dual()` TODO |
| `QuadraticTetradModel.kt` | 905-907 | `farkasDual()` TODO |
| `MechanismModel.kt` | 811-819 | `generateOptimalCut()` TODO |
| `MechanismModel.kt` | 821-827 | `generateFeasibleCut()` TODO |
| `SolverOutput.kt` | 56 行 | 无 dual 字段 |
| `IISConfig.kt` | 10-25 | `threadNum`/`notImprovementTime` 定义但未消费 |

#### 4.1 `QuadraticTetradModel.dual()` 实现

**目标**：给定可行二次解，计算对应的 dual 变量（拉格朗日乘子）。

**算法思路**：
1. 基于当前 primal solution 计算 active constraints（松弛 = 0 的不等式）
2. 构造 KKT 线性系统：`∇f(x) + Σ λ_i ∇g_i(x) = 0`
3. 求解 λ（dual values），区分 linear dual 和 quadratic dual
4. 返回新的 `QuadraticTetradModel` 实例，携带 dual 值

**验收**：
- [ ] `dual()` 返回的模型携带 dual values
- [ ] 对凸二次问题，dual objective ≤ primal objective（弱对偶）
- [ ] 端到端测试：已知问题的 dual 值与手动计算一致

#### 4.2 `QuadraticTetradModel.farkasDual()` 实现

**目标**：给定不可行二次问题，计算 Farkas 证书（证明不可行的对偶向量）。

**算法思路**：
1. 基于 `elastic()` 方法的结果识别不可行源
2. 构造 Farkas 对偶系统：`A^T y = 0, b^T y < 0, y ≥ 0`
3. 返回携带 Farkas dual 值的 `QuadraticTetradModel`

**验收**：
- [ ] `farkasDual()` 返回的模型携带 Farkas 证书
- [ ] 对已知不可行问题，Farkas dual 可验证 `y^T b < 0`
- [ ] 端到端测试：不可行二次模型 → farkasDual → 验证证书

#### 4.3 `QuadraticMechanismModel.generateOptimalCut()` 实现

**目标**：基于二次子问题的 dual 解生成 Benders 最优割。

**签名**（已定义）：
```kotlin
fun generateOptimalCut(
    objective: Flt64,
    objectVariable: AbstractVariableItem<*, *>,
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
    dualSolution: QuadraticDualSolution,
): Ret<List<MathLinearInequality>>
```

**算法**：
1. 从 `dualSolution` 提取约束 dual 值
2. 计算割的系数：`c + Σ λ_i A_i`（目标梯度 + dual 加权约束）
3. 计算割的 RHS：`objective + Σ λ_i b_i`
4. 返回线性不等式列表

**验收**：
- [ ] 对已知二次子问题，生成的割排除当前主问题解
- [ ] 割不切掉任何可行解（有效性）
- [ ] 端到端 Benders 迭代测试

#### 4.4 `QuadraticMechanismModel.generateFeasibleCut()` 实现

**目标**：基于 Farkas dual 生成 Benders 可行割（当二次子问题不可行时）。

**签名**（已定义）：
```kotlin
fun generateFeasibleCut(
    fixedVariables: Map<AbstractVariableItem<*, *>, Flt64>,
    farkasDualSolution: QuadraticDualSolution,
): Ret<List<MathLinearInequality>>
```

**算法**：
1. 从 `farkasDualSolution` 提取 Farkas 证书
2. 构造可行割：排除导致子问题不可行的主问题解
3. 返回线性不等式列表

**验收**：
- [ ] 可行割排除当前不可行的主问题解
- [ ] Benders 循环中可行割 + 最优割交替收敛

#### 4.5 Solver 输出层补 dual 值

**目标**：在 `FeasibleSolverOutput` 中新增 dual 字段，并统一 linear/quadratic dual。

**操作**：
1. `SolverOutput.kt`：`FeasibleSolverOutput` 新增 `dualValues: Map<String, Flt64>?` 字段
2. 统一求解器输出：linear dual 和 quadratic dual 统一为约束名 → Flt64 映射
3. 插件适配：
   - Gurobi：已在 Benders 中读取 `Pi`，需在标准求解路径也读取
   - SCIP：已在 Benders 中读取 `getDual()`，需在标准求解路径也读取
   - CPLEX：已在 Benders 中读取 `getDual()`，需在标准求解路径也读取
   - COPT：新增 dual 读取
   - MindOPT：新增 dual 读取

**验收**：
- [ ] `FeasibleSolverOutput` 包含 `dualValues` 字段
- [ ] 所有 5 个插件（gurobi/scip/cplex/copt/mindopt）透传 dual
- [ ] 端到端测试：求解 LP → 验证 dual 值与手动计算一致

#### 4.6 Phase 4 总验收

- [ ] 二次子问题可从 solver 输出直接生成可行割与最优割
- [ ] 有端到端测试覆盖 full Benders loop（主问题 → 二次子问题 → dual → cut → 主问题）
- [ ] 无 P0/P1 缺陷挂起

---

### Phase 5：性能与稳定性优化

**当前状态**：
| 问题 | 文件 | 现状 | 风险 |
|------|------|------|------|
| Value cache key 使用大对象 | `TokenCacheContexts.kt` 84-168 | `Pair<Any, List<Flt64>?>` / `Pair<Any, Map<Symbol, Flt64>>` 作为哈希键 | 哈希冲突 + 序列化开销 |
| IIS 停止条件不完整 | `IISConfig.kt` 10-25 | `time` 已消费，`threadNum`/`notImprovementTime` 定义但未消费 | IIS 长跑无界 |
| ThreadLocal policy | `SolveValueConversionContext.kt` 5-22 | `ThreadLocal<SolveValueConversionPolicy?>` | 协程上下文切换时 policy 丢失 |

#### 5.1 重构 Value Cache Key

**问题**：`ValueCacheContext` 使用 `Pair<Any, List<Flt64>?>` 和 `Pair<Any, Map<Symbol, Flt64>>` 作为缓存键。`List` 和 `Map` 作为哈希键有两个问题：
1. `equals()`/`hashCode()` 需要遍历所有元素，O(n) 开销
2. 大 Map 的哈希计算在热点路径上成为瓶颈

**方案**：
1. 为 `List<Flt64>` 创建 `SolutionHash` 包装类：
   - 预计算哈希值（构造时一次性计算）
   - 使用增量哈希或 MurmurHash3 减少碰撞
2. 为 `Map<Symbol, Flt64>` 创建 `FixedValueHash` 包装类：
   - 使用 `Symbol.identifier`（UInt64）+ `Flt64` 位表示的有序序列
   - 预计算哈希，O(1) 比较
3. 缓存键改为 `Pair<Any, SolutionHash>` / `Pair<Any, FixedValueHash>`

**验收**：
- [ ] 缓存键构造时间 < 1μs（基准算例）
- [ ] 缓存命中率不下降（对比重构前）
- [ ] 无哈希碰撞导致的缓存误用（回归测试通过）

#### 5.2 IIS 真正消费停止条件

**问题**：`IISConfig.threadNum` 和 `IISConfig.notImprovementTime` 已定义但在 `Linear.kt` / `Quadratic.kt` 的 IIS 计算逻辑中未被消费。

**操作**：
1. `Linear.kt` / `Quadratic.kt` IIS 循环中新增：
   - `notImprovementTime` 计时器：记录上次 IIS 集合缩小的时间点
   - 超过 `notImprovementTime` 无改善时，提前退出 IIS 计算
   - 日志：`IIS stopped after X iterations, no improvement for Y ms`
2. `threadNum` 用于并行 IIS 候选集评估（可选，P2）

**验收**：
- [ ] IIS 长跑不会无界（`notImprovementTime` 强制退出）
- [ ] 停止条件日志输出可观测
- [ ] 基准算例 IIS 耗时不增加

#### 5.3 ThreadLocal Policy 改为协程上下文安全方案

**问题**：`SolveValueConversionContext.kt` 使用 `ThreadLocal<SolveValueConversionPolicy?>` 存储策略。在 Kotlin 协程中，协程可能在不同线程间切换，导致 policy 丢失。

**方案**（按优先级）：
1. **方案 A（推荐）**：使用 `kotlinx.coroutines.ThreadContextElement`
   - 创建 `SolveValueConversionContextElement : ThreadContextElement<SolveValueConversionPolicy?>`
   - 协程切换时自动传递 policy
2. **方案 B**：显式参数透传
   - 移除 ThreadLocal，所有需要 policy 的地方显式传入
   - 更安全，但 API 变更较大

**验收**：
- [ ] 协程切换线程后 policy 保持一致
- [ ] 并发求解下 policy 不串（不同协程互不干扰）
- [ ] 现有 `withSolveValueConversionPolicy` API 行为不变

#### 5.4 Phase 5 总验收

- [ ] IIS 长跑不会无界
- [ ] 并发求解下 policy 不串
- [ ] 缓存命中率与耗时在基准算例上可观测改善
- [ ] 无 P0/P1 缺陷挂起

---

### Phase 6：测试补齐与达标判定

#### 6.1 测试簇补齐

**当前测试**（17 个测试文件）：
| 测试文件 | 覆盖领域 |
|----------|---------|
| `QuadraticElasticModelTest.kt` | 二次弹性模型 ✅ |
| `InfeasibleOutputFieldsTest.kt` | 不可行输出字段 ✅ |
| `SolverOutputCompatibilityTest.kt` | 输出兼容性 ✅ |
| `SolveOptionsTest.kt` | 求解选项 ✅ |
| `SolverExtIISOptionsTest.kt` | IIS 选项 ✅ |
| `SolveValueConversionContextTest.kt` | 值转换上下文 ✅ |
| `SolveValueValidationTest.kt` | 值验证 ✅ |
| `TokenCacheContextsTest.kt` | Token 缓存上下文 ✅ |
| `ModelBuildingStatusBridgeTest.kt` | 建模状态桥接 ✅ |
| `PrepareCacheKeyRegressionTest.kt` | 缓存键回归 ✅ |
| `LinearPolynomialBaselineTest.kt` | 线性多项式基线 ✅ |
| `QuadraticPolynomialBaselineTest.kt` | 二次多项式基线 ✅ |
| `MultiObjectCallBackModelTest.kt` | 多目标回调 ✅ |
| `FlattenUtilityTest.kt` | 展平工具 ✅ |
| `MonomialCoefficientPreservationTest.kt` | 单项式系数保持 ✅ |
| `SubObjectTest.kt` | 子对象 ✅ |
| `ConstraintPriorityPropagationTest.kt` | 约束优先级 ✅ |

**需新增的测试簇**：

| 测试簇 | 测试文件 | 覆盖内容 | 优先级 |
|--------|---------|---------|--------|
| **correctness** | `QuadraticDualTest.kt` | `dual()` 数学等价性、弱对偶验证 | P0 |
| **correctness** | `QuadraticFarkasDualTest.kt` | `farkasDual()` 证书验证 | P0 |
| **correctness** | `QuadraticBendersCutTest.kt` | `generateOptimalCut()` / `generateFeasibleCut()` | P0 |
| **correctness** | `CoefficientPreservationTest.kt` | 全链路系数保持（已有部分） | P1 |
| **architecture** | `TokenCacheContextRebindTest.kt` | context 重绑定/隔离 | P1 |
| **architecture** | `CoroutinePolicyTest.kt` | 协程 policy 传递一致性 | P1 |
| **performance-guard** | `IISStoppingConditionTest.kt` | IIS 停止条件与超时退出 | P1 |
| **performance-guard** | `CacheKeyDegradationTest.kt` | 缓存键退化保护 | P2 |
| **compatibility** | 8 个 plugin 编译回归 | copt/gurobi/scip/cplex/mindopt + 二次插件 | P0 |
| **compatibility** | `SolverDualOutputTest.kt` | 所有插件 dual 输出一致性 | P0 |

#### 6.2 文档更新

| 文档 | 内容 | 优先级 |
|------|------|--------|
| 迁移说明 | Phase 4-6 变更对旧 API 的影响 | P1 |
| API 对照 | `LinearMechanismModel` vs `QuadraticMechanismModel` cut API 对照 | P1 |
| 已知不兼容点 | dual 输出格式变更、cache key 内部重构 | P1 |
| Rust 对齐文档 | 更新 dual/cut 实现差异 | P2 |

#### 6.3 达标判定（可宣称"达到 design.md"）

验收清单：
1. [ ] Phase 1~5 全部完成且无 P0/P1 缺陷挂起
2. [ ] Phase 4 两个关键钩子（`dual()` + `generateOptimalCut()`）已落地并有测试
3. [ ] 二次 dual/Farkas/cut 与 solver dual 输出链路闭环
4. [ ] Phase 5 三项优化（cache key、IIS、policy）均已落地
5. [ ] 全量回归命令通过并附结果记录：
   - `mvn -pl ospf-kotlin-core -am test` — 全绿
   - 8 plugin 编译 — BUILD SUCCESS
   - 框架模块编译 — BUILD SUCCESS

---

## 已知问题
1. SCIP 插件存在 `EventHandler/EventMask` API 兼容问题（当前迁移主线非阻断）。
2. 当前代码存在大量 `cells` 相关 deprecation warning（属过渡态，待旧目录删除后清零）。

---

## 代码组织结构变更

### 当前结构（frontend/backend 分轨）

```
core/
├── frontend/
│   ├── variable/              (7 文件) — 变量定义
│   ├── expression/
│   │   ├── monomial/          (3) — DSL 单项式
│   │   ├── polynomial/        (3) — DSL 多项式
│   │   ├── symbol/
│   │   │   ├── (2 主文件)     — IntermediateSymbol, SymbolCombination
│   │   │   ├── linear_function/   (33) — 旧函数符号
│   │   │   └── quadratic_function/ (21) — 旧二次函数符号
│   │   ├── bridge/            (8) — 操作符桥接
│   │   ├── adapter/           (5) — 适配器
│   │   ├── flatten/           (1) — 展平工具
│   │   └── dsl/               — DSL 入口
│   └── model/
│       ├── Model.kt, MultiObject.kt
│       ├── status/            (2)
│       ├── callback/          (2)
│       └── mechanism/         (18) — MetaModel, MechanismModel, TokenTable
├── backend/
│   ├── intermediate_model/    (6) — LinearTriadModel, QuadraticTetradModel
│   └── solver/                (30) — 求解器接口、配置、启发式、IIS、输出
└── function/                  (24) — MathFunctionSymbol<T> 新实现
```

**问题**：frontend/backend 分轨制造了"前端建模、后端求解"的错误认知。实际上整个 core 都是中间层——真正的后端是 solver 插件。

### 目标结构（5 模块扁平化）

```
core/
├── variable/                  ← frontend/variable/
├── intermediate_symbol/       ← function/ + expression/symbol/ + expression/flatten/
├── model/                     ← frontend/model/ (Model, callback, status)
├── intermediate_model/        ← frontend/model/mechanism/ + backend/intermediate_model/
└── solver/                    ← backend/solver/
```

### 文件映射表

| 新模块 | 来源路径 | 文件数 | 说明 |
|--------|---------|--------|------|
| **variable** | `frontend/variable/` | 7 | AbstractVariableItem, Token, TokenList, Type, VariableCombination, VariableRange |
| **intermediate_symbol** | `function/` | 24 | MathFunctionSymbol<T> 接口 + 全部实现 |
| | `expression/symbol/` 主目录 | 2 | IntermediateSymbol.kt（清理后）, SymbolCombination.kt |
| | `expression/flatten/` | 1 | FlattenUtility.kt |
| | `expression/symbol/linear_function/` | 33 | 全部标记 @Deprecated → 待删除 |
| | `expression/symbol/quadratic_function/` | 21 | 全部标记 @Deprecated → 待删除 |
| **model** | `frontend/model/Model.kt` | 1 | Model 接口 |
| | `frontend/model/MultiObject.kt` | 1 | 多目标 |
| | `frontend/model/callback/` | 2 | CallBackModel, CallBackModelInterface |
| | `frontend/model/status/` | 2 | 建模阶段/状态 |
| **intermediate_model** | `frontend/model/mechanism/` | 18 | MetaModel, MechanismModel, TokenTable, TokenCacheContexts 等 |
| | `backend/intermediate_model/` | 6 | LinearTriadModel, QuadraticTetradModel, ModelFileFormat 等 |
| **solver** | `backend/solver/` 全部 | 30 | 求解器接口、配置、启发式、IIS、输出、value |

### 不纳入 5 模块的内容（DSL 层，保留但不移动）

| 路径 | 文件数 | 去向 |
|------|--------|------|
| `expression/monomial/` | 3 | **保留不动** — DSL 单项式，用户建模入口 |
| `expression/polynomial/` | 3 | **保留不动** — DSL 多项式，用户建模入口 |
| `expression/bridge/` | 8 | **保留不动** — DSL 操作符桥接 |
| `expression/adapter/` | 5 | **保留不动** — DSL 适配器 |
| `expression/dsl/` | — | **保留不动** — DSL 入口 |

这些 DSL 文件是**用户建模入口**，不属于中间层。它们提供 `Variable * coeff + Variable2` 这样的建模语法。

### 包名变更

| 旧包名 | 新包名 |
|--------|--------|
| `fuookami.ospf.kotlin.core.frontend.variable` | `fuookami.ospf.kotlin.core.variable` |
| `fuookami.ospf.kotlin.core.function` | `fuookami.ospf.kotlin.core.intermediate_symbol.function` |
| `fuookami.ospf.kotlin.core.frontend.expression.symbol` | `fuookami.ospf.kotlin.core.intermediate_symbol` |
| `fuookami.ospf.kotlin.core.frontend.expression.symbol.linear_function` | `fuookami.ospf.kotlin.core.intermediate_symbol.legacy.linear_function` |
| `fuookami.ospf.kotlin.core.frontend.expression.symbol.quadratic_function` | `fuookami.ospf.kotlin.core.intermediate_symbol.legacy.quadratic_function` |
| `fuookami.ospf.kotlin.core.frontend.expression.flatten` | `fuookami.ospf.kotlin.core.intermediate_symbol.flatten` |
| `fuookami.ospf.kotlin.core.frontend.model` | `fuookami.ospf.kotlin.core.model` |
| `fuookami.ospf.kotlin.core.frontend.model.callback` | `fuookami.ospf.kotlin.core.model.callback` |
| `fuookami.ospf.kotlin.core.frontend.model.status` | `fuookami.ospf.kotlin.core.model.status` |
| `fuookami.ospf.kotlin.core.frontend.model.mechanism` | `fuookami.ospf.kotlin.core.intermediate_model` |
| `fuookami.ospf.kotlin.core.backend.intermediate_model` | `fuookami.ospf.kotlin.core.intermediate_model` |
| `fuookami.ospf.kotlin.core.backend.solver` | `fuookami.ospf.kotlin.core.solver` |

### 执行顺序

包重命名是整个项目最大的变更（影响全项目 import），应在**阶段 0（补齐函数符号）+ 阶段 1（函数符号废弃）+ 阶段 5（框架迁移）+ 阶段 6（删除旧目录）** 完成后、**阶段 7（回归）之前**执行。

或者更激进的做法：**先改名再重写**。一次性完成，不维持两个包名的双轨。

---

## 下一步行动计划（Big-Bang 重写路线 — 细化版）

### 阶段 0：补齐函数符号（B3 剩余，~7 文件）

**目标**：创建剩余 7 个函数符号到 `intermediate_symbol/function/`，使新系统覆盖所有旧函数。

| 步骤 | 函数 | 约束模式 | 参考旧文件 | 预估行数 |
|------|------|---------|-----------|---------|
| 1 | `BalanceTernaryzationFunction` | BigM 三值分解（y in {-1,0,1}） | `legacy/linear_function/BalanceTernaryzation.kt` | 150-180 |
| 2 | `InStepRangeFunction` | 分段阶跃：在 [lo,hi) 内返回 1 | `legacy/linear_function/InStepRangeFunction.kt` | 80-100 |
| 3 | `SlackRangeFunction` | SlackRange (quadratic 变体) | `legacy/quadratic_function/SlackRange.kt` | 120-150 |
| 4 | `InequalityFunction` | 不等式满足量 | `legacy/linear_function/Inequality.kt` | 100-120 |
| 5 | `SatisfiedAmountInequalityFunction` | 复杂组合（InListFunction + AndFunction） | `legacy/linear_function/SatisfiedAmountInequality.kt` | 200-250 |
| 6 | `SinFunction` | 委托 UnivariateLinearPiecewise | `legacy/linear_function/Sin.kt` (3 行 stub) | 50-60 |
| 7 | `CosFunction` | 委托 UnivariateLinearPiecewise | `legacy/linear_function/Cos.kt` (3 行 stub) | 50-60 |

**验收**：`legacy/linear_function/` 33 文件和 `legacy/quadratic_function/` 22 文件中，所有函数符号类都有 `function/` 对应实现。

---

### 阶段 1：intermediate_symbol 模块重写（B3 核心）

**当前状态**：
- `intermediate_symbol/` 主文件：2（`IntermediateSymbol.kt` 1169 行，`SymbolCombination.kt` ~520 行）
- `legacy/linear_function/`：33 文件，19,667 行
- `legacy/quadratic_function/`：21 文件，10,453 行
- 总计：56 文件，~30,120 行
- `function/` 新系统：24 文件，4,561 行（已有 22 个函数符号类）

#### 1.1 IntermediateSymbol.kt 重写

**当前问题**：
- 1169 行，包含 `IntermediateSymbol`、`LinearIntermediateSymbol`、`QuadraticIntermediateSymbol`、`ExpressionSymbol`、`LinearExpressionSymbol`、`QuadraticExpressionSymbol`、`FunctionSymbol` 等多个类
- 深度耦合旧 monomial/polynomial 类型（`ToLinearPolynomial`、`ToQuadraticPolynomial` 接口）
- `cells` 属性已标记 `@Deprecated`，但 `flattenedMonomials` 仍使用旧类型

**重写方案**：

| 子步骤 | 操作 | 变更 | 验收 |
|--------|------|------|------|
| 1.1.1 | 保留 `IntermediateSymbol` 接口作为**桥接接口** | 移除 `Expression` 继承，仅保留 `Symbol` 继承；移除 `cells` 属性；保留 `category`、`dependencies`、`prepare()`、`flush()` | 编译通过，`cells` 零引用 |
| 1.1.2 | 标记 `LinearIntermediateSymbol` / `QuadraticIntermediateSymbol` 为 `@Deprecated` | `ReplaceWith` 指引使用 `MathFunctionSymbol<T>`；保留接口以兼容框架 | 框架文件仍有 `@Suppress`，但不新增使用点 |
| 1.1.3 | 标记 `LinearExpressionSymbol` / `QuadraticExpressionSymbol` 为 `@Deprecated` | 这些是 DSL 表达式构建器，框架代码仍在使用；保留但标记废弃 | 框架文件 `@Suppress("DEPRECATION")` 覆盖 |
| 1.1.4 | 删除 `FunctionSymbol` / `LogicFunctionSymbol` / `LinearFunctionSymbol` / `LinearLogicFunctionSymbol` / `QuadraticFunctionSymbol` / `QuadraticLogicFunctionSymbol` 基类 | 旧函数符号基类，已被 `MathFunctionSymbol<T>` 替代 | 无外部引用，旧函数文件改用 `@Suppress` |
| 1.1.5 | `ExpressionSymbol` 标记 `@Deprecated` | DSL 表达式包装器 | 框架文件 `@Suppress` 覆盖 |

#### 1.2 legacy/linear_function/ 目录重写

**当前状态**：33 文件，19,667 行。其中 24 个已有 `function/` 对应实现。

**重写策略**：按函数类型分组处理。

| 组别 | 文件 | 状态 | 处理方式 |
|------|------|------|---------|
| 已有对应实现（24 文件） | And, Or, Not, Xor, If, Max, Min, Slack, Abs, Semi, Floor, Ceiling, Rounding, Mod, IfIn, IfThen, OneOf, First, SameAs, SatisfiedAmount, Masking, Binaryzation, UnivariateLinearPiecewise, BivariateLinearPiecewise, Sigmoid | ✅ 已创建 | 全部添加 `@Deprecated("Use intermediate_symbol.function.X instead")` |
| 缺失实现（7 文件） | BalanceTernaryzation, InStepRangeFunction, Inequality, SlackRange, MaskingRange, SatisfiedAmountInequality | ❌ 待创建 | **阶段 0** 创建到 `function/` |
| Stub 文件（2 文件） | Sin.kt (3 行), Cos.kt (3 行) | ⚠️ 空壳 | 直接删除，功能已包含在 Sigmoid 分段点中 |

**具体操作**：

1. 对 24 个已有对应实现的旧文件：添加 `@file:Suppress("DEPRECATION")` + `@Deprecated` 类注解
2. 对 7 个缺失文件：**阶段 0** 完成后同样处理
3. 对 2 个 stub 文件：直接删除

#### 1.3 legacy/quadratic_function/ 目录重写

**当前状态**：21 文件，10,453 行。其中 15 个有线性版对应实现。

**重写策略**：二次变体函数统一合并到线性版中。

| 文件 | 处理方式 | 理由 |
|------|---------|------|
| Binaryzation, BivariateLinearPiecewise, Ceiling, Floor, Masking, MaskingRange, Max, Min, Mod, Rounding, Semi, Sigmoid, Slack, SlackRange, UnivariateLinearPiecewise | 标记 `@Deprecated`，指引使用 `function/` 线性版 | `MathFunctionSymbol` 的 `LinearPolynomial<T>` 已支持二次输入，无需独立二次版 |
| Inequality (2 行) | 直接删除 | 空文件 |
| Linear (295 行) | 分析后决定 | 检查是否为纯线性包装器，如是则标记废弃 |
| Product (331 行) | 分析后决定 | 检查是否已被 `math.symbol` 的乘法操作符覆盖 |
| Cos, Sin (3 行各) | 直接删除 | stub |

#### 1.4 SymbolCombination.kt 重写

**当前状态**：~520 行，包含 `AbstractSymbolCombination`、`SymbolCombination`、`QuantitySymbolCombination`。

**操作**：
- 保留（DSL 核心能力，提供 `map`/`flatMap` 组合器）
- 更新 import：将 `LinearMonomial` 相关类型替换为 `math.symbol.monomial.LinearMonomial<Flt64>` 的桥接
- 添加对 `MathFunctionSymbol` 的 `map` 支持

#### 阶段 1 验收标准

- [ ] `function/` 包含所有旧函数的 `MathFunctionSymbol<T>` 实现
- [ ] `legacy/linear_function/` 全部文件标记 `@Deprecated`
- [ ] `legacy/quadratic_function/` 全部文件标记 `@Deprecated` 或删除
- [ ] `IntermediateSymbol.kt` 中 `FunctionSymbol` 基类已删除
- [ ] 无新增旧 monomial/polynomial import 到 `function/`

---

### 阶段 2：Variable 模块（B2 细化）

**当前状态**：
- Kotlin：`AbstractVariableItem.kt` (145 行)、`Type.kt` (119 行)、`VariableIndependentItem.kt` (46 行)、`VariableCombinationItem.kt` (306 行)、`VariableRange.kt` (182 行)、`Token.kt` (90 行)、`TokenList.kt` (360 行)，总计 ~1,248 行
- Rust：`variable_type.rs`、`variable_item.rs`、`variable_range.rs`，更精简的设计

**Kotlin vs Rust 差异**：

| 维度 | Kotlin | Rust | 对齐方向 |
|------|--------|------|---------|
| 类型参数 | `<T, Type>` 双泛型 | 关联类型 `Value` | 保持 Kotlin（类型安全更强） |
| 变量 ID | `identifier: UInt64` + `index: Int` | `VariableId` 结构体 | 保持 Kotlin（支持变量组） |
| 克隆 | 标准 copy | `Arc::clone()` 共享 | 保持 Kotlin（不可变场景够用） |
| 多维变量 | `VariableCombination` | 无 | **保留 Kotlin 独有功能** |
| 物理量变量 | `QuantityVariable` | 无 | **保留 Kotlin 独有功能** |

**操作**：
- **不需要重写**：variable 模块已是成熟设计，且有多维变量和物理量等 Rust 没有的优势功能
- `AbstractVariableItem` 已实现 `Symbol`（第 53 行），无需额外转换方法
- **仅需对齐**：统一命名和补齐 TokenTable 辅助方法
- **包迁移**：`frontend.variable` → `variable`

| 子步骤 | 操作 | 文件 |
|--------|------|------|
| 2.1 | 统一 `VariableType.isIntegerType` 命名（与 Rust `is_integer()` 对齐） | `Type.kt` |
| 2.2 | 包路径重命名：`frontend/variable/` → `variable/` | 全部 7 文件 |
| 2.3 | 更新全项目 import | grep + replace |

---

### 阶段 3：Token 模块（B2 细化）

Token 在 `variable/` 目录下（`Token.kt` + `TokenList.kt`）。TokenTable 在 `intermediate_model/` 下。

**当前状态**：
- Kotlin：`Token` (90 行)、`TokenList` (360 行)、`TokenTable` (1,522 行)、`TokenCacheContexts`（未计入）
- Rust：`token.rs`、`token_list.rs`、`token_table.rs`，更精简

**Kotlin vs Rust 差异**：

| 维度 | Kotlin | Rust | 对齐方向 |
|------|--------|------|---------|
| 结果存储 | 可变属性 + 回调 | `RwLock<Option<V>>` | 保持 Kotlin（回调机制更有用） |
| Token 查找 | `Map<VariableItemKey, Token>` | `HashMap<VariableId, usize>` + Vec | **优化**：添加索引加速 |
| 并发变体 | `ConcurrentAutoTokenTable` / `ConcurrentManualAddTokenTable` | `ConcurrentTokenTable` | 保持 Kotlin（已有） |
| 按类型分组 | 无 | `tokens_by_type()` | **补充**：添加按变量类型分组 |
| Token 缓存上下文 | 4 种缓存（value/range/linear flatten/quadratic flatten） | Lazy 上下文 | 保持 Kotlin（更丰富） |

**具体操作**：

| 子步骤 | 操作 | 文件 | 变更 |
|--------|------|------|------|
| 3.1 | 为 `TokenList` 添加 `tokensByType(varType: VariableType): List<Token>` | `TokenList.kt` | 新增方法 |
| 3.2 | 优化 `find()` 性能：添加 `Int` 索引作为主键 | `TokenList.kt` | `Map<VariableItemKey, Token>` → 添加 `Map<Int, Token>` 二级索引 |
| 3.3 | 统一 `TokenTable` 接口：添加 `register()` / `unregister()` 方法签名 | `TokenTable.kt` | 对齐 Rust 接口命名 |
| 3.4 | 清理 `TokenCacheContexts` 中对旧 `cells` 属性的依赖 | `TokenCacheContexts.kt` | 已全部迁移到 `flattenedMonomials`，验证零残留 |

---

### 阶段 4：中间模型重写（B4 细化）

**当前状态**：
- Kotlin：`MetaModel.kt` (1,219 行)、`TokenTable.kt` (1,522 行)、`CallBackModel.kt` (658 行)、`CallBackModelInterface.kt` (141 行)、`backend/intermediate_model/` (6 文件)
- Rust：`basic_model.rs`、`meta_model.rs`、`callback/` 目录

**Kotlin vs Rust 差异**：

| 维度 | Kotlin | Rust | 对齐方向 |
|------|--------|------|---------|
| 层次结构 | 接口继承链 | 组合模式（MetaModel 包装 BasicModel） | **保持 Kotlin**（接口更灵活） |
| Token 存储 | `AbstractMutableTokenTable` | `Vec<Token<V>>` + `HashMap` | 保持 Kotlin |
| 符号依赖追踪 | 隐式（`FunctionSymbolRegistrationScope`） | 显式 `HashMap<u64, HashSet<u64>>` | **改进**：添加显式依赖追踪 |
| 导出 | `suspend fun export()` | 无 | **保留 Kotlin 独有功能** |
| Benders 分解 | 6 个求解器插件 | 仅框架基础结构 | **保留 Kotlin 核心优势** |
| 回调模型 | Policy-based + Extractor | Executor/Provider 回调 | 保持各自设计 |
| 多目标 | `MultiObjectCallBackModel` | `MultiObjectiveModelInterface` | 保持 Kotlin（更完整） |

**具体操作**：

| 子步骤 | 操作 | 文件 | 变更 |
|--------|------|------|------|
| 4.1 | 为 `AbstractMetaModel` 添加显式符号依赖追踪 | `MetaModel.kt` | 新增 `symbolDependencies: MutableMap<UInt64, MutableSet<UInt64>>` |
| 4.2 | 将 `add(symbol: IntermediateSymbol)` 标记 `@Deprecated` | `MetaModel.kt` | `ReplaceWith` 指引使用 `addSymbol(symbol: MathFunctionSymbol<T>)` |
| 4.3 | 新增 `addSymbol(function: MathFunctionSymbol<T>): Try` | `MetaModel.kt` | 新入口，内部调用 `function.register(this)` |
| 4.4 | 清理 `MetaModel` 中对旧 `LinearInequality` / `QuadraticInequality` 的残留引用 | `MetaModel.kt` | 仅保留 `MathLinearInequality` / `MathQuadraticInequality` |
| 4.5 | `CallBackModel` 添加对 `MathFunctionSymbol` 的支持 | `CallBackModel.kt` | 新增 `addConstraint(symbol: MathFunctionSymbol<T>)` |
| 4.6 | 为 `MechanismModel` 添加显式 `generateBendersCut()` 方法文档 | `MechanismModel.kt` | 文档 + KDoc，标记为 Kotlin 独有功能 |
| 4.7 | 合并 `backend/intermediate_model/` 到 `intermediate_model/` | 目录移动 | 6 文件 |

---

### 阶段 5：框架迁移（B5 细化）

**当前状态**：
- gantt-scheduling：~46 文件引用旧函数符号
- bpp3d：~4 文件引用旧函数符号
- 全部使用 `@file:Suppress("DEPRECATION")`

**迁移策略**：按模块逐个迁移，每个模块编译通过后再进入下一个。

| 步骤 | 模块 | 文件数 | 迁移内容 | 预估工作量 |
|------|------|--------|---------|-----------|
| 5.1 | bpp3d-domain-layer-assignment-context | 4 | ItemDemandConstraint, Assignment, Capacity, Load | 0.5 天 |
| 5.2 | gantt-scheduling-domain-task-compilation-context | 14 | Makespan, Switch, TaskTime, Compilation 等 | 1 天 |
| 5.3 | gantt-scheduling-domain-resource-context | 8 | Resource, ConnectionResource, ExecutionResource 等 | 0.5 天 |
| 5.4 | gantt-scheduling-domain-produce-context | 10 | Produce, Consumption, CapacitySchedulingProduce 等 | 0.5 天 |
| 5.5 | gantt-scheduling-domain-capacity-scheduling-context | 4 | Capacity, CapacityCompilation 等 | 0.5 天 |
| 5.6 | gantt-scheduling-domain-bunch-compilation-context | 2 | TaskTime, Compilation | 0.5 天 |

**每个模块的迁移步骤**：
1. 扫描使用的旧函数符号（`AbsFunction`, `SlackFunction`, `MaxFunction` 等）
2. 替换为 `intermediate_symbol.function/` 新实现（`AbsFunction<Flt64>`, `SlackFunction<Flt64>`, `MaxFunction<Flt64>`）
3. 更新 import 路径（`frontend.*` → 新路径）
4. 检查 API 变更（构造函数参数、属性名）
5. 编译验证
6. 移除该模块的 `@file:Suppress("DEPRECATION")`

**风险控制**：
- 每步迁移后立即编译验证
- 遇到 API 不兼容时，优先在新函数符号中添加兼容构造函数，而非修改框架代码
- 保留一个版本的应急开关（可选）

---

### 阶段 6.5：删除前功能核对（新增）

**在阶段 6 执行删除之前，必须逐文件逐功能核对，确认新代码已覆盖旧代码的全部行为。**

**核对方法**：

| 步骤 | 操作 | 验收 |
|------|------|------|
| 6.5.1 | 对每个旧函数符号文件，列出其**全部公共 API**（类、构造函数、属性、方法） | 生成核对清单 |
| 6.5.2 | 逐一对应到新 `function/` 实现 | 每个 API 都有对应 |
| 6.5.3 | 核对**约束生成的逻辑分支**（固定值模式、extract 模式、threshold 模式等） | 每个分支都有对应实现 |
| 6.5.4 | 核对**helper 变量创建逻辑**（变量类型、范围设置、条件创建） | 变量创建逻辑一致 |
| 6.5.5 | 核对**evaluate() 返回值** | 给定相同输入，新旧返回值一致 |
| 6.5.6 | 核对**边界条件处理**（null 值、空输入、极端值） | 边界行为一致 |
| 6.5.7 | 为每个旧函数符号编写**行为对比测试** | 新旧输出对比通过 |

**核对清单模板**（每个旧文件一个）：

```
文件: BalanceTernaryzation.kt
新实现: BalanceTernaryzationFunction<T>

□ 公共类/构造函数签名一致
□ 所有公共属性/方法一致
□ 约束生成: 分支 1 (xxx) → 新实现
□ 约束生成: 分支 2 (xxx) → 新实现
□ 约束生成: 分支 3 (xxx) → 新实现
□ helper 变量创建逻辑一致
□ evaluate() 返回值对比通过
□ 边界条件: null/极端值 一致
□ 行为对比测试通过
```

**只有核对清单全部打勾的文件才能删除。** 如有缺失项，必须在删除前补齐。

---

### 阶段 6：包重命名 + 删除旧目录（B6 细化）

**前提条件**：
- 阶段 0-5 全部完成
- 框架代码零引用旧函数符号
- `@file:Suppress("DEPRECATION")` 全部移除

**包重命名**（全局 import 替换）：

| 旧包 | 新包 | 影响范围 |
|------|------|---------|
| `frontend.variable` | `variable` | core 内部 + 框架 |
| `frontend.model` | `model` | core 内部 + 框架 |
| `frontend.model.mechanism` | `intermediate_model` | core 内部 + 框架 |
| `frontend.expression.symbol` | `intermediate_symbol` | core 内部 + 框架 |
| `frontend.expression.symbol.linear_function` | `intermediate_symbol.legacy.linear_function` | core 内部（仅废弃引用） |
| `frontend.expression.symbol.quadratic_function` | `intermediate_symbol.legacy.quadratic_function` | core 内部（仅废弃引用） |
| `backend.intermediate_model` | `intermediate_model` | core 内部 |
| `backend.solver` | `solver` | core 内部 + 插件 |
| `function` | `intermediate_symbol.function` | core 内部 + 框架 |

**删除清单**：

| 路径 | 文件数 | 行数 | 操作 |
|------|--------|------|------|
| `intermediate_symbol/legacy/linear_function/` | 33 | 19,667 | 全部删除 |
| `intermediate_symbol/legacy/quadratic_function/` | 21 | 10,453 | 全部删除 |
| `intermediate_symbol/IntermediateSymbol.kt` 中的废弃类 | — | ~800 | 删除废弃类（FunctionSymbol 基类等），保留 ExpressionSymbol |

**不删除**（永久保留）：
- `expression/monomial/` — DSL 核心
- `expression/polynomial/` — DSL 核心
- `expression/symbol/ExpressionSymbol` — DSL 表达式包装器（标记废弃）
- `expression/symbol/SymbolCombination.kt` — DSL 组合器（移至 intermediate_symbol/）
- `expression/bridge/` — DSL 操作符桥接
- `expression/adapter/` — DSL 适配器

**操作**：
1. 全局 import 替换（grep + sed 批量替换）
2. 确认零引用（`grep` 全项目搜索旧路径）
3. 删除废弃文件
4. 清理残留 import
5. 编译验证到全绿

---

### 阶段 7：集中回归（B7-B8）

| 步骤 | 操作 | 命令 | 预期 |
|------|------|------|------|
| 7.1 | 阶段小回归 | `mvn -pl ospf-kotlin-core "-Dtest=..." test` | 全绿 |
| 7.2 | 全量回归 | `mvn -pl ospf-kotlin-core -am test` | 91 tests, 0 failures |
| 7.3 | 框架编译 | `mvn compile -pl ospf-kotlin-framework -am` | BUILD SUCCESS |
| 7.4 | 插件编译 | `mvn compile -pl ospf-kotlin-core-plugin/ospf-kotlin-core-plugin-copt,ospf-kotlin-core-plugin-gurobi,ospf-kotlin-core-plugin-scip -am` | BUILD SUCCESS |
| 7.5 | 按优先级修复 | 阻断 > 功能 > 性能 | 全绿 |

---

### 阶段 8：封口门禁（B9）

| 步骤 | 操作 | 验收 |
|------|------|------|
| 8.1 | CI 守卫：禁止 `intermediate_symbol/legacy/linear_function` import | 新增 import 时 CI 失败 |
| 8.2 | CI 守卫：禁止 `intermediate_symbol/legacy/quadratic_function` import | 新增 import 时 CI 失败 |
| 8.3 | CI 守卫：禁止 `.cells` 参与主计算 | 新增 `.cells` 调用时 CI 失败 |
| 8.4 | 更新迁移文档 | 文档明确 "core 是中间层，符号运算由 math.symbol 提供" |
| 8.5 | 更新 Rust 对齐文档 | 记录 Kotlin 独有功能（物理量、Benders、多维变量） |

---

## 回归命令

1. 阶段小回归：
   `mvn -pl ospf-kotlin-core "-Dtest=MonomialCoefficientPreservationTest,FlattenMigrationGuardTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest,InequalityNormalizeBaselineTest,TokenCacheContextsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

2. 全量回归：
   `mvn -pl ospf-kotlin-core -am test`

3. 插件编译检查：
   `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`

## 止损条件

1. 阶段 0 后函数符号层仍大量依赖旧 inequality 路径
2. 阶段 6 后 1 天内无法恢复 core 可编译状态
3. 阶段 7 后核心回归仍存在阻断级失败

回退策略：回退到阶段 0 冻结点或上一个稳定阶段，24h 内决策是否继续 big-bang 或切回增量路线。

---

## 最终完成标准

1. `core` 按 5 模块组织：`variable`、`intermediate_symbol`、`model`、`intermediate_model`、`solver`
2. `intermediate_symbol/function/` 包含所有旧函数的 `MathFunctionSymbol<T>` 实现
3. 无旧路径 import（`frontend.variable`、`frontend.model`、`frontend.expression.symbol`、`backend.*`）
4. `mvn -pl ospf-kotlin-core -am test` 通过
5. 插件编译检查通过
6. `cell`、`value`、`range` 运行期处理集中在 `intermediate_model` 上下文
