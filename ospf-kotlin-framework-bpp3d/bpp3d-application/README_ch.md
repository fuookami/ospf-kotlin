# BPP3D 应用层

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-application` 是 BPP3D 的应用层。编排列生成工作流，将所有领域上下文组合为完整的装箱管线：物品建模、层生成、层分配、装箱和结果输出。

## 核心组件

| 组件 | 描述 |
| --- | --- |
| `ColumnGenerationApplicationService` | 顶层应用服务入口。接受装箱输入，配置列生成算法，返回装箱结果。 |
| `ColumnGenerationAlgorithm` | 列生成算法编排器。管理迭代 LP → 定价 → 加列 → 最终 MILP 工作流。 |
| `ColumnGenerationPackingAnalyzer` | 分析列生成结果，计算 KPI（装载率、利用率），生成最终装箱计划。 |
| `ColumnGenerationStandardExecutors` | 列生成生命周期钩子的标准执行器实现（定价、列管理、定稿）。 |
| `DepthBoundaryLayerOrientationPolicy` | 应用层策略，约束首尾深度层允许的圆柱轴向和长方体方向。作为最终 MILP 求解后的硬验证。 |
| `LayerPlacementAdapter` | 将生成的层候选转换为箱位内的具体三维放置适配器。 |

## 工作流

```
输入物品 → ItemContext → LayerGenerationContext → LayerAssignmentContext → PackingContext → 输出
                            │                         │                      │
                      生成候选列             列生成 LP              物料装箱
                      (BLA、块装载)          影子价格提取            几何验证
                                                                   渲染器输出
```

1. **物品建模** — `ItemContext` 注册物品、包装和物料。
2. **层生成** — `LayerGenerationContext` 通过 BLA、circle packing 或块装载生成候选箱层。
3. **层分配** — `LayerAssignmentContext` 运行列生成以最优地将物品分配到层。
4. **装箱** — `PackingContext` 在几何验证后完成物料装箱。
5. **输出** — 结果导出为渲染器 DTO 和 KPI 报告。

## 依赖

- `bpp3d-infrastructure` — 几何原语、渲染器 DTO
- `bpp3d-domain-item-context` — 物品领域
- `bpp3d-domain-bla-context` — BLA 放置
- `bpp3d-domain-block-loading-context` — 块装载
- `bpp3d-domain-layer-assignment-context` — 层分配
- `bpp3d-domain-layer-generation-context` — 层生成
- `bpp3d-domain-packing-context` — 装箱

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
