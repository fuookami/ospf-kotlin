# ospf-kotlin-framework-network-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-network-scheduling` 提供可独立运行的网络拓扑、供需流配置、资源路径、结构决策和显式场景韧性，并保留 VRPTW（带时间窗的车辆路径问题）分支定价入口。公共路径内核同时承接 Gantt 的网络子问题；全局时空编排仍由 Gantt 负责。

## 模块结构

```
ospf-kotlin-framework-network-scheduling/
├── network-scheduling-infrastructure/          # 泛型图、弧、容量、供需、值适配器
├── network-scheduling-domain-path-context/     # 有限域资源标签路径搜索
├── network-scheduling-domain-vrp-context/      # VRPTW 实例、路线、校验器、成本策略
├── network-scheduling-domain-flow-context/     # 通用单商品/多商品网络流
├── network-scheduling-domain-design-context/   # 设施、通道、容量等级与建设预算
├── network-scheduling-domain-resilience-context/ # 连通辅助流与显式故障场景
├── network-scheduling-domain-route-generation-context/  # ESPPRC 定价器、定价图、标签支配
├── network-scheduling-domain-route-compilation-context/ # 列池、列生成生命周期、影子价格
├── network-scheduling-domain-allocation-context/ # 请求分配、兼容、配额与枢纽
├── network-scheduling-domain-path-compilation-context/ # 整条路径二元选择及列生命周期
├── network-scheduling-domain-structure-context/ # 根树/Steiner 中转与设施归属
├── network-scheduling-domain-generalized-flow-context/ # 损耗/产率与单输入转换
└── network-scheduling-application/             # 分支定价算法、应用服务、求解结果
```

依赖图（箭头表示"依赖"）：

```
infrastructure
     ↑
     ├── domain-vrp-context ──→ gantt-scheduling-infrastructure
     ├── domain-flow-context ──→ framework / quantities
     ├── domain-path-context
     ├── domain-design-context ──→ domain-flow-context
     ├── domain-resilience-context ──→ domain-design-context
     ├── domain-route-generation-context ──→ domain-vrp-context / domain-path-context
     └── domain-route-compilation-context ──→ domain-vrp-context
                                      ↑
                                 application
```

`domain-route-generation` 与 `domain-route-compilation` 互不依赖；`application` 负责将 compilation 提取的 `PricingDuals` 交给 generation。

## 独立 Network 与 Gantt 子问题

Network 提供拓扑、流配置、资源路径、结构决策与显式场景韧性；Gantt 负责全局任务、执行器、工作日历、分支状态和排程。Network 新领域模块不引用 Gantt 任务或 application，既有 VRPTW 对 Gantt `TimeWindow` 的依赖暂时保留。

- `domain-path-context`：无求解器的有限资源状态路径搜索；VRPTW 与 Demo4 共用。
- `domain-flow-context`：不可变 `FlowProblem<V>`、每模型独立 `FlowContext<V>`/`FlowAggregation<V>`，以及守恒、容量和费用 pipeline。
- `domain-design-context`：`NetworkDesignContext<V>` 管理设施、通道、离散容量等级、预算与流量联动。
- `domain-resilience-context`：`NetworkResilienceContext<V>` 管理共享建设、独立场景流、辅助连通流与最坏运行成本。
- `application`：`NetworkFlowApplicationService`、`NetworkDesignApplicationService`、`NetworkResilienceApplicationService` 调用现有 `ColumnGenerationSolver`，不重复拼装领域模型。
- Gantt 的 `gantt-scheduling-domain-network-context` 仅依赖任务/束合同与 Network 路径/流组件；使用独立 `ospf-kotlin-starter-gantt-network`，普通 Gantt starter 不强制引入 Network。

### 流与建设合同

每个商品声明自身归一化单位、可用弧、差异费用及可选整数步长。整数流表示计量步长，不表示整单不可拆分。默认弧容量按一个商品单位的资源消耗计算；量纲不兼容时必须配置 `capacityUsage`。额外 `FlowCapacity` 行可表示跨弧共享运输资源或节点处理能力。默认硬供需；`FlowSlack` 显式声明弃供/缺货上界和单位罚成本。结果包含商品-弧流量、资源利用、供需满足及费用分解。

