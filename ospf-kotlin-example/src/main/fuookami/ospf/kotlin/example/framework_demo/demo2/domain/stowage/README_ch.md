# 装载分配 — README

:us: [English](README.md) | :cn: 简体中文

## 概述

管理货物装载分配决策——确定各货物项装载到哪个位置——包括载荷重量计算、载荷量计算、总重量和最大装载重量。

### 依赖上下文

1. 飞机（aircraft）

## 目录结构

```
stowage/
├── model/          # 领域模型（实体、值对象）
│   ├── Item.kt              # 货物项
│   ├── Position.kt          # 装载位置
│   ├── Stowage.kt           # 装载分配决策
│   ├── Load.kt              # 装载量
│   ├── Payload.kt           # 载荷
│   ├── TotalWeight.kt       # 总重量
│   ├── MaxLoadWeight.kt     # 最大装载重量
│   ├── Ballast.kt           # 压舱物
│   ├── Flight.kt            # 航班
│   ├── Cargo.kt             # 货物类型
│   ├── Appointment.kt       # 预约
│   ├── Solution.kt          # 解
│   └── BiologicalLimit.kt   # 生物限制
├── service/        # 领域服务
│   ├── limits/              # 约束定义
│   │   ├── ItemAssignmentLimit.kt
│   │   ├── LoadAmountLimit.kt
│   │   ├── LoadWeightLimit.kt
│   │   ├── AppointmentLimit.kt
│   │   ├── StowageLimit.kt
│   │   ├── ELDAdjacentLimit.kt
│   │   ├── LoadingOrderLimit.kt
│   │   ├── BiologicalAdjacentLimit.kt
│   │   ├── AOGMATBulkConflictLimit.kt
│   │   ├── PredicateLoadWeightLimit.kt
│   │   ├── RecommendLoadWeightLimit.kt
│   │   ├── EmptyForbiddenLimit.kt
│   │   ├── ItemAdjustmentLimit.kt
│   │   ├── NormalBulkDestinationAssignmentLimit.kt
│   │   └── BiologicalBulkConflictLimit.kt
│   ├── AggregationInitializer.kt
│   ├── PipelineListGenerator.kt
│   └── SolutionAnalyzer.kt
├── Aggregation.kt  # 聚合根
└── StowageContext.kt  # 上下文入口
```

## 核心概念

- **货物项**（Item）：货物项，含目的地、重量、ULD、位置标签、货物类型、优先级和状态。
- **装载位置**（Position）：装载位置，含最大装载数量（MLA）、谓词装载重量（PLW）、推荐装载重量和状态。
- **装载分配**（Stowage）：决策模型：货物-位置对的二元分配变量 x[i,j] 和调整变量 u[i,j]。
- **装载量**（Load）：各位置的装载重量和装载数量，包括谓词/推荐重量松弛、扭矩、CLIM 和指数计算。
- **载荷**（Payload）：计划/最大/计算载荷量，依装载模式注册。
- **总重量**（Total Weight）：各飞行阶段的飞机总重量及计算值。
- **最大装载重量**（Max Load Weight）：基于总重量约束的各位置最大允许装载重量。
- **压舱物**（Ballast）：需要平衡修正的飞机的压舱物重量。
- **航班**（Flight）：航班信息，含出发/到达机场和航班号。
- **预约**（Appointment）：预分配的货物-位置预约。

## 约束列表

- **货物分配限制**（Item Assignment Limit）：每个货物必须分配到恰好一个位置（可选货物可为零）。
- **装载数量限制**（Load Amount Limit）：每个位置的装载数量不得超过 MLA。
- **装载重量限制**（Load Weight Limit）：每个位置的装载重量不得超过最大装载重量。
- **预约限制**（Appointment Limit）：必须遵守预分配的预约。
- **装载限制**（Stowage Limit）：货物只能分配到兼容的位置。
- **ELD 邻接限制**（ELD Adjacent Limit）：空载集装器邻接约束。
- **装载顺序限制**（Loading Order Limit）：必须遵守位置的装载顺序。
- **生物邻接限制**（Biological Adjacent Limit）：生物货物的邻接限制。
- **AOG/MAT 散装冲突限制**：飞机停场和邮件/AT 散装冲突约束。

## 目标函数

本上下文不定义独立目标函数，仅提供约束。

## 与其他上下文的关系

**上游**：飞机

**下游**：MAC、适航安全、快递效能、装载效能、MAC优化、载荷最大化、推荐重量均衡、冗余、软安全
