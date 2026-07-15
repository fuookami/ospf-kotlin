# 快递效能 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理快递效能约束，优化货物项优先级排序——确保高优先级货物优先装载。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
express_effectiveness/
├── model/          # 领域模型
│   ├── AbsoluteOrder.kt     # 绝对排序
│   └── RelativeOrder.kt     # 相对排序
├── service/        # 领域服务
│   ├── limits/
│   │   ├── MustShipLimit.kt
│   │   ├── ItemPriorityLimit.kt
│   │   └── ItemPriorityReverseLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── ExpressEffectivenessContext.kt  # 上下文入口
```

## 核心概念

- **绝对排序**（Absolute Order）：定义预分配模式下的绝对优先级排序。
- **相对排序**（Relative Order）：定义满载模式下的相对优先级排序。
- **必须发运项**（Must-Ship Items）：无论优先级如何都必须发运的货物项。

## 约束列表

- **必须发运限制**：必须发运项必须被装载。
- **货物优先级限制**：高优先级货物应在低优先级货物之前装载。
- **货物优先级逆序限制**：逆优先级顺序装载的惩罚。

## 目标函数

最小化优先级违规成本。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端约束上下文）
