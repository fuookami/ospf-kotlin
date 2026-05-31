# BPP3D 圆柱体装载支持计划

日期：2026-05-26

## 1. 背景

当前 `ospf-kotlin-framework-bpp3d` 的求解、放置、碰撞、支撑、空间切分、block generation、layer generation、BLA/DFS 搜索和渲染导出基本都以长方体为核心抽象。

`bpp3d-infrastructure` 中已经存在 `Cylinder` / `AbstractCylinder`，但它只提供半径、高度、轴向和 `QuantityCylinder3` 几何桥接，尚未接入现有装箱主流程。关键限制包括：

1. `QuantityPlacement3<T : Cuboid<T>>` 只能放置 `CuboidView<T>`。
2. `QuantityPlacement2`、`Projection`、`bottomSupport`、`topPlacements`、`bottomPlacements` 默认使用矩形投影。
3. `Container`、`Bin`、`Item`、`SimpleBlockGenerator`、`ItemMerger` 和 layer 相关模型都默认 item 是长方体。
4. `RendererDTO.kt` 只导出 `width`、`height`、`depth`、`x`、`y`、`z`，无法表达圆柱的 `radius`、`axis`、真实 shape、真实体积和兼容用外接盒字段。

本计划采用已经确认的方向：**不接受外接盒近似作为圆柱装载语义**。外接盒只能用于候选位置粗筛、旧 renderer 兼容字段或调试辅助；最终边界、碰撞、支撑和 loading rate 必须基于竖直圆柱真实几何。

## 2. 已确认决策

1. 不接受外接盒近似。第一阶段直接做竖直圆柱真实几何 MVP。
2. 圆柱默认轴向为 `Axis3.Y`。在当前 BPP3D 坐标语义中，`x` 是宽度方向，`y` 是高度方向，`z` 是深度方向；圆柱轴向表示圆柱中心线方向，因此默认竖直圆柱的高度沿 `Y` 轴，底面圆落在 `X-Z` 平面。
3. 不允许横放或躺放圆柱。若业务需要横放，通常应先打卡板或包装成稳定装载单元，此时在算法里按长方体货物处理。
4. loading rate 使用圆柱真实体积，圆柱体积为 `pi * radius * radius * height`。
5. Renderer DTO 和内部 shape 类型新增 enum，不使用裸字符串作为长期契约。
6. 圆柱半径可能是可变决策，且半径和物料重量正相关；当需求以物料重量统计时，layer generation 需要能根据平面圆密排结果推算候选层的最优半径。

## 3. 目标

### 3.1 总目标

让 BPP3D 能接收、计算、导出并最终渲染竖直圆柱体货物，同时保持现有长方体装载能力、接口兼容性和求解稳定性。

### 3.2 第一阶段目标：竖直圆柱真实几何 MVP

1. 圆柱 SKU 可以进入 BPP3D 输入模型。
2. 圆柱仅允许竖直放置，轴向固定为 `Axis3.Y`。
3. 圆柱与圆柱之间使用底面圆形 footprint 做真实碰撞判断。
4. 圆柱与长方体之间使用圆-矩形 footprint 做真实碰撞判断。
5. 容器边界判断基于圆柱真实 footprint 和高度。
6. 支撑与堆叠至少能识别圆柱真实底面面积，不把外接盒底面积当作支撑面积。
7. loading rate、volume KPI 使用真实圆柱体积。
8. 渲染导出保留圆柱真实几何元数据，用于后续 three.js renderer 适配。
9. 长方体-only 用例行为不变。
10. 支持固定半径圆柱作为基础场景，并为可变半径圆柱预留半径-重量函数和候选半径选择入口。

### 3.3 后续目标

第二阶段优化圆柱排布和候选生成：

1. 针对圆形 footprint 增加更有效的候选点生成。
2. 支持圆柱与长方体混装的更高装载率启发式。
3. 支持半径可变圆柱的候选半径生成和 layer-local 最优半径选择。
4. 支持以物料重量统计需求时的圆柱层价值评估。
5. 保持现有 BLA/DFS/layer 主流程可回退。

第三阶段才考虑更通用的 shape-aware 装箱：

1. 引入通用 3D shape placement。
2. 支持更多 shape 类型。
3. 若未来确有横放圆柱需求，需要作为独立重构课题处理，不进入当前圆柱 MVP。

## 4. 非目标

第一阶段不做以下内容：

