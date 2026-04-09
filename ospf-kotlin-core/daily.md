# OSPF Kotlin Core Daily

日期：2026-04-09
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
10. **`M6`（函数符号迁移）R0-R4 已完成** ✅ - 按 `R0~R4` 分批落地
11. **`M7`（删除 adapter）并入重拆流程** - 按依赖收敛顺序删除
12. **`M8`（最终目录删除）进入重拆执行版** - 按 `R5~R9` 实施

**下一步行动**：
1. ~~进入 `M6/R3`，迁移函数符号 off `eq/leq/geq` DSL~~ → **M6/R3 进行中，编译未通过**
2. 优先修复 M6/R3 编译错误（见"最新状态"中的问题列表）
3. 完成 M6/R3 后继续 `R5-continue`：删除 `frontend/inequality` 主目录。
4. 按 `R6 -> R9` 完成目录级删除与 M8 收口。
5. 最后执行 `Phase 4 -> Phase 6` 功能与稳定化收敛。

**架构边界（2026-04-09 明确）**：
1. 线性与二次型符号运算能力由 `math.symbol` 提供。
2. `ospf-kotlin-core` 负责变量、中间值、模型装配与桥接，不再扩展新的符号运算实现。
3. 原 `monomial/polynomial` 的额外能力（`cell` 直接使用 `math.symbol.monomial`、`value`、`range`）迁移到元模型上下文统一处理（`TokenCacheContexts` / `MetaModel` / `MechanismModel`）。

---

## 最新状态（2026-04-09 交接点）

### M6/R3: 函数符号 off `eq/leq/geq` DSL 迁移（进行中，未完成）

**已完成**：
1. 新建 `MathInequalityDsl.kt`（`mechanism` 包下），提供与 `frontend.inequality` 同名的 infix 函数：
   - `AbstractLinearPolynomial` vs `AbstractLinearPolynomial`/`Flt64`/`Int`/`Double`/`Symbol`
   - `Symbol` vs `Flt64`/`Symbol`/`AbstractLinearPolynomial`/`Boolean`
   - `AbstractVariableItem` vs `Flt64`/`Boolean`/`AbstractVariableItem`/`AbstractLinearPolynomial`
   - `LinearMonomial<Flt64>` vs `AbstractLinearPolynomial`（双向）
   - `normalize()` 扩展函数（MathLinearInequality / MathQuadraticInequality）
   - `leq/le/geq/ge/eq/neq/ne/ls/lt/gr/gt` 别名全覆盖
   - UInt8/UInt64 比较辅助函数
2. 批量替换 45 个函数符号文件的 import：
   - `import frontend.inequality.{eq,leq,geq,neq,ls,gt}` → `import mechanism.{eq,leq,geq,neq,ls,gt}`
   - 通配符 import 已在 4 个文件中改为精确 import + `mechanism.*`
3. `model.addConstraint(constraint = ...)` → `model.addConstraint(relation = ...)` 全局 sed 替换
4. 修复 sed 过度替换问题：将以下文件中构造函数/超类调用错误改回的 `relation = constraint` → `constraint = constraint`：
   - `SameAs.kt`
   - `SatisfiedAmountInequality.kt`（AtLeastInequalityFunction, NumerableFunction）
   - `SatisfiedAmount.kt`（AtLeastFunction 工厂）
   - `IfThen.kt`
   - `quadratic_function/SlackRange.kt`
   - `quadratic_function/Slack.kt`
   - `linear_function/Slack.kt`
   - `linear_function/SlackRange.kt`

**编译仍失败（未通过）**，主要错误类别：
1. `Abs.kt:318,338` - `m * pos` 返回类型与 DSL 不匹配（`m: Flt64 * pos: PctVar` → 需要确认返回类型并添加对应 infix）
2. `BalanceTernaryzation.kt` 多处 - 类似 `m * y[1] geq x` 模式，`Flt64 * AbstractVariableItem` 返回类型不匹配
3. `Binaryzation.kt` 多处 - 类似问题
4. `IfIn.kt:156,157` - `normalize()` 返回对象是 `val` 不可变，`.name` 赋值报错（frontend 版 LinearInequality 的 `name` 是 `var`，MathLinearInequality 的 `name` 是 `val`）
5. `IfIn.kt` 多处 - `2 type arguments expected` 用于 `Ok/Failed/Fatal`（可能是不相关的 when 分支问题或 addConstraint 返回类型不匹配）
6. `IfIn.kt` - `.isTrue` 未解析（`lowerBoundInequality/upperBoundInequality` 是 `MathLinearInequality`，没有 `isTrue` 方法）

**关键问题**：
- `MathLinearInequality`（math 模块）的 `name`/`displayName` 是 `val`，而 `frontend.inequality.LinearInequality` 的 `name`/`displayName` 是 `var`。需要 IfIn.kt 改用其他方式传递 name，或 math 模块改为 `var`
- `MathLinearInequality` 没有 `isTrue()` 方法，需要适配或改用 `frontend.inequality.LinearInequality.isTrue()` 的等价逻辑

**下一步需要**：
1. 修复 IfIn.kt 的 `val` 赋值问题（name 不可变）和 `isTrue` 缺失问题
2. 排查 `Flt64 * Variable` 返回的具体类型，补充对应 infix 重载
3. 排查 `Ok/Failed/Fatal` 类型参数问题（可能是 `model.addConstraint` 返回类型变了）
4. 编译通过后再跑测试

