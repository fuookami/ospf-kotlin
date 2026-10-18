# ospf-kotlin-framework-csp2d-domain-wasting-minimization-context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-domain-wasting-minimization-context` 提供余料最小化目标，以及按物料最小可用余料阈值判定余料可用性的汇总。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
