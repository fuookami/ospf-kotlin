# BPP3D 领域 — 物品上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-item-context` 是 BPP3D 的物品领域上下文，定义了物品、包装、物料、箱位、层和装箱方案等核心领域模型，以及聚合和列生成模型组件。

## 核心组件

### 模型（`model/`）

| 组件 | 描述 |
| --- | --- |
| `Item` | 物品模型，含需求、形状规格和方向约束。 |
| `Package` | 包装模型，含形状、重量和属性。 |
| `Material` | 物料模型，含编号、类型、重量和尺寸约束。 |
| `Bin` | 箱位模型，表示具有容量和约束的装箱容器。 |
| `Layer` | 层模型，用于箱内的垂直分层。 |
| `Block` | 块模型，用于块装载候选生成。 |
| `Pattern` | 装箱方案，表示箱位的解决方案计划。 |
| `Schema` | 装箱规格，描述物品-包装配置规则。 |
| `ItemContainer` | 物品容器，管理具有形状和需求统计的物品集合。 |
| `DemandStatistics` / `QuantityDemandStatistics` | 需求统计（总量、重量、体积）。 |
| `DemandReducedCost` / `QuantityDemandReducedCost` | 需求缩减成本模型，用于列生成定价。 |
| `ShadowPriceMap` | 影子价格映射，按物品/物料查找对偶值。 |
| `PlacementFactory` | 从物品和方向创建放置实例的工厂。 |
| `PlacementPlaneMapping` | 3D 放置与 2D 射影平面投影之间的映射。 |
| `PlacementTyping` | 放置类型变体的类型别名。 |
| `CylinderShapeContract` | 圆柱形状契约，用于轴向能力检查。 |
| `PackageAttribute` | 包装属性（易碎性、可堆叠性等）。 |
| `ContinuousRadiusModelComponent` | 连续半径圆柱变量注册、PWL 近似和结果提取的模型组件。 |
| `ContinuousRadiusSelectionExtractor` | 从求解器解中提取连续半径选择结果的提取器。 |
| `QuantityDomainModels` | 物理量类型领域模型别名和适配器。 |

### 服务（`service/`）

| 组件 | 描述 |
| --- | --- |
| `Bpp3dItemServiceAsync` | 物品领域操作的异步服务。 |
| `ItemMerger` | 合并具有相同形状和方向的物品为分组需求。 |
| `ItemHeightCombinator` | 按高度组合物品用于层生成。 |
| `LoadingOrderCalculator` | 计算物品在箱位中的装载顺序。 |

### 聚合

| 组件 | 描述 |
| --- | --- |
| `Aggregation` | 物品聚合，组合多个模型组件用于模型注册。 |
| `ItemContext` | 顶层上下文，向应用层暴露物品领域能力。 |

## 依赖

- `bpp3d-infrastructure` — 几何原语、类型
- `ospf-kotlin-core` — 优化模型、变量、约束
- `ospf-kotlin-quantities` — 物理量类型

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
