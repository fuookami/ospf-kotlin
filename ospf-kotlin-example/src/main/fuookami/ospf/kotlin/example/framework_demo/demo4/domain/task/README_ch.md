# 航班任务（Task）

:us: [English](README.md) | :cn: 简体中文

负责在航班恢复调度系统中定义核心航班任务领域模型的限界上下文。提供所有其他领域依赖的基础实体——机场、飞机、航段、维修、AOG（飞机停场）、转场和航班任务束。

## 职责

- 定义具有 ICAO 代码、类型和中转时间的机场实体。
- 建模具有注册号、子机型、容量和可用性跟踪的飞机。
- 定义航班任务层次结构：`FlightTask`（抽象），具体类型为 `FlightLeg`、`Maintenance`、`AOG`、`Transfer`。
- 建模具有计划/估计/实际时间和恢复策略的航班任务计划。
- 定义 `FlightTaskBunch` —— 分配给单架飞机的有序任务序列。
- 为列生成提供影子价格类型。
- 跟踪飞行小时、飞行周期和飞机变更检测。

## 核心类

| 类名 | 说明 |
|---|---|
| `FlightTaskContext` | 航班任务域操作的入口上下文。 |
| `Aggregation` | 组合机场、飞机、航段、维修、AOG、转场和原始束。 |
| `Airport` | 通过 ICAO 代码标识的机场，具有类型和中转时间。 |
| `Aircraft` | 通过注册号标识的飞机，具有子机型和容量。 |
| `AircraftUsability` | 跟踪飞机位置、启用时间和飞行周期。 |
| `FlightTask` | 所有航班任务的抽象基类，具有计划、恢复和延迟跟踪。 |
| `FlightLeg` | 具有可选恢复飞机/时间的具体航段。 |
| `FlightLegPlan` | 具有计划/估计/实际时间的航段计划。 |
| `FlightTaskBunch` | 分配给单架飞机的有序任务序列。 |
| `FlightTaskAssignment` | 指定可选飞机、时间和航线变更的恢复策略。 |
| `ShadowPriceMap` | 用于列生成定价的影子价格映射。 |

## 依赖

- **infrastructure** — 提供 `ICAO`、`AircraftRegisterNumber`、`AircraftMinorType`、`AircraftCapacity`、`PassengerClass`。
- **framework (gantt_scheduling)** — 提供基础 `Executor`、`AbstractTask`、`AbstractTaskBunch`、`TimeRange`、`TimeWindow`。
