# OSPF Kotlin Core Daily

日期：2026-04-13
交接目标：下一个执行环境
Rust 对齐参考：`E:\workspace\ospf-rust`

## 当前结论
1. `Phase 1`（正确性热修）已完成并通过回归。
2. `Phase 2`（Context 生命周期）已完成并接入主链路。
3. `Phase 3` 已完成基础迁移与主路径切换，但尚未收口到”可删除 `expression(非symbol)+inequality` 目录”。
4. **`P0`（Phase 3 正确性阻断）已完成** ✅
5. **`M1`（统一代数内核，历史阶段名）已完成** ✅
6. **`M2`（表达层收口，历史阶段名）已完成** ✅
7. **`M3`（引入新关系类型）已完成** ✅
8. **`M4`（机制层去旧泛型）已完成** ✅
9. **`M5`（模型 API 迁移）已完成** ✅
10. **`M6`（函数符号迁移）R0-R4 已完成，R3 已完成** ✅ - 按 `R0~R4` 分批落地
11. **`M7`（删除 adapter）inequality/adapter 目录和测试已删除** ✅
12. **`M8`（最终目录删除）进入重拆执行版** - 按 `R5~R9` 实施
13. **`M8/Phase3`（math.symbol 函数符号层）进行中** - SlackFunction 编译通过

**下一步行动**：
1. 继续创建剩余 math 函数符号：Masking.kt, If.kt, And.kt, Or.kt, Max.kt, MinMax.kt, Binaryzation.kt
2. 创建 MathSymbolContainers.kt（math 版 LinearExpressionSymbols1/2, LinearIntermediateSymbols1/2）
3. 框架代码迁移（import 替换，64 文件）
4. 验证：mvn compile + mvn test

**历史行动（已完成）**：
1. ~~进入 `M6/R3`，迁移函数符号 off `eq/leq/geq` DSL~~ → ~~**M6/R3 进行中，编译未通过**~~ → **M6/R3 已完成** ✅
2. ~~优先修复 M6/R3 编译错误~~
3. ~~删除 `frontend/inequality/adapter/` 目录~~ → **已删除（adapter 源码和测试）** ✅
4. ~~删除 `frontend/inequality/Judgement.kt`~~ → **已删除（无任何依赖者）** ✅
5. ✅ **`frontend/inequality` 主目录已完全删除** — 外部模块（framework、plugins）已迁移，零引用
6. ~~按 `R6 -> R9` 完成目录级删除与 M8 收口~~ → **R7 分析结论：monomial/polynomial 不应删除，需重新定义 M8**
7. 最后执行 `Phase 4 -> Phase 6` 功能与稳定化收敛。

**架构边界（2026-04-09 明确）**：
1. 线性与二次型符号运算能力由 `math.symbol` 提供。
2. `ospf-kotlin-core` 负责变量、中间值、模型装配与桥接，不再扩展新的符号运算实现。
3. 原 `monomial/polynomial` 的额外能力（`cell` 直接使用 `math.symbol.monomial`、`value`、`range`）迁移到元模型上下文统一处理（`TokenCacheContexts` / `MetaModel` / `MechanismModel`）。

### M8/Phase 3 详情（2026-04-13）

**新建文件**：
- `core/function/FunctionSymbol.kt` — `MathFunctionSymbol` 接口 + `evaluate()` 扩展
- `core/function/Slack.kt` — math 版 SlackFunction（编译通过 ✅）

**SlackFunction 设计**：
- 分解：`x - y = neg - pos`，创建 helper 变量 negVar/posVar
- `polyX` = `x + negVar - posVar`，约束 `polyX eq y`
- threshold mode：`x + negVar >= y` 或 `x - posVar <= y`
- 暴露 `neg`/`pos` 属性为 `LinearPolynomial<Flt64>?`，供框架引用
- 支持类型：UContinuous（URealVar）/ UInteger（UIntVar）

**验证**：
- `mvn compile -pl ospf-kotlin-core`：BUILD SUCCESS ✅

**剩余工作**：
- 6 个函数符号文件待创建
- MathSymbolContainers.kt 待创建
- 框架代码 import 迁移（64 文件）

---

## 最新状态（2026-04-12 交接点）

### M8/Phase 3：Deprecated 标记与框架 Suppress（2026-04-12）

**核心模块 deprecated 标记**：
1. `LinearPolynomial.kt`：~153 个 standalone extension 函数标记 `@Deprecated`
   - 保留：sum/sumVars/sumSymbols/flatSum/qtySum/flatQtySum 聚合工具函数（无 bridge 等价物）
   - 添加 `@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")`
2. `QuadraticPolynomial.kt`：~281 个 standalone extension 函数标记 `@Deprecated`
   - 保留：qsum 系列聚合函数
3. `LinearMonomial.kt`：~68 个 standalone extension 函数标记 `@Deprecated`
4. `QuadraticMonomial.kt`：~59 个 standalone extension 函数标记 `@Deprecated`
5. 44 个 core symbol 函数文件添加 `@file:Suppress("DEPRECATION")`

**框架模块 suppress 注解**：
- 185 个框架文件（gantt-scheduling + bpp3d）添加 `@file:Suppress("DEPRECATION")`
- 覆盖所有使用 deprecated monomial/polynomial operator 的文件

**验证**：
- `mvn compile -DskipTests`：BUILD SUCCESS（全模块）✅
- `mvn -pl ospf-kotlin-core -am test`：BUILD SUCCESS ✅

### M8/Phase 4 分析结论：框架文件不可迁移（2026-04-12）

**结论**：框架文件（gantt-scheduling + bpp3d）当前**无法迁移**到 bridge layer math.types，`@file:Suppress("DEPRECATION")` 是正确最终状态。

**原因**：
1. **Model API 仍接受前端类型**：
   - `model.minimize(polynomial: AbstractLinearPolynomial<*>)`
   - `model.add(symbol: IntermediateSymbol)`
   - bridge 层产生的 `math.symbol.LinearPolynomial<Flt64>` 无法传入这些 API
2. **类型层次不兼容**：`math.symbol.polynomial.LinearPolynomial<Flt64>` 不继承 `frontend.expression.polynomial.AbstractLinearPolynomial<*>`
3. **符号容器无对应物**：`LinearIntermediateSymbols1/2`、`SlackFunction`、`MaskingFunction` 等在 math.symbol 中无等价类型
4. **Symbol 容器工厂 lambda** 返回前端类型，需要逐文件改造

**架构确认**：
- `math.symbol` 提供**通用符号代数**能力（纯数学运算、求值、微分等）
- `frontend.expression` 提供**优化建模 DSL** 能力（变量构造、表达式组合、token 集成、约束注册）
- 两者是互补关系，不是替代关系
- 框架文件使用 `frontend.expression.monomial/polynomial` 是**合理的**

**影响**：
- M8 Phase 4 框架迁移任务**终止**
- `frontend.expression.monomial/polynomial` 目录**永久保留**
- 框架文件的 `@file:Suppress("DEPRECATION")` 为长期解决方案
- 未来如需迁移，需要先将 Model API 改为接受 math.symbol 类型

### M8/Phase 2 进展（2026-04-12 第一轮）

**MechanismModel.kt 迁移到 math 类型**：
1. `generateOptimalCut` / `generateFeasibleCut` 方法从 `frontend.LinearMonomial` / `frontend.LinearPolynomial` 改为 `math.symbol.LinearMonomial<Flt64>` / `math.symbol.LinearPolynomial<Flt64>`
2. 相应不等式构造改为 `MathLinearInequality` 直接构造
3. 新增 `Symbol` vs `UtilsLinearPolynomial<Flt64>` 的 `leq`/`geq` DSL 函数到 `MathInequalityDsl.kt`（4 个 infix 函数）

提交：`a71e8e2a` feat(core): M8/Phase2 migrate MechanismModel to math.symbol types

