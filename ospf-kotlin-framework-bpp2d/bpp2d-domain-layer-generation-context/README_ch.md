# BPP2D Domain — Layer Generation Context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-domain-layer-generation-context` 提供带形状分派的候选层生成：块装载、空间切分、BLA、图样、圆形网格（直角/六角）与混合形状。

## 公开 API

`LayerGenerationContext` — 带形状分派的组合生成器
`Bpp2dLayerGenerationRequest` / `Bpp2dLayerGenerationResult`
`CircleGridGenerator` — 直角与六角圆形网格
`MixedShapeLayerGenerator` — 矩形行加圆形网格，逐对校验
`HistoricalLayerGenerator` — 回放已有列

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
