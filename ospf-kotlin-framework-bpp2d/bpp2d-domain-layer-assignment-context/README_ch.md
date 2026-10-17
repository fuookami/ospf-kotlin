# BPP2D Domain — Layer Assignment Context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-domain-layer-assignment-context` 提供列生成模型：赋值、负载、容量、带影子价格的需求约束与目标函数。这是唯一接触 `FltX` 的模块。

## 公开 API

`ImpreciseAssignment` — RMP 每列一个整数变量
`PreciseAssignment` — 最终 MILP 的每列份数及份数上界
`ImpreciseLoad` / `PreciseLoad` — 需求负载符号
`PreciseLoadCapacity` — 每列装载面积与装载率
`DemandConstraint` — 覆盖下界与可选上界，并提取影子价格
`AreaMinimization` / `SheetAmountMinimization` / `SheetCapacityConstraint`
`LayerAssignmentContext` — 五阶段生命周期

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