**TokenCacheContext.kt 废弃 Cell 转换函数**：
1. `toLinearFlattenData()` — 标记 `@Deprecated(level = WARNING)`，ReplaceWith 指引使用 `polynomial.flattenedMonomials`
2. `toLinearMonomialCells()` — 标记废弃，仅用于 deprecated `cells` 属性兼容
3. `toQuadraticFlattenData()`（Linear→Quadratic 升级）— 保留不废弃
4. `toQuadraticFlattenData()`（Cell→FlattenData）— 标记废弃
5. `toQuadraticMonomialCells()` — 标记废弃
6. `Polynomial<*, *, Cell>.flattenedMonomials` extension properties — 标记废弃 + `@JvmName` 区分签名
7. `IntermediateSymbol.kt` 的 `flattenedMonomials` 调用废弃函数处加 `@Suppress("DEPRECATION")`

**SubObject.kt 废弃 Polynomial 构造函数**：
1. `LinearSubObject.invoke(category, poly, tokens, name)` — 标记废弃，ReplaceWith 指引 `LinearSubObject(category, poly.flattenedMonomials, tokens, name)`
2. `QuadraticSubObject.invoke(category, poly, tokens, name)` — 同上

**Model.kt / CallBackModel.kt 审查**：
- `Model.kt` 接口已有 `addObject(flattenData)` 和 `addConstraint(relation: MathLinearInequality)` 新 API
- `CallBackModel.kt` 基于 `Expression` 回调，不涉及 Cell 类型，无需修改

**验证**：
- `mvn compile -DskipTests`：BUILD SUCCESS（所有模块）✅
- `mvn -pl ospf-kotlin-core -am test`：**Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅

提交：`19106e96` feat(core): M8/Phase2 deprecate Cell conversion functions and Polynomial constructors

### M8/Phase 2 进展（2026-04-12）

**MechanismModel.kt 迁移到 math 类型**：
1. `generateOptimalCut` / `generateFeasibleCut` 方法从 `frontend.LinearMonomial` / `frontend.LinearPolynomial` 改为 `math.symbol.LinearMonomial<Flt64>` / `math.symbol.LinearPolynomial<Flt64>`
2. 相应不等式构造改为 `MathLinearInequality` 直接构造
3. 新增 `Symbol` vs `UtilsLinearPolynomial<Flt64>` 的 `leq`/`geq` DSL 函数到 `MathInequalityDsl.kt`（4 个 infix 函数）
4. `MathInequalityDsl.kt` 新增 `mathLe`/`mathGe` 导入

**验证**：
- `mvn compile -DskipTests`：BUILD SUCCESS（所有模块）✅
- `mvn -pl ospf-kotlin-core -am test`：**Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅

### 编译错误修复（2026-04-11）

**修复 Pipeline.kt 类型推断错误**：
1. `MetaConstraint` 现在实现 `MathConstraint` 接口（deprecated 但类型兼容）
2. `MetaDualSolution.constraints` 键类型从 `MetaConstraint<*>` 改为 `MathConstraint`
3. 修复 `Pipeline.kt` 中 `shadowPrices.constraints[constraint]` 类型推断失败

**修复 BendersDecompositionSolver 插件编译错误**（6 个插件）：
1. `copt/CoptBendersDecompositionSolver.kt`：`LinearInequality` → `MathLinearInequality`
2. `cplex/CplexBendersDecompositionSolver.kt`：同上
3. `gurobi/GurobiBendersDecompositionSolver.kt`：同上
4. `gurobi11/GurobiBendersDecompositionSolver.kt`：同上
5. `mindopt/MindOPTBendersDecompositionSolver.kt`：同上
6. `scip/ScipBendersDecompositionSolver.kt`：同上

### 全量回归（2026-04-11）
- `mvn -pl ospf-kotlin-core -am test`：**Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅
- `mvn compile -pl ospf-kotlin-core,ospf-kotlin-core-plugin,ospf-kotlin-framework -am`：**BUILD SUCCESS** ✅

### 全模块编译修复（2026-04-11）
- `mvn compile -DskipTests`：**BUILD SUCCESS**（所有模块）✅
- `mvn -pl ospf-kotlin-core -am test`：**Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅

**bpp3d/gantt-scheduling 编译修复**：
1. **Symbol vs UInt64 缺少 leq/geq 重载**：
   - `MathInequalityDsl.kt` 新增 6 个 infix 函数：`Symbol.leq/geq/eq(UInt64)`、`UInt64.eq/leq/geq(Symbol)`
   - 修复 `ItemDemandConstraint.kt` 中 `load.load[item] leq UInt64` 类型不匹配
2. **IfFunction 构造函数参数变更**（`inequality` → `input: LinearConstraintInput`）：
   - 修复 `Switch.kt`（1 处）和 `TaskTime.kt`（4 处）调用方式
   - 改为 `LinearConstraintInput.from(relation = ..., lhsRange = expr.range.range!!)`

### 全量依赖扫描（2026-04-11 更新）
1. `frontend.expression.monomial/polynomial` 外部 import：
   - ospf-kotlin-core: 51 函数符号文件 + 9 模型层文件 = **60 文件**
   - gantt-scheduling: **20 文件**
   - bpp3d: **8 文件**
   - 总计：~88 文件需迁移
2. `frontend.inequality` 外部 import：**0 文件**（已清零 ✅）

### R8/R9 阻塞分析（已解决）
1. ~~R9 目录删除被 gantt-scheduling（20 文件）+ bpp3d（8 文件）阻塞~~ → **已解决**
2. ~~bpp3d/gantt-scheduling 框架模块存在独立编译错误~~ → **已修复**
3. `frontend.expression.monomial/polynomial` 为前端 DSL 核心，保留不删（见 R7 分析结论）

### M6/R7: IntermediateSymbol/SymbolCombination 迁移（状态：分析完成，结论 — 不可迁移）

**分析结论（2026-04-11）**：`frontend.expression.monomial/polynomial` 类型**不能**被 `math.symbol` 等价替代，原因如下：

1. **类型语义根本不同**：
   - `frontend.LinearMonomial` (933 行) — 核心是 `LinearMonomialSymbol`，包装 `AbstractVariableItem` 或 `LinearIntermediateSymbol`，深度集成 token/token table 系统
   - `math.symbol.LinearMonomial<T>` — 仅是 `data class(coefficient: T, symbol: Symbol)` 的纯数学类型
   - 两者服务于完全不同的抽象层次，无法通过 typealias 或桥接替换

2. **前端 DSL 能力无法被 math.symbol 覆盖**：
   - `frontend.LinearPolynomial` (2503 行) 包含 ~100 个操作符重载，覆盖 Variable/Symbol/Monomial/Polynomial/Quantity 的任意组合
   - `MutableLinearPolynomial` 支持增量构建表达式（前端建模核心能力）
   - 数量系统（Quantity）集成 — `PhysicalUnit * Variable → Quantity<LinearMonomial>`

3. **IntermediateSymbol 系统与多项式类型深度耦合**：
   - `LinearExpressionSymbol` 从 `MutableLinearPolynomial` 构造
   - `LinearIntermediateSymbol` 实现 `ToLinearPolynomial<LinearPolynomial>` / `ToQuadraticPolynomial<QuadraticPolynomial>`
   - `FunctionSymbol` 的 `register()` 方法与 `AddableTokenCollection` 交互
   - `flattenedMonomials` → `LinearFlattenData` → 机制层约束注册，整条链路依赖前端类型

4. **SymbolCombination.kt 的 `map`/`flatMap` 工厂函数**以 `LinearMonomial` 和 `AbstractLinearPolynomial<*>` 作为构造器 lambda 返回类型

**架构结论**：
- `math.symbol` 提供**通用符号代数**能力（纯数学运算、求值、微分、因式分解等）
- `frontend.expression` 提供**优化建模 DSL** 能力（变量构造、表达式组合、token 集成、约束注册）
- 两者是互补关系，不是替代关系
- `frontend.expression.monomial/polynomial` 是前端建模 DSL 的**规范位置**，应保留

**对 R8/R9 的影响**：
- R9（删除 `frontend/expression/monomial/polynomial/adapter` 目录）需要重新评估
- `adapter` 目录不存在（已确认），无需删除
- `monomial` 和 `polynomial` 目录**不应删除**，它们是前端 DSL 的核心
- 目录删除目标应改为：清理冗余代码、消除未使用的导入、标记 deprecated 层