1. 不做横放、躺放或任意角度旋转圆柱。
2. 不用外接盒近似代替真实圆柱碰撞。
3. 不重写完整 BLA、DFS、layer generation 和 block generation。
4. 不修改 `E:\workspace\ospf\framework\bpp3d-interface-renderer` 渲染工具；只先修改本仓库导出数据契约，渲染工具等 BPP3D 侧完成后再适配。
5. 不保证圆形最密堆积达到数学最优。
6. 不把打卡板后的圆柱组继续当圆柱求解；打卡板后按长方体装载单元处理。

## 5. 核心原则

1. 圆柱可行性必须基于真实几何，外接盒只能做粗筛或兼容输出。
2. 竖直圆柱轴向固定为 `Axis3.Y`，`enabledAxes` 第一阶段只允许包含 `Axis3.Y`。
3. 长方体既有行为默认不变，圆柱能力通过显式 shape type 开启。
4. 物理量单位继续使用 `Quantity` 体系，不引入无单位几何字段。
5. shape 类型、axis 类型和 renderer shape 类型使用 Kotlin enum，并通过 kotlinx serialization 稳定导出。
6. loading rate 使用真实体积；需要调试外接盒浪费时，可额外输出 bounding volume，但不能作为主指标。
7. DTO 变更要向后兼容，旧 renderer 在未适配前仍能读取长方体字段。
8. 圆柱真实几何判断必须集中在 infrastructure 或明确的 geometry service 中，避免业务模块散落手写半径判断。
9. 每个阶段都要有独立验收，不把完整 shape-aware 重构压进第一阶段。
10. 可变半径不能隐式改变物料语义，必须由显式的 `radiusRange`、`radiusStep` 或 `radiusWeightFunction` 描述。
11. 连续半径优化第一版不直接进入主求解器，优先离散化为有限候选半径后生成 layer。

## 6. 支持层级

| 层级 | 名称 | 算法语义 | 渲染语义 | 推荐顺序 |
|------|------|----------|----------|----------|
| L1 | 竖直圆柱真实几何 MVP | 轴向固定 `Axis3.Y`，真实圆形 footprint 碰撞和边界 | DTO 导出 cylinder enum、radius、height、axis | 第一优先级 |
| L2 | 圆柱排布优化 | 增强候选点、混装启发式、可变半径候选优化、圆形 footprint 支撑近似 | renderer 按真实圆柱画 | 第二优先级 |
| L3 | 通用 shape-aware | placement/projection/support 对 shape 泛化 | renderer 按 shape/orientation 画 | 第三优先级 |

## 7. 第一阶段方案：竖直圆柱真实几何 MVP

### 7.1 建模

需要新增或扩展 item/package attribute 的形状描述：

1. `Cuboid`：保持现状。
2. `Cylinder`：包含 `radius`、`height`、`axis = Axis3.Y`、`enabledAxes = listOf(Axis3.Y)`、`weight`。
3. `actualVolume`：圆柱真实体积。
4. `boundingCuboid`：仅用于粗筛、旧 renderer 兼容和调试，不参与最终真实碰撞语义。
5. `shapeType`：使用新增 enum，不使用长期裸字符串。
6. 可变半径圆柱额外包含 `radiusRange`、`radiusStep` 或 `radiusCandidates`，以及物料重量函数 `weight(radius)`。

建议新增 enum：

```kotlin
@Serializable
enum class PackingShapeType {
    Cuboid,
    Cylinder
}

@Serializable
enum class PackingAxis3 {
    X,
    Y,
    Z
}

@Serializable
enum class PackingAlgorithmShapeType {
    Cuboid,
    VerticalCylinder,
    BoundingCuboid
}
```

命名可按现有代码风格调整，但 enum 语义必须稳定。

### 7.2 默认轴向

圆柱轴向是圆柱中心线方向，不是普通长方体的 orientation。

当前 BPP3D 坐标语义：

1. `x`：宽度方向。
2. `y`：高度方向。
3. `z`：深度方向。

因此竖直圆柱：

1. `axis = Axis3.Y`。
2. `height` 沿 `Y` 轴。
3. 底面圆位于 `X-Z` 平面。
4. footprint 圆心为 `(x + radius, z + radius)` 或等价的 placement anchor 语义。
5. 边界条件为 `x >= 0`、`z >= 0`、`x + 2 * radius <= bin.width`、`z + 2 * radius <= bin.depth`、`y + height <= bin.height`。

### 7.3 几何判断

