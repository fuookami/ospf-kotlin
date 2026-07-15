# BPP3D 领域 — BLA 上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-bla-context` 是 BPP3D 的底部向上左对齐分配（BLA）领域上下文。提供 BLA 放置算法，使用底部向上左对齐启发式在二维射影平面上定位物品。

## 核心组件

| 组件 | 描述 |
| --- | --- |
| `BLAContext` | 顶层 BLA 上下文，向应用层暴露放置能力。 |
| `BottomUpLeftJustifiedAlgorithm` | 二维 BLA 算法，在射影平面上按序放置物品，始终选择最低可用位置，然后选择最左位置。 |
| `BottomUpLeftJustifiedAlgorithm3D` | BLA 算法的三维扩展，在完整三维空间中操作，含深度堆叠。 |
| `Bpp3dBlaAsync` | BLA 领域操作的异步服务门面。 |

## 算法详情

BLA 算法按给定顺序处理物品：

1. 对每个物品，在射影平面上找到最低可用 Y 坐标。
2. 在该 Y 层级的位置中，找到最左可用 X 坐标。
3. 如果物品在容器范围内适配，则放置在该位置。

三维变体扩展了此算法，同时考虑 Z 轴深度排序，从底到顶、从前到后堆叠物品。

## 依赖

- `bpp3d-infrastructure` — 几何原语、射影平面、放置类型
- `bpp3d-domain-item-context` — 物品和包装模型

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