**对 gantt-scheduling / bpp3d 的影响**：
- 这些框架模块使用 `frontend.expression.monomial/polynomial` 是**合理的**
- 不需要迁移到 `math.symbol`
- 需要解决的是它们独立的编译错误（Ok/Failed/Fatal 类型推断），非迁移问题

---

### M6/R7-R9 深度类型分析（2026-04-11）

**分析目标**：评估将 `frontend.expression.monomial/polynomial` 替换为 `math.symbol` 类型的可行性。

**分析结论**：`Polynomial<Self, M, Cell>` 接口中的 `Cell` 类型参数仅用于已废弃的 `cells: List<Cell>` 属性。但移除 `Cell` 参数会波及二次多项式类型，改动面过大。`LinearMonomialCell` 不能被 `math.symbol` 替代的原因：
1. `LinearCellPair.variable` 类型为 `AbstractVariableItem<*, *>`，被 token 查找系统（`tokenList.find`、`tokenTable.find`）强依赖
2. 改为 `Symbol` 会导致 9 处编译错误（token 查找类型不匹配）
3. `SubObject`/`MetaModel`/`Relation` 使用 `Polynomial<*, *, LinearMonomialCell>` 约束

**前端 LinearMonomial vs math LinearMonomial 根本差异**：
- `frontend.LinearMonomial` (1278 行) — 核心是 `LinearMonomialSymbol = Either<AbstractVariableItem, LinearIntermediateSymbol>`，深度集成 token 系统、DSL 操作符、Quantity 系统
- `math.LinearMonomial<T>` (309 行) — 纯数据类 `data class(coefficient: T, symbol: Symbol)`，无 token/token table 集成
- 两者服务于完全不同的抽象层次，无法通过 typealias 或薄桥接替换

**前端 LinearPolynomial vs math LinearPolynomial 根本差异**：
- `frontend.AbstractLinearPolynomial` (2503 行) — 实现 `Polynomial<Self, LinearMonomial, LinearMonomialCell>`，100+ 操作符重载
- `math.LinearPolynomial<T>` — 简单数据类，仅有基础代数运算
- `MutableLinearPolynomial` 支持增量构建表达式（前端建模核心能力）

**最终结论**：`frontend.expression.monomial/polynomial` 是前端建模 DSL 的核心基础设施，应保留。M8 目标应聚焦于清理冗余代码和未使用导入，而非目录删除。

日期：2026-04-11
交接目标：下一个执行环境
Rust 对齐参考：`E:\workspace\ospf-rust`

## 当前结论
1. `Phase 1`（正确性热修）已完成并通过回归。
2. `Phase 2`（Context 生命周期）已完成并接入主链路。
3. `Phase 3` 已完成基础迁移与主路径切换，但尚未收口到"可删除 `expression(非symbol)+inequality` 目录"。
4. **`P0`（Phase 3 正确性阻断）已完成** ✅
5. **`M1`（统一代数内核，历史阶段名）已完成** ✅
6. **`M2`（表达层收口，历史阶段名）已完成** ✅
7. **`M3`（引入新关系类型）已完成** ✅
8. **`M4`（机制层去旧泛型）已完成** ✅
9. **`M5`（模型 API 迁移）已完成** ✅
10. **`M6`（函数符号迁移）R0-R4 已完成，R3 已完成** ✅ - 按 `R0~R4` 分批落地
11. **`M7`（删除 adapter）inequality/adapter 目录和测试已删除** ✅
12. **`M8`（最终目录删除）进入重拆执行版** - 按 `R5~R9` 实施

### M6/R3: 函数符号 off `eq/leq/geq` DSL 迁移（已完成 ✅）

**2026-04-10 完成**：
1. 批量转换 verbose `when` 块为 `.takeUnless { it.ok }?.let { return it }` 模式：
   - `Abs.kt` - 4 个 when 块（register 方法中的 Ok/Failed/Fatal 分支）
   - `BivariateLinearPiecewise.kt` - 21 个 when 块（从 git 恢复后重新转换）
   - `Semi.kt` - 7 个 when 块
   - `Binaryzation.kt`（quadratic）- 已转换
   - 其他文件在上一会话中已完成
2. 统一所有符号文件的 import 从 `frontend.inequality.geq/leq` 改为 `model.mechanism.geq/leq`：
   - `Abs.kt`, `BalanceTernaryzation.kt`, `Masking.kt`, `MaskingRange.kt`, `Max.kt`, `Min.kt`, `Not.kt`
   - `Product.kt`, `BivariateLinearPiecewise.kt`, `Inequality.kt`（quadratic）, `Binaryzation.kt`（quadratic）, `Semi.kt`
   - `IfIn.kt` 保持 `frontend.inequality.geq/leq`（因为它使用 `Inequality.register()` 而非 `model.addConstraint()`）

**编译错误修复**（~208 → 0）：
1. 补充 `MathInequalityDsl.kt` 缺失的 infix 函数：
   - 跨类型 `AbstractLinearPolynomial` vs `AbstractQuadraticPolynomial` 双向
   - `AbstractQuadraticPolynomial` vs `LinearMonomial<Flt64>`（math 类型）双向
   - `LinearMonomial<Flt64>` vs `AbstractQuadraticPolynomial` 双向
   - `Symbol` vs `LinearMonomial<Flt64>` 和 `FrontendLinearMonomial` 双向
   - `AbstractVariableItem` vs `FrontendLinearMonomial`
   - `AbstractQuadraticPolynomial` vs `FrontendLinearMonomial`
   - `AbstractLinearPolynomial` vs `UInt64` 双向
   - `Flt64.lt/gt/ne` vs `AbstractLinearPolynomial<*>`
   - 修复 `UInt64.toDouble()` → `UInt64.toFlt64()`
2. `Binaryzation.kt`（quadratic）:375 修复 `unwrap().toLong()` → `unwrap().toFlt64()`
3. `SatisfiedAmountInequality.kt`:550 `InListFunction` 直接构造 frontend `LinearInequality` 避免类型冲突
4. `MathInequalityBridge.kt` 新增 `Comparison.toSign()` 和 `LinearPolynomial.toFrontendPolynomial()` 转换函数
5. 全量回归：`Tests run: 130, Failures: 0, Errors: 0, Skipped: 0` ✅

### 交接注意事项
1. ~~优先补充 MathInequalityDsl 缺失的 infix 函数~~ → **已完成**
2. ~~编译错误 ~208，集中在 4-5 个文件~~ → **已清零**
3. ~~建议先修复 Binaryzation.kt（quadratic）的 `leq`/`geq` 类型不匹配~~ → **已修复**
4. ~~SatisfiedAmountInequality.kt 需要单独分析~~ → **已通过直接构造 frontend LinearInequality 修复**
5. ~~编译通过后执行：`mvn -pl ospf-kotlin-core -am test`~~ → **130 tests passed**
6. ~~下一步：继续 `R5-continue`：删除 `frontend/inequality` 主目录~~ → **已完成可删除部分（adapter 3 文件 + adapter 测试 2 文件 + Judgement.kt）**
7. **无法继续删除 `frontend/inequality` 主目录** — 被 46 个外部模块文件引用，需跨模块迁移

---

## 最近完成事项

### M6/R5: 删除 `frontend/inequality/adapter` 目录（2026-04-09）
1. 删除 `frontend/inequality/adapter/` 目录（3 文件）：
   - `InequalityAdapters.kt` - Core/Utils Inequality 双向转换
   - `AdvancedInequalityAdapters.kt` - JSON/XML/LaTeX 序列化适配器
   - `NormalizeAdapters.kt` - Monomial 合并适配（已内联到 Inequality 文件）
2. 删除 `frontend/symbol_migration/adapter/` 测试目录（2 文件）：
   - `AdapterRoundTripTest.kt`
   - `AdvancedAdapterFeatureTest.kt`