`NetworkService` 的 `CapacityLevel.capacity` 是建成后的总容量，不是增量；`FlowArc.capacity.upper` 是各等级必须遵守的有限容量包络。关闭通道无流量，故障降级上界与建成等级容量取较小值。设施可通过 `NetworkFacility.capacityRows` 管理共享处理容量，不使用无来源大 M。扩容可通过多个容量等级表达；固定现有通道设置 `required = true`。

### 显式韧性合同

连通要求支持可达、边不相交及内部节点不相交。连通辅助流只证明结构冗余，不证明实际需求容量。给定场景的节点/弧关闭、容量降级、显式供需覆盖和有界松弛分别注册；不自动枚举多故障组合，不自动免除故障节点需求。建设变量所有场景共用，各场景流量独立，默认硬满足所有场景并最小化建设加最坏运行费用。结果的保障只适用于给定场景集合，不能外推到未知故障；修复作业时间和人员仍归 Gantt。

### 独立与嵌入用法

```kotlin
// problem、solver、adapter 由业务及后端注入。
// Inject the business problem, backend and numeric adapter.
val result = NetworkFlowApplicationService(solver, adapter).solve(problem)

// 嵌入 Gantt 或其他模型只注册，不启动第二次求解。
// Embedded use registers only; it does not start a second solve.
val context = FlowContext(problem, adapter)
val registered = context.register(model)
```

独立入口返回 `NetworkSolveResult`：无 incumbent 的限时、不可行等正常终态保留 solver 报告且 `solution = null`；技术失败使用 `Ret`。领域物理量保持 `V`/`Quantity<V>`，仅注册和提取边界转换 `Flt64`。下游通过 context 的表达式、聚合和 pipeline 追加约束或目标，无需改 application 主流程。

无原生求解器的可运行算例见 `FlowContextTest`（运输/中转、混合单位、整数步长与软供需）、`NetworkDesignContextTest`（选址、平行服务、扩容、预算枚举）、`NetworkResilienceContextTest`（场景重路由、不相交与缺口）；独立入口状态见 `NetworkApplicationServiceTest`。真实求解使用显式 profile，跳过不等于通过。

迁移细节见 [迁移说明](MIGRATION_ch.md)。

## Public API

### Infrastructure

| 类型 | 用途 |
|------|------|
| `NetworkNodeId` | 网络节点的稳定字符串标识 |
| `NetworkArcId` | 用于区分平行弧的稳定字符串标识 |
| `NetworkNode<V>` | 带通用 `Quantity<V>` 属性的节点，不定义路由坐标语义 |
| `NetworkArc<V>` | 带成本与通用属性的有向弧，支持负成本 |
| `NetworkGraph<N, A>` | 基于通用载荷的不可变有向多重图，提供身份快照、邻接、过滤和反向视图 |
| `NetworkGraphAccess<N, A>` / `NetworkGraphBuilder<N, A>` | 身份/端点选择器及已校验图构建 |
| `NetworkTopology<N, A>` | 可达性、路径见证、DAG 排序、SCC 与目标相关最短路 |
| `CapacityBounds<V>` | 带单位校验的非负容量上下界 |
| `NodeBalance<V>` | 有符号节点供需量 |
| `NetworkCost<V>` | 可归一化的弧成本，支持负值 |
| `NetworkSchedulingSolverValueAdapter<V>` | 领域值与 solver `Flt64` 的转换边界 |
| `Flt64NetworkSchedulingSolverValueAdapter` | 内置 `Flt64` 适配器单例 |
| `FltXNetworkSchedulingSolverValueAdapter` | 内置 `FltX` 适配器单例 |

### 通用路径搜索

