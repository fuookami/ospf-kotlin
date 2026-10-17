# BPP2D Application（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-application` 提供列生成编排、标准执行器、应用服务、KPI 分析器，以及真实 Gurobi 集成测试。

## 公开 API

`ColumnGenerationAlgorithm` — 含种子列与失败阶段的编排
`ColumnGenerationStandardExecutors` — RMP / 最终 MILP / 请求构建器
`ColumnGenerationApplicationService` — 端到端入口
`ColumnGenerationPackingAnalyzer` — 基于面积的 KPI
`GurobiDelegatingColumnGenerationSolver` / `GurobiColumnGeneration2DTest` — Gurobi 接线与集成测试

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
