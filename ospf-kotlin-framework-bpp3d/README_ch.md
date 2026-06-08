# OSPF Kotlin Framework BPP3D（中文）

:us: [English](README.md) | :cn: 简体中文

## 模块范围

`ospf-kotlin-framework-bpp3d` 是 OSPF Kotlin 的 BPP3D 领域框架。
当前重点能力：

1. 长方体装箱基线能力。
2. 竖直圆柱 MVP（`Axis3.Y`）及 shape-aware 几何判定。
3. `Axis3.X` / `Axis3.Z` 横向圆柱固定半径/离散半径可通过 axis-aware circle-packing grid 和保守的 supported-stack 生成候选，并进入 column generation。
4. 在最终坐标已确定的已知坐标装箱/渲染路径中，通过真实几何 guard 支持 `Axis3.X` / `Axis3.Z` 横向圆柱。
5. 横向圆柱在贴地或全长长方体支撑下可通过保守的 3D stacking 支撑检查。

## 圆柱几何语义

当前 MVP 的语义约束：

1. 固定半径和离散半径的 `Axis3.X` / `Axis3.Z` 横向圆柱可进入 axis-aware circle-packing 网格生成候选路径，以及保守的全长长方体支撑 supported-stack 路径。其它候选生成、block loading、无坐标 hanging 和 pile 支撑路径仍只支持竖直圆柱（`Axis3.Y`），除非某条路径另行明确说明。
2. 最终装箱转换/渲染路径和泛型已知坐标分析入口可以接受通过生成候选或已知坐标进入的 `Axis3.X` / `Axis3.Z` 横向圆柱，并由 `PackingGeometryGuard` 执行真实轴对齐圆柱几何校验。
3. 横向圆柱必须贴在箱底，或由下方长方体提供全长支撑；无支撑或局部支撑的横向圆柱会在最终校验和 3D stacking 支撑检查中被拒绝。
4. 单个 `BinLayer` 内不能混放多个圆柱轴向；同一 bin 的不同 layer 可以使用不同轴向。
5. 已支持的竖直圆柱路径中，底面重叠与支撑使用真实 footprint 几何。
6. 连续半径优化元数据在产出生产 `PackingShape3` 前会被拒绝；固定半径和离散半径候选仍保持支持。
7. 渲染输出中的装载率基于 `actualVolume` 计算，不仅依赖外接长方体体积。

当前仍未完成或未完全泛型化：

1. 圆柱任意三维旋转。
2. 全主链 legacy 长方体算法的 shape-generic 迁移。
3. solver 原生连续半径优化完整闭环。
4. 外部 renderer 源码不属于本仓；本模块负责输出用于外部 renderer 验收的 shape metadata。

重构进度请查看 [refactor.md](./refactor.md)。

## 圆柱轴向支持矩阵

| 轴向 | 含义 | 当前状态 |
| --- | --- | --- |
| `Axis3.Y` | 竖直圆柱；装载平面上的 footprint 为圆。 | 已在带门禁的竖直圆柱路径支持，并使用真实 footprint 校验。 |
| `Axis3.X` | 横向圆柱，轴向沿 X；截面圆位于 YZ 平面。 | 支持固定/离散半径的 axis-aware circle-packing 网格候选、保守的全长长方体支撑 generated supported-stack 候选、已知坐标最终装箱/渲染路径，以及贴地/全长长方体支撑下的 3D stacking 检查；无坐标 hanging 和 cuboid-only 生成路径仍不支持。 |
| `Axis3.Z` | 横向圆柱，轴向沿 Z；截面圆位于 XY 平面。 | 支持固定/离散半径的 axis-aware circle-packing 网格候选、保守的全长长方体支撑 generated supported-stack 候选、已知坐标最终装箱/渲染路径，以及贴地/全长长方体支撑下的 3D stacking 检查；无坐标 hanging 和 cuboid-only 生成路径仍不支持。 |

## Shape 路径支持矩阵

生产路径标签和 unsupported 文案集中在 `CylinderCapabilityPath` / `CylinderShapeContract`。
候选生成、cuboid-only search/merge、支撑检查、已知坐标终态校验、renderer 终态校验和 depth boundary 终态校验必须使用共享契约，不允许散落维护路径字符串。

