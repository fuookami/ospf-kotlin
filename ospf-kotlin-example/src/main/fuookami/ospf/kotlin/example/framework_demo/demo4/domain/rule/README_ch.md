# 规则（Rule）

:us: [English](README.md) | :cn: 简体中文

负责在航班恢复调度系统中定义和评估运营规则、限制和流量控制的限界上下文。提供其他领域（尤其是 bunch_generation）用于判断恢复计划可行性和成本的规则检查基础设施。

## 职责

- 定义连续航班任务间的航班链接（连接、经停、忽略连接时间）。
- 建模阻止特定任务修改的锁定。
- 定义机场的流量控制规则（出发/到达容量限制）。
- 定义具有严重性级别（弱、可违反强、强）的限制（基于关系和通用）。
- 计算任务序列的成本、连接时间、最早出发时间和可行性。

## 核心类

| 类名 | 说明 |
|---|---|
| `RuleContext` | 规则域操作的入口上下文。 |
| `Aggregation` | 规则域对象聚合。 |
| `Link` | 密封类：`ConnectingLink`、`StopoverLink`、`ConnectionTimeIgnoringLink`。 |
| `LinkMap` | 按前驱/后继任务提供所有链接类型的查找。 |
| `Lock` | 阻止航班任务修改。 |
| `FlowControl` | 给定场景和时间范围的机场容量限制规则。 |
| `Flow` | 表示机场一组流量控制规则的流量资源。 |
| `FlowControlScene` | 枚举：`Departure`、`Arrival`、`DepartureArrival`、`Stay`。 |
| `Restriction` | 密封接口：`RelationRestriction`、`GeneralRestriction`。 |
| `RestrictionType` | 枚举：`Weak`、`ViolableStrong`、`Strong`。 |
| `CostCalculator` | 计算任务序列的成本。 |
| `ConnectionTimeCalculator` | 计算任务间的连接时间。 |
| `MinimumDepartureTimeCalculator` | 计算最早出发时间。 |
| `FeasibilityJudger` | 评估任务序列的可行性。 |

## 依赖

- **task** — 提供 `FlightTask`、`FlightTaskBunch`、`Aircraft`、`Airport`、`FlightType`。
- **framework (gantt_scheduling)** — 提供 `ConnectionResource`、`AbstractResourceCapacity`。