**剩余未编译文件**：
- `And.kt` 修复了 `Boolean` 比较，但仍有类型不匹配
- `First.kt` - `Ok/Failed/Fatal` 类型参数问题
- `BivariateLinearPiecewise.kt` - `Ok/Failed/Fatal` + `leq` 类型不匹配

### 交接注意事项
1. **不要盲目继续加 infix 函数** — 先查每个报错点的具体类型（receiver 和 operand 的真实类型），精准匹配
2. `m * pos` 这类：`m` 是 `Flt64`，`pos` 是 `PctVar`（extends `AbstractVariableItem`），`Flt64 * PctVar` 返回 `LinearMonomial<Flt64>`，已在 DSL 中添加了 `LinearMonomial<Flt64>.geq(AbstractLinearPolynomial<*>)` 等，但可能还不够覆盖所有模式
3. 建议先手动修复 IfIn.kt（最复杂），因为它暴露了 MathLinearInequality 与 frontend LinearInequality 的 API 差异
4. 编译通过后执行：`mvn -pl ospf-kotlin-core -am test`

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

### R5 Mechanism 层去 inequality 依赖（2026-04-09）
1. 机制层所有 `addConstraint(LinearInequality)`/`addConstraint(QuadraticInequality)` 标记 `@Deprecated` + `ReplaceWith`
2. `LinearConstraint`/`QuadraticConstraint` 的 `Inequality`-based 构造器标记 `@Deprecated`
3. `LinearConstraintInput.from(LinearInequality)` 标记 `@Deprecated`
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

### 9. M8：最终目录删除（重拆执行版）
目标：完成目录级收敛，删除旧表达与 inequality 实现。

当前规模（2026-04-09 更新）：
1. `frontend.inequality` 外部 import：~120 处（55+ 文件，排除 inequality 目录自身 5 文件）
2. `frontend.expression.monomial/polynomial` 外部 import：216 处（70 文件）
3. 已删除：`frontend/inequality/adapter/`（3 文件）
4. 已删除：`frontend/symbol_migration/adapter/` 测试（2 文件）

前置条件：
1. M6 的 `R0~R4` 全部完成并通过阶段门禁。
2. M6/R5 部分完成（adapter 子目录已删除）

任务拆解（R5~R9）：
1. `[x]` `R5` 删除 `frontend/inequality/adapter` 子目录
   - 内联 `mergeMonomialsByUtils` 到 `LinearInequality.kt`/`QuadraticInequality.kt`
   - 删除 adapter 测试文件
2. `[ ]` `R5-continue` 删除 `frontend/inequality` 主目录（含 Inequality.kt 等 5 文件）
   - 需先完成 M6/R3：迁移函数符号 off `eq/leq/geq` DSL
   - 外部模块（framework、plugins）也需迁移
3. `[ ]` `R6` 清理函数符号侧残余运算实现，统一改为调用 `math.symbol` 并输出 flatten/relation 桥接载荷；同时将 `cell -> math.symbol.monomial`、`value`、`range` 处理迁移到元模型上下文。
3. `[ ]` `R7` 迁移 `IntermediateSymbol` / `SymbolCombination` 到 `math.symbol` 桥接接口（不再暴露 monomial/polynomial 细节），并移除表达类型内分散的 `value/range` 处理职责。
4. `[ ]` `R8` 完成 `linear_function(33)` + `quadratic_function(21)` 全量迁移并回归。
5. `[ ]` `R9` 删除：
   - `frontend/expression/monomial`
   - `frontend/expression/polynomial`
   - `frontend/expression/adapter`
   - `frontend/expression/Expression.kt`
6. `[ ]` 执行全量回归：
   - `mvn -pl ospf-kotlin-core -am test`
7. `[ ]` 执行插件编译检查（见“回归命令 3”）。

完成定义（DoD）：
1. `src/main` 不再出现上述旧路径 import。
2. `ospf-kotlin-core` 全量测试通过。
3. `core` 不再承载线性/二次符号运算实现。
4. `cell`、`value`、`range` 的运行期处理集中在元模型上下文，不再散落在旧表达类型中。

### 10. M9：封口门禁（强化）
目标：建立“删完后不可回流”的 CI 门禁与文档封口。

任务拆解：
1. `[ ]` 新增 CI 扫描测试，禁止以下 import：
   - `frontend.inequality`
   - `frontend.expression.monomial`
   - `frontend.expression.polynomial`
   - `frontend.expression.adapter`
2. `[ ]` 更新迁移文档（旧 DSL -> relation/flatten API 对照表）。
3. `[ ]` 在 PR 模板或检查脚本加入迁移项核对（旧路径 import、`.cells` 计算读取）。

完成定义（DoD）：
1. 后续 PR 无法回引旧路径。
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
3. M8/M9 完成后：
   - `mvn -pl ospf-kotlin-core-plugin/... -am -DskipTests compile`

---

## 已知问题
1. SCIP 插件存在 `EventHandler/EventMask` API 兼容问题（当前迁移主线非阻断）。
2. 当前代码存在大量 `cells` 相关 deprecation warning（属过渡态，M6~M9 清理）。

---

## 下个环境起手顺序（严格执行）
1. **先修复 M6/R3 编译错误**（见"最新状态"），不要从头开始
2. 编译通过后执行 `mvn -pl ospf-kotlin-core -am test`
3. 完成 R3 后执行 `R5-continue`：删除 `frontend/inequality` 主目录（5 文件）
4. 按 `R6 -> R9` 完成目录级删除与 M8 收口。
5. 最后执行 `Phase 4 -> Phase 6` 功能与稳定化收敛。
6. 每个阶段回写本文件：新增完成项、剩余缺口、回归命令结果。
