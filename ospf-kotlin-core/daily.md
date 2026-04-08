# OSPF Kotlin Core Daily

日期：2026-04-08
交接目标：下一个执行环境
Rust 对齐参考：`E:\workspace\ospf-rust`

## 当前结论
1. `Phase 1`（正确性热修）已完成并通过回归。
2. `Phase 2`（Context 生命周期）已完成并接入主链路。
3. `Phase 3` 已完成基础迁移与主路径切换，但尚未收口到”可删除 `expression(非symbol)+inequality` 目录”。
4. **`P0`（Phase 3 正确性阻断）已完成** ✅
5. **`M1`（统一代数内核）已完成** ✅
6. **`M2`（表达层收口）已完成** ✅
7. **`M3`（引入新关系类型）已完成** ✅
8. **`M4`（机制层去旧泛型）部分完成**（M4-1 ✅，M4-2/3 待后续）
9. 当前首要任务是 **`M5`（模型 API 迁移）**。

---

## 最近完成事项

### M3: 引入新关系类型（2026-04-08）
1. 新建 `Relation.kt`：`LinearRelation`/`QuadraticRelation` 接口及实现
2. 添加适配器：`LinearInequality.toRelation()` / `QuadraticInequality.toRelation()`
3. `Constraint.kt` 新增 `LinearRelation`/`QuadraticRelation` 构造器
4. `RelationTest.kt`：8 tests

### M4: 机制层去旧泛型（部分完成 2026-04-08）
1. `SubObject.kt` 新增 `FlattenData` 构造器
2. `SubObjectTest.kt`：4 tests
3. M4-2 评估结论：转换函数保留到 M8
4. M4-3 待 M5 处理

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

### D. math.symbol 运算能力补齐
1. `LinearMonomial` / `QuadraticMonomial` 运算符重载补齐。
2. `LinearPolynomial` / `QuadraticPolynomial` 运算符重载补齐。
3. `LinearInequality` / `QuadraticInequality` 比较重载补齐。
4. canonical 类型补回（`CanonicalMonomial` / `CanonicalPolynomial`）。

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

### 2. M1：统一代数内核（去重复实现）✅ 已完成
目标：flatten 合并与乘法展开单点收口。

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
1. ✅ flatten 核心算法仅保留一套实现。
2. ✅ 现有行为与 baseline 一致。

### 3. M2：表达层收口（保留兼容壳）✅ 已完成
目标：表达层核心计算全部走 flatten，`cells` 仅保留只读兼容。

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
1. ✅ 主链路无 `.cells` 计算依赖（`cells` 仅为兼容视图）。
2. ✅ baseline 全绿。

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

### 5. M4：机制层去旧泛型与桥接（部分完成）
目标：mechanism 层去掉旧 cell 泛型耦合。

任务拆解：
1. `[x]` `SubObject/Constraint` 输入签名移除 `*MonomialCell` 约束。
   - 新增 `LinearSubObject.invoke(flattenData: LinearFlattenData, ...)` 构造器
   - 新增 `QuadraticSubObject.invoke(flattenData: QuadraticFlattenData, ...)` 构造器
   - 新增 `SubObjectTest.kt` 测试（4 tests）
2. `[-]` 删除 `TokenCacheContext` 中 flatten <-> cell 双向桥接。
   - **评估结论**：转换函数用于 deprecated `cells` 属性兼容层
   - 保留到 M8 删除旧类型时再移除
3. `[ ]` `MetaModel._subObjects` 去 cell 类型耦合。
   - 需要 M5 中重构 `MetaModel.SubObject` 类定义
4. `[ ]` 修复受影响调用点并补回归。

产出物：
1. `SubObject.kt` 新增 FlattenData 构造器
2. `SubObjectTest.kt` - 4 tests
3. `Constraint.kt` 已在 M3 添加 LinearRelation/QuadraticRelation 构造器

完成定义（DoD）：
1. ✅ 新调用方可直接使用 FlattenData，无需依赖 frontend 多项式类型。

### 6. M5：模型 API 迁移（最大工作包）
目标：模型对外 API 切换到新关系对象与新表达签名。

任务拆解：
1. `[ ]` 迁移 `frontend/model/Model.kt`。
2. `[ ]` 迁移 `MetaConstraint.kt`。
3. `[ ]` 迁移 `MetaModel.kt`。
4. `[ ]` 迁移 `MechanismModel.kt`。
5. `[ ]` `addConstraint/partition/addObject` 改为新签名。
6. `[ ]` 旧签名保留短期 deprecated shim，并标注移除窗口。
7. `[ ]` 运行 `mvn -pl ospf-kotlin-core -am test` 并记录结果。

完成定义（DoD）：
1. 模型主 API 不再 import 旧 `monomial/polynomial/inequality` 包。

### 7. M6：函数符号批量迁移
目标：函数符号层脱离旧表达类型与旧 inequality。

任务拆解：
1. `[ ]` 迁移 `expression/symbol/linear_function/*` 到 flatten + 新关系对象。
2. `[ ]` 迁移 `expression/symbol/quadratic_function/*` 到 flatten + 新关系对象。
3. `[ ]` 清理 `override val cells get() = ...cells` 与内部 `.cells` 读取。
4. `[ ]` 补函数符号层回归测试。

完成定义（DoD）：
1. 函数符号层不再依赖 `frontend/inequality` 与旧表达主路径。

### 8. M7：删除 `expression/adapter`
目标：完全清空并移除 adapter 目录。

任务拆解：
1. `[ ]` 盘点 `frontend/expression/adapter` 能力与引用点。
2. `[ ]` 仍需能力迁到 `symbol` 邻域或直接改用 `math.symbol` API。
3. `[ ]` 删除 adapter 目录与全部引用。
4. `[ ]` 执行编译与回归验证。

完成定义（DoD）：
1. `src/main` 无 `frontend.expression.adapter` import。

### 9. M8：最终目录删除
目标：删除旧表达与 inequality 目录，完成目录级收敛。

任务拆解：
1. `[ ]` 删除：
   - `frontend/expression/monomial`
   - `frontend/expression/polynomial`
   - `frontend/expression/adapter`
   - `frontend/expression/Expression.kt`
   - `frontend/inequality`
2. `[ ]` 修复编译错误直到 `ospf-kotlin-core` 全绿。
3. `[ ]` 执行 `mvn -pl ospf-kotlin-core -am test`。
4. `[ ]` 执行插件编译检查（见“回归命令 3”）。

完成定义（DoD）：
1. `src/main` 不再出现上述路径 import。

### 10. M9：封口门禁
目标：建立 CI 防回流机制 + 文档封口。

任务拆解：
1. `[ ]` 新增“禁止旧路径 import”扫描测试（CI 必跑）。
2. `[ ]` 更新迁移文档（新旧 API 对照、不兼容变更、替换示例）。
3. `[ ]` 在 PR 模板或检查脚本加入迁移项核对。

完成定义（DoD）：
1. 后续 PR 无法回引旧路径。

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
1. 先做 `P0`（交叉项/归并策略修复 + 守卫测试）。
2. 再按 `M1 -> M9` 完成目录级迁移与删除。
3. 最后执行 `Phase 4 -> Phase 6` 功能与稳定化收敛。
4. 每个阶段回写本文件：新增完成项、剩余缺口、回归命令结果。