第一阶段必须支持：

1. cylinder-cylinder footprint 碰撞。
2. cylinder-cuboid footprint 碰撞。
3. cylinder-bin 边界判断。
4. cylinder footprint 面积。
5. cylinder 真实体积。
6. cylinder 与 cuboid 的高度区间重叠判断。

碰撞判断应采用两步：

1. 外接盒粗筛：快速排除明显不相交对象。
2. 真实几何精筛：圆-圆或圆-矩形判断，最终结果以精筛为准。

### 7.4 求解接入

第一阶段主线：

1. 输入解析阶段识别圆柱 item。
2. item 保留真实 `Cylinder` shape。
3. placement 层支持圆柱或通过 shape-aware adapter 保留真实 shape。
4. 候选位置生成可以复用部分长方体候选点，但 feasibility check 必须调用真实圆柱几何判断。
5. layer/block/BLA/DFS 先以最小侵入方式接入真实圆柱 feasibility。
6. placement 输出时保留 `shapeType`、`radius`、`height`、`axis`、`actualVolume`。
7. 渲染 DTO 同时输出真实 shape 字段和旧 renderer 兼容尺寸字段。

### 7.5 可行性语义

1. 边界判断：使用真实圆柱 footprint 和高度。
2. 碰撞判断：使用真实圆形 footprint，不接受外接盒近似。
3. 支撑面积：使用圆柱真实底面面积，混装时使用真实 footprint intersection 的可解释近似。
4. 重量与承重：使用真实 weight。
5. 体积利用率：使用真实圆柱体积。
6. 装载顺序：使用真实 placement 与当前装载方向规则。
7. 旧 renderer 兼容字段不能反向影响求解结果。

### 7.6 风险

1. 现有 `QuantityPlacement3<T : Cuboid<T>>` 对 `Cuboid` 的硬绑定会限制改造方式。
2. 圆-矩形和圆-圆碰撞会让现有 projection/support 逻辑复杂化。
3. 现有 block generator 的网格枚举对圆形 footprint 不天然高效。
4. 支撑面积若第一阶段只做近似，必须清楚标注近似边界。

## 8. 第二阶段方案：圆柱排布优化

第二阶段只建议在第一阶段稳定后实施。

需要支持：

1. 基于圆形 footprint 的候选点生成。
2. 同半径圆柱的行列错位排布启发式。
3. 不同半径圆柱混装候选点。
4. 圆柱与长方体边缘贴合候选点。
5. 圆柱真实支撑面积更精确计算。
6. 与现有 layer generation / block generation 的性能对比。

### 8.1 Layer Generation 委托边界

参考现有 layer generation 设计，具体生成算法不应硬编码在 layer generation 主流程中。目标结构应保持“placer / adapter 负责领域适配，算法通过函数或接口委托出去”：

1. `PatternPlacer` 风格：由外部 `Pattern` 生成 placements，placer 转换为 `BinLayer`。
2. `BlockPlacer` 风格：由外部 block loading algorithm 生成 `Bin<Block>`，placer 切分并转换为 layer。
3. `BLLocalPlacer` / `BLGlobalPlacer` 风格：由外部 BLA 函数处理投影序列，placer 负责候选序列、过滤和并发。
4. `Filler` 风格：由外部 BLA 函数完成填充，service 只负责前后处理和 reduced cost 判断。

圆密排也应按这个模式接入：

```kotlin
interface CirclePackingLayerGenerator<V> {
    suspend fun generate(
        input: CirclePackingLayerGenerationInput<V>
    ): List<CirclePackingLayerCandidate<V>>
}
```

BPP3D layer generation context 负责：

1. 从 item、bin、剩余需求、shadow price 构造 `CirclePackingLayerGenerationInput`。
2. 调用委托的 `CirclePackingLayerGenerator`。
3. 把返回的圆心、半径、物料重量统计转换为 `BinLayer`。
4. 统一执行 feasibility check、deduplicate、reduced cost / objective 过滤。

具体圆密排算法实现可以先放在 `bpp3d-domain-layer-generation-context` 的 `service/circle_packing` 子包中，但必须只通过接口被主流程调用，避免主流程依赖某一个算法类。

如果后续确认圆密排可被 BPP2D、其他装箱问题或通用几何算法复用，则再抽到通用数学库：

