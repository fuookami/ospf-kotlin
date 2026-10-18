# ospf-kotlin-framework-csp2d-domain-sheet-assignment-context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-domain-sheet-assignment-context` 提供卷材的消耗段高（条带排样）与定尺板材的张数，并提供由使用次数计算消耗长度、超长量与张数的纯函数。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
