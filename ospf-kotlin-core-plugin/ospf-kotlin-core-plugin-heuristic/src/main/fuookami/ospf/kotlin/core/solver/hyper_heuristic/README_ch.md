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


## 非平稳信用与正交组合

`OrthogonalOperatorPortfolio.continuous()` 提供分布、自适应差分、围捕、重尾/对立、局部精调五个生态位，
`permutation()` 提供保序交叉和交换/反转邻域，不将全部群智能更新塞入同一动作空间。
组合不是跨问题的性能保证；请按相同评估预算与单算法、随机和轮询基线比较。

两层 `selection` 均支持：

| ID | 实现及参数 | 信用语义 |
|---|---|---|
| `frr-mab` | `FRRMABSelector`；`window=50`、`exploration=0.5`、`decay=0.8` | 最近反馈窗口内累积正收益，按秩衰减分配信用，再加窗口 UCB 探索项 |
| `choice-function` | `ChoiceFunctionSelector`；`rate=0.2`、`recencyWeight=0.1` | 单动作与二阶转移收益的指数均值，加等待奖励；前驱在派发时冻结，异步完成不重配对 |
| `adaptive-pursuit` | `AdaptivePursuitSelector`；`rate=0.2`、`minimumProbability=0.01` | 更新已执行动作的收益，追逐当前赢家，保留全动作探索概率下界 |

`NonStationarySelector` 提供上述完整参数入口；动作掩码重新归一化概率，禁用动作不被选择。
快照按 ID 保存滑动窗口、二阶分数、概率、派发上下文和修订号；冻结推理不改变学习状态。

### 奖励与数值边界

`learning.reward` 新增：

- `dominance`：有效非支配新贡献比例，而不是子代数量或前沿大小。
- `time-normalized`：`max(0, old-new)/(seconds+epsilonSeconds)`，默认 `epsilonSeconds=1e-9`。
- `extreme-value`：最近 `window=50` 次观测中，该实际动作的最大标量突破；旧突破随全局窗口过期。
- `hypervolume-delta`：多维最小化超体积并集增量。`reference` 是归一化后的劣参考坐标数组，
  也保留二维 `referenceX`/`referenceY` 写法。`mode="exact"` 使用递归切片，几何操作额度
  `maxWork=1000000` 耗尽时明确失败，绝不自动退化到采样。
  高维可以显式选择 `mode="sampled"`、`samples=16384`、`seed=1`；共同随机数确保重复测量可复现。
  `HypervolumeDeltaReward.measure`/`lastMeasurement` 提供样本数、标准误差和 99% Hoeffding 绝对误差界。
  几何计算不调用业务目标，不消耗目标评估令牌；采样误差界不等于质量改善保证。

不配置 `reference` 时，按 `calibrationObservations=32` 次有效候选观测校准，再添加 10% 余量并冻结。
校准期及冻结动作奖励为零，之后不随档案漂移参考点。校准数据、冻结参考点和最近误差指标均进入检查点。
参考点溢出时明确要求配置显式参考点，不生成不可用信用。

标量收益和 HV 要求 `SearchProblem.objectiveCoordinates`：业务值在这个唯一适配边界投影为有限、
已归一化、越小越优的坐标。最大化需显式取反；不同量纲需显式缩放，并将定义纳入问题指纹。
`multiObjectiveProblem` 默认按明确的 `directions` 变换符号，也允许覆盖坐标映射。
`singleObjectiveProblem` 不猜测业务目标的数值或方向。标量收益不接受多目标问题。
耗时记录为 `SelectionTransition.elapsedNanos`；采用真实耗时奖励后，即使同步/评估预算模式也不保证策略轨迹逐位复现。

每个 worker 和协调器有独立奖励实例；有状态奖励随会话/全局组件检查点恢复，不跨并发会话共享窗口。
算子信用只反馈实际使用的动作；未执行的槽位会释放派发上下文。

## 异构桥接与精调

`ContextStateSynchronizer` 在已重评估解导入时：

- PSO 按最近目的粒子匹配，用导入位置与其当前位置的差建立夹紧速度，并保留真实历史最优。
  这不是源算法两代轨迹的臆造；如业务有真实配对位置，可直接调用 `velocity(previous,current)`。
- SHADE/L-SHADE 合并工作种群、将淘汰成员放入有界外部档案，并保留真实 `M_F/M_CR`，
  **不根据位置伪造成功的 F/CR 样本**。
- CMA-ES 仍需显式 `restartOnImport=true`；群体导入估计含相关项的正则化协方差，
  单解只重定心并保留原协方差；重置演化路径，历史最优保留。
- 轨迹会话从导入群体中选择优解。算法切换本身只暂停调度，不清空目的会话状态。