| 类型 | 用途 |
|------|------|
| `PathSearchContext` | 与 solver 无关的有限域资源标签搜索 |
| `PathProblem<N, A, S>` | 搜索定义，支持开始/转移/终止贡献、完整 trace 独立校验、过滤器和可选的领域认证支配 |
| `PathSearchBudget` | 标签总数/节点标签上限及协作式中断 |
| `PathSearchResult<S, A>` | 路径标签、trace 回溯、输出数量及真实完成状态 |

`NetworkGraph` 在构建时快照节点/弧身份与端点，后续修改载荷不会悄悄改写邻接关系。不同 `NetworkArcId` 的平行弧受支持；自环也是有效弧。`filtered(...)`、`filteredNodes(...)` 和 `reverse()` 会保留稳定身份。`NetworkTopology.shortestPath` 对非负成本使用 Dijkstra，对含负成本的图使用 Bellman-Ford；只有从起点可达且能到达目标的负环才会报告 `NegativeCycle`。

`PathSearchContext` 不假设资源单调，也没有隐式支配规则。开始/转移/终止回调可分别初始化、扩展或拒绝状态；额外的 `validate` 回调会独立检查完整标签 trace，而不会与增量转移混在一起。Elementary 搜索保留访问历史；非 elementary 的循环游走必须设置 `maximumSteps`，而 DAG 会按拓扑顺序传播标签且无需该上限。调用方提供的支配关系还会受 elementary 已访问节点集合和剩余步数边界约束；若发生未认证支配剪枝，或完整 trace 校验拒绝候选路径，结果会分别标记为未经认证的支配/校验状态，`complete` 不会宣称搜索穷尽可用路径。校验回调返回 `Ret` 错误时会原样传播。`maximumResults` 仅限制返回路径数，不会提前停止搜索；标签预算或中断会停止搜索并使 `complete` 为 false。

### VRP 领域

| 类型 | 用途 |
|------|------|
| `Customer<V>`、`Depot<V>`、`VehicleType<V>` | 持有 `Quantity<V>` 属性的核心领域实体 |
| `Route<V>`、`RouteStop<V>` | 含有序 stop、资源状态与成本的 elementary 路线 |
| `VrptwInstance<V>` | 完整校验的 VRPTW 实例（depot、客户、车辆类型、单位、容差） |
| `VrptwSolution<V>` | 含汇总距离与成本的已校验路线集合 |
| `PricingDuals` | 不可变定价对偶快照（客户覆盖 + 车队对偶） |
| `RouteValidator` | 独立的资源递推与成本复核校验器 |
| `BranchMask<K>`、`ResourceArc<K>` | 车辆资源分支约束 |
| `TravelTime`、`ServiceTimeWindow` | VRP 行驶时间与闭区间服务时间窗值对象 |

### 网络流领域

| 类型 | 用途 |
|------|------|
| `FlowNode<V>`、`FlowArc<V>` | 通用流图节点与带容量上下界的弧 |
| `FlowCommodity<V>`、`FlowProblem<V>`、`FlowContext<V>` | 商品供需、流聚合及 solver 变量注册 |
| `SupplyDemand<V>` | 节点 ID 到有符号供需量的绑定 |
| `FlowConservationConstraint` | 流出减流入等于节点供需 |
| `CapacityBoundConstraint` | 跨商品共享弧容量上下界 |
| `MinCostFlowObjective` | 流量乘弧成本求和，支持负成本 |

### Application

| 类型 | 用途 |
|------|------|
| `VrptwApplicationService<V>` | 顶层入口：校验输入、执行分支定价、应用 enricher |
| `BranchAndPriceAlgorithm<V>` | best-bound 分支定价编排器（队列式，非递归） |
| `VrptwSolveResult<V>` | 公共求解结果，含状态、解、上下界、gap 和 trace |
| `BranchAndPriceStatus` | `Optimal \| Feasible \| Infeasible \| TimeLimit \| NodeLimit \| SolverStopped` |
| `BranchDecision` | 密封类：`ForbidVehicleType`、`RequireVehicleType`、`ForbidArc`、`RequireArc` |


## Kotlin 交付状态与验证

本节仅记录 Kotlin 网络调度模块的范围，不代表全仓验收，也不报告原生求解器基准结果。