1. 纯数值几何版本放 `ospf-kotlin-math`。
2. 带 `Quantity` 单位的几何版本放 `ospf-kotlin-quantities`。
3. BPP3D 特有的 item、weight demand、shadow price、layer candidate adapter 继续留在 BPP3D。

### 8.2 可变半径与重量统计

存在一种业务场景：圆柱半径不是固定 SKU 尺寸，而是可变决策；半径越大，单个圆柱承载的物料重量越大。最终需求也可能不是按 item 数量统计，而是按物料总重量统计。此时 layer generation 不能只枚举固定半径圆柱，还需要在平面圆密排算法外层选择一个“对当前层最有价值”的半径。

建议将该问题拆成两层：

1. 半径-重量模型：给定物料 `m` 和半径 `r`，计算单个圆柱重量 `w_m(r)`。
2. 圆密排模型：给定 bin 底面尺寸、半径 `r` 和圆柱高度，计算可放圆柱数量、坐标和层价值。

如果圆柱高度固定且物料密度稳定，可使用：

```text
w_m(r) = density_m * pi * r * r * height
```

如果重量来自业务标定，不应硬编码平方关系，而应提供单调函数或分段表：

```kotlin
interface CylinderRadiusWeightFunction<V> {
    fun weight(radius: Quantity<V>): Quantity<V>
}
```

### 8.3 最优半径问题

对于单一物料、同一层只使用一个半径的基础版本，可把 layer-local 半径选择写成：

```text
maximize    value_m(r, pattern)
subject to  r_min <= r <= r_max
            pattern in CirclePackingPatterns(bin.width, bin.depth, r)
            n(pattern, r) * w_m(r) <= remainingWeight_m
            n(pattern, r) * w_m(r) <= layerWeightCapacity
```

其中：

1. `r` 是候选半径。
2. `pattern` 是平面圆密排模式，例如矩形排布、六角/错位排布。
3. `n(pattern, r)` 是该半径下该 pattern 可放圆柱数量。
4. `w_m(r)` 是单个圆柱重量。
5. `value_m` 可以是装载重量、reduced cost、shadow price 加权收益或业务目标函数。

如果当前 column generation 使用 shadow price，则 layer 价值应优先对齐既有 reduced cost 语义：

```text
value_m(r, pattern) = shadow_m * n(pattern, r) * w_m(r) - layer_cost(pattern)
```

如果需求是纯重量满足，则可先使用：

```text
value_m(r, pattern) = min(n(pattern, r) * w_m(r), remainingWeight_m)
```

### 8.4 候选半径生成

连续半径选择会形成非线性、非凸、带阶梯函数的优化问题。第一版不建议把连续优化直接放进主求解流程，推荐生成有限候选半径：

1. 业务给定的离散半径列表。
2. `radiusRange + radiusStep` 生成的网格半径。
3. 圆密排临界半径：当 `floor(width / (2r))`、`floor(depth / (2r))` 或六角排布行列数发生变化时的半径。
4. 重量需求临界半径：使 `n(r) * w_m(r)` 接近剩余需求、车辆承重或 layer 承重上限的半径。
5. shadow price 变化时重新排序的 top-k 半径。

候选半径生成后，对每个半径运行圆密排评估，选择 top-k layer candidates 进入后续 layer selection。

### 8.5 圆密排模式

第一版至少评估两类同半径模式：

1. 矩形排布：行距 `2r`，列距 `2r`。
2. 六角/错位排布：相邻行水平错位 `r`，行距 `sqrt(3) * r`。

对于每个模式，必须输出：

1. 半径 `r`。
2. 圆心坐标列表。
3. 圆柱数量 `n`。
4. 单个圆柱重量 `w_m(r)`。
5. 层总重量 `n * w_m(r)`。
6. 真实体积 `n * pi * r * r * height`。
7. 需求统计贡献。

### 8.6 版本边界

基础版本只支持“同一 layer 内同物料、同半径”的可变半径圆柱。以下内容后置：

1. 同一层内多个半径混排。
2. 多物料、多重量函数混排。
3. 连续非线性优化器直接求半径。
4. 与长方体同时参与同一个圆密排优化模型。

这些能力可以在离散候选半径稳定后再扩展。

## 9. 第三阶段方案：通用 shape-aware 支持

第三阶段应视为架构重构，而不是圆柱小功能。

### 9.1 通用抽象

需要引入类似概念：

