# 推荐重量均衡 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理推荐重量均衡——确保货物重量按优先级预约在各位置间均匀分布。

### 依赖上下文

1. 飞机（aircraft）
2. 装载分配（stowage）

## 目录结构

```
recommended_weight_equalization/
├── model/          # 领域模型
│   └── PriorityAppointment.kt  # 优先级预约
├── service/        # 领域服务
│   ├── limits/
│   │   ├── ItemOrderLimit.kt
│   │   ├── PriorityAppointmentLimit.kt
│   │   └── RecommendedWeightEqualizationLimit.kt
│   ├── AggregationInitializer.kt
│   └── PipelineListGenerator.kt
├── Aggregation.kt  # 聚合根
└── RecommendedWeightEqualizationContext.kt  # 上下文入口
```

## 核心概念

- **优先级预约**（Priority Appointment）：基于优先级的货物-位置预约及重量均衡。

## 约束列表

- **货物顺序限制**：货物必须按优先级顺序装载。
- **优先级预约限制**：必须遵守优先级预约。
- **推荐重量均衡限制**：装载重量应在各位置间均衡分布。

## 目标函数

最小化重量偏离推荐值的程度。

## 与其他上下文的关系

**上游**：飞机、装载分配

**下游**：无（终端约束上下文）
