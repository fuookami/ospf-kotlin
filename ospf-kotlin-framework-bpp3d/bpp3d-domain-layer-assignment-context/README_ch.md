# BPP3D 领域 — 层分配上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-layer-assignment-context` 是 BPP3D 的层分配领域上下文。实现了将物品分配到箱位的列生成模型，包括变量注册、约束管线、影子价格提取和求解提取。

## 核心组件

### 上下文与聚合

| 组件 | 描述 |
| --- | --- |
| `LayerAssignmentContext` | 层分配顶层上下文，管理模型注册和列生命周期。 |
| `ImpreciseAggregation` | 松弛 LP 列生成阶段的不精确聚合。 |
| `PreciseAggregation` | 最终 MILP 求解的精确聚合。 |
| `Bpp3dLayerAssignmentServiceAsync` | 层分配操作的异步服务门面。 |

### 模型（`model/`）

| 组件 | 描述 |
| --- | --- |
| `Assignment` | 将物品分配到箱位的决策变量。 |
| `Capacity` | 箱位容量约束（重量、体积、深度）。 |
| `Load` | 装载模型，表示箱位的重量/体积利用率。 |
| `LayerAggregation` | 箱位内层的聚合，用于模型注册。 |
| `Bpp3dSolverValueAdapter` | 求解器值（FltX）与领域物理量之间的转换适配器。 |
| `ScaledBpp3dSolverValueAdapter` | 求解器值适配器的缩放变体。 |
| `LayerAssignmentAliases` | 层分配领域类型的类型别名。 |

### 模型组件与管线

| 组件 | 描述 |
| --- | --- |
| `LayerAssignmentModelComponent` | 核心模型组件，注册分配变量、容量约束和目标。 |
| `LayerAssignmentExtraContext` | 扩展点，用于向层分配模型添加自定义变量、约束和目标。 |
| `LayerAssignmentReflux` | 列管理（添加/移除列）的回流机制。 |
| `LayerAssignmentShadowPricePipeline` | 从 LP 松弛中提取影子价格的管线。 |
| `LayerAssignmentSolutionExtractor` | 从求解器结果中提取解（分配决策、KPI）。 |
| `SolutionAnalyzer` | 分析求解器解的可行性和质量指标。 |

### 约束管线（`service/limits/`）

| 组件 | 描述 |
| --- | --- |
| `BetterLayerMaximization` | 目标管线，最大化层质量（装载率、支撑）。 |
| `BinAmountMinimization` | 目标管线，最小化总箱位数。 |
| `BinCapacityConstraint` | 箱位容量限制的约束管线。 |
| `BinDepthConstraint` | 箱位深度限制的约束管线。 |
| `BinLoadingOrderConstraint` | 装载顺序优先级的约束管线。 |
| `TailBinAssignmentConstraint` | 尾箱分配限制的约束管线。 |
| `TailBinLoadingRateMinimization` | 最小化尾箱装载率的目标管线。 |
| `VolumeMinimization` | 最小化总体积的目标管线。 |
| `ItemDemandConstraint` | 确保需求满足的约束管线。 |
| `LoadingConstraint` / `LoadingUpperBoundConstraint` | 装载限制的约束管线。 |
| `MaterialUsageConstraint` | 物料使用约束的约束管线。 |
| `PackagingConstraint` | 包装规则的约束管线。 |
| `SupportAreaConstraint` | 最小支撑面积的约束管线。 |
| `WeightBalanceConstraint` / `WeightConstraint` | 重量限制的约束管线。 |

## 依赖

- `bpp3d-infrastructure` — 几何原语、影子价格映射、求解器值适配器
- `bpp3d-domain-item-context` — 物品、物料、需求模型
- `ospf-kotlin-core` — MetaModel、变量、约束、列生成

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
