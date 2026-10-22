# 网络调度分配上下文

[English](README.md)

本模块在既有网络 Flow 与 Network Design 上提供运输/分配业务输入层。模块将地点、有向服务、运输请求、共享配额、可选枢纽及容量等级转换为底层不可变问题快照。

## 公共 API

- `AllocationProblem<V>` 是不可变输入快照。`V` 保持为 `RealNumber<V>`；业务数量、容量、费用和配额均使用 `Quantity<V>`。
- `AllocationLocation<V>` 表达稳定网络地点和可选整数层级。服务两端都声明层级时，只允许从较低或相同层级流向较高或相同层级；递增层级的路线仍可通过中间地点中转。
- `AllocationService<V>` 定义每请求单位运行费用和容量上下界。可选的 `AllocationCapacityLevel<V>` 映射到建设决策；等级容量表示建成后的总容量，而非增量。无等级的服务固定启用；非必选且有等级的服务可以关闭。
- `AllocationRequest<V>` 定义起点、终点、正数量、可选兼容服务、各服务单位费用覆盖和整数流步长。每个请求都会转换为独立流商品。
- `AllocationQuota<V>` 定义跨请求-服务流共享的资源行。请求单位与服务容量量纲不同时，可用 `AllocationProblem.serviceUsage` 覆盖默认的单位服务容量消耗。
- `AllocationHub<V>` 映射为可选设施。`quotaIds` 指定由枢纽启用状态控制的共享行；与设施相连的服务流也受设施启停控制。
- `AllocationContext.create(...)` 校验输入并创建独立 `NetworkDesignContext`。`register(...)` 注册到 `MetaModel`，`extractSolution(...)` 提取请求运输量、资源使用、运行/建设费用、已选服务等级和已开放枢纽。

## 费用与数量语义

服务费用和请求费用覆盖均表示每一个请求单位的运行费用。请求覆盖优先于对应服务费用。运行费用和建设费用会在注册/提取边界由网络调度数值适配器统一换算到 `AllocationProblem.costUnit`。

容量与共享配额上下界均为物理量。默认弧容量消耗假设一个请求单位消耗一个服务容量单位；若两者量纲不同，应提供以“每请求单位消耗的容量单位”表示的正 `serviceUsage` 系数。配额消耗系数同样使用“每请求单位消耗的配额单位”。不兼容物理量会通过 `Ret` 报错，不会以无单位裸值混算。

整数步长表示每个请求-服务流量是该步长的整数倍，**不**表示整张请求不可拆分。只要守恒、容量及配额约束允许，同一个请求可以拆分到多个兼容服务并经过中间地点。

## 示例

```kotlin
val problem = AllocationProblem(
    locations = listOf(
        AllocationLocation(NetworkNodeId("warehouse"), layer = 0),
        AllocationLocation(NetworkNodeId("hub"), layer = 1),
        AllocationLocation(NetworkNodeId("store"), layer = 2)
    ),
    services = listOf(
        AllocationService(
            id = NetworkArcId("warehouse-hub"),
            from = NetworkNodeId("warehouse"),
            to = NetworkNodeId("hub"),
            cost = Quantity(Flt64(1.0), NoneUnit),
            capacity = capacity
        ),
        AllocationService(
            id = NetworkArcId("hub-store"),
            from = NetworkNodeId("hub"),
            to = NetworkNodeId("store"),
            cost = Quantity(Flt64(1.0), NoneUnit),
            capacity = capacity
        )
    ),
    requests = listOf(
        AllocationRequest(
            id = "order-42",
            origin = NetworkNodeId("warehouse"),
            destination = NetworkNodeId("store"),
            quantity = Quantity(Flt64(12.0), NoneUnit),
            integerStep = Quantity(Flt64(1.0), NoneUnit)
        )
    ),
    zero = Flt64.zero
)
val context = AllocationContext.create(problem, Flt64NetworkSchedulingSolverValueAdapter).value
```

每次模型注册都应创建独立上下文；不要将一个上下文复用于多个模型。模块复用 Flow 与 Network Design 内核，不依赖 Gantt 调度或原生求解器。