| 路径 | 长方体 | `Axis3.Y` 圆柱 | `Axis3.X` / `Axis3.Z` 圆柱 |
| --- | --- | --- | --- |
| 显式 final bins / 已知坐标装箱 | 支持 | 支持，并使用真实几何 guard | 支持，并使用真实几何 guard，要求贴地或全长长方体支撑 |
| 泛型已知坐标分析 | 支持，可选 depth boundary 最终校验 | 支持，并使用真实几何 guard 与可选 depth boundary 最终校验 | 支持，并使用真实几何 guard，要求贴地或全长长方体支撑，可选 depth boundary 最终校验 |
| 默认生成候选的 layer placement adapter | 支持 | 支持 | 仅支持已验证的 axis-aware 生成候选；手工伪造候选会被拒绝 |
| layer generation fallback / circle packing / pile | 支持 | 支持竖直圆柱候选；pile 支撑仅限直立 `Axis3.Y` 圆柱 | circle packing 支持固定/离散半径的 axis-aware 横向网格候选和保守的全长长方体支撑 supported-stack 候选；fallback 和 pile 仍不支持 |
| BLA placement | 支持当前已生成 layer | 仅通过已验证的竖直圆柱生成层支持 | 仅通过已验证的 axis-aware circle-packing 生成层支持 |
| simple block generation | 支持 | 仅支持直立 `Axis3.Y` 圆柱 | 不支持 |
| DFS / MLHS space splitting | 支持 cuboid-only 路径 | 不支持 | 不支持 |
| stacking / hanging 支撑语义 | 支持长方体语义 | 仅在明确 guard 的直立 `Axis3.Y` 支撑检查中受限支持 | 3D stacking 支持贴地/全长长方体支撑；无坐标 hanging 和局部支撑仍不支持 |
| packing program / material packing | 支持，并在生成 item 时保留 `PackingProgram.shape.shapeSpec` | 作为 item shape metadata 保留；后续能力服从 item/generated/final-path guard | 作为 item shape metadata 保留；仅 axis-aware generated 或已知坐标终态路径可开放 |
| depth boundary policy | application 层最终校验 | application 层最终校验 | 仅在已知坐标存在后做 application 层最终校验 |

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
5. `radius_weight_function_key`：预留的连续半径优化 key；当前会被拒绝。
6. `axis`：圆柱可选，默认 `Y`，可接受值：`Y`、`AXIS3.Y`、`X`、`AXIS3.X`、`Z`、`AXIS3.Z`。

圆柱行至少需要提供 `radius_meter`、`radius_min` 或 `diameter_min` 之一。`axis = X` / `Z` 是固定/离散半径 axis-aware circle-packing 候选生成的生产输入，也可用于已知坐标最终装箱/渲染路径；两者都必须通过真实 3D 几何校验。material-width-amount CSV 中，X/Z 行的 `width` 会解释为圆柱轴向长度。除 3D 贴地/全长长方体支撑检查外，其它支撑路径仍保持门禁。

动态半径/直径当前是离散能力：区间列会展开为固定半径候选，circle packing 最终输出确定半径、确定 placement 和确定 `actualVolume`。连续半径优化还不是生产能力；`radiusWeightFunctionKey` 元数据会在产出生产 `PackingShape3` 前被拒绝，CSV `radius_weight_function_key` 也会被 Gurobi dataset parser 拒绝，不能静默按固定半径求解。

### 深度边界层策略列

可选场景级策略列：

1. `first_layer_allowed_cylinder_axes`
2. `last_layer_allowed_cylinder_axes`
3. `first_layer_allowed_cuboid_orientations`
4. `last_layer_allowed_cuboid_orientations`

这些字段只约束最终收集出的 depth 方向第一个和最后一个 layer。它们是最终 MILP 求解后，或泛型已知坐标终态 bin 构造后的 application 层硬校验，不是 MILP 原生约束，也不会在候选生成阶段提前过滤候选。

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
4. depth boundary 的圆柱轴向或长方体朝向策略非法值会被显式拒绝。
5. CSV 重复列会被显式拒绝。
6. grouped-layer 或 material-width-amount schema 之外的未知 CSV 列会被显式拒绝。
7. 可变半径/直径区间必须是离散区间。`*_min` + `*_max` 但缺少 `*_step` 时会被拒绝，除非 `radius_meter` 已给出固定的具体半径。
8. `radius_weight_function_key` 会被拒绝，直到 solver 原生连续半径变量和最终实际半径校验完成。
9. 在 dataset suite 模式下，文件名可声明场景类型：
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
3. `bpp3d-application/src/test/resources/gurobi/grouped-layer-depth-boundary-sample.csv`

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
2. `bpp3d-application/src/test/resources/gurobi/material-width-amount-dynamic-diameter-sample.csv`

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

覆盖 `Axis3.X`、`Axis3.Y`、`Axis3.Z` 三种圆柱轴向的 renderer 坐标 fixture 位于：
`bpp3d-infrastructure/src/test/resources/renderer/cylinder-axis-renderer-schema.json`。
该 fixture 遵循外部 renderer 的坐标指南：`x` / `y` / `z` 表示外接盒最小角点，X/Z 横向圆柱使用 `boundingHeight = diameter`，且 `y = 0` 表示贴地。

外部 renderer 源码不纳入本仓。该工程的构建、类型检查或显示一致性结果，只有在实际执行外部 renderer 命令并打开本仓 fixture 或实际求解输出核对后，才能记录为通过。