新增更新 ID：`pattern-search`（Hooke-Jeeves，`initialStep=0.1`、`contraction=0.5`、`sweeps=32`）、
`opposition`、`pipeline`、`vnd`、`elite-local`、`mutation-stage`、`neighborhood-stage`。
对立跳跃只对有有限上下界的坐标生效；缺界坐标保留原值。
组合的 `parameters.stages` 是 `ComponentSpec` 数组，`vnd` 可设 `maxTrials`，`elite-local` 可设 `amount`。
变异/邻域阶段用 `parameters.operator` 引用注册算子，变异阶段另有 `rate`。
流水线中间产物重新计费评估后才传给下一阶段；VND 改善后重置到第一邻域。
`EliteLocalSearchWrapper` 接收已排序父代，仅精调 Top-K。`NeighborhoodUpdate` 可以将轨迹邻域提升为标准更新阶段。
组合配置目前只接受显式无状态的兼容编码组件；有状态业务组合需通过独立可恢复组件注册，不能丢失子状态。
所有试探、中间评估及最终评估均受同一个父级令牌预算约束；请为精调按维度配置足够片段额度，
不足时不提交残缺原生状态，但已完成的有效评估仍可进入全局档案。

## 接受、排列与约束

设置 `memory.acceptance=ComponentSpec("auto")` 使用 `AutoAcceptance`，无需用户指定温度、历史长度或容差。
容量从已配置的全局预算自动推导并设界，延迟跨度按近期严格改善自适应调整；直接比较业务目标，
支持单目标全序和多目标支配，对保持方向的单调目标缩放不敏感。拒绝也会推进历史。
检查点保存适应计数和无损业务目标历史。这是配置边界免调参，不宣称实现内部不存在常数。
既有默认仍为 `not-worse`。

显式替代项为 `late`（`LateAcceptance`，`length=50`）和 `record-to-record`
（`RecordToRecordAcceptance`，`initialRatio=0.05`、`horizon=1000`）。两者只支持单目标并保存历史。
LAHC 免温度、对目标尺度不敏感，但仍有历史长度参数；记录到记录按显式标量坐标的历史量级缩放动态阈值，
并非严格免调参。接受规则只影响工作解集，不回滚算法私有状态或删除有效档案。

排列 ALNS 组件位于 `heuristic.operator.neighborhood`：`RandomRuin`、`WorstCostRuin`、`RelatednessRuin`、
`GreedyInsertRepair`、`RegretKInsertRepair`、`RuinRecreateOperator`。
元素须有唯一身份；代价/相关性通过类型化业务回调注入，不序列化闭包。
遗憾插入优先处理可行位置不足 K 的元素，拒绝非有限代价，并在无法修复时返回错误而非丢弃元素。
插入代价是部分解的业务估计，不应在闭包中绕过预算调用完整目标。
将完整邻居交给 `SearchProblem.evaluate`，在自定义 `SearchSession` 中将每个 destroy/repair 配对注册为独立动作，
即可沿用两层选择和高层协调器，无需修改主循环。

`RuinRecreateSearchSession` 提供可直接注册的完整排列会话；例如对已定义的业务插入代价 `cost`：

```kotlin
registry.registerAlgorithm("domain-ruin") { context ->
    Ok(RuinRecreateSearchSession(
        problem = context.problem,
        operator = RuinRecreateOperator(RandomRuin(), RegretKInsertRepair(cost)),
        amount = 2,
        random = context.random,
        statelessEstimates = true
    ))
}
```

随后用 `AlgorithmConfig(id = "ruin", algorithm = ComponentSpec("domain-ruin"))` 加入算法池。
`statelessEstimates=true` 只适用于真正无状态的业务估计；否则提供 `estimateState`，启用完整检查点时会明确校验。
完整目标统一计费，部分解插入估计不冒充完整目标调用；此扩展及恢复路径有协调器集成测试。

原生约束处理不再要求自定义会话。给 `SearchProblem.constraintViolation` 提供有限非负的归一化违约度回调
（单/多目标便利工厂也接受该参数），再为叶算法设置 `AlgorithmConfig.constraints`：

```kotlin
constraints = ConstraintHandlingConfig(
    mode = ConstraintMode.Epsilon,
    horizon = 1000
)
```

支持 `FeasibilityFirst`、`Epsilon`、`StochasticRanking`。epsilon 初值默认从初始化候选校准，
也可显式给 `initialEpsilon`，按完整步逐渐收紧到零。GA/群智能/PSO/CLPSO/SHADE/CMA-ES、轨迹接受、
个体/历史最优、导入及 NSGA-II/III 共用类型化桥；NSGA 在约束层内保留原生 Pareto、拥挤度和参考点选择。
随机排序采用有界批扫描和独立可恢复随机流；逐对接受和历史比较仍使用确定性的可行性优先规则，
不把随机过程塞进 comparator。预算中断或失败时，约束适应状态与原生状态一同回滚。

显式启用处理后会放宽默认可行性 admission，但保留自定义 `admissible` 业务禁区和编码检查。
业务目标不替换为惩罚值，严格 `feasible` 仍控制全局档案及工作集，违约度为零不等于已证明可行。
违约估计回调不得绕过预算调用完整目标，其归一化定义应纳入问题指纹。
组合根节点不接受约束配置，应配置到叶子；自定义算法须注册 `supportsConstraints=true` 并遵守
`compareIndividuals`/`rankIndividuals` 协议，否则启动前明确失败。
直接打开原生 runner 会话时也可用 `SearchProblem.withConstraints`。
底层 `EpsilonConstraintEvaluator.rank`、`StochasticRanking.rank` 继续供自定义业务会话使用。