3. `LinearInequality.kt`：内联 `mergeMonomialsByUtils` 函数，移除 adapter import
4. `QuadraticInequality.kt`：内联 `mergeMonomialsByUtils` 函数，移除 adapter import
5. `NormalizeAdapters.kt` 核心逻辑（monomial 合并）已内联到对应 Inequality 文件
6. **删除 adapter 阻塞**：其余 adapter 功能（JSON 序列化、toUtils 转换等）仍被外部模块使用（gantt-scheduling、bpp3d、core-plugin），不能继续删除
7. 全量回归：`Tests run: 152, Failures: 0, Errors: 0, Skipped: 0`

### R6: 清理函数符号侧残余运算实现（2026-04-10 已完成 ✅）
1. `IntermediateSymbol.kt`: `LinearExpressionSymbol.prepare()` 和 `QuadraticExpressionSymbol.prepare()` 中的 `.cells` 缓存预热改为 `.flattenedMonomials`
2. 消除了最后对 deprecated `cells` 属性的计算依赖（不再是 `.cells` → `flattenedMonomials.toMonomialCells()` 的间接路径）
3. 确认所有 54 个 `override val cells get() = ...` 仅为接口契约实现（deprecated 兼容），不参与计算
4. 全量回归：`Tests run: 91, Failures: 0, Errors: 0, Skipped: 0` ✅

**R6 完成定义**：
- [x] 函数符号 `.prepare()` 方法不再调用 `.cells`
- [x] `IntermediateSymbol.kt` 表达式符号类的 `prepare()` 使用 `.flattenedMonomials`
- [x] 所有 `override val cells` 确认为纯兼容层，无计算逻辑

**R6 剩余（R7 范围）**：
- 51 个函数符号文件仍有 `frontend.expression.monomial/polynomial` 导入（用于类型声明，非计算）
- `IntermediateSymbol.kt`：7 个 `frontend.expression` 导入（`Expression`, `Monomial`, `Polynomial`, `MonomialCell`）
- `SymbolCombination.kt`：2 个 `frontend.expression` 导入（`LinearMonomial`, `AbstractLinearPolynomial`）
- 这些属于 R7 的 IntermediateSymbol 到 math.symbol 桥接迁移

### M6/R5 最终态：完全删除 `frontend/inequality` 主目录（2026-04-10）
1. `frontend/inequality` 主目录已完全删除（Sign.kt, Inequality.kt, LinearInequality.kt, QuadraticInequality.kt）
2. 全模块零 `frontend.inequality` 外部 import（ospf-kotlin-core、core-plugin、framework 均确认）
3. `Constraint.kt`：移除所有 `Inequality`-based 构造器，仅保留 `MathLinearInequality`/`MathQuadraticInequality` 构造器
4. `MetaConstraint.kt`：新增 `MathConstraint` 接口及 `LinearInequalityConstraint`/`QuadraticInequalityConstraint` 实现
5. `LinearConstraintInput.kt`：`evaluateFlattenData`/`evaluateQuadraticFlattenData`/`Comparison.compare` 改为 `internal`
6. `CallBackModel.kt`：`addConstraint` 改为使用 `LinearConstraintInput`，约束评估改为 `constraint.isTrue(solution, tokens)`
7. `MechanismModel.kt`：所有 `_constraints` 引用改为 `_relationConstraints`，返回类型改为 `MathLinearInequality`
8. 删除测试文件：`RelationTest.kt`、`FlattenMigrationGuardTest.kt`、`InequalityNormalizeBaselineTest.kt`（依赖已删除类型）
9. 全量回归：`Tests run: 91, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS ✅

### R5 Mechanism 层去 inequality 依赖（2026-04-09）
1. 机制层所有 `addConstraint(LinearInequality)`/`addConstraint(QuadraticInequality)` 标记 `@Deprecated` + `ReplaceWith`
2. `LinearConstraint`/`QuadraticConstraint` 的 `Inequality`-based 构造器标记 `@Deprecated`
3. `LinearConstraintInput.from(LinearInequality)` 标记 `@Deprecated`

### R5-continue: 删除 adapter 文件（2026-04-10 第三次执行 — 最终态）
1. 删除 `frontend/inequality/adapter/` 目录（3 文件）：
   - `InequalityAdapters.kt` - Core/Utils Inequality 双向转换
   - `AdvancedInequalityAdapters.kt` - JSON/XML/LaTeX 序列化适配器
   - `NormalizeAdapters.kt` - Monomial 合并适配
2. 删除 `frontend/symbol_migration/adapter/` 测试目录（2 文件）：
   - `AdapterRoundTripTest.kt`
   - `AdvancedAdapterFeatureTest.kt`
3. 删除 `frontend/inequality/Judgement.kt`（无任何依赖者）
4. 全量回归：`Tests run: 130, Failures: 0, Errors: 0, Skipped: 0` ✅
5. ~~**阻塞**：`frontend/inequality` 主目录剩余 4 文件（Sign.kt, Inequality.kt, LinearInequality.kt, QuadraticInequality.kt）仍被 46 个外部模块文件引用~~ → **已解决**
6. **M6/R5 最终态**：`frontend/inequality` 主目录已完全删除，全模块零引用，91 tests PASS ✅

**外部依赖分析（已解决）**：
- ~~`frontend/inequality` 主目录剩余 4 文件不可删除~~ → **已完全删除**
- 外部模块 46 个文件依赖已全部迁移：
  - 6 个 solver 插件（copt/cplex/gurobi/gurobi11/mindopt/scip）：已清除 `frontend.inequality` 导入
  - `ospf-kotlin-framework`：已清除 `frontend.inequality` 导入
  - `ospf-kotlin-framework-gantt-scheduling`：已清除 `frontend.inequality` 导入
  - `ospf-kotlin-framework-bpp3d`：已清除 `frontend.inequality` 导入
- **结论**：全模块零 `frontend.inequality` 引用，目录已完全删除 ✅
4. 所有调用点添加 `@Suppress("DEPRECATION")`
5. 全量回归：`Tests run: 152, Failures: 0, Errors: 0, Skipped: 0`

### M5: 模型 API 迁移（2026-04-08）
1. `MetaConstraint.kt` 新增 `LinearRelationConstraint`/`QuadraticRelationConstraint` 类型
2. `MetaConstraint.kt` 新增 `LinearFlattenSubObject`/`QuadraticFlattenSubObject` 类型
3. `MetaModel.kt` 新增 `_relationConstraints` 和 `_flattenSubObjects` 存储
4. `MetaModel.kt` 新增 `addConstraint(relation: ...)` 和 `addObject(flattenData: ...)` 方法
5. `Model.kt` 接口新增 Relation 和 FlattenData 类型的 API
6. `MechanismModel.kt` 新增 `addConstraint(relation: ...)` 方法
7. 回归测试：`Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`

### M4: 机制层去旧泛型（已完成 2026-04-08）
1. `SubObject.kt` 新增 `FlattenData` 构造器
2. `SubObjectTest.kt`：4 tests
3. M4-2 评估结论：转换函数保留到 M8
4. M4-3 已在 M5 中处理 ✅

### M3: 引入新关系类型（2026-04-08）
1. 新建 `Relation.kt`：`LinearRelation`/`QuadraticRelation` 接口及实现
2. 添加适配器：`LinearInequality.toRelation()` / `QuadraticInequality.toRelation()`
3. `Constraint.kt` 新增 `LinearRelation`/`QuadraticRelation` 构造器
4. `RelationTest.kt`：8 tests

### M6/R0: 基线扫描与阶段回归（2026-04-09）
1. 基线扫描结果：
   - `frontend.inequality` 外部 import：52 文件（排除 inequality 目录自身 8 文件）
   - `frontend.expression.monomial` 外部 import：66 文件（排除 monomial 目录自身 2 文件）
   - `frontend.expression.polynomial` 外部 import：40 文件（排除 polynomial 目录自身 2 文件）
   - `.cells` 读取：139 处（60 文件）
2. 阶段回归：`Tests run: 60, Failures: 0, Errors: 0, Skipped: 0`
3. 回归命令耗时：~57s

### M6/R1: 补齐 mechanism 层 relation DSL 壳（2026-04-09）
1. `Relation.kt` 新增 DSL 扩展函数：
   - `LinearFlattenData` 算术：`minus`, `plus`, `unaryMinus`, `times`
   - `QuadraticFlattenData` 算术：`minus`, `plus`, `unaryMinus`, `times`
   - `LinearFlattenData` 中缀操作符：`leq/geq/eq/neq/lt/gt`（vs `Flt64` 和 vs `LinearFlattenData`）
   - `QuadraticFlattenData` 中缀操作符：`leq/geq/eq/neq/lt/gt`（vs `Flt64` 和 vs `QuadraticFlattenData`）
   - `linearRelation()` / `quadraticRelation()` 命名构造器
   - `relation(sign, name, displayName)` 扩展函数
2. `RelationDslTest.kt`：22 tests 覆盖算术、中缀、命名构造、normalize
3. 阶段回归：`Tests run: 86, Failures: 0, Errors: 0, Skipped: 0`

### M6/R2: 显式 inequality 类型迁移（2026-04-09）
1. 新建 `LinearConstraintInput` 数据类，携带 `LinearFlattenData`、`Sign`、`lhsRange`、`rhsConstant` 元数据
2. `LinearConstraintInput` 新增 `isTrue()` 多态重载（5 种签名）和 `evaluateFlattenData` 辅助函数
3. `Inequality.kt` 新增 6 个 `LinearConstraintInput.register()` 扩展函数，匹配原有 `LinearInequality` 注册逻辑
4. 迁移 5 个函数符号文件：
   - `linear_function/If.kt`：构造函数改为 `input: LinearConstraintInput`，保留 `ToLinearInequality` 兼容工厂
   - `linear_function/IfThen.kt`：构造函数改为 `pInput/qInput: LinearConstraintInput`，保留 `ToLinearInequality` 兼容工厂
   - `linear_function/SameAs.kt`：构造函数改为 `inputs: List<LinearConstraintInput>`
   - `linear_function/SatisfiedAmountInequality.kt`：`AbstractSatisfiedAmountInequalityFunction` 基类改为 `inputs: List<LinearConstraintInput>`
   - `linear_function/Inequality.kt`：新增 `LinearConstraintInput.register()` 扩展（含 Big-M 注册逻辑）
5. 新增 `evaluateFlattenDataWithValuesAndTokenList` 辅助函数，支持 `AbstractTokenList` fallback
6. 全量回归：`Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`（`mvn -pl ospf-kotlin-core -am test`）

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
   - `ensureFlattenContext()`
   - `ensureValueCacheContext()`
   - `ensureRangeCacheContext()`
   - `rebindContexts()`
   - `invalidateAllCaches()`
   - `invalidateSolutionCaches()`
2. `MutableTokenTable` 已实现上述方法。
3. `add(symbol)` 自动绑定、`remove(symbol)` 自动解绑已接入。

### G. Phase 3（已完成部分）
1. `flattenedMonomials` 已在 monomial/polynomial/inequality/symbol 主类型接入。
2. `Constraint` / `SubObject` / `TokenTable` 已切到 `flattenedMonomials` 主路径。
3. `cells` 已在核心类型标注 `@Deprecated(level = WARNING)`，作为过渡兼容层。
4. 已新增 `FlattenMigrationGuardTest`（12 tests）用于迁移守卫。

### H. 最近验证结果
1. `mvn -pl ospf-kotlin-core "-Dtest=MonomialCoefficientPreservationTest,FlattenMigrationGuardTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest,InequalityNormalizeBaselineTest,TokenCacheContextsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test` 通过（53 tests）。
2. `mvn -pl ospf-kotlin-core -am test` 通过（core 101 tests，Reactor 全绿）。
3. 历史记录：8 plugin 联编曾通过；SCIP 插件存在独立 API 兼容问题（见“已知问题”）。

