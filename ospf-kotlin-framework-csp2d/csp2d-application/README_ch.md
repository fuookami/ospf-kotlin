# ospf-kotlin-framework-csp2d-application（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-application` 提供问题定义、MILP 路径、迭代式列生成路径（剩余需求驱动）、解恢复，以及真实 Gurobi 集成测试。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
