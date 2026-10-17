# BPP2D Infrastructure（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-infrastructure` 提供量纲几何、装载形状（矩形/圆形）、板材形状、放置、内部重叠判定、渲染 DTO 与领域错误，供所有子模块共享。

## 公开 API

`PackingShape2` / `RectanglePackingShape2` / `CirclePackingShape2` — 形状抽象，提供实际面积与外接面积
`Sheet2Shape` / `Sheet2Geometry` — 板材几何与可容纳判定
`LayerPlacement2` — 放置，支持重叠、包含与平移
`Box2Geometry` — 矩形、圆与混合配对的内部重叠判定
`quantityPlusSafe` / `quantityGreater` / `countFitting` / `scaledQuantity` — 泛型量值运算
`RendererDTO` — 二维渲染 DTO
`Bpp2dValidationError` / `Bpp2dSolvingError` / `Bpp2dCapabilityError` — 领域错误

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