---

## 详细实施清单（按顺序执行）

说明：
1. 状态使用：`[ ]` 未开始，`[-]` 进行中，`[x]` 已完成。
2. 提交粒度建议：每个阶段至少 1 次可回滚提交；P0 建议拆 2~3 次小提交。
3. 硬门禁：`P0` 未通过前，禁止进入 `M1+`。

### 1. P0：Phase 3 正确性阻断（不通过不得继续）✅ 已完成
目标：修复二次拍平正确性，建立长期守卫，阻断回归。

任务拆解：
1. `[x]` 定位 `QuadraticMonomialSymbol.flattenedMonomials` 的展开路径，补全 `constant * monomial` 分支。
   - **修复**：在 `QuadraticMonomialSymbol.flattenedMonomials` 中添加了交叉项 `m1*c2` 和 `c1*m2`。
2. `[x]` 检查并修复二次拍平组合中符号载荷丢失点（组合、复制、合并三个路径都检查）。
   - **确认**：组合路径已修复，复制和合并路径无问题。
3. `[x]` 重写 `QuadraticPolynomial` 对称项归并 key，改为确定性规范化策略（禁止依赖 `hashCode` 与插入顺序）。
   - **修复**：改用 `identifier` 进行确定性排序，确保 `x*y` 和 `y*x` 产生相同 key。
4. `[x]` 新增守卫测试 A：`(x + 1) * (y + 1)`，断言项集合完整（`xy/x/y/1`）。
5. `[x]` 新增守卫测试 B：`x*y` 与 `y*x` 结果完全一致（项、系数、key）。
6. `[x]` 新增守卫测试 C：同一语义表达式多次构造后 key 序列稳定一致。
7. `[x]` 运行阶段小回归（见”回归命令 1”）并记录结果。

产出物：
1. 修复代码 + 3 组守卫测试（`FlattenMigrationGuardTest` 现有 15 tests）。
2. 回归命令结果：`Tests run: 56, Failures: 0, Errors: 0, Skipped: 0`

完成定义（DoD）：
1. ✅ 新增守卫测试全部通过。
2. ✅ 原有 baseline 不退化。

风险与回退：
1. 若归并策略改动导致波及面过大，先加兼容开关，再逐步替换旧逻辑。

### 2. M1：统一代数内核（去重复实现，历史阶段）✅ 已完成
目标：历史阶段目标为 flatten 合并与乘法展开收口；后续不再在 core 扩展代数内核能力，统一以 `math.symbol` 为符号运算来源。

说明：
1. 以下条目为历史完成记录，保留用于回溯，不作为后续扩展方向。

任务拆解：
1. `[x]` 建立 internal utility（拆成 merge/multiply/normalize 三类函数）。
   - 新建 `FlattenUtility.kt`：`mergeLinearMonomials`, `mergeQuadraticMonomials`, `mergeLinearFlattenData`, `mergeQuadraticFlattenData`, `multiplyLinear`, `normalizeLinear`, `normalizeQuadratic`
2. `[x]` 迁移线性主路径到 utility。
   - `LinearPolynomial.calculateFlattenedMonomials` 调用 `mergeLinearFlattenData`
3. `[x]` 迁移二次主路径到 utility。
   - `QuadraticPolynomial.calculateFlattenedMonomials` 调用 `mergeQuadraticFlattenData`
4. `[x]` 单项式/多项式/不等式统一调用 utility，删除分散实现。
5. `[x]` 补充 utility 级单测（线性、二次、边界输入）。
   - `FlattenUtilityTest`：10 tests
6. `[x]` 执行阶段小回归并记录结果。
   - `Tests run: 114, Failures: 0, Errors: 0, Skipped: 0`

产出物：
1. `FlattenUtility.kt` - flatten 单点实现
2. `FlattenUtilityTest.kt` - utility 级单测
3. LinearPolynomial/QuadraticPolynomial 改用 utility

完成定义（DoD）：
1. ✅ 当时目标：flatten 核心算法收口完成。
2. ✅ 当前约束：后续符号运算能力统一由 `math.symbol` 提供，core 仅保留桥接与装配。

### 3. M2：表达层收口（保留兼容壳，历史阶段）✅ 已完成
目标：历史阶段目标为表达层切到 flatten 主路径；后续继续朝“`math.symbol` 运算 + core 桥接装配”收敛，`cells` 仅保留兼容。

说明：
1. 以下条目为历史完成记录，保留用于回溯，不作为后续扩展方向。