### 已覆盖的 Kotlin 测试

模块内测试覆盖：

- 网络基础设施与流模型注册
- 多重图身份、反向/过滤拓扑、负边最短路、DAG 穷举 oracle 对比及通用路径搜索 oracle 对比
- VRPTW 值校验、路线校验与定价辅助逻辑
- ESPPRC 标签扩展、资源可行性、禁止客户可达性、支配、分支掩码与列数限制
- 路线编译以及应用层分支定价编排

使用以下命令运行本模块的 Kotlin 测试：

```powershell
mvn -B -ntp -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C
```

## 扩展点

所有策略均通过 fun interface 注入，框架不硬编码单一策略：

| 扩展 | 接口 | 内置实现 |
|------|------|----------|
| 距离计算 | `DistanceCalculator<V>` | `EuclideanDistanceCalculator`、`SolomonDistancePolicy` |
| 行驶时间计算 | `TravelTimeCalculator<V>` | `DistanceAsTravelTimeCalculator` |
| 弧成本 | `ArcCostCalculator<V>` | `DistanceArcCostCalculator` |
| 路线成本聚合 | `RouteCostPolicy<V>` | `FixedPlusArcCostPolicy`、`Demo17CostPolicy` |
| 弧可行性 | `ArcFeasibilityPolicy<V>` | `DefaultArcFeasibilityPolicy` |
| ESPPRC 标签支配 | `LabelDominancePolicy` | `LabelDominancePolicy.Default` |
| 列选择 | `PricingColumnSelector<V>` | `PricingColumnSelector.default()` |
| 进度轨迹 | `BranchAndPriceTraceListener` | `BranchAndPriceTraceListener.None` |
| 求解后处理 | `SolutionEnricher<V>` | —（用户提供） |
| solver 值转换 | `NetworkSchedulingSolverValueAdapter<V>` | `Flt64…`、`FltX…` |
| 额外 CG 管线 | `CGPipeline<VrpShadowPriceArguments, …>` | —（用户提供） |
| 路线主问题扩展贡献 | `BranchAndPriceAlgorithm.Policy.extraContributionFactory` 创建 `RouteCompilationContribution<V>` | —（用户提供） |

`extraContributionFactory(instance, model)` 会为每个分支节点主问题模型调用一次，必须为该模型返回独立的新贡献实例。贡献对象参与主问题注册、路线列添加/移除、对偶刷新和结果提取。其 `RouteArcCoefficientProvider.coefficients(arc)` 提供按稳定扩展行 ID 索引的领域数值系数；弧系数会累加成 RMP 路线列系数，并使用同一套定义计算定价 reduced cost，因此系数键必须与 `refreshShadowPrices` 返回的行键一致。即使实际对偶为零，也必须显式返回该行；缺少当前对偶或系数无法转换到 solver 时定价返回错误，不静默忽略扩展行。

## 泛型数值边界

领域 API 基于 `V : RealNumber<V>` 泛型化。所有 `Quantity<V>` 属性（距离、需求、容量、供需、成本）保留调用方的数值类型。`Flt64` 仅出现在：

- solver adapter 注册与变量/约束提取
- 对偶值与 reduced cost 计算
- solver 边界的 `LinearMetaModel<Flt64>`
- flow-context 在 solver 边界的模型注册、约束和目标构造
- 测试代码

`NetworkSchedulingSolverValueAdapter<V>` 接口集中负责领域 `Quantity<V>` 与 solver `Flt64` 的转换。领域模型不直接引用 `Flt64`。

## 物理量边界

| 物理量 | 类型 | 单位来源 |
|--------|------|----------|
| 距离 | `Quantity<V>` | `VrptwUnits.distanceUnit` |
| 需求 / 容量 | `Quantity<V>` | `VrptwUnits.loadUnit` |
| 固定成本 / 弧成本 | `Quantity<V>` | `VrptwUnits.costUnit` |
| 行驶时间 | `Duration` | `TimeWindow<V>` 时间轴 |
| 服务时间 | `Duration` | 按客户 |
| ready / due time | `Instant` | `TimeWindow<V>` 时间轴 |
| 流容量 / 节点供需 | `Quantity<V>` | 每个商品与资源行各自声明单位；异量纲用量显式映射 |

