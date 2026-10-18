# ospf-kotlin-framework-csp2d-domain-material-context（中文）

:us: [English](README.md) | :cn: 简体中文

## 概述

`csp2d-domain-material-context` 提供产品、需求、双模式物料（卷材/定尺板材）、设备、副产物、裁切图案与裁切方案，含几何与贡献校验。

## 边界

属于 [csp2d](../README.md) 框架。泛型分层纪律要求领域模块不出现 `FltX`，`FltX` 只出现在建模边界
（`produce`）与 `application`。二维几何通过
[ospf-kotlin-framework-geometry2d](../../ospf-kotlin-framework-geometry2d/README_ch.md) 共享。

## 父模块

[OSPF Kotlin Framework CSP2D](../README_ch.md)
