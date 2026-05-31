# OSPF Kotlin Framework BPP3D（中文）

[English README](./README.md)

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
3. 旧 three.js renderer 对圆柱新字段的适配。

重构进度请查看 [refactor.md](./refactor.md)。

## CSV 输入协议（Gurobi 数据集）

当前 `GurobiColumnGenerationTest` 支持两类 CSV 形态：

1. 分组分层格式（以 `group_index`、`layer_index` 作为场景键）。
2. 物料宽度数量格式（以 `material`、`width`、`amount` 作为场景键）。

### 通用 shape 列

可选形状元数据列：

1. `shape_type`：`cuboid` 或 `vertical_cylinder`（兼容别名：`vertical-cylinder`、`verticalcylinder`、`cylinder`）。
2. `radius_meter`：当 `shape_type` 为圆柱时必填。
3. `axis`：圆柱可选，默认 `Y`，可接受值：`Y`、`AXIS3.Y`、`X`、`AXIS3.X`、`Z`、`AXIS3.Z`。

schema 门禁规则：

1. 如果存在 `radius_meter` 或 `axis` 列，必须同时存在 `shape_type` 列。
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
