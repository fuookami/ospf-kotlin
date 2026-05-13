# 泛型化交接（2026-05-13）

## 目标

将 `ospf-kotlin-math`、`ospf-kotlin-core`、`ospf-kotlin-framework` 从“以 `Flt64` 为主、外层局部泛型”的状态推进到“建模、表达式、中间模型、框架算法全链路支持泛型数值类型”。

外部求解器若只能接收 double，可继续以 `Flt64` 作为 solver adapter 原生值类型；但 `Flt64` 不应泄漏到泛型建模 API、框架算法 API、约束/目标/解输出主类型签名中。

## 总体原则

1. 泛型主类型统一记为 `V`，约束为 `where V : RealNumber<V>, V : NumberField<V>`。
2. `Flt64` 仅允许出现在兼容层、求解器适配层、`adapter.flt64` 包、测试基准与数值转换边界。
3. 建模层、机制模型层、框架算法层主 API 返回 `V` 类型结果（如 `FeasibleSolverOutput<V>`、`LinearInequality<V>`、`QuadraticInequalityOf<V>`）。
4. `IntoValue<V>` 仅作为边界转换能力，不能替代内部泛型数据结构。
5. 旧 `Flt64` API 可保留为 deprecated/compat wrapper，但不再承载唯一业务实现。

## 已完成事项（清空）

已完成事项滚动列表已清空，不在本文件继续累积。历史记录统一以 `git log --oneline` 为准。

## 本轮已完成总结（截至 2026-05-13）

1. 修复了“无法编译/无法测试”的流程问题：统一通过 `-am` 联动构建上游模块，确保本地依赖与源码 API 对齐。
2. `framework/solver` 完成列生成与 Benders 的 `V` 结果桥接落地：
   - 已支持 `solveMILPV/solveLPV`、Benders `solveMasterV/solveSubV`。
   - 已补齐 `LinearMetaModel<V>/QuadraticMetaModel<V>` 输入重载（不要求调用方显式传 converter）。
3. Benders 异步桥接已收口：
   - 增加 `solveMasterVAsync` 的泛型输入重载（线性/二次）。
   - 增加 `solveSubVAsync`（线性/二次，含 `Flt64+converter` 与泛型输入入口）。
4. 测试覆盖已同步补齐并通过：
   - `ColumnGenerationSolverVBridgeTest`
   - `BendersSolverVBridgeTest`
5. `math` 模块完成一轮兼容清理：
   - 将内部 `QuadraticInequality` 旧别名使用替换为 `QuadraticInequalityOf<Flt64>`，降低后续升级噪音。
6. 回归结果：
   - 多次定向测试通过（bridge 相关用例）。
   - `mvn --% -pl ospf-kotlin-framework -am test` 全量通过。

## 归档提交（最近）

1. `8fe8d9c3` feat: add async bridges for benders sub v-solvers
2. `68c26140` feat: add generic async bridges for benders master v-solvers
3. `6e9b3830` refactor: replace deprecated quadratic inequality alias usage
4. `2ed2e5da` feat: add generic meta-model input bridges for benders v-solvers
5. `cbd1fc11` feat: add generic meta-model input bridges for column generation solver
6. `98f5cb46` feat: add v-typed bridges for benders master solvers
7. `3052d15e` feat: add v-typed bridges for benders sub-solvers
8. `53a1b024` feat: add v-typed bridges for column generation solver
9. `0b99e2ac` refactor: normalize framework solver flt64 signatures

## 下一步计划（交接给下个会话）

1. `framework` 接口形态统一收尾：
   - 检查并补齐 ColumnGeneration 是否需要与 Benders 对齐的异步 `V` 泛型输入入口。
   - 复核 solver 包内 `V` 接口命名与参数顺序，减少重载歧义。
2. `core` `.toDouble()` 主链路治理继续推进：
   - 保持 `SolveValueConversionContext.kt` 作为唯一边界点。
   - 扩展/固化门禁规则，防止新增 `.toDouble()` 回流到主链路。
3. 增量回归策略：
   - 每完成一组改动先跑对应定向测试。
   - 每完成一个阶段跑一次 `mvn --% -pl ospf-kotlin-framework -am test`。
4. 提交策略：
   - 每个提交只覆盖单一主题（接口桥接、测试、门禁、清理分开提交）。

## 建议下个会话起手命令

1. `git status -sb`
2. `git log --oneline -12`
3. `mvn --% -pl ospf-kotlin-framework -am -Dtest=BendersSolverVBridgeTest,ColumnGenerationSolverVBridgeTest -Dsurefire.failIfNoSpecifiedTests=false test`

## 当前状态

- 分支：`rewrite-bigbang`
- 工作区：干净
- 进度：`origin/rewrite-bigbang` 之上 `ahead 11`

## 本会话任务单（提交级执行版）

1. `chore: establish genericization baseline and gap list`
   - 执行基线命令：`git status -sb`、`git log --oneline -12`、bridge 定向测试。
   - 盘点 `framework/solver` 中 `ColumnGeneration` 与 `Benders` 的异步 `V` 入口差异。
   - 产出差异清单（命名、参数顺序、重载入口是否对齐）。
2. `feat: align column-generation async v-input bridges with benders`
   - 若 `ColumnGeneration` 缺失与 `Benders` 对齐的异步 `V` 入口，补齐线性/二次泛型输入重载。
   - 保持旧 `Flt64` 入口兼容，主实现收敛到 `V` 桥接。
   - 补充/调整对应测试，覆盖新增入口与向后兼容路径。
3. `refactor: normalize v-solver method naming and parameter order`
   - 统一 `framework/solver` 包内 `V` 相关方法命名与参数顺序。
   - 消除歧义重载，必要时通过 wrapper 保持二进制/源码兼容。
   - 保证公共 API 文义一致：输入模型、converter、上下文参数顺序稳定。
4. `chore: enforce toDouble boundary in core pipeline`
   - 全仓扫描 `.toDouble()` 调用并分类（允许区/禁止区）。
   - 主链路仅允许通过 `SolveValueConversionContext.kt` 进入 double 边界。
   - 新增或强化门禁规则，阻止 `.toDouble()` 回流到泛型主链路目录。
5. `test: run staged regression for framework generic bridges`
   - 每完成一个主题提交先跑对应定向测试。
   - 阶段完成后执行 `mvn --% -pl ospf-kotlin-framework -am test` 全量回归。
   - 若失败，先最小化修复再继续下一提交，避免跨主题污染。

## 本会话执行顺序（从现在开始）

1. 先执行任务 1：基线确认 + 差异清单。
2. 立即进入任务 2：按差异补齐 `ColumnGeneration` 异步 `V` 入口并补测试。
3. 然后执行任务 3：统一命名与参数顺序并回归。
4. 最后推进任务 4：`.toDouble()` 门禁治理并跑阶段全量测试。
