# 装载效能 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理装载效能约束以提升操作效率——包括拖车装载、顺序装载、转运邻接和始发/目的地分组。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
loading_effectiveness/
├── model/          # 领域模型
│   ├── AdviceLoading.kt           # 建议装载
│   ├── TransferAdjacentLoading.kt # 转运邻接装载
│   ├── SequentialLoading.kt       # 顺序装载
│   ├── TrailerLoading.kt          # 拖车装载
│   └── Trailer.kt                 # 拖车
├── service/        # 领域服务
│   ├── limits/
│   │   ├── AdviceLoadAmountLimit.kt
│   │   ├── AdviceLoadWeightLimit.kt
│   │   ├── SameSourceAdjacentLimit.kt
│   │   ├── SameDestinationAdjacent.kt
│   │   ├── ItemOrderReverseLimit.kt
│   │   ├── PriorityOrderLimit.kt
│   │   ├── TrailerChangeLimit.kt
│   │   ├── TrailerCirclingLimit.kt
│   │   ├── ItemAheadLoadLimit.kt
│   │   ├── ItemReserveLimit.kt
│   │   ├── SourceEarlyLimit.kt
│   │   └── ItemReweighNeededLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── LoadingEffectivenessContext.kt  # 上下文入口
```

## 核心概念

- **建议装载**（Advice Loading）：预分配模式下各位置的建议装载数量和重量。
- **转运邻接装载**（Transfer Adjacent Loading）：同源/同目的地邻接约束以提升转运效率。
- **顺序装载**（Sequential Loading）：基于位置排序的顺序装载约束。
- **拖车装载**（Trailer Loading）：满载模式下的拖车变更和绕行约束。

## 约束列表

- **建议装载数量限制**：各位置的装载数量应匹配建议值。
- **建议装载重量限制**：各位置的装载重量应匹配建议值。
- **同源邻接限制**：同源货物应在相邻位置装载。
- **同目的地邻接限制**：同目的地货物应在相邻位置装载。
- **货物顺序逆序限制**：逆序装载的惩罚。
- **优先级顺序限制**：基于优先级的装载顺序约束。
- **拖车变更限制**：最小化装载过程中的拖车变更。
- **拖车绕行限制**：最小化装载过程中的拖车绕行。
- **货物提前装载限制**：提前装载的约束。
- **货物保留限制**：保留货物不得装载。
- **始发提前限制**：始发提前装载约束。
- **货物重称限制**：需要重称的货物必须在可访问位置。

## 目标函数

最小化装载操作成本。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端约束上下文）
