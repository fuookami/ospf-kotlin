# 超启发式搜索

[English](README.md) | 简体中文

本包直接属于 **ospf-kotlin-core-plugin-heuristic**，不新增 Maven 制品。
依赖方向为 `hyper_heuristic -> heuristic`，低层算法不依赖高层调度策略。

## 包职责

| 包 | 职责 |
| --- | --- |
| 根包 | 配置、求解器、独立运行实例、组合门面 |
| `selection` | 算法/算子选择、动作掩码、类型安全槽位 |
| `scheduling` | 预算分片、组合会话、调度扩展 |
| `memory` | 有效档案、已接受工作解集、接受与交换策略 |
| `learning` | 特征、奖励、实例训练、评估、轨迹重放 |
| `configuration` | Kotlin DSL、JSON/YAML、注册与能力校验 |
| `persistence` | 独立版本的策略/检查点、原子文件存储 SPI |

## 注册与配置

使用 `HyperHeuristics.registry<O, V>(converter)` 创建注册表，通过
`registerProblem(id, statelessCallbacks, factory)` 注册问题。每次工厂调用必须创建**独立模型、
令牌表及回调/缓存状态**。已有模型可用 `singleObjectiveProblem` 包装；多目标使用
`multiObjectiveProblem` 和 `HyperHeuristics.multiObjectiveRegistry`。
问题指纹应覆盖输入数据、编码、目标方向及回调语义。不根据类名反射实例化，也不序列化闭包。

```kotlin
val configured = hyperHeuristic {
    problem = ComponentSpec("my-problem")
    budget = BudgetConfig(evaluations = 10_000, sliceEvaluations = 256)
    execution = ExecutionConfig(
        mode = ExecutionMode.Synchronous,
        parallelism = 2,
        seed = 42
    )
    selection = ComponentSpec("ucb1")
    algorithm(id = "swarm", type = "pso") { size = 20 }
    algorithm(id = "adaptive-ga", type = "genetic") {
        size = 20
        updates = listOf(ComponentSpec("gwo"), ComponentSpec("sca"))
        mutations = listOf(ComponentSpec("gaussian"), ComponentSpec("none"))
        selection = ComponentSpec("q-learning")
    }
    persistence = PersistenceConfig(enabled = true)
}
// 处理 Ret 失败后，将配置及注册表传给 HyperHeuristicSolver。
// Handle Ret failures before constructing the solver.
```

`ConfigurationFormats.encode/decode` 将同一个配置模型读写为 JSON 或 YAML。
以下 YAML 只需注册 `my-problem` 问题工厂即可执行：

```yaml
version: 1
problem: {type: my-problem}
algorithms:
  - id: swarm
    algorithm: {type: pso}
    size: 20
  - id: adaptive-ga
    algorithm: {type: genetic}
    size: 20
    updates: [{type: gwo}, {type: sca}]
    mutations: [{type: gaussian}, {type: none}]
    selection: {type: q-learning}
budget: {evaluations: 10000, sliceEvaluations: 256}
selection: {type: ucb1}
execution: {mode: Synchronous, parallelism: 2, seed: 42}
persistence: {enabled: true}
```

组件引用包含 `type`、`version` 和 `parameters`；业务输入放在问题组件的 `parameters` 中。
算法实例使用稳定 ID（字母、数字、`.`、`_`、`-`）。默认同步执行、轮询选择、固定预算片段、
只接受不劣解。必须显式指定全局评估、决策、低层步数或有效时间上限中的至少一种。
未知组件、参数、版本、不支持的槽位或不兼容的编码均返回 `Ret` 错误，不静默退化。

## 内置算法与槽位

单目标算法 ID：

- `genetic`、`gwo`、`sca`、`mvo`、`hs`、`wca`、`soa`、`warso`、`evo`、`gco`、`de`、`woa`、
  `hho`、`aoa`、`tlbo`、`ssa`、`gsa`、`fa`、`cs`、`ba`、`mfo`、`goa`。
- `pso`、`apso`、`clpso`、`shade`、`l-shade`、`cma-es`。
- `hill-climbing`、`ns`、`random-walk`、`simulated-annealing`。
- 多目标门面另注册 `nsga-ii`、`nsga-iii`。
- `island` / `island-ga` 轮流推进持久化的 `children`，通过 `migrationPeriod` 控制迁移；
  `memetic` 按配置顺序推进子阶段，通过 `phaseSteps` 控制阶段长度。组合共享父级预算，子会话保留独立随机流。

种群算法和 NSGA 支持更新/变异槽位，轨迹算法支持邻域槽位。
PSO 参数源槽位为 `inertia`、`personalWeight`、`socialWeight`；CLPSO 为 `inertia`；
L-SHADE 为 `sizeRatio`；模拟退火为 `temperature`。仅接受实际支持的槽位。
排列或自定义编码需要显式兼容的算子池，不能直接套用连续算法默认算子。

选择器包含 `fixed`（固定循环序列）、`round-robin`、`stagnation`、`random`、`epsilon-greedy`、
`ucb1`、`q-learning`。参数为 `epsilon`、`alpha`、`gamma`、`states`、`stagnationLimit`。
内置进度特征空间需要至少 64 个离散状态。冻结模式禁止学习参数更新及学习策略的探索，
但规则策略维持调度所需的游标仍会推进。

多目标问题中的单目标算法**必须**通过 `AlgorithmConfig.scalarization` 引用显式注册的标量化。
不会默认向量求和；多目标 `SearchResult.best` 为 null。即使候选来自标量算法，
全局档案中也保留原始目标向量。

## 生命周期、预算与并行