任务拆解：
1. `[x]` `Linear/QuadraticMonomial` 主路径切到 flatten。
   - 已使用 `symbol.flattenedMonomials` 作为主计算路径
2. `[x]` `Linear/QuadraticPolynomial` 主路径切到 flatten。
   - 已使用 `mergeLinearFlattenData` / `mergeQuadraticFlattenData`
3. `[x]` `Linear/QuadraticInequality` 主路径切到 flatten。
   - 已直接使用 `lhs.flattenedMonomials` / `rhs.flattenedMonomials`
4. `[x]` `cells` getter 降级为兼容视图，移除参与核心计算的调用点。
   - 所有 `cells` 标记 `@Deprecated(level = WARNING)`
   - `cells` getter 改为 `flattenedMonomials.toMonomialCells()`
5. `[x]` 增补”flatten 与 cells 结果一致性”回归测试。
   - `FlattenMigrationGuardTest` 新增 4 个一致性测试（共 19 tests）
6. `[x]` 执行阶段小回归并记录结果。
   - `Tests run: 118, Failures: 0, Errors: 0, Skipped: 0`

完成定义（DoD）：
1. ✅ 当时目标：主链路无 `.cells` 计算依赖（`cells` 仅为兼容视图）。
2. ✅ 当前约束：后续符号运算实现继续收敛到 `math.symbol`，core 只保留桥接装配职责。

### 4. M3：引入新关系类型，替代 `frontend/inequality` ✅ 已完成
目标：建立新关系对象，先引入后替换。

任务拆解：
1. `[x]` 定义线性/二次关系对象（含 sign、lhs、rhs 或规范化表示）。
   - 新建 `Relation.kt`：`LinearRelation`/`QuadraticRelation` 接口
   - `LinearRelationImpl`/`QuadraticRelationImpl` 实现
   - 包含 `normalize()` 方法和 `toInequality()` deprecated 适配器
2. `[x]` 提供旧 `Inequality -> 新关系对象` 适配器（单向优先）。
   - `LinearRelationImpl.from(inequality: LinearInequality)`
   - `QuadraticRelationImpl.from(inequality: QuadraticInequality)`
   - 扩展函数 `LinearInequality.toRelation()` / `QuadraticInequality.toRelation()`
3. `[x]` `Constraint` 构造新增新关系对象入口。
   - `LinearConstraint.invoke(relation: LinearRelation, ...)` 构造器
   - `QuadraticConstraint.invoke(relation: QuadraticRelation, ...)` 构造器
4. `[x]` 对新增入口补单测与示例调用。
   - `RelationTest.kt`：8 tests

产出物：
1. `Relation.kt` - 新关系类型定义
2. `RelationTest.kt` - 关系类型测试
3. `Constraint.kt` 新增两个构造器

完成定义（DoD）：
1. ✅ 新调用方可不依赖 `frontend/inequality`。
2. ✅ 旧调用方经适配后继续可用。
3. ✅ 全量回归通过：126 tests

### 5. M4：机制层去旧泛型与桥接 ✅ 已完成
目标：mechanism 层去掉旧 cell 泛型耦合。

任务拆解：
1. `[x]` `SubObject/Constraint` 输入签名移除 `*MonomialCell` 约束。
   - 新增 `LinearSubObject.invoke(flattenData: LinearFlattenData, ...)` 构造器
   - 新增 `QuadraticSubObject.invoke(flattenData: QuadraticFlattenData, ...)` 构造器
   - 新增 `SubObjectTest.kt` 测试（4 tests）
2. `[x]` 删除 `TokenCacheContext` 中 flatten <-> cell 双向桥接。
   - **评估结论**：转换函数用于 deprecated `cells` 属性兼容层
   - 保留到 M8 删除旧类型时再移除
3. `[x]` `MetaModel._subObjects` 去 cell 类型耦合。
   - 在 M5 中新增 `_flattenSubObjects` 存储 FlattenData 类型
4. `[x]` 修复受影响调用点并补回归。

产出物：
1. `SubObject.kt` 新增 FlattenData 构造器
2. `SubObjectTest.kt` - 4 tests
3. `Constraint.kt` 已在 M3 添加 LinearRelation/QuadraticRelation 构造器

完成定义（DoD）：
1. ✅ 新调用方可直接使用 FlattenData，无需依赖 frontend 多项式类型。

### 6. M5：模型 API 迁移 ✅ 已完成
目标：模型对外 API 切换到新关系对象与新表达签名。

任务拆解：
1. `[x]` 迁移 `frontend/model/Model.kt`。
   - 新增 `addConstraint(relation: LinearRelation/QuadraticRelation)` 接口方法
   - 新增 `addObject(flattenData: LinearFlattenData/QuadraticFlattenData)` 接口方法
2. `[x]` 迁移 `MetaConstraint.kt`。
   - 新增 `LinearRelationConstraint`/`QuadraticRelationConstraint` 类型
   - 新增 `LinearFlattenSubObject`/`QuadraticFlattenSubObject` 类型
   - 新增 `toRelationConstraint()` 适配器
3. `[x]` 迁移 `MetaModel.kt`。
   - 新增 `_relationConstraints` 和 `_flattenSubObjects` 存储
   - 新增 `addConstraint(relation: ...)` 实现
   - 新增 `addObject(flattenData: ...)` 实现
4. `[x]` 迁移 `MechanismModel.kt`。
   - 新增 `addConstraint(relation: ...)` 方法实现
5. `[x]` `addConstraint/partition/addObject` 改为新签名。
6. `[x]` 旧签名保留短期 deprecated shim，并标注移除窗口。
7. `[x]` 运行 `mvn -pl ospf-kotlin-core -am test` 并记录结果。
   - `Tests run: 130, Failures: 0, Errors: 0, Skipped: 0`

完成定义（DoD）：
1. ✅ 模型主 API 新增 Relation 和 FlattenData 类型入口。
2. ✅ 旧 API 保留为 deprecated 兼容层。
3. ✅ 全量回归通过：130 tests

### 7. M6：函数符号迁移（重拆执行版，2026-04-09）
目标：优先清理函数符号对 `frontend/inequality` 的直接依赖，再清理 `.cells` 计算读取，并收敛到 `math.symbol + relation/flatten` 桥接路径。

基线盘点（2026-04-09）：
1. `expression/symbol` 总计 56 文件，其中 52 文件仍有旧路径 import（259 处）。
2. 显式 `LinearInequality/QuadraticInequality` 类型依赖集中在 7 文件。
3. `eq/leq/geq` DSL 使用 682 处（51 文件）。
4. `.cells` 读取 140 处（48 文件）。

任务拆解（R0~R4）：
1. `[x]` `R0` 冻结扫描基线与回归脚本，提交”仅统计无行为变更”提交。
2. `[x]` `R1` 在 mechanism 层补齐 relation DSL 壳（`flattenEq/flattenLeq/flattenGeq` 或等价 API），覆盖函数符号当前使用组合。
3. `[x]` `R2` 优先迁移 7 个显式 inequality 类型文件：
   - `linear_function/If.kt`
   - `linear_function/IfThen.kt`
   - `linear_function/Inequality.kt`
   - `linear_function/SameAs.kt`
   - `linear_function/SatisfiedAmount.kt`
   - `linear_function/SatisfiedAmountInequality.kt`
   - `quadratic_function/Inequality.kt`
4. `[ ]` `R3` 分 4 批迁移其余 operator-only 函数符号文件（逻辑类/离散化类/区间分段类/取整极值类），每批一条可回滚提交。
5. `[x]` `R4` 清理函数符号内 `.cells` 读取，统一改为 `flattenedMonomials` 主路径，并补充对应回归。

阶段门禁（M6 完成定义）：
1. `expression/symbol` 目录外部 `frontend.inequality` import 归零。
2. 函数符号内 `.cells` 读取归零（保留 deprecated getter 但不参与计算）。
3. 阶段回归绿灯：
   - `mvn -pl ospf-kotlin-core "-Dtest=MonomialCoefficientPreservationTest,FlattenMigrationGuardTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest,InequalityNormalizeBaselineTest,TokenCacheContextsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`

