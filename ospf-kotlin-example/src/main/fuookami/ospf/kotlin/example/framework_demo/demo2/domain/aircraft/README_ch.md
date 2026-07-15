# 飞机 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理飞机配置数据，包括型号分类、机身参数、燃油常数、甲板布局、货物位置和邻接关系。

### 依赖上下文

无上游依赖。

## 目录结构

```
aircraft/
├── model/          # 领域模型（实体、值对象）
│   ├── AircraftModel.kt    # 飞机型号与类型枚举
│   ├── Deck.kt              # 甲板与舱门
│   ├── Position.kt          # 货物位置坐标与形状
│   ├── Fuselage.kt          # 机身属性
│   ├── Fuel.kt              # 燃油常数
│   ├── Formula.kt           # 计算公式
│   ├── ULD.kt               # 集装器
│   ├── HatchDoor.kt         # 舱门
│   ├── Neighbour.kt         # 邻接关系
│   ├── FlightPhase.kt       # 飞行阶段
│   └── LoadingOrder.kt      # 装载顺序
├── service/        # 领域服务
│   ├── AggregationInitializer.kt  # 聚合初始化
│   ├── NeighbourCalculator.kt     # 邻接关系计算
│   └── LoadingOrderOutputExporter.kt  # 装载顺序导出
├── Aggregation.kt  # 聚合根
└── AircraftContext.kt  # 上下文入口
```

## 核心概念

- **飞机型号**（Aircraft Model）：飞机类型分类（B737/B757/B767/B747）及物理单位定义。
- **机身**（Fuselage）：飞机机身属性，包括操作空重（DOW）、平衡臂、操作指数和救生筏。
- **甲板**（Deck）：飞机上的物理甲板，包含舱门、货物位置和舱门邻近映射。
- **货物位置**（Position）：货物位置，包含坐标（纵向/横向臂）、形状、位置标签和装载顺序。
- **燃油**（Fuel）：各飞行阶段（起飞、着陆、零燃油）的燃油常数，含重量和指数。
- **集装器**（ULD）：集装器（Unit Load Device），含代码和尺寸。
- **邻接关系**（Neighbour）：位置间的邻接关系，用于约束生成。
- **装载顺序**（Loading Order）：定义各位置的装载顺序。

## 约束列表

本上下文不定义约束，仅提供基础配置数据供其他上下文使用。

## 目标函数

本上下文不定义目标函数。

## 与其他上下文的关系

**下游**：装载分配、MAC、适航安全、快递效能、装载效能、MAC优化、载荷最大化、推荐重量均衡、冗余、软安全