`AbstractRunner.run` 现在返回 `Ret<List<Individual<O, V>>>`，调用方需处理或传播失败。
`RunnerSolver` 和 NSGA 门面传播错误。`openSession(problem, codec, importer)` 与完整求解
共用 initialize/advance 钩子，不反复执行完整 solver。`RunnerStateCodecs` 保留种群、嵌套岛屿、
粒子、CLPSO 学习样本、SHADE 历史及 CMA-ES 协方差/演化路径。
多个同时存活的会话应使用不同 runner 实例。

初始化、导入、重复或缓存评估请求均消耗原子全局令牌。
`SearchResult.evaluations` 统计请求数；`objectiveCalls` 统计可行性检查后的实际模型目标调用数，
不展开业务回调内部的工作或多个目标项。自定义会话应使用
`SearchProblem.evaluate(solution, budget)` 统一计费。
分别统计高层决策、完整低层步数、评估和有效时间；残缺代不替换原生状态，已完成的候选仍可归档。
时间限制为协作式，不能强行中断阻塞业务回调。

协调器独占修改全局记忆与高层学习状态，worker 拥有独立模型和随机流。
片段内部 runner 任务串行执行，由外层统一控制最大并发度。
同步批次按稳定实例 ID 归并；异步按完成顺序归并，不保证不同时序得到相同结果。
反馈携带派发状态、决策 ID、实际动作和策略版本，不归因给后续无关动作。

`solver.open()` 返回独立 `HyperHeuristicRun`，提供 `advance`、`run`、`requestStop`、`result`、
`transitions`、`exportPolicy`、`checkpoint`、`close`。完整 `solver()` 入口负责资源清理；
显式管理运行时需在 `finally` 中关闭。停止、保存和关闭会归并在途片段，不丢弃反馈。
算法轨迹作用域为 `algorithm`；算子轨迹采用稳定路径，例如 `adaptive-ga/update`、
`islands/child-a/mutation`。

有效档案与已接受工作解集分别维护，高层拒绝不回滚粒子、历史或分布私有状态。
只将目标有效且明确可行的候选纳入档案。
`initialization = WorkingSet` 在首个完整原生初始化后导入当时可用工作解；重新评估并计费。
`importMode = Disabled` 禁止外部导入；组合会话因子会话交换需要原生导入能力，不支持禁用。
周期交换使用 `periodic` 组件，参数为正整数 `period`、`amount`。
CMA-ES 必须设置 `restartOnImport: true`，明确表示围绕导入种子重启分布。

## 训练、重放与恢复

`PolicyTrainer.train(config, TrainingSplit(...), seeds, initial)` 先按**互不重叠的问题实例**划分数据，
再分配种子。每个 episode 创建新会话，训练策略可跨实例继续更新；验证与测试始终冻结。
`PolicyEvaluator.evaluate` 可在相同显式评估预算下运行固定算法、随机、轮询或学习策略。
报告保留原始目标档案和成本，方便业务方选择适用的质量度量。
`PolicyEvaluator.compare` 在相同评估额度下比较命名基线，报告质量样本、总成本与失败率。
`PolicyTrainer.replay` 显式选择轨迹作用域；Q-learning 支持掩码、离策略和终止转移，
Bandit 仅更新实际执行动作。
`TrajectoryFiles` 提供原子 JSON Lines 写入和完整读取，配置 `trajectoryPath` 可导出完成运行。
不依赖外部机器学习平台。

`FileSearchStore(directory)` 按安全文件键存取 `PolicyArchive` 和 `RunCheckpoint`。
策略保存学习表/统计及组件定义；检查点额外保存全部原生状态、算子策略、档案、工作集、预算、
计时、随机流、派发序号、问题回调状态和已完成轨迹。
保存先暂停派发、等待在途、归并并反馈，再创建静止快照；暂停/存储时间不计有效时间。
文件原子替换及载荷校验和防止接受残缺文档。

恢复时先加载文档，再显式调用 `solver.open(checkpoint = saved)` 或
`solver.open(policy = savedPolicy)`，不能同时提供两者。
检查点要求配置与问题指纹一致，策略动作按稳定 ID 对齐。
配置、策略和检查点分别校验版本；可向格式解码或文件存储传入显式
`ConfigurationMigration` 映射，未知版本拒绝加载。

启用完整恢复时，问题工厂必须声明回调无状态或提供 `SearchProblemState`；
有状态自定义算子必须实现 `RecoverableComponent`，明确声明无状态的算子无需状态编码。
自定义选择器负责自身 snapshot/restore 协议；特征、奖励、接受、交换及预算分配器
同样必须显式声明无状态，或实现 `RecoverableComponent` 并参与完整检查点恢复。不可恢复组件在启动时失败，不生成缺失状态的检查点。

## 扩展与验证

通过注册问题、会话、类型安全算子、选择器、特征、奖励、接受、交换及预算分配器扩展，
无需修改主调度循环。业务目标和解值通过显式 codec 无损保存，数值转换集中于适配边界和
已有低层算法数学实现；公开学习特征与奖励使用无量纲 `Flt64`。

可执行测试位于 `src/test/.../hyper_heuristic/HyperHeuristicTest.kt` 和 `SelectionPolicyTest.kt`，
覆盖配置等价与迁移、预算、全部内置原生 ID、组合及原生恢复、异步归并、执行失败、
Q/UCB 手算、冻结推理、训练集隔离、多目标标量化及文件损坏。

边界：单 JVM 多协程、选择型超启发式及固定组合；不含算法程序生成、数据库存储、分布式调度或深度学习平台。
