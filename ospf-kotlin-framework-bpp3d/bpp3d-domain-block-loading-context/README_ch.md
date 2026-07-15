# BPP3D 领域 — 块装载上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-block-loading-context` 是 BPP3D 的块装载领域上下文。提供块生成算法和搜索策略，用于从物品组合构建装箱候选。

## 核心组件

### 上下文与聚合

| 组件 | 描述 |
| --- | --- |
| `BlockLoadingContext` | 顶层块装载上下文，暴露候选生成能力。 |
| `Bpp3dBlockLoadingAsync` | 块装载操作的异步服务门面。 |

### 模型（`model/`）

| 组件 | 描述 |
| --- | --- |
| `Space` | 搜索算法用于递归空间分割的空间区域模型。 |

### 块生成器（`service/`）

| 组件 | 描述 |
| --- | --- |
| `SimpleBlockGenerator` | 从单项竖直堆叠生成简单块（仅支持 `Axis3.Y` 圆柱）。 |
| `ComplexBlockGenerator` | 通过组合多个物品生成异构堆叠的复杂块，含支撑覆盖检查。 |

### 搜索算法（`service/`）

| 组件 | 描述 |
| --- | --- |
| `DepthFirstSearchAlgorithm` | 基于深度优先搜索的空间分割算法，仅用于长方体装箱路径。 |
| `MultiLayerHeuristicSearchAlgorithm` | 多层启发式搜索，结合逐层堆叠与横向圆柱支撑验证。 |
| `CylinderUnsupportedGuard` | 在生成的候选块中拒绝无支撑横向圆柱的守卫。 |

## 依赖

- `bpp3d-infrastructure` — 几何原语、支撑覆盖
- `bpp3d-domain-item-context` — 物品、包装和层模型

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