裸数值仅用于无量纲权重、比例和归一化 solver 评分。无业务货币单位时使用 `NoneUnit` 归一化成本。

## 错误模式

所有可失败的公开操作返回 `Ret<T>`（`Ok | Failed | Fatal`），校验失败时不抛异常：

- **校验错误**（重复 ID、负值、单位不兼容、不可行时间窗）：`Failed(ErrorCode.IllegalArgument, 双语消息)`
- **算法正常终态**（`Infeasible`、`TimeLimit`、`NodeLimit`）：作为 `Ok(VrptwSolveResult)` 返回，携带诚实下界
- **合同违例**（整数性不变量失败、solver 调用失败、对偶不可靠）：作为 `Failed` 返回
- 错误消息格式：`"操作失败：原因 / Operation failed: reason"`

## 快速开始

```kotlin
// 创建实例、策略与求解器
val instance: VrptwInstance<Flt64> = ...
val service = VrptwApplicationService(
    instance = instance,
    solver = GurobiColumnGenerationSolver(),
    configuration = BranchAndPriceAlgorithm.Configuration(
        timeLimit = 300.seconds,
        nodeLimit = 100,
        relativeGapTolerance = Flt64(1e-4)
    ),
    policy = BranchAndPriceAlgorithm.Policy(
        valueAdapter = Flt64NetworkSchedulingSolverValueAdapter,
        distanceCalculator = EuclideanDistanceCalculator(Meter),
        travelTimeCalculator = DistanceAsTravelTimeCalculator(...),
        arcCostCalculator = DistanceArcCostCalculator(Meter, NoneUnit),
        routeCostPolicy = Demo17CostPolicy(NoneUnit)
    )
)

val result: Ret<VrptwSolveResult<Flt64>> = service.solve()
when (result) {
    is Ok -> {
        val solveResult = result.value
        println("状态: ${solveResult.status}")
        println("路线数: ${solveResult.solution?.routes?.size}")
        println("总成本: ${solveResult.solution?.totalCost}")
    }
    is Failed -> println("求解失败: ${result.error}")
    is Fatal -> println("致命错误: ${result.errors}")
}
```

## 本地验证

```powershell
mvn -B -ntp -f ospf-kotlin-framework-network-scheduling/pom.xml test -T 0.75C
mvn -B -ntp -pl ospf-kotlin-example -am -Pdemo5-gurobi-bp test -T 0.75C
mvn -B -ntp -pl ospf-kotlin-starters/ospf-kotlin-starter-network-scheduling -am -DskipTests package -T 0.75C
```

## 相关

- [迁移说明](MIGRATION_ch.md) — 公共接口迁移与证明边界
- [ospf-kotlin-framework](../ospf-kotlin-framework/README_ch.md) — 共享求解器、pipeline 和远程求解抽象
- [ospf-kotlin-framework-gantt-scheduling](../ospf-kotlin-framework-gantt-scheduling/README_ch.md) — Gantt 调度框架（共享 `TimeWindow<V>` 基础设施）

## 业务构造与通用路径选择

新增四个独立领域模块，扩展 Network，但不建设第二套全局调度框架：

| 模块 | 公共入口 | 业务场景 |
|---|---|---|
| [allocation-context](network-scheduling-domain-allocation-context/README_ch.md) | `AllocationProblem<V>`、`AllocationContext.create` | 兼容运输分配、共享配额、分层中转及可选枢纽，复用 Flow/Design |
| [path-compilation-context](network-scheduling-domain-path-compilation-context/README_ch.md) | `PathCompilationProblem<V, C>`、`PathCompilationContext<V, C>` | 整条路径二元选择、不可拆订单、共享资源、有序服务链、多式服务及弧覆盖 |
| [structure-context](network-scheduling-domain-structure-context/README_ch.md) | `NetworkStructureProblem<V>`、`NetworkStructureContext<V>` | 根有向树、可选 Steiner 中转、设施资格覆盖/归属与数量配额 |
| [generalized-flow-context](network-scheduling-domain-generalized-flow-context/README_ch.md) | `GeneralizedFlowProblem<V>`、`GeneralizedFlowContext<V>` | 投入与到达产出、运输损耗、产率、回收和显式单输入转换 |

