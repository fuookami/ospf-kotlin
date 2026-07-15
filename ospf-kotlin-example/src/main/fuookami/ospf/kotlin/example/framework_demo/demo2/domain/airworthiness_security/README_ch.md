# 适航安全 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

执行适航和安全约束，包括线密度/面密度限制、区域载荷重量限制、累积载荷重量限制、CLIM 限制、包络线约束和载荷限制。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）
3. 平均气动弦（mac）

## 目录结构

```
airworthiness_security/
├── model/          # 领域模型
│   ├── LinearDensity.kt           # 线密度
│   ├── SurfaceDensity.kt          # 面密度
│   ├── MaxZoneLoadWeight.kt       # 最大区域载荷重量
│   ├── MaxCumulativeLoadWeight.kt # 最大累积载荷重量
│   ├── MaxUnsymmetricalLinearDensity.kt  # 最大非对称线密度
│   ├── MaxCLIM.kt                 # 最大CLIM
│   ├── MinLowPayload.kt          # 最小下层载荷
│   ├── Envelope.kt                # 包络线
│   └── model/                     # 其他模型
├── service/        # 领域服务
│   ├── limits/                    # 约束定义
│   │   ├── LinearDensityLimit.kt
│   │   ├── SurfaceDensityLimit.kt
│   │   ├── ZoneLoadWeightLimit.kt
│   │   ├── CumulativeLoadWeightLimit.kt
│   │   ├── CLIMLimit.kt
│   │   ├── EnvelopeLimit.kt
│   │   ├── PayloadLimit.kt
│   │   ├── LowPayloadLimit.kt
│   │   ├── TotalWeightLimit.kt
│   │   ├── BallastWeightLimit.kt
│   │   ├── HorizontalStabilizerLimit.kt
│   │   ├── UnsymmetricalLinearDensityLimit.kt
│   │   └── AdjacentGapLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── AirworthinessSecurityContext.kt  # 上下文入口
```

## 核心概念

- **线密度**（Linear Density）：各机身区域的线性重量密度及上下限。
- **面密度**（Surface Density）：各区域的面重量密度及上下限。
- **最大区域载荷重量**（Max Zone Load Weight）：各机身区域的最大允许载荷重量。
- **最大累积载荷重量**（Max Cumulative Load Weight）：从机头/机尾起的最大累积载荷重量。
- **最大非对称线密度**（Max Unsymmetrical Linear Density）：宽体飞机的最大允许非对称线密度。
- **最大 CLIM**（Max CLIM）：宽体飞机的最大 CLIM 限制。
- **最小下层载荷**（Min Low Payload）：下层甲板所需的最小载荷量。
- **包络线**（Envelope）：各飞行阶段的重量-重心包络线约束。
- **压舱物重量**（Ballast Weight）：平衡所需的最小压舱物重量。
- **邻接间隙**（Adjacent Gap）：相邻位置之间的最大允许重量差。

## 约束列表

- **线密度限制**：各区域的线密度必须在限制范围内。
- **面密度限制**：各区域的面密度必须在限制范围内。
- **区域载荷重量限制**：区域载荷重量不得超过最大值。
- **累积载荷重量限制**：从机头/机尾起的累积载荷重量不得超过最大值。
- **CLIM 限制**：CLIM 必须在允许范围内。
- **包络线限制**：重量-重心组合必须在包络线内。
- **载荷限制**：载荷必须在计划和最大范围内。
- **下层载荷限制**：下层载荷必须满足最小要求。
- **总重量限制**：各飞行阶段的总重量不得超过最大值。
- **压舱物重量限制**：压舱物重量必须满足最小要求。
- **水平安定面限制**：水平安定面位置必须在限制范围内。
- **非对称线密度限制**：非对称线密度不得超过最大值。
- **邻接间隙限制**：相邻位置之间的重量差不得超过最大值。

## 目标函数

本上下文不定义目标函数，仅提供约束。

## 与其他上下文的关系

**上游**：飞机、装载分配、平均气动弦

**下游**：无（终端约束上下文）
