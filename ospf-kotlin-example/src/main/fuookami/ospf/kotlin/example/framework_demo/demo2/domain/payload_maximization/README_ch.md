# 载荷最大化 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

在所有安全和结构约束内最大化装载到飞机上的总载荷（货物重量）。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
payload_maximization/
├── service/        # 领域服务
│   ├── limits/
│   │   └── MaxPayloadLimit.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── PayloadMaximizationContext.kt  # 上下文入口
```

## 核心概念

- **载荷**（Payload）：需要最大化的总货物重量。

## 约束列表

- **最大载荷限制**：载荷不得超过飞机最大载荷能力。

## 目标函数

$$
\max \sum_{i \in I} \sum_{j \in J} weight_i \cdot x_{ij}
$$

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端目标函数上下文）
