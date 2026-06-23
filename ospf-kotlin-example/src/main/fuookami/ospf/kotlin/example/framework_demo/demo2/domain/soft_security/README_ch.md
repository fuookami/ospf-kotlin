# 软安全 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理软安全约束，包括空载分离、主甲板舱门空载偏好和压舱物重量建议——这些约束可提升安全性，但必要时可放松。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
soft_security/
├── model/          # 领域模型
│   └── DivideEmptyLoading.kt  # 空载分离
├── service/        # 领域服务
│   ├── limits/
│   │   ├── EmptyHatedLimit.kt
│   │   ├── MainDeckDoorEmptyLimit.kt
│   │   ├── DivideEmptyLoadingLimit.kt
│   │   └── AdviceBallastWeightLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── SoftSecurityContext.kt  # 上下文入口
```

## 核心概念

- **空载分离**（Divide Empty Loading）：确保空载位置分散而非集中，以保障结构安全。

## 约束列表

- **空载厌恶限制**：空载位置的惩罚（软偏好填满位置）。
- **主甲板舱门空载限制**：主甲板舱门位置应优先空载（B757/B767）。
- **空载分离限制**：空载位置应在飞机上分散分布。
- **建议压舱物重量限制**：压舱物重量应满足建议最小值。

## 目标函数

最小化软安全违规惩罚。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端约束上下文）