```kotlin
sealed interface PackingShape3 {
    val type: PackingShapeType
    val boundingWidth: Quantity<InfraScalar>
    val boundingHeight: Quantity<InfraScalar>
    val boundingDepth: Quantity<InfraScalar>
    val actualVolume: Quantity<InfraScalar>
}

data class CuboidShape3(...)
data class VerticalCylinderShape3(...)

data class ShapePlacement3<S : PackingShape3>(
    val shape: S,
    val position: QuantityPoint3,
    val axis: PackingAxis3? = null
)
```

命名不必照搬，但要把“shape”“axis”“placement”“bounding box”“actual geometry”分开。

### 9.2 需要重构的模块

1. `bpp3d-infrastructure`：shape、placement、projection、support、container。
2. `bpp3d-domain-item-context`：Item、Bin、PackageAttribute、DemandStatistics、LoadingOrderCalculator、ItemMerger。
3. `bpp3d-domain-block-loading-context`：SimpleBlockGenerator、DFS、MLHS。
4. `bpp3d-domain-layer-generation-context`：layer 生成与 layer 内 shape 排布。
5. `bpp3d-domain-layer-assignment-context`：需求统计、体积目标、约束映射。
6. `bpp3d-application`：输入输出、DTO adapter、兼容旧 API。
7. `bpp3d-infrastructure/dto/RendererDTO.kt`：渲染数据契约。

## 10. 渲染导出数据修改

渲染导出 DTO 位于：

`ospf-kotlin-framework-bpp3d/bpp3d-infrastructure/src/main/fuookami/ospf/kotlin/framework/bpp3d/infrastructure/dto/RendererDTO.kt`

当前 `RenderLoadingPlanItemDTO` 只能表达长方体：

```kotlin
val width: FltX
val height: FltX
val depth: FltX
val x: FltX
val y: FltX
val z: FltX
```

圆柱支持需要在 BPP3D 侧先扩展导出数据。`E:\workspace\ospf\framework\bpp3d-interface-renderer` 暂不修改，等本仓库 DTO 和输出逻辑稳定后再适配 three.js renderer。

### 10.1 DTO 目标

DTO 必须同时支持：

1. 旧长方体 renderer 继续读取 `width`、`height`、`depth`、`x`、`y`、`z`。
2. 新 renderer 能根据 enum 判断画 cuboid 还是 cylinder。
3. 圆柱可表达 `radius`、`diameter`、`axis`、`actualVolume`、`boundingWidth`、`boundingHeight`、`boundingDepth`。
4. 能区分真实 shape 和兼容用 bounding box，避免把圆柱误认为真实长方体货物。

### 10.2 建议 DTO enum

```kotlin
@Serializable
enum class RenderShapeTypeDTO {
    Cuboid,
    Cylinder
}

@Serializable
enum class RenderAxis3DTO {
    X,
    Y,
    Z
}

@Serializable
enum class RenderAlgorithmShapeTypeDTO {
    Cuboid,
    VerticalCylinder,
    BoundingCuboid
}
```

### 10.3 建议字段

建议在 `RenderLoadingPlanItemDTO` 增加向后兼容字段：

```kotlin
val shapeType: RenderShapeTypeDTO = RenderShapeTypeDTO.Cuboid
val renderShapeType: RenderShapeTypeDTO = shapeType
val algorithmShapeType: RenderAlgorithmShapeTypeDTO = RenderAlgorithmShapeTypeDTO.Cuboid
val radius: FltX? = null
val diameter: FltX? = null
val axis: RenderAxis3DTO? = null
val boundingWidth: FltX? = null
val boundingHeight: FltX? = null
val boundingDepth: FltX? = null
val actualVolume: FltX? = null
```

字段语义：

1. `shapeType`：业务 item 的真实形状，第一阶段取 `Cuboid` 或 `Cylinder`。
2. `renderShapeType`：three.js 应渲染的形状，圆柱取 `Cylinder`。
3. `algorithmShapeType`：算法使用的真实几何语义，竖直圆柱取 `VerticalCylinder`；只有旧兼容、粗筛或调试字段可使用 `BoundingCuboid`。
4. `radius`：圆柱半径。
5. `diameter`：圆柱直径，便于 renderer 不重复推导。
6. `axis`：圆柱轴向，第一阶段圆柱固定为 `Y`。
7. `width`、`height`、`depth`：保留为旧 renderer 兼容尺寸；圆柱场景填外接盒尺寸，但不能代表算法真实可行性语义。
8. `boundingWidth`、`boundingHeight`、`boundingDepth`：显式兼容外接盒尺寸。
9. `actualVolume`：真实形状体积，用于 KPI 或 tooltip。

