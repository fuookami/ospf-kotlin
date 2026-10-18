# ospf-kotlin-framework-csp2d-domain-produce-context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-domain-produce-context` 提供主问题：图案使用量变量、面积/段高聚合表达式、需求覆盖、物料可用量、设备产能与可配置的使用量目标。这里是建模边界，允许出现 FltX。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