新增依赖为 `allocation -> design -> flow`、`structure -> design`、`path-compilation -> path -> infrastructure`、`generalized-flow -> infrastructure`。这些模块共享 framework/modeling 基础，不引用 Gantt 任务或应用算法。Network starter 暴露新能力；嵌入业务可只依赖需要的 domain。Gantt 桥接仍是独立可选依赖。

独立入口分别为 `NetworkAllocationApplicationService`、`NetworkPathApplicationService`、`NetworkStructureApplicationService`、`NetworkGeneralizedFlowApplicationService`，每次创建独立 context，与嵌入式用法走相同注册路径：

```kotlin
val result = NetworkPathApplicationService(solver, adapter).solve(pathProblem)
val embedded = PathCompilationContext(pathProblem, adapter)
val registered = embedded.register(model)
```

### 整条路径与定价合同

每个 `PathColumn<V, C>` 对应一个完整不可拆选择及一个二元变量；payload 保留内部活动、服务顺序和业务时间。`PathCompilationRow<V>` 用显式单位表达精确覆盖、上下界、资源及配额。缺省系数为零，未知行键失败。分配中的整数流计量步长仍可在不同服务间拆分，整单不可拆必须用整条路径二元选择。

成本和行系数按**完整路径**计算，支持开始/结束及非可加贡献，不强行假设可逐弧分解，也不自动认证支配。注册、增列、删列、目标清理、单位归一化和 reduced cost 使用同一个列定义。稳定 ID 必须对应相同载荷和规范化经济含义；冲突重用返回失败。删除后重新加入相同 ID 会创建新的模型变量。

对偶快照绑定当前 context 修订及调用方提供的定价范围版本（含有效图、规则、分支和固定状态）。缺失行对偶、外部/过期快照、模型结构或变量边界改变、范围不符都返回失败。调用方必须传本轮 LP 求解的对偶；原始 `MetaDualSolution` 自身没有求解来源信息。参与定价的所有约束应声明为问题行，不能将未映射的外部约束静默按零价格处理。完整路径 reduced cost 等于总费用减全部声明行的有符号对偶贡献，但这个数值本身不是最优性证书。

MILP 的最优状态仅证明最终 `candidateIds` 候选集合内最优。`PathSearchResult.complete`、共享 `ColumnGenerationBatch` 的证据及二元主问题最优状态是不同合同；生成截断不能证明不存在改进列，也没有新增精确分支定价树。跨独立活动的时间容量协调继续由 Gantt 处理。

### 结构与转换边界

根树采用显式唯一父弧和有界深度无环约束，而不是依赖低成本“自然成树”；排除自环，平行服务仍以稳定弧 ID 区分。可选设施控制归属和树节点启用；覆盖归属不等于流量需求，也不会凭空产生配送。建设费用使用明确的服务容量等级/设施费用，不隐式套用商品运行流费用。

广义流按节点单位表达“流出投入减流入到达产出等于供需”。业务产率/损耗与单位换算分开；跨量纲转换必须声明每份投入对应的产出物理量，不能靠无单位比例伪装量纲转换。每条弧都有有限投入容量，增益循环也不例外。只支持单输入转换，不把多输入 BOM/MRP、加工同步或修复排程混入本模块。

可执行无原生算例见 `AllocationContextTest`、`PathCompilationContextTest`（不可拆订单、有序多式服务、稳定弧覆盖）、`NetworkStructureContextTest`、`GeneralizedFlowContextTest`。这些测试以手算见证和小域枚举作独立对照。真实 SCIP 测试通过 `-Pscip-integration-tests` 显式启用，须核对实际执行/跳过数量，不能只看 BUILD SUCCESS；测试证据和日志不属于仓库交付源码。