### 10.4 DTO 兼容原则

1. 不删除现有字段。
2. 新字段必须有默认值或 nullable，避免破坏旧序列化消费者。
3. 旧长方体 item 输出 `shapeType = Cuboid`，圆柱相关字段为 `null`。
4. 圆柱 item 仍填充 `width`、`height`、`depth`，旧 renderer 可暂时按外接盒显示。
5. 新 renderer 适配前，本仓库测试只验证 DTO 可序列化和字段语义，不要求 three.js 已经显示圆柱。

## 11. 实施计划

### 11.1 第一阶段：竖直圆柱真实几何 MVP

1. 梳理 `Item`、`PackageAttribute`、`Bin` 和输入 DTO 中 item 尺寸来源。
2. 定义 `PackingShapeType`、`PackingAxis3`、`PackingAlgorithmShapeType`。
3. 为圆柱定义业务形状描述，轴向固定为 `Axis3.Y`。
4. 实现圆柱真实体积、footprint、外接盒粗筛数据。
5. 改造 placement / feasibility，使圆柱可执行真实边界和碰撞判断。
6. 实现 cylinder-cylinder 和 cylinder-cuboid footprint collision。
7. 修改体积统计，loading rate 使用真实体积。
8. 修改 `RendererDTO.kt`，增加 enum 和 shape metadata。
9. 修改渲染导出 mapper，圆柱输出真实 shape 字段和兼容外接盒字段。
10. 增加单元测试和集成测试。
11. 运行 BPP3D 全模块测试和 geometry boundary 脚本。

### 11.2 第二阶段：排布优化

1. 引入圆形 footprint 候选点生成。
2. 实现同半径圆柱错位排布启发式。
3. 实现圆柱与长方体混装候选点。
4. 优化底部支撑面积计算。
5. 定义 `CirclePackingLayerGenerator` 委托接口。
6. 实现 BPP3D adapter，将圆密排候选转换为 `BinLayer`。
7. 定义可变半径圆柱的半径范围、候选半径和半径-重量函数。
8. 实现基于矩形排布和六角排布的同半径圆密排评估。
9. 实现 layer-local 最优半径选择，输出 top-k layer candidates。
10. 对接重量统计需求和 shadow price 价值计算。
11. 对比第一阶段和优化阶段的装载率差异。

### 11.3 第三阶段：通用 shape-aware 重构

1. 解除 `QuantityPlacement3<T : Cuboid<T>>` 对 `Cuboid` 的硬绑定。
2. 替换矩形投影为通用 footprint。
3. 改造 block/layer/BLA/DFS 的 shape 输入。
4. 更新所有 DTO、PO、mapper、示例和文档。
5. 再适配 `E:\workspace\ospf\framework\bpp3d-interface-renderer`。

## 12. 事项清单

### 12.1 需求确认

- [x] 第一阶段不接受外接盒近似。
- [x] 圆柱默认轴向为 `Axis3.Y`，即高度方向。
- [x] 不允许横放或躺放。
- [x] loading rate 使用真实体积。
- [x] Renderer DTO 新字段使用 enum。
- [x] 可变半径场景中半径和物料重量正相关，layer generation 需要推算候选层最优半径。
- [ ] 确认圆柱是否允许和长方体混装。
- [ ] 确认业务输入中圆柱字段名称和单位。
- [ ] 确认可变半径是连续区间、离散候选列表，还是业务标定表。
- [ ] 确认半径-重量关系使用密度公式还是业务函数。

### 12.2 Infrastructure

- [ ] 定义 shape type / axis / algorithm shape enum。
- [ ] 补齐 `Cylinder` 的真实体积和 footprint 能力。
- [ ] 固定第一阶段 `enabledAxes = listOf(Axis3.Y)`。
- [ ] 实现圆柱外接盒粗筛数据，但不作为最终可行性判断。
- [ ] 实现 cylinder-cylinder footprint collision。
- [ ] 实现 cylinder-cuboid footprint collision。
- [ ] 实现 cylinder-bin boundary check。
- [ ] 补测试覆盖 `Axis3.Y` 竖直圆柱。

### 12.3 Domain Item

