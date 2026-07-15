# 平均气动弦 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

从飞机和装载数据计算平均气动弦（MAC）百分比、纵向/横向扭矩、CLIM 和各飞行阶段的指数。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
mac/
├── model/          # 领域模型
│   ├── MAC.kt              # 平均气动弦计算
│   ├── Torque.kt           # 扭矩计算
│   └── HorizontalStabilizer.kt  # 水平安定面
├── service/        # 领域服务
│   └── AggregationInitializer.kt
├── Aggregation.kt  # 聚合根
└── MacContext.kt   # 上下文入口
```

## 核心概念

- **扭矩**（Torque）：从装载、燃油、机身和公式数据计算各飞行阶段的纵向扭矩、横向扭矩、CLIM 和指数。
- **平均气动弦**（MAC）：从扭矩指数和总重量计算 MAC 百分比作为线性中间符号。
- **水平安定面**（Horizontal Stabilizer）：用于平衡计算的水平安定面位置和限制。

## 约束列表

本上下文不定义约束，仅提供中间计算值。

## 目标函数

本上下文不定义目标函数。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：适航安全、MAC优化
