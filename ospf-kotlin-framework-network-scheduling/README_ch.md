# ospf-kotlin-framework-network-scheduling

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-framework-network-scheduling` 提供面向 VRP 及其他网络流问题族的通用网络基础设施，并包含完整的 VRPTW（带时间窗的车辆路径问题）分支定价求解器。涵盖不可变网络图、带容量与供需约束的通用流建模、ESPPRC 精确定价、Phase I/II 增量列生成，以及 best-bound 分支定价编排器。

## 模块结构

```
ospf-kotlin-framework-network-scheduling/
├── network-scheduling-infrastructure/          # 泛型图、弧、容量、供需、值适配器
├── network-scheduling-domain-vrp-context/      # VRPTW 实例、路线、校验器、成本策略
├── network-scheduling-domain-flow-context/     # 通用单商品/多商品网络流
├── network-scheduling-domain-route-generation-context/  # ESPPRC 定价器、定价图、标签支配
├── network-scheduling-domain-route-compilation-context/ # 列池、列生成生命周期、影子价格
└── network-scheduling-application/             # 分支定价算法、应用服务、求解结果
```

依赖图（箭头表示"依赖"）：

```
infrastructure
     ↑
     ├── domain-vrp-context ──→ gantt-scheduling-infrastructure
     ├── domain-flow-context ──→ framework / quantities
     ├── domain-route-generation-context ──→ domain-vrp-context
     └── domain-route-compilation-context ──→ domain-vrp-context
                                      ↑
                                 application
```

`domain-route-generation` 与 `domain-route-compilation` 互不依赖；`application` 负责将 compilation 提取的 `PricingDuals` 交给 generation。

## Public API

### Infrastructure

| 类型 | 用途 |
|------|------|
| `NetworkNodeId` | 网络节点的稳定字符串标识 |
| `NetworkNode<V>` | 带通用 `Quantity<V>` 属性的节点，不定义路由坐标语义 |
| `NetworkArc<V>` | 带成本与通用属性的有向弧，支持负成本 |
| `NetworkGraph<V>` | 具有稳定索引、邻接访问和谓词过滤能力的不可变有向图 |
| `CapacityBounds<V>` | 带单位校验的非负容量上下界 |
| `NodeBalance<V>` | 有符号节点供需量 |
| `NetworkCost<V>` | 可归一化的弧成本，支持负值 |
| `NetworkSchedulingSolverValueAdapter<V>` | 领域值与 solver `Flt64` 的转换边界 |
| `Flt64NetworkSchedulingSolverValueAdapter` | 内置 `Flt64` 适配器单例 |
| `FltXNetworkSchedulingSolverValueAdapter` | 内置 `FltX` 适配器单例 |

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
| `FlowCommodity<V>`、`FlowGraph<V>` | 商品供需、流聚合及 solver 变量注册 |
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
| 流容量 / 节点供需 | `Quantity<V>` | 流图 `flowUnit` |

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

- [daily.md](daily.md) — 分阶段实现计划与数学合同
- [ospf-kotlin-framework](../ospf-kotlin-framework/README_ch.md) — 共享求解器、pipeline 和远程求解抽象
- [ospf-kotlin-framework-gantt-scheduling](../ospf-kotlin-framework-gantt-scheduling/README_ch.md) — Gantt 调度框架（共享 `TimeWindow<V>` 基础设施）