- [ ] 扩展 item/package attribute 支持圆柱形状。
- [ ] 保留真实圆柱元数据直到最终输出。
- [ ] 检查 `DemandStatistics` 是否受 shape 类型影响。
- [ ] 检查 `LoadingOrderCalculator` 对竖直圆柱的排序语义。
- [ ] 检查 `ItemMerger` 是否应跳过圆柱合并，或只合并同轴同尺寸圆柱。

### 12.4 Block / Layer / Packing

- [ ] 验证 `SimpleBlockGenerator` 能接入圆柱真实 feasibility。
- [ ] 验证 DFS / MLHS 中空间切分不会把圆柱按外接盒直接判定为最终可行。
- [ ] 验证 layer generation 不丢失圆柱 item 身份。
- [ ] 验证 layer assignment 的统计约束仍按原始 item 计数。
- [ ] 补混装测试：长方体 + 竖直圆柱。
- [ ] 新增圆密排 layer generation 委托接口。
- [ ] 主流程只依赖委托接口，不直接依赖具体圆密排算法实现。
- [ ] 新增圆密排结果到 `BinLayer` 的 adapter。
- [ ] adapter 统一执行 feasibility check、deduplicate 和 reduced cost / objective 过滤。
- [ ] 为可变半径圆柱新增候选半径生成器。
- [ ] 为同半径圆柱新增矩形排布和六角排布评估器。
- [ ] 为重量统计需求新增 layer 价值函数：`n(radius) * weight(radius)`。
- [ ] 在 column generation 场景下接入 shadow price 加权价值。
- [ ] 限制第一版同一 layer 内只使用同物料、同半径圆柱。

### 12.5 Renderer DTO

- [x] 修改 `RendererDTO.kt`，增加 `RenderShapeTypeDTO`、`RenderAxis3DTO`、`RenderAlgorithmShapeTypeDTO`。
- [x] 给 `RenderLoadingPlanItemDTO` 增加 shape metadata。
- [ ] 修改 BPP3D 导出 mapper，圆柱 item 输出 `shapeType = Cylinder`。
- [ ] 圆柱 item 输出 `radius`、`diameter`、`axis = Y`。
- [ ] 圆柱 item 输出 `actualVolume`。
- [ ] 可变半径圆柱按最终求得的半径逐 item 输出，不输出未决策的半径范围。
- [ ] 圆柱 item 保持 `width`、`height`、`depth` 为旧 renderer 兼容尺寸。
- [x] 长方体 item 输出保持兼容。
- [ ] 增加 DTO serialization 测试。
- [ ] 暂不修改 `E:\workspace\ospf\framework\bpp3d-interface-renderer`，只在后续 renderer 任务中消费新增字段。

### 12.6 文档与示例

- [ ] 更新 BPP3D README 或示例，说明圆柱第一阶段是真实竖直圆柱，不是外接盒近似。
- [ ] 添加圆柱输入示例。
- [ ] 添加圆柱渲染 DTO 输出示例。
- [ ] 标注 loading rate 使用真实体积。

## 13. 测试计划

### 13.1 单元测试

1. 竖直圆柱 `axis = Axis3.Y`。
2. 圆柱真实体积为 `pi * radius * radius * height`。
3. `enabledAxes` 不允许 `Axis3.X` 和 `Axis3.Z`。
4. 圆柱边界判断基于真实 footprint。
5. cylinder-cylinder 碰撞。
6. cylinder-cuboid 碰撞。
7. Renderer DTO 对长方体保持默认字段。
8. Renderer DTO 对圆柱输出 enum 和 shape metadata。
9. 半径-重量函数单调性和单位正确。
10. 候选半径生成能覆盖业务给定半径和圆密排临界半径。
11. 同半径矩形排布和六角排布的数量、坐标、边界校验正确。
12. layer generation 主流程可用 mock `CirclePackingLayerGenerator` 验证委托边界。

### 13.2 集成测试

1. 单个竖直圆柱可装入足够大的 bin。
2. 单个竖直圆柱因真实 footprint 或高度超出 bin 被拒绝。
3. 多个竖直圆柱按圆形 footprint 不重叠装载。
4. 竖直圆柱与长方体混装。
5. 圆柱 item 的 demand 统计正确。
6. 圆柱 item 的 loading order 可生成。
7. loading rate 使用真实圆柱体积。
8. 旧长方体用例结果不回退。
9. 可变半径圆柱在重量统计需求下能生成满足需求的 layer candidate。
10. layer generation 能在多个候选半径中选出目标函数最优或 top-k 半径。

### 13.3 回归测试命令

