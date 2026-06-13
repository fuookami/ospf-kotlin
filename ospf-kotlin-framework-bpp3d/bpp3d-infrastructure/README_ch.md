# BPP3D 基础设施

:us: [English](README.md) | :cn: 简体中文

## 概述

`bpp3d-infrastructure` 是 BPP3D 领域框架的基础设施层，提供所有领域子模块和应用层共享的几何原语、类型别名和工具函数。

## 核心组件

### 几何原语

| 组件 | 描述 |
| --- | --- |
| `Container` | 容器形状定义（2D/3D），包括 `Container2Shape`、`Container3Shape` 及其物理量类型变体。支持剩余空间计算、容量估算和放置可行性检查。 |
| `Cuboid` | 长方体形状定义，含物理量类型的尺寸、体积和重量。`CuboidView` 提供方向相关的尺寸访问。 |
| `Cylinder` | 圆柱形状定义，含半径、轴向和外接长方体。支持竖直（`Axis3.Y`）和横向（`Axis3.X`/`Axis3.Z`）圆柱。 |
| `PackingShape` | 装箱导向的形状抽象（`PackingShape2`、`PackingShape3`），统一长方体和圆柱几何用于放置和投影面积计算。 |
| `Placement` | 放置定义（`QuantityPlacement2`、`QuantityPlacement3`、`ShapePlacement3`），含位置、重叠检测和包含检查。支持父子嵌套的容器内容器场景。 |
| `Projection` | 投影定义，将 3D 长方体视图映射到 2D 射影平面（Bottom、Side、Front）。 |
| `ProjectivePlaneGeometryMapping` | 3D 几何与 2D 射影平面投影之间的映射。 |

### 方向与轴向

| 组件 | 描述 |
| --- | --- |
| `Orientation` | 6 种长方体方向的密封类层次（Upright、UprightRotated、Side、SideRotated、Lie、LieRotated）。支持尺寸置换、类别分组和去重。 |
| `OrientationAxisPermutationMapping` | 方向置换与轴置换之间的映射，用于 shape-aware 坐标变换。 |
| `PackageType` | 包装类型分类（CartonContainer、Pallet 等）。 |

### 物理量与数值工具

| 组件 | 描述 |
| --- | --- |
| `FltXAliases` | FltX 类型的便捷别名（`fltXZero`、`fltXInfinity`、`fltXEpsilon`）及物理量类型几何的转换辅助。 |
| `QuantityOperators` | 物理量类型算术运算符（加、缩放、乘、比率、序、最小、最大），含 FltX 值提取。 |
| `QuantityContainerCore` | 物理量类型容器核心定义（`QuantityContainer2Shape`、`QuantityContainer3Shape`）。 |
| `QuantityGeometryCore` | 物理量类型几何核心定义（`QuantityPoint2/3`、`QuantityVector2/3`、`QuantityCuboid3` 等）。 |
| `SemanticParameter` | 装箱配置的语义参数定义。 |

### 圆柱支撑与近似

| 组件 | 描述 |
| --- | --- |
| `ConservativeRadiusEnvelope` | 保守半径包络（`rMax`），用于连续半径圆柱的安全投影面积边界。 |
| `HorizontalCylinderSupportCoverage` | 横向圆柱支撑覆盖检查器，用于贴地/长方体支撑区间验证。 |
| `PWLRadiusSquaredApproximation` | 半径平方的分段线性（PWL）近似，用于连续半径圆柱体积。 |
| `PWLRadiusApproximationConfig` | PWL 半径近似配置（段数、误差界）。 |

### 其他

| 组件 | 描述 |
| --- | --- |
| `ShadowPriceMap` | 影子价格映射基础设施，用于列生成对偶值提取。 |
| `RendererDTO` | 渲染器输出数据传输对象（装载方案项、形状元数据、圆柱轴向/半径/体积）。 |

## 依赖

- `ospf-kotlin-utils` — 函数式抽象、概念
- `ospf-kotlin-math` — 代数、几何
- `ospf-kotlin-quantities` — 物理量类型和单位

## 父模块

[OSPF Kotlin Framework BPP3D](../README_ch.md)
