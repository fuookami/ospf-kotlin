# 批次生成（Bunch Generation）

:us: [English](README.md) | :cn: 简体中文

负责为每架飞机生成可行航班任务束的限界上下文。为每架飞机构建路线图，生成初始束，并在列生成迭代中利用影子价格驱动的定价发现具有负缩减成本的新束。

## 职责

- 为每架飞机构建有向路线图，编码可行的任务序列。
- 从路线图生成初始航班任务束。
- 利用主问题的影子价格在每次迭代中生成新束。
- 评估任务序列的可行性（连接时间、规则、飞机可用性）。
- 管理可反转航班任务对以支持顺序变更操作。

## 核心类

| 类名 | 说明 |
|---|---|
| `BunchGenerationContext` | 管理聚合、可行性判定器和每飞机生成器的入口上下文。 |
| `Aggregation` | 持有每飞机路线图、`FlightTaskReverse` 映射和初始束。 |
| `Graph` | 表示可行任务转移的 `Node` 和 `Edge` 有向图。 |
| `Node` | 密封类：`RootNode`、`EndNode`、`TaskNode`。 |
| `FlightTaskReverse` | 管理顺序变更操作的可反转任务对。 |
| `FlightTaskBunchGenerator` | 使用共享 Network 路径搜索内核和 Demo4 状态转移为单架飞机生成束。 |
| `InitialFlightTaskBunchGenerator` | 生成初始可行束集合。 |
| `FlightTaskFeasibilityJudger` | 检查任务序列是否可行。 |
| `AggregationInitializer` | 从飞机/任务数据初始化聚合。 |

## 依赖

- **task** — 提供 `FlightTask`、`Aircraft`、`FlightTaskBunch`、`ShadowPriceMap`。
- **rule** — 提供 `Lock`、`ConnectionTimeCalculator`、`MinimumDepartureTimeCalculator`、`CostCalculator`、`RuleChecker`。
- **Gantt network bridge** — 将 Network 路径搜索结果转换为带作用域的 `ColumnGenerationBatch`。

## 搜索与完成状态

`Graph` 仍是航空领域图构建器，负责发布具有稳定标识的 Network 拓扑快照。通用路径遍历委托给 `PathSearchContext`；转移回调仍执行 Demo4 的航班恢复、时刻、成本和影子价格规则，提取阶段校验总成本并构造 `FlightTaskBunch`。

`ColumnGenerationBatch` 会报告 `Exhausted`、`BudgetLimit`、`Interrupted` 或 `Heuristic`，并携带请求/模型/对偶/分支/执行器时隙作用域。穷尽只表示作用域完成；可选的 `noImprovingColumns` 必须来自独立定价证据，不能由空输出推导。每节点标签上限和完整路径校验拒绝会阻止穷尽证明；返回列上限只限制输出数量，不截断搜索。无效总代价终点会阻止无改进证明。旧支配比较不再使用：它只比较缩减成本、延迟和飞机变更，却忽略到达时刻和已访问路径状态，可能错误抑制不同的可行航线。候选列不能证明最小缩减成本或整数最优性。

路线图 BFS 使用 FIFO 双端队列，将队列操作改为摊销常数时间；候选回调、备选到达机场入队顺序、节点复用及诊断计数保持原遍历语义。
