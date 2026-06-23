# 乘客（Passenger）

:us: [English](README.md) | :cn: 简体中文

负责航班恢复调度系统中乘客域操作的限界上下文。建模具有多航段行程的乘客，跟踪取消、舱位变更和航班变更，并将乘客相关约束和目标注册到列生成模型中。

## 职责

- 建模具有数量、路线和每航段舱位分配的乘客。
- 将乘客取消作为决策变量跟踪。
- 为中断乘客建模舱位变更和航班变更变量。
- 计算每航班每舱位的乘客数量表达式。
- 注册取消最小化、舱位变更最小化、航班变更最小化、路线取消约束和航班容量约束。
- 为乘客约束生成列生成管线。

## 核心类

| 类名 | 说明 |
|---|---|
| `PassengerContext` | 管理聚合和管线注册的入口上下文。 |
| `Aggregation` | 组合 `PassengerCancel`、`PassengerChange` 和 `PassengerAmount`。 |
| `Passenger` | 具有数量和多航段航班列表的乘客。 |
| `FlightPassenger` | 将乘客链接到特定航班（含可选前一航段）。 |
| `PassengerCancel` | 跟踪取消决策变量。 |
| `PassengerChange` | 跟踪舱位变更和航班变更决策变量。 |
| `PassengerAmount` | 计算每航班每舱位的乘客数量表达式。 |
| `PipelineListGenerator` | 为乘客约束生成列生成管线。 |

## 依赖

- **task** — 提供 `FlightTask`、`FlightTaskBunch`、`PassengerClass`。
- **bunch_compilation** — 提供 `FlightCapacity`、`TaskTime`。