### M6/R5: 删除 `frontend/inequality/adapter` 目录（2026-04-09）
1. 删除 `frontend/inequality/adapter/` 目录（3 文件）
2. 删除 `frontend/symbol_migration/adapter/` 测试目录（2 文件）
3. `LinearInequality.kt`/`QuadraticInequality.kt` 内联 `mergeMonomialsByUtils` 函数
4. 全量回归：`Tests run: 152, Failures: 0, Errors: 0, Skipped: 0`
5. **阻塞**：剩余 `frontend/inequality` 主目录（5 文件）仍被 55+ 外部文件引用，需先完成 M6/R3

### M6/R4: 清理 .cells 读取（2026-04-09）
1. 从 36 个函数符号文件的 `prepare()` 方法中移除 `.cells` 缓存预热调用
2. 涉及文件：
   - `linear_function`：And.kt, Or.kt, Xor.kt, Not.kt, BalanceTernaryzation.kt, SatisfiedAmount.kt, OneOf.kt, Binaryzation.kt
   - `quadratic_function`：Binaryzation.kt, Ceiling.kt, Floor.kt, Masking.kt, MaskingRange.kt, Max.kt, Min.kt, Mod.kt, Product.kt, Rounding.kt, Semi.kt, Sigmoid.kt, Slack.kt, SlackRange.kt, UnivariateLinearPiecewise.kt, InStepRangeFunction.kt, BivariateLinearPiecewise.kt, Linear.kt
3. `cells` 属性声明（`override val cells`）保留为 deprecated 兼容 getter，不参与计算
4. 全量回归：`Tests run: 152, Failures: 0, Errors: 0, Skipped: 0`（`mvn -pl ospf-kotlin-core -am test`，~11m）

### 9. M7：删除 `expression/adapter`（并入重拆流程）
目标：按依赖收敛顺序删除 adapter，不再与旧表达层耦合。

任务拆解：
1. `[x]` 删除 `frontend/inequality/adapter`（M6/R5 已完成）
2. `[ ]` 在 `R9` 时删除 `frontend/expression/adapter`（与 monomial/polynomial 同步删除）
3. `[ ]` 增加守卫扫描，禁止新增 adapter import 回流

完成定义（DoD）：
1. `src/main` 不再存在 `frontend.expression.adapter` / `frontend.inequality.adapter` import。

### 9. M8：最终目录删除（重拆执行版 — 已重新定义）
目标：~~完成目录级收敛，删除旧表达与 inequality 实现~~ → **已重新定义，不再删除 monomial/polynomial**

**R7 分析结论（2026-04-11）**：
- `frontend.expression.monomial/polynomial` 是前端建模 DSL 的核心，不能被 `math.symbol` 替代
- `adapter` 目录不存在，无需删除
- `frontend.expression` 目录应保留为前端 DSL 的规范位置

当前规模（2026-04-09 更新）：
1. ~~`frontend.inequality` 外部 import：~120 处（55+ 文件，排除 inequality 目录自身 5 文件）~~ → **已完全删除 ✅**
2. ~~`frontend.expression.monomial/polynomial` 外部 import：216 处（70 文件）~~ → **保留，为前端 DSL 核心**
3. 已删除：`frontend/inequality/adapter/`（3 文件）
4. 已删除：`frontend/symbol_migration/adapter/` 测试（2 文件）

前置条件：
1. M6 的 `R0~R4` 全部完成并通过阶段门禁。
2. M6/R5 部分完成（adapter 子目录已删除）

任务拆解（重新定义）：
1. `[x]` `R5` 删除 `frontend/inequality/adapter` 子目录
2. `[x]` `R5-continue` 删除 `frontend/inequality` 主目录（含 Inequality.kt 等 4 文件）→ **已完成 ✅**
3. `[x]` `R6` 清理函数符号侧 `.cells` 缓存预热调用 → **已完成 ✅**
4. `[x]` `R7` 分析 IntermediateSymbol/SymbolCombination 迁移可行性 → **结论：不可迁移，保留 frontend 类型**
5. `[ ]` `R8-new` 修复 gantt-scheduling / bpp3d 的独立编译错误（Ok/Failed/Fatal 类型推断）
6. `[ ]` `R9-new` 清理 `frontend.expression.monomial/polynomial` 中的未使用导入和冗余代码
7. `[ ]` 执行全量回归：
   - `mvn -pl ospf-kotlin-core -am test`
8. `[ ]` 执行插件编译检查

完成定义（DoD）：
1. `ospf-kotlin-core` 全量测试通过。
2. gantt-scheduling / bpp3d 编译通过。
3. `frontend.expression` 代码整洁，无冗余导入。

### 10. M9：封口门禁（强化 — 已重新定义）
目标：建立”删完后不可回流”的 CI 门禁与文档封口。

**R7 分析结论影响**：`frontend.expression.monomial/polynomial` 为前端 DSL 核心，不应禁止 import。

任务拆解：
1. `[ ]` 新增 CI 扫描测试，禁止以下 import：
   - ~~`frontend.inequality`~~ → **已删除，无需扫描**
   - ~~`frontend.expression.monomial`~~ → **保留，前端 DSL 核心**
   - ~~`frontend.expression.polynomial`~~ → **保留，前端 DSL 核心**
   - `[ ]` `frontend.expression.adapter`（如存在）
2. `[ ]` 更新迁移文档（旧 DSL -> relation/flatten API 对照表）。
3. `[ ]` 在 PR 模板或检查脚本加入迁移项核对。

完成定义（DoD）：
1. 后续 PR 无法回引已删除的路径（`frontend.inequality`）。
2. 新增功能默认按 relation/flatten API 开发。

### 11. Phase 4：功能闭环
目标：二次子问题 dual/cut 端到端打通。

任务拆解：
1. `[ ]` 完成 `QuadraticTetradModel.dual()/farkasDual()`。
2. `[ ]` 完成 `QuadraticMechanismModel.generateOptimalCut()/generateFeasibleCut()`。
3. `[ ]` 统一 solver dual 输出并打通插件透传。
4. `[ ]` 新增端到端测试：可行割与最优割。

完成定义（DoD）：
1. 二次子问题可端到端生成可行割与最优割。

### 12. Phase 5：性能稳定化
目标：缓存键、IIS 控制与并发模型收敛。

任务拆解：
1. `[ ]` 重构 value cache key（避免 `List/Map` 大对象直接做哈希键）。
2. `[ ]` IIS 真正消费 `time/threadNum/notImprovementTime`。
3. `[ ]` `ThreadLocal` 改协程安全方案或显式透传。
4. `[ ]` 增加性能观察指标（耗时、命中率、终止行为）。

完成定义（DoD）：
1. IIS 可控终止、并发不串、性能可观测改善。

### 13. Phase 6：达标收敛
目标：测试簇补齐，形成可对外交付状态。

任务拆解：
1. `[ ]` 补齐 correctness 测试簇。
2. `[ ]` 补齐 architecture guard 测试簇。
3. `[ ]` 补齐 performance-guard 测试簇。
4. `[ ]` 补齐 plugin-compatibility 测试簇。
5. `[ ]` 回写最终迁移文档与发布说明。

完成定义（DoD）：
1. 无 P0/P1 挂起，可对外声明达到 design 对齐目标。

---

## 回归命令（固定节奏）
1. 阶段小回归：
   - `mvn -pl ospf-kotlin-core "-Dtest=MonomialCoefficientPreservationTest,FlattenMigrationGuardTest,LinearPolynomialBaselineTest,QuadraticPolynomialBaselineTest,InequalityNormalizeBaselineTest,TokenCacheContextsTest" "-Dsurefire.failIfNoSpecifiedTests=false" test`
2. 每两到三阶段：
   - `mvn -pl ospf-kotlin-core -am test`
