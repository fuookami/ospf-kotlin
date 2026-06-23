# MAC优化 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理 MAC 优化，包括纵向平衡（MAC 范围约束）和横向平衡约束，用于飞机重量分布。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）
3. 平均气动弦（mac）

## 目录结构

```
mac_optimization/
├── model/          # 领域模型
│   ├── MACRange.kt           # MAC范围
│   ├── LongitudinalBalance.kt # 纵向平衡
│   └── LateralBalance.kt     # 横向平衡
├── service/        # 领域服务
│   ├── limits/
│   │   ├── LongitudinalBalanceLimit.kt
│   │   ├── LateralBalanceLimit.kt
│   │   ├── HorizontalStabilizerLimit.kt
│   │   └── AggregationInitializer.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── MacOptimizationContext.kt  # 上下文入口
```

## 核心概念

- **MAC范围**（MAC Range）：基于总重量定义允许的 MAC 百分比范围。
- **纵向平衡**（Longitudinal Balance）：纵向平衡约束，确保各飞行阶段 MAC 在允许范围内。
- **横向平衡**（Lateral Balance）：宽体飞机的横向平衡约束，确保对称装载。

## 约束列表

- **纵向平衡限制**：MAC 百分比必须在各飞行阶段的允许范围内。
- **横向平衡限制**：横向扭矩必须在允许范围内（仅宽体飞机）。
- **水平安定面限制**：水平安定面位置必须与 MAC 匹配。

## 目标函数

最小化 MAC 偏离目标范围的程度。

## 与其他上下文的关系

**上游**：飞机、装载分配、平均气动弦

**下游**：无（终端约束上下文）
