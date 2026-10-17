# BPP2D Domain — Block Loading Context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-domain-block-loading-context` 提供二维空间模型、简单/复杂块生成器，以及 DFS 与多行空间切分搜索。

## 公开 API

`Space2` / `Block2` — 区域与块模型
`SimpleBlockGenerator` — 整行规则块
`ComplexBlockGenerator` — 异构行块
`DepthFirstSearchAlgorithm2D` / `MultiRowHeuristicAlgorithm2D` — 空间切分
`BlockLoadingContext` — 上下文门面
`requireRotationAllowed` — 旋转守卫

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
