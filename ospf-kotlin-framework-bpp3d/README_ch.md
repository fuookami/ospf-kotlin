# OSPF Kotlin Framework BPP3D（中文）

:us: [English](README.md) | :cn: 简体中文

## 模块范围

`ospf-kotlin-framework-bpp3d` 是 OSPF Kotlin 的 BPP3D 领域框架。
当前重点能力：

1. 长方体装箱基线能力。
2. 竖直圆柱 MVP（`Axis3.Y`）及 shape-aware 几何判定。

## 竖直圆柱几何语义

当前 MVP 的语义约束：

1. 仅支持竖直圆柱（`Axis3.Y`）。
2. 横放/躺放圆柱在已加门禁路径会被显式拒绝。
3. 底面重叠与支撑使用真实 footprint 几何（圆-圆、圆-矩形、矩形-矩形）。
4. 渲染输出中的装载率基于 `actualVolume` 计算，不仅依赖外接长方体体积。

当前仍未完成或未完全泛型化：

1. 圆柱任意三维旋转。
2. 全主链 legacy 长方体算法的 shape-generic 迁移。
3. 外部 renderer 源码不属于本仓；本模块负责输出用于外部 renderer 验收的 shape metadata。

重构进度请查看 [refactor.md](./refactor.md)。

## 圆柱轴向支持矩阵

| 轴向 | 含义 | 当前状态 |
| --- | --- | --- |
| `Axis3.Y` | 竖直圆柱；装载平面上的 footprint 为圆。 | 已在带门禁的竖直圆柱路径支持，并使用真实 footprint 校验。 |
| `Axis3.X` | 横向圆柱，轴向沿 X；截面圆位于 YZ 平面。 | 可用于元数据解析和策略校验；除非某条路径显式补齐真实几何支持，否则 placement 仍为 unsupported。 |
| `Axis3.Z` | 横向圆柱，轴向沿 Z；截面圆位于 XY 平面。 | 可用于元数据解析和策略校验；除非某条路径显式补齐真实几何支持，否则 placement 仍为 unsupported。 |

## CSV 输入协议（Gurobi 数据集）

当前 `GurobiColumnGenerationTest` 支持两类 CSV 形态：

1. 分组分层格式（以 `group_index`、`layer_index` 作为场景键）。
2. 物料宽度数量格式（以 `material`、`width`、`amount` 作为场景键）。

### 通用 shape 列

可选形状元数据列：

1. `shape_type`：`cuboid` 或 `vertical_cylinder`（兼容别名：`vertical-cylinder`、`verticalcylinder`、`cylinder`）。
2. `radius_meter`：固定圆柱半径。
3. `radius_min` / `radius_min_meter`、`radius_max` / `radius_max_meter`、`radius_step` / `radius_step_meter`：动态半径区间。
4. `diameter_min` / `diameter_min_meter`、`diameter_max` / `diameter_max_meter`、`diameter_step` / `diameter_step_meter`：动态直径区间。
5. `axis`：圆柱可选，默认 `Y`，可接受值：`Y`、`AXIS3.Y`、`X`、`AXIS3.X`、`Z`、`AXIS3.Z`。

圆柱行至少需要提供 `radius_meter`、`radius_min` 或 `diameter_min` 之一。`axis = X` / `Z` 当前可用于元数据解析和策略校验，但生产装载主链仍只在已加真实几何校验门禁的路径支持 `Axis3.Y` 竖直圆柱摆放。

### 深度边界层策略列

可选场景级策略列：

1. `first_layer_allowed_cylinder_axes`
2. `last_layer_allowed_cylinder_axes`
3. `first_layer_allowed_cuboid_orientations`
4. `last_layer_allowed_cuboid_orientations`

这些字段只约束最终收集出的 depth 方向第一个和最后一个 layer。它们是最终 MILP 求解后的 application 层硬校验，不是 MILP 原生约束，也不会在候选生成阶段提前过滤候选。

字段语义：

1. 列缺失表示对应边界/类型不限制。
2. 列存在但单元格为空是配置错误，不能解释为“不限制”。
3. 多个值可用 `|` 或 `;` 分隔。
4. 轴向值使用与 `axis` 相同的解析规则。
5. 长方体朝向值使用现有 `Orientation` 标签，例如 `Upright`、`Side`、`Lie`。
6. 这些值是场景级配置；若同一列在 CSV 多行中出现不一致值，加载器会拒绝该文件。

schema 门禁规则：

1. 如果存在形状元数据列，必须同时存在 `shape_type` 列。
2. 若 `shape_type` 为空，按 `cuboid` 处理。
3. 圆柱 `axis` 非法值会被显式拒绝。
4. 在 dataset suite 模式下，文件名可声明场景类型：
   `grouped-layer` / `grouped_layer` => 分组分层 CSV，
   `material-width-amount` / `material_width_amount` => 物料宽度数量 CSV。
   文件名声明类型与表头识别类型不一致时会被显式拒绝。

### 分组分层 CSV

必填列：

1. `group_index`
2. `layer_index`
3. `item_id`
4. `material_no`
5. `material_name`
6. `material_weight_kg`

样例文件：

1. `bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv`
2. `bpp3d-application/src/test/resources/gurobi/grouped-layer-cylinder-mixed-sample.csv`

### 物料宽度数量 CSV

必填列：

1. `material`
2. `width`
3. `amount`

可选列：

1. `material_no`
2. `material_name`
3. `material_weight_kg`
4. 上述通用 shape 列

样例文件：

1. `bpp3d-application/src/test/resources/gurobi/material-width-amount-cylinder-sample.csv`

## 示例：固定半径竖直圆柱输入

```kotlin
val cylinderItem = ActualItem(
    id = "cyl-1",
    name = "cyl-1",
    pack = Package.innerPackage(
        shape = PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.2) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            packageType = PackageType.CartonContainer,
            shapeSpec = PackageShapeSpec.VerticalCylinder(
                radius = infraScalar(0.5) * Meter,
                axis = Axis3.Y
            )
        ),
        materials = mapOf(material to UInt64.one)
    ),
    enabledOrientations = listOf(Orientation.Upright),
    batchNo = BatchNo("B-cyl-1"),
    packageAttribute = packageAttribute,
    shapeSpecOverride = PackageShapeSpec.VerticalCylinder(
        radius = infraScalar(0.5) * Meter,
        axis = Axis3.Y
    )
)
```

## 示例：圆柱 Renderer DTO 输出

`PackingRendererAdapter` 会在 `RenderLoadingPlanItemDTO` 输出圆柱元数据：

```json
{
  "name": "cyl-1",
  "shapeType": "Cylinder",
  "renderShapeType": "Cylinder",
  "algorithmShapeType": "VerticalCylinder",
  "axis": "Y",
  "radius": 0.5,
  "diameter": 1.0,
  "boundingWidth": 1.0,
  "boundingHeight": 1.2,
  "boundingDepth": 1.0,
  "actualVolume": 0.942477796
}
```

字段定义位置：
`bpp3d-infrastructure/src/main/.../dto/RendererDTO.kt`。

最小 `Cuboid + Axis3.Y Cylinder` 混装 renderer fixture 位于：
`bpp3d-infrastructure/src/test/resources/renderer/mixed-shape-renderer-schema.json`。