3. ~~M8/M9 完成后~~ → **M8/M9 已重新定义**：
   - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`
   - `mvn compile -pl ospf-kotlin-framework-gantt-scheduling,ospf-kotlin-framework-bpp3d -am`

---

## 已知问题
1. SCIP 插件存在 `EventHandler/EventMask` API 兼容问题（当前迁移主线非阻断）。
2. 当前代码存在大量 `cells` 相关 deprecation warning（属过渡态，M6~M9 清理）。

---

## 下个环境起手顺序（严格执行）
1. **编译修复完成（2026-04-11）**：
   - `MetaConstraint` 实现 `MathConstraint` 接口
   - `MetaDualSolution.constraints` 键类型改为 `MathConstraint`
   - 6 个 BendersDecompositionSolver 插件修复 `MathLinearInequality/MathQuadraticInequality` 别名
   - `MathInequalityDsl.kt` 新增 `Symbol/UInt64` leq/geq/eq 重载
   - gantt-scheduling `Switch.kt`/`TaskTime.kt` 修复 `IfFunction` 调用方式
2. **全量编译通过**：`mvn compile -DskipTests` BUILD SUCCESS ✅
3. **全量回归通过**：`mvn -pl ospf-kotlin-core -am test` 91 tests, BUILD SUCCESS ✅
4. **M8（monomial/polynomial 目录删除）进入全量迁移** — 推翻 R7 结论，按 Phase 0~6 实施
5. 最后执行 `Phase 4 -> Phase 6` 功能与稳定化收敛
6. 每个阶段回写本文件：新增完成项、剩余缺口、回归命令结果

---

## M8: monomial/polynomial 目录删除（全量迁移版，2026-04-11）

### 架构决策变更

**R7 结论推翻（2026-04-11）**：
- ~~`frontend.expression.monomial/polynomial` 不能被 `math.symbol` 替代~~ → **推翻**
- `AbstractVariableItem` 已实现 `math.symbol.Symbol` 接口，可直接作为 math 类型的 symbol 参数
- Token 集成由 MetaModel 上下文负责（`TokenCacheContexts`）
- DSL 操作符 math.symbol 已经提供（infix le/ge/eq、plus/minus/times/div）
- Quantity 系统 `quantities` 已经提供（`Quantity<LinearPolynomial<Flt64>>` 等）

### 深度 Gap 分析

| Gap | 解决方案 |
|-----|----------|
| math.symbol 缺少 combineTerms() | 已确认存在：`operation/CombineTerms.kt` 和 `operation/LinearQuadraticOps.kt` |
| math.symbol 类型为泛型 `<T>` | bridge 层使用显式 `<Flt64>` |
| math.symbol 无 name/displayName/range | 改为 bridge 扩展属性，从 IntermediateSymbol/TokenTable 读取 |
| math.symbol 无 cells 属性 | 用 combineTerms() 替代 |
| MutableLinearPolynomial 缺少操作符 | bridge 层复制 plusAssign/minusAssign 等重载 |
| Quantity 操作符冲突 | bridge 层提供独立 @JvmName 函数 |
| MonomialCell 系统 | 删除，不再需要 |
| MonomialSymbol 包装器 | 删除，`AbstractVariableItem` 已实现 `Symbol` |

### 迁移规模

- `frontend.expression.monomial`：126 文件引用，227 处
- `frontend.expression.polynomial`：93 文件引用，132 处
- 其中 ospf-kotlin-core 内部 ~60 文件，framework 模块 ~65 文件

### 分阶段计划

#### Phase 0：math.symbol 基础补充 ✅ 已完成
1. `LinearMonomial<T>.toLinearPolynomial()` 泛型扩展（已添加到 LinearPolynomial.kt）
2. `LinearPolynomial<T>.toMutable()` 已确认存在（MutableLinearPolynomial.kt:192）
3. 验证 combineTerms()、evaluate() 功能完整 — 均已确认存在
4. `zeroOf()` 从 private 改为 internal，供扩展函数复用
5. `mvn -pl ospf-kotlin-math test`: **722 tests, BUILD SUCCESS** ✅

#### Phase 1：创建 bridge 层 ✅ 已完成
新建 `frontend.expression.bridge` 包，提供 operator 重载、Quantity 操作符、sum 函数：
- `VariableOperators.kt` — AbstractVariableItem 操作符重载（~50 函数）
- `SymbolOperators.kt` — LinearIntermediateSymbol 操作符重载（~25 函数）
- `MonomialOperators.kt` — LinearMonomial<Flt64> 操作符重载（~20 函数）
- `PolynomialOperators.kt` — LinearPolynomial<Flt64> 操作符重载（~15 函数）
- `MutablePolynomialOperators.kt` — MutableLinearPolynomial<Flt64> 赋值操作符（~15 函数）
- `QuantityOperators.kt` — Quantity 包装操作符（~50 @JvmName 函数）
- `SumFunctions.kt` — sum/flatSum/qtySum/flatQtySum（~10 函数）
- `PolynomialExtensions.kt` — flattenedMonomials/dependencies/range/cached/flush 扩展

**关键修复**：
1. `LinearPolynomial` 构造函数需要 `monomials` + `constant` 两个参数，所有调用点补充 `Flt64.zero`
2. `this.unit * rhs.unit` 中 PhysicalUnit 的 `times`/`div` 是 top-level 扩展函数，需要从 `quantities.unit` 显式导入（不能 alias）
3. `Quantity.to(PhysicalUnit)` 与 stdlib `to` infix 冲突，需要显式 `import ...quantity.to`
4. `MutableLinearPolynomial<Flt64>` 替换错误的 typealias shadowing
5. `zeroOf()` 为 math 模块 internal 函数，不能从 core 模块导入
6. `.reciprocal()` 在 Flt64 上不可用，替换为 `Flt64.one / value`
7. `this.value * rhs.value` 类型推断失败（泛型 T vs Flt64），改为显式 `.toFlt64()` 转换后构造 LinearMonomial

**验证**：
- `mvn compile -pl ospf-kotlin-core -am`: BUILD SUCCESS ✅
- `mvn -pl ospf-kotlin-core -am test`: **Tests run: 91, Failures: 0, Errors: 0, Skipped: 0** ✅

#### Phase 1：创建 bridge 层
新建 `frontend.expression.bridge` 包：
- `VariableOperators.kt` — AbstractVariableItem 操作符重载（~30 函数）
- `SymbolOperators.kt` — LinearIntermediateSymbol 操作符重载（~25 函数）
- `MonomialOperators.kt` — LinearMonomial<Flt64> 操作符重载（~20 函数）
- `PolynomialOperators.kt` — LinearPolynomial<Flt64> 操作符重载（~15 函数）
- `MutablePolynomialOperators.kt` — MutableLinearPolynomial<Flt64> 赋值操作符（~15 函数）
- `QuantityOperators.kt` — Quantity 包装操作符（~50 @JvmName 函数）
- `SumFunctions.kt` — sum/flatSum/qtySum/flatQtySum（~10 函数）
- `PolynomialExtensions.kt` — flattenedMonomials/dependencies/range/cached/flush 扩展

#### Phase 2：核心基础设施迁移
1. 接口瘦身（Polynomial.kt, Monomial.kt 移除 Cell 泛型）
2. Adapter 层简化（MonomialAdapters.kt, PolynomialAdapters.kt）
3. TokenCacheContext 迁移到 math 类型
4. MetaModel / MechanismModel / MathInequalityDsl 迁移
5. Model / CallBackModel 迁移

#### Phase 3：函数符号迁移（~60 文件）
1. IntermediateSymbol.kt + SymbolCombination.kt（基础）
2. linear_function 目录（~30 文件）
3. quadratic_function 目录（~15 文件）

#### Phase 4：删除旧类型目录
1. 删除 `frontend.expression.monomial/` 目录
2. 删除 `frontend.expression.polynomial/` 目录
3. 清理死代码 Adapter 函数

#### Phase 5：framework 模块迁移
1. gantt-scheduling（~50 文件）
2. bpp3d（~8 文件）

#### Phase 6：清理收尾
1. 全量回归测试
2. 零引用验证
3. 更新迁移文档

### 门禁标准
- `mvn -pl ospf-kotlin-core -am test`: BUILD SUCCESS, 91+ tests
- `mvn compile -DskipTests`: 全模块 BUILD SUCCESS
- 零 `frontend.expression.monomial` 引用
- 零 `frontend.expression.polynomial` 引用