```powershell
mvn -f ospf-kotlin-framework-bpp3d/pom.xml test "-Dgpg.skip=true"
pwsh.exe -File ospf-kotlin-framework-bpp3d/scripts/geometry-boundary-check.ps1
pwsh.exe -File ospf-kotlin-framework-bpp3d/scripts/geometry-module-dry-run.ps1
git diff --check
```

如果只改 infrastructure 和 DTO，可先运行：

```powershell
mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure test "-Dgpg.skip=true"
```

## 14. 验收标准

### 14.1 第一阶段验收

- [ ] BPP3D 可以接收竖直圆柱 item，并能生成装载方案。
- [ ] 圆柱轴向固定为 `Axis3.Y`，横放/躺放输入被拒绝或转换为长方体装载单元。
- [ ] 圆柱边界判断使用真实 footprint 和高度。
- [ ] 圆柱碰撞判断使用圆-圆、圆-矩形真实 footprint，不接受外接盒近似。
- [ ] 圆柱真实 shape metadata 在最终结果中没有丢失。
- [ ] loading rate 使用真实圆柱体积。
- [ ] `RendererDTO.kt` 导出的 item 同时包含兼容旧 renderer 的尺寸字段和新 renderer 所需的圆柱 enum/字段。
- [ ] 旧 three.js renderer 未适配时，仍可按兼容尺寸显示圆柱 item。
- [ ] 长方体-only 测试结果不受影响。
- [ ] 圆柱 + 长方体混装测试通过。
- [ ] DTO serialization 测试通过。
- [ ] BPP3D 全模块测试通过。
- [ ] geometry boundary 和 dry-run 脚本通过。

### 14.2 第二阶段验收

- [ ] 圆柱候选点生成能提高或不降低可行装载率。
- [ ] 竖直圆柱之间的错位排布有测试覆盖。
- [ ] 竖直圆柱与长方体混装候选点有测试覆盖。
- [ ] 可变半径圆柱支持半径范围、离散候选或业务标定表。
- [ ] 圆密排算法通过委托接口接入，主流程不硬编码具体算法。
- [ ] 圆密排 adapter 能把委托结果转换为合法 `BinLayer`。
- [ ] 半径-重量函数参与 layer generation 价值计算。
- [ ] 重量统计需求下，layer candidate 的统计贡献按 `n(radius) * weight(radius)` 计算。
- [ ] 同一 layer 内同物料、同半径的版本边界被测试固定。
- [ ] 连续半径优化未直接进入主求解器，当前实现通过有限候选半径生成 top-k layer candidates。
- [ ] 支撑面积计算结果可解释。
- [ ] 优化模式可通过配置开启，默认不破坏第一阶段行为。

### 14.3 第三阶段验收

- [ ] placement 不再强绑定 `Cuboid<T>`。
- [ ] block/layer/BLA/DFS 能处理通用 shape placement。
- [ ] 渲染 DTO 能稳定表达 cuboid 和 vertical cylinder。
- [ ] `E:\workspace\ospf\framework\bpp3d-interface-renderer` 已按 DTO 新契约完成 three.js 圆柱渲染适配。

## 15. 推荐落地顺序

1. 先做第一阶段竖直圆柱真实几何 MVP。
2. 同步修改 `RendererDTO.kt` 和 BPP3D 导出 mapper，但暂不改 renderer 项目。
3. 第一阶段稳定后，再做固定半径圆柱候选点和排布优化。
4. 固定半径排布稳定后，再引入可变半径候选生成和 layer-local 最优半径选择。
5. 只有当更多 shape 成为明确需求时，才启动通用 shape-aware 重构。

## 16. 决策记录

1. 第一阶段不接受外接盒近似，最终可行性以真实圆柱几何为准。
2. 圆柱默认轴向为 `Axis3.Y`，即高度方向；底面圆在 `X-Z` 平面。
3. 不允许横放或躺放；打卡板后的稳定装载单元按长方体处理。
4. loading rate 使用真实体积。
5. Renderer DTO 新字段使用 enum，并通过 serialization 导出稳定值。
6. 可变半径圆柱按“候选半径生成 + 圆密排评估 + top-k layer candidate”实现，第一版不直接求连续非线性全局最优。
7. 重量统计需求下，半径选择目标函数必须使用物料重量贡献，而不是 item 数量。
8. 圆密排具体算法通过委托接口接入；BPP3D 保留 adapter，纯几何圆密排成熟后可抽到通用数学库。
