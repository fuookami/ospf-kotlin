# BPP3D 领域 — 层生成上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-layer-generation-context` 是 BPP3D 的层生成领域上下文。为列生成模型生成候选箱层（列），包括 circle-packing、回退和堆叠策略。

## 核心组件

| 组件 | 描述 |
| --- | --- |
| `LayerGenerationContext` | 层生成顶层上下文，管理候选生成策略和适配器注册。 |
| `LayerGenerationProgramCandidateAdapters` | 将生成的装箱候选转换为层分配列的适配器。 |
| `Bpp3dLayerGenerationServiceAsync` | 层生成操作的异步服务门面。 |

## 生成策略

层生成上下文支持多种候选生成策略：

1. **Circle Packing** — 轴向感知的 circle-packing 网格，用于固定/离散半径圆柱和长方体物品。
2. **回退** — 简单的回退层生成，用于基本物品组合。
3. **堆叠** — 基于堆叠的竖直 `Axis3.Y` 圆柱放置（有限支持）。

生成的候选通过 `LayerGenerationProgramCandidateAdapters` 转换为 `BinLayer` 列，并注册到列生成模型中。

## 依赖

- `bpp3d-infrastructure` — 几何原语、circle packing
- `bpp3d-domain-item-context` — 物品、包装模型
- `bpp3d-domain-bla-context` — BLA 放置算法
- `bpp3d-domain-block-loading-context` — 块生成算法

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
