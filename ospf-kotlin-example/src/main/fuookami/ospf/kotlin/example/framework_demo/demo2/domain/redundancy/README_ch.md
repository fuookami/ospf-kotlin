# 冗余 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理冗余和实验纵向平衡约束，用于重量分布分析和安全裕度。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
redundancy/
├── model/          # 领域模型
│   ├── Redundancy.kt                      # 冗余模型
│   └── ExperimentalLongitudinalBalance.kt # 实验纵向平衡
├── service/        # 领域服务
│   ├── limits/
│   │   ├── RedundancyLimit.kt
│   │   └── ExperimentalLongitudinalBalanceLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── RedundancyContext.kt  # 上下文入口
```

## 核心概念

- **冗余**（Redundancy）：重量分布安全裕度的冗余模型。
- **实验纵向平衡**（Experimental Longitudinal Balance）：基于冗余计算的实验纵向平衡模型。

## 约束列表

- **冗余限制**：冗余必须在可接受范围内。
- **实验纵向平衡限制**：实验纵向平衡必须在范围内。

## 目标函数

本上下文不定义目标函数，仅提供约束。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端约束上下文）
