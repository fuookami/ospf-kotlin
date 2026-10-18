# ospf-kotlin-framework-csp2d-domain-yield-context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-domain-yield-context` 提供超产上界。需求覆盖是硬下界，因此只需给超产设上限，且不引入额外松弛变量。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
