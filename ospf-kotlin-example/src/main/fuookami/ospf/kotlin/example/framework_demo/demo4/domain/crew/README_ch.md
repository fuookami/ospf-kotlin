# 机组（Crew）

:us: [English](README.md) | :cn: 简体中文

负责航班恢复调度系统中机组域操作的限界上下文。建模机组成员（飞行员和非飞行员）、其排班以及连续航班间的中转时间要求。

## 职责

- 建模具有身份、职级和类型信息的机组成员。
- 跟踪将航班任务映射到分配职级的机组排班。
- 定义中转时间场景（同一飞机、国内转机、国际转机）。
- 为批次生成中的可行性检查和成本计算提供机组数据。

## 核心类

| 类名 | 说明 |
|---|---|
| `CrewContext` | 机组域操作的入口上下文。 |
| `Aggregation` | 持有 `Crew` 列表、`CrewSchedule` 列表和 `TransitTimeMap`。 |
| `Crew` | 分配给航班任务的机组，由飞行员和非飞行员成员组成。 |
| `CrewMember` | 密封接口：`CrewPilotMember` 或 `CrewNotPilotMember`。 |
| `CrewSchedule` | 将航班任务映射到机组成员的分配职级。 |
| `TransitTime` | 将中转时间场景与其所需时长关联。 |
| `TransitTimeScene` | 枚举：`SameAircraft`、`DomainNotSameAircraft`、`InternationNotSameAircraft`。 |

## 依赖

- **task** — 提供 `FlightTask`、`Aircraft`、`Airport`、`AirportType`。
- **infrastructure** — 提供 `WorkerNo`、`Pilot`、`CrewMan`、`PilotRank`、`CrewManRank`。
