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
| `FlightTaskBunchGenerator` | 给定影子价格为单架飞机生成束。 |
| `InitialFlightTaskBunchGenerator` | 生成初始可行束集合。 |
| `FlightTaskFeasibilityJudger` | 检查任务序列是否可行。 |
| `AggregationInitializer` | 从飞机/任务数据初始化聚合。 |

## 依赖

- **task** — 提供 `FlightTask`、`Aircraft`、`FlightTaskBunch`、`ShadowPriceMap`。
- **rule** — 提供 `Lock`、`ConnectionTimeCalculator`、`MinimumDepartureTimeCalculator`、`CostCalculator`、`RuleChecker`。
