# BPP3D 领域 — 装箱上下文

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-domain-packing-context` 是 BPP3D 的装箱领域上下文。管理最终装箱流程，包括物料装箱、几何验证、渲染器输出和方案组装。

## 核心组件

### 上下文与聚合

| 组件 | 描述 |
| --- | --- |
| `PackingContext` | 装箱顶层上下文，管理物料装箱执行和结果收集。 |
| `Aggregation` | 装箱聚合，组合物料装箱计划和 KPI。 |

### 模型（`model/`）

| 组件 | 描述 |
| --- | --- |
| `MaterialAttribute` | 物料属性（尺寸、重量、形状），用于装箱输入。 |
| `MaterialPackingNumbers` | 物料装箱编号别名，用于数量追踪。 |
| `MaterialPackingPlan` | 物料装箱计划，表示每种物料的装箱决策。 |
| `PackageSolutionLikeAdapter` | 将装箱方案转换为下游消费的类方案接口适配器。 |

### 服务（`service/`）

| 组件 | 描述 |
| --- | --- |
| `Packer` | 核心装箱服务，编排物料到箱位的分配和坐标确定。 |
| `MaterialPacker` | 单物料装箱场景的专用装箱器。 |
| `MaterialPackingSolverExecutor` | 物料装箱的求解器执行策略。 |
| `ExhaustiveMaterialPackingSolverExecutor` | 穷举搜索策略，尝试所有物料装箱组合。 |
| `PackingGeometryContract` | 几何契约，定义长方体和圆柱装箱的能力路径。 |
| `PackingGeometryGuard` | 守卫，针对真实圆柱形状验证装箱几何（重叠、包含、支撑）；保守扫掠 broad-phase 只剪枝非圆柱 pair，再调用精确重叠 predicate。 |
| `PackingRendererAdapter` | 将装箱结果转换为渲染器 DTO 输出的适配器。 |

重叠 broad-phase 只扫掠非圆柱 pair 已捕获的端点；任何包含 `CylinderPackingShape3` 的 pair 始终保留并执行精确检查。只有所选坐标轴上的保守扩展范围不相交时，才会排除非圆柱 pair；报告前会恢复候选 pair 的原始顺序。自定义非圆柱 `PackingShape3` 直接复用已捕获端点，不会再次读取形状 getter。物品数较少、候选过密或数值范围存在风险时会回退穷举检查。数值边界和误差分析记录在临时 F19 设计说明中。

## 依赖

- `bpp3d-infrastructure` — 几何原语、渲染器 DTO、支撑覆盖
- `bpp3d-domain-item-context` — 物品、包装、物料模型
- `bpp3d-domain-layer-assignment-context` — 层分配结果
- `bpp3d-domain-layer-generation-context` — 层生成结果
- `bpp3d-domain-bla-context` — BLA 放置算法

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
