# BPP2D Domain — Item Context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-domain-item-context` 提供物品、板材、层、排样程序、需求统计、求解器值适配器，以及既有 `*Need` 兼容桥接。

## 公开 API

`ActualItem` / `PatternItem` / `AbstractItem` — 基于装载形状的物品
`SheetType` / `Sheet` / `Layer` / `SheetLayer` — 容器层次
`PackingProgram` — 可重复的排样程序
需求统计相关类型
`Bpp2dSolverValueAdapter` — 求解器数值转换边界
`LegacyBridge` — `*Need` 到新模型的适配器

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
