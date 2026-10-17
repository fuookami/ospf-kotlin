# BPP2D Domain — Packing Context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp2d-domain-packing-context` 提供终态装箱：几何校验（越界、内部重叠、旋转合法性）、装箱器、物料汇总与渲染适配器。

## 公开 API

`PackingGeometryGuard` — 越界、重叠与旋转校验
`Packer` — 板材到已装箱板材，含装载顺序
`PackingResult` / `PackingAggregation` / `PackedItem`
`PackingRendererAdapter` — 带实际面积的渲染 DTO 输出

## 父模块

[OSPF Kotlin Framework BPP2D](../README_ch.md)
