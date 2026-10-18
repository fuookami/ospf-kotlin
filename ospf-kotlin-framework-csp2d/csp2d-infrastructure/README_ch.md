# ospf-kotlin-framework-csp2d-infrastructure（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-infrastructure` 提供裁切区域、带刀缝的区域切分、断头台刀线与 k 级切分树、裁切工艺配置、错误类型与渲染 DTO。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
