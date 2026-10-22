# Network 路径上下文

[English](README.md)

在 `NetworkGraph<N, A>` 上提供不依赖求解器的资源约束路径生成。
VRPTW 定价与 Demo4 航空任务束生成共用该搜索内核。

## 合同

- 每次请求构造独立的 `PathProblem<N, A, S>`。业务状态 `S` 承载时间、
  载荷、访问顺序、恢复及其他资源；内核不假设资源单调。
- 注入起始、转移、终止贡献、局部可行性、完整路径独立校验，以及可选的
  结果排序和筛选策略。
- Elementary 路径以有限节点访问域保证终止；允许重复的循环游走必须声明
  `maximumSteps`。DAG 无需额外步数上限。
- 默认不使用支配。只有业务确认支配保留后续可行性、成本及主问题系数后，
  才能设置 `dominanceIsExact`；内核还会检查访问集合与剩余步数条件。
- `PathSearchBudget` 约束计算量，不是业务可行性条件。中断或裁剪标签不能
  产生完整搜索证明。
- `maximumResults` 仅限制返回数量，搜索仍继续；空输出不证明不可行，
  也不证明不存在改进列。

`PathSearchResult` 提供完成状态、标签诊断及 `trace(path)` 回溯。
应分别读取完成度与 `paths`。无效请求、回调失败返回 `Ret` 失败；预算耗尽
是正常结果。状态和回调不得跨请求复用可变的定价数据。

## 集成

```kotlin
val result = PathSearchContext().search(
    problem = problem,
    budget = PathSearchBudget(maximumLabels = 100_000),
    maximumResults = 20
)
```

内核不限定数值类型或单位；在 `S` 中保留领域物理量，仅在定价适配边界转换
reduced cost。Gantt 任务束转换使用可选的
`gantt-scheduling-domain-network-context`，本模块不反向依赖 Gantt 或应用求解器。

`PathSearchContextTest` 覆盖小图穷举对照、非单调资源、访问历史、终止贡献、
独立校验与真实预算状态。