## 双端奖励状态协议

受控会话与 Rust 共同使用 `rewardStateVersion: 1` 及 `rewardState`；全局奖励状态仍保存于
`components.reward`。每个 worker 和组合叶子均通过工厂创建独立实例；在搜索前拒绝跨作用域共享的
可恢复奖励单例。仅真实调用的槽位推进局部奖励状态，顺序为 update、mutation、neighborhood，
然后按名称字典序遍历参数槽位。初始化和未使用槽位不产生虚假奖励观测。

读取时兼容无版本的旧 `rewardState` 和 Rust `reward`，拒绝别名冲突、未知版本和版本化状态缺失。
自定义载荷仍需双方约定共同 schema，组件及配置身份必须相同；不代表任意闭包或新增自适应组件自动互通。
无状态会话省略这些字段，继续支持 Kotlin `2eb7943` 读取；原始读取器无法恢复有状态内层奖励。
Rust 的 `save_kotlin_checkpoint` 面向原始读取器，使用本协议时应调用普通 `save_checkpoint`。
数值逐位一致性和异步调度重放属于独立契约，本协议不对此作保证。

## SHADE 数值契约

SHADE 与 L-SHADE 在算法参数中支持 `"numerics": "shade-fdlibm-v1"`。该模式固定柯西参数采样采用 fdlibm 正切（本端 `StrictMath.tan`，Rust 精确依赖 `libm=0.2.16`）、binary64 运算顺序及每对参数五次均匀抽样。省略或使用 `"native"` 保留历史实现；未知 ID 或不兼容的算法/契约组合会在预检阶段失败。直接构建 runner 时显式使用 `ShadeNumerics.FdlibmV1`。

模式纳入配置与策略指纹，更换模式后检查点和策略不兼容。原始 Kotlin `2eb7943` 读取器不支持该参数。本契约只覆盖该采样器，不承诺自定义目标、参数源、CMA-ES 或异步执行的逐位一致；JVM/Rust 位模式和完整状态比较与数值容差检查分别验收。

## CMA 数值契约

CMA-ES 支持 `"numerics": "cma-polar-fdlibm-v1"`。直接使用 runner 时，Rust 通过
`with_numerics` 选择 `CmaNumerics::PolarFdlibmV1`，Kotlin 选择 `CmaNumerics.PolarFdlibmV1`。
采用 Marsaglia 极坐标高斯采样：拒绝零半径和单位圆外的抽样对，每次尝试消耗两次均匀抽样，
只使用第一分量、不缓存第二分量；保留高斯分布，不依赖平台余弦。默认 native Box-Muller 不变。

共同模式固定原始 fdlibm 对数的运算分组（不是分组不同的 musl log），Rust `libm=0.2.16`
exp/pow/sqrt 对应 Kotlin `StrictMath`，以正零起始顺序累加，Jacobi 分解固定 60 轮、
停止及逐对阈值 `1e-12`，不重排特征值、按列输出特征向量。模式进入配置及策略指纹。
restartOnImport 使用全部导入候选的均值及样本协方差；单候选保留协方差，更优历史 best 不丢失。
多维、标量化及 SHADE 转 CMA 的 memetic 轨迹已进行完整状态及 binary64 位模式测试；
这不是任意回调算术、舍入环境或所有机器的通用认证。

## 异步调度重放

仅支持 `Asynchronous`、parallelism >= 2 且**无时间预算**的执行模式。首次派发前调用
`record_schedule()`（Rust）或 `recordSchedule()`（Kotlin）；终止后通过 `export_schedule()` /
`exportSchedule()` 导出版本 1 的 `ScheduleLog`。`FileSearchStore` 提供校验并原子写入的
`save_schedule` / `load_schedule`，Kotlin 对应 `saveSchedule` / `loadSchedule`。
新运行绑定 `replay_schedule(&log)` / `replaySchedule(log)` 后真实执行算法，等待日志指定的
worker，即使自然完成先后反转也不改变归并顺序；不是重放目标值替身或只重放学习更新。

`begin/dispatch/complete/drain/yield/end` 记录 advance 边界、按序可用动作、随机位模式、
预留及实际预算、停止/失败标志和静止排空屏障。校验规范 JSON SHA-256、配置/问题身份、
事件配对及总量。偏离立即报错，并对已派发任务恰好一次排空；偏离后禁止生成检查点。
损坏、截断和不兼容身份不会静默回退到自然完成顺序。

静止检查点的可选 `schedule` 保存已消费前缀及游标。常规恢复后，在推进前必须显式重新绑定
**完整源日志**继续重放，或显式继续录制；源日志须匹配该前缀，检查点身份及完成计数须与运行一致。
已覆盖停止及 worker 失败排空。原始 Kotlin 导出保护拒绝该新状态。自定义回调和组件仍须有
相同的确定性语义，依赖时钟的奖励及外部副作用不在契约内；普通未录制的异步模式仍然非确定。
