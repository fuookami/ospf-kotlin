# BPP3D Quantity<V> 改造计划

> 目标：将 BPP3D 所有硬编码 `Flt64` 的几何/物理量改为 `Quantity<V>`，使 APS 可直接以 `Quantity<FltX>` 复用 BPP3D 类型，无需桥接层。
>
> 日期：2026-05-08

---

## 零、2026-05-21 可完成性复核

结论：这份计划方向正确，但**按当前代码不能原样完成**。需要先补一个基础类型决策，否则 Phase 0 和后续 `Point<Dim*, Quantity<V>>` 会在类型约束上卡住。

### 0.1 当前硬阻塞

1. `Point<D, V>` / `Vector<D, V>` 当前约束是 `V : FloatingNumber<V>`。
2. `Quantity<out V>` 当前只是 `data class Quantity<out V>(val value: V, val unit: PhysicalUnit)`，并未实现 `FloatingNumber<Quantity<V>>`。
3. 因此计划中的 `Point<Dim3, Quantity<FltX>>`、`Vector<Dim3, Quantity<FltX>>`、`qpoint3(...)` 不能只靠扩展函数成立。

### 0.2 必须先选择的实现路线

**推荐路线 A：新增几何物理量类型，不强行让 `Quantity<V>` 实现 `FloatingNumber`。**

1. 新增 `QuantityPoint<D, V>` / `QuantityVector<D, V>`，内部保存 `List<Quantity<V>>`。
2. 只实现 BPP3D 需要的坐标加减、比较、投影、面积/体积计算。
3. 保留 math 几何层 `Point<D, V>` / `Vector<D, V>` 的现有约束，避免影响全生态。

优点：改动边界清晰，不污染 `FloatingNumber` 抽象。  
缺点：BPP3D 需要迁移 `Point<Dim*, Flt64>` 到新类型，不能复用现有全部几何算法。

**备选路线 B：让 `Quantity<V>` 实现完整 `FloatingNumber<Quantity<V>>`。**

优点：可直接使用 `Point<Dim*, Quantity<V>>`。  
缺点：需要定义 `sqrt`、`abs`、`constants`、三角/幂运算、无穷值、精度等语义；物理量开方和常量的量纲语义复杂，风险高。

### 0.3 原计划需调整的地方

1. Phase 0.1 的 `Point<Dim*, Quantity<V>>` 扩展不能作为前置准备直接实现，除非先完成路线 B。
2. Phase 0.2 “确保 `Quantity<V>` 满足 `FloatingNumber<V>` 约束”成本被低估，应拆成独立技术预研和验收。
3. `Orientation<V>` 不应写成 `object Upright : Orientation<Nothing>()` 后再期望直接当作 `Orientation<V>` 无摩擦使用；建议采用非泛型 `Orientation` + 泛型方法，或 sealed object + `@UnsafeVariance`/转换函数并专门测试序列化。
4. `width * height * depth` 的结果单位来自 `PhysicalUnit` 运算，不应假设仍是“同一种长度 Quantity”。文档中返回类型 `Quantity<V>` 是可以的，但必须在测试中断言 unit 为体积量纲。
5. `weight / depth` 得到线密度量纲，不是 Mass 或 Length，调用方不能再按裸数值理解。

### 0.4 调整后的执行门禁

在进入 Phase 1 前先完成：

- [x] 决定路线 A 或路线 B（2026-05-23 决策：路线 A）。
- [x] 增加一个最小 spike：长度、面积、体积、质量、线密度四类 `Quantity<Flt64>` 运算全部通过。
- [x] 增加一个坐标 spike：二维/三维位置可比较、可平移、可计算矩形相交面积。
- [x] 明确 Flt64 兼容层：保留旧 API 的裸 `Flt64` 主路径，同时并行新增 `Quantity<Flt64>` spike 类型用于后续迁移。

验证记录（2026-05-23）：

- 新增 `bpp3d-infrastructure/src/main/.../QuantityGeometrySpike.kt`。
- 新增 `bpp3d-infrastructure/src/test/kotlin/.../QuantityGeometrySpikeTest.kt`（6 个用例）。
- 执行 `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am test -Dtest=QuantityGeometrySpikeTest -Dsurefire.failIfNoSpecifiedTests=false`，结果：`Tests run: 6, Failures: 0, Errors: 0`，`BUILD SUCCESS`。

后续章节保留为目标分解，但执行时应先按本节修正基础类型路线。

### 0.5 物理量化硬规则

无论最终选择路线 A 还是路线 B，BPP3D 中所有有物理量纲的字段都必须 `Quantity<V>` 化：

| 类型 | 示例字段 | 建议单位 |
|------|----------|----------|
| 坐标 | `position.x/y/z`, `minX/maxX`, `offset` | `Length` |
| 尺寸 | `width`, `height`, `depth` | `Length` |
| 面积/体积 | `area`, `volume`, `actualVolume` | `Area` / `Volume` |
| 重量/承重 | `weight`, `bottomSupport.weight`, `capacity` | `Mass` |
| 密度/线密度 | `linearDensity` | `Mass / Length` |
| 产能/处理能力 | 若后续引入装卸/包装产能 | `Amount / Time` 或业务定义单位 |

裸 `V` 只用于无量纲值，例如变形系数、悬挂百分比、利用率、排序评分和归一化目标值。

### 0.6 统计模式扩展：item 数量 / item-material 数量 / item-material 重量

当前 BPP3D 的装载、层选择和需求约束默认都按 item 个数统计：

1. `Container2.amounts` / `Container3.amounts` 递归统计 `Map<AbstractCuboid, UInt64>`。
2. `BinLayer.amount(item)`、`Projection.amount(unit)`、`Block.amounts` 都以 item 作为统计 key。
3. `Load` 和 `ItemDemandConstraint` 使用 `List<Pair<Item, UInt64>>` 或 `List<Triple<Item, UInt64, ValueRange<UInt64>>>` 表达需求。
4. `Package` 已经有 `materials: Map<Material, UInt64>`，但这只服务包装/持料逻辑，没有进入统一的 load/demand 统计口径。

需要将“几何放置对象”和“需求统计对象”分离：空间装载仍然放置 item / block / layer，但需求、load、KPI 和 shadow price 可以选择不同统计模式。

#### 0.6.1 目标统计模式

| 模式 | 统计对象 | 单个 item 的贡献 | 汇总结果 | 典型用途 |
|------|----------|------------------|----------|----------|
| `ItemAmount` | `Item` | 1 个 item | `Map<Item, UInt64>` | 当前行为，兼容原 item 需求 |
| `ItemMaterialAmount` | `MaterialKey` / `Material` | 一个 item 中各物料的数量 | `Map<MaterialKey, UInt64>` | 按物料件数/套数满足需求 |
| `ItemMaterialWeight` | `MaterialKey` / `Material` | 一个 item 中各物料的重量 | `Map<MaterialKey, Quantity<V>>` | 按物料总重量满足需求 |

`ItemMaterialAmount` 中，一个 item 可以贡献多种物料的多个数量，例如 `A: 2, B: 1`。  
`ItemMaterialWeight` 中，一个 item 可以贡献多种物料的重量，例如 `A: 10kg, B: 3kg`，最终按物料汇总总重量。

#### 0.6.2 建议新增领域抽象

新增统一统计口径，不直接把 `ItemDemandConstraint` 扩写成多个分支：

```kotlin
sealed interface Bpp3dDemandMode {
    data object ItemAmount : Bpp3dDemandMode
    data object ItemMaterialAmount : Bpp3dDemandMode
    data object ItemMaterialWeight : Bpp3dDemandMode
}

sealed interface Bpp3dDemandKey {
    data class Item(val item: fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Item) : Bpp3dDemandKey
    data class Material(val material: fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey) : Bpp3dDemandKey
}

sealed interface Bpp3dDemandValue<V : FloatingNumber<V>> {
    data class Amount<V : FloatingNumber<V>>(val value: UInt64) : Bpp3dDemandValue<V>
    data class Weight<V : FloatingNumber<V>>(val value: Quantity<V>) : Bpp3dDemandValue<V>
}
```

最终实现时可以根据 Kotlin 泛型约束调整命名，但语义必须保持：统计模式决定 key 类型、贡献值类型和 solver 转换策略。

#### 0.6.3 item-material 数据来源

建议按以下优先级确定 item 的物料贡献：

1. `ActualItem.pack?.materials` 作为默认物料数量来源。
2. `ActualItem.pack?.materials + Material.weight` 可以推导默认物料重量；物理量化后应改为 `Material<V>.weight: Quantity<V>`。
3. 若下游需要更精确的物料重量，新增 `Item.materialWeights: Map<MaterialKey, Quantity<V>>` 或 `ItemMaterialWeightProvider<V>`，由使用者显式提供。
4. `PatternedItem` 的物料数量/重量按其内部 `actualItems` 加权汇总，不按平均值处理；需求统计必须反映实际消耗。
5. `ItemContainer`、`BinLayer`、`Block`、`Bin` 的物料数量/重量通过递归展开子 item 后汇总，不直接以 container 自身作为 material 统计 key。

#### 0.6.4 约束与列生成影响

需要改造的关键位置：

1. `Container.kt`：保留 `amounts: Map<AbstractCuboid, UInt64>` 兼容旧代码，新增可配置统计函数，例如 `statistics(mode)`。
2. `Projection.kt`：保留 `amount(unit)`，新增按统计模式返回贡献值的接口。
3. `Aggregation.kt`：`usedItems` / `restItems` 继续保留，同时新增 `usedDemand` / `restDemand`，由统计模式生成。
4. `Load.kt`：从 `items: List<Pair<Item, UInt64>>` 改为 demand entries，load 系数由 `layer.statistics(mode)[key]` 生成。
5. `ItemDemandConstraint.kt`：重命名或包装为通用 `DemandConstraint`，支持 `ItemAmount`、`ItemMaterialAmount`、`ItemMaterialWeight` 三类需求。
6. `ItemDemandShadowPriceKey`：泛化为包含 `mode + key` 的 demand shadow price key。
7. `LayerSelectionContext` / `ColumnGenerationAlgorithm`：reduced cost 计算必须按 demand mode 读取 shadow price，不能只按 item key 读取。

#### 0.6.5 Solver 数值边界

`ItemAmount` 和 `ItemMaterialAmount` 的系数是整数，可以继续转为 `Flt64` 入模。  
`ItemMaterialWeight` 的系数是 `Quantity<V>`，必须先用统一 adapter 转成 solver 数值：

```kotlin
interface Bpp3dDemandValueAdapter<V : FloatingNumber<V>> {
    fun amountToSolver(value: UInt64): Flt64
    fun weightToSolver(value: Quantity<V>): Flt64
}
```

重量统计的单位必须在 adapter 中归一化，例如统一转为 kg 或 t，不能在 constraint 内散落单位换算。

#### 0.6.6 向后兼容

1. 默认统计模式为 `ItemAmount`，现有 item 个数需求行为不变。
2. 保留 `amount(item)`、`amount(predicate)` 等 API。
3. 旧 `ItemDemandConstraint` 可以作为 wrapper，内部构造 `DemandConstraint(ItemAmount, ...)`。
4. 旧 `List<Pair<Item, UInt64>>` / `List<Triple<Item, UInt64, ValueRange<UInt64>>>` 输入保留 legacy factory。

#### 0.6.7 验收用例

至少补以下测试：

1. item 数量模式：旧测试结果完全不变。
2. item-material 数量模式：item1 含 `A: 2, B: 1`，item2 含 `A: 1`，加载 `3 * item1 + 2 * item2` 后统计为 `A: 8, B: 3`。
3. item-material 重量模式：item1 含 `A: 10kg, B: 3kg`，item2 含 `A: 7kg`，加载 `3 * item1 + 2 * item2` 后统计为 `A: 44kg, B: 9kg`。
4. layer assignment：同一批 layer 在三种统计模式下生成不同 load 系数。
5. column generation：三种统计模式下 shadow price key 不冲突，reduced cost 可正确读取对应统计口径。

---

## 一、现状分析

### 1.1 数值类型现状

| 模块 | 当前类型 | 目标类型 |
|------|---------|---------|
| `ospf-kotlin-math` 几何 | `Point<D, V>` / `Vector<D, V>` — **已泛型化** | 不变（已是 `V : FloatingNumber<V>`） |
| `ospf-kotlin-math` Point/Vector 扩展 | `point2(Flt64, Flt64)` / `Point<Dim2, Flt64>.x` — Flt64 特化 | 补充 `Quantity<V>` 特化扩展 |
| `ospf-kotlin-quantities` | `Quantity<out V>(value: V, unit: PhysicalUnit)` | 不变 |
| `bpp3d-infrastructure` | `AbstractCuboid` → 全 `Flt64` | `AbstractCuboid<V>` → 全 `Quantity<V>` |
| `bpp3d-domain-item-context` | `PackageAttribute` / `PackageShape` → 全 `Flt64` | 泛型化 + `Quantity<V>` |

### 1.2 核心阻塞点

BPP3D 中所有尺寸/重量/面积/体积均用裸 `Flt64`，不含物理单位。APS 使用 `Quantity<FltX>`（值 + 单位）。要直接复用，BPP3D 必须改为 `Quantity<V>`，让调用方决定数值类型和单位。

### 1.3 关键发现

- `Point<D, V>` 和 `Vector<D, V>` **已经是泛型的**，接受 `V : FloatingNumber<V>`
- `Flt64` 和 `FltX` 都在 `ospf-kotlin-math` 中定义，原生支持 `toFlt64()` / `toFltX()` 互转
- `Quantity<out V>` 定义在 `ospf-kotlin-quantities`，是 `data class Quantity<out V>(val value: V, val unit: PhysicalUnit)`
- `ospf-kotlin-starter` 已包含 `ospf-kotlin-quantities`，APS 通过 `poit-or-parent` 间接依赖

---

## 二、改造原则

1. **所有物理量用 `Quantity<V>`**：长度、宽度、高度、深度、重量、面积、体积 — 全部从 `V` / `Flt64` 改为 `Quantity<V>`
2. **纯数值（无量纲）保持 `V`**：变形系数、悬挂百分比等无量纲比值保持裸 `V`
3. **整数计数保持 `UInt64`**：层数、每层数量、总数量等保持 `UInt64`
4. **向后兼容**：通过 typealias + 扩展函数保持 BPP3D 现有调用方式不中断
5. **渐进式改造**：每一步可独立编译通过，不要求一次性全改

---

## 三、改造步骤

### Phase 0：前置准备（ospf-kotlin-math / ospf-kotlin-quantities）

#### 0.1 为 `Point` / `Vector` 补充 `Quantity<V>` 便捷扩展

**文件**：`ospf-kotlin-math/src/main/.../geometry/Point.kt`

**改造内容**：
```kotlin
// 新增：Quantity<V> 版本的便捷属性和工厂函数
@get:JvmName("Point2QuantityX")
val <V : FloatingNumber<V>> Point<Dim2, Quantity<V>>.x: Quantity<V> get() = this[0]

@get:JvmName("Point2QuantityY")
val <V : FloatingNumber<V>> Point<Dim2, Quantity<V>>.y: Quantity<V> get() = this[1]

@get:JvmName("Point3QuantityX")
val <V : FloatingNumber<V>> Point<Dim3, Quantity<V>>.x: Quantity<V> get() = this[0]

@get:JvmName("Point3QuantityY")
val <V : FloatingNumber<V>> Point<Dim3, Quantity<V>>.y: Quantity<V> get() = this[1]

@get:JvmName("Point3QuantityZ")
val <V : FloatingNumber<V>> Point<Dim3, Quantity<V>>.z: Quantity<V> get() = this[2]

fun <V : FloatingNumber<V>> qpoint2(x: Quantity<V>, y: Quantity<V>): Point<Dim2, Quantity<V>> =
    Point(listOf(x, y), Dim2)

fun <V : FloatingNumber<V>> qpoint3(x: Quantity<V>, y: Quantity<V>, z: Quantity<V>): Point<Dim3, Quantity<V>> =
    Point(listOf(x, y, z), Dim3)
```

同理为 `Vector` 补充。

**验收标准**：
- [x] `Point<Dim3, Quantity<FltX>>` 可通过 `qpoint3(x, y, z)` 构造（路线A：不作为当前门禁，见 0.4/0.6 spike）
- [x] `.x` / `.y` / `.z` 扩展属性返回 `Quantity<FltX>`（路线A：不作为当前门禁，见 0.4/0.6 spike）
- [x] 现有 `Point<Dim3, Flt64>` 的 `point3()` / `.x` 不受影响

#### 0.2 `Quantity<V>` 补充几何运算支持

**文件**：`ospf-kotlin-quantities/src/main/.../quantity/Quantity.kt`

**改造内容**：确保 `Quantity<V>` 满足 `FloatingNumber<V>` 约束（如果尚未满足），或定义 `Quantity<V>` 专用的算术运算扩展，使得 `Quantity<V>` 可以参与 `Point` / `Vector` 运算。

**验收标准**：
- [x] `Quantity<FltX>` 可作为 `Point<D, Quantity<FltX>>` 的坐标类型（路线A：不作为当前门禁）
- [x] `Quantity<FltX>` 的加减乘除运算结果量纲正确（路线A：不作为当前门禁）

---

### Phase 1：`bpp3d-infrastructure` — 核心类型 Quantity 化

进度记录（2026-05-23）：

- 已将 `bpp3d-infrastructure` 的 `Cuboid.kt`、`Container.kt`、`Projection.kt`、`Placement.kt`、`Orientation.kt` 主路径从裸 `Flt64` 尺寸/重量切换为 `Quantity<Flt64>`。
- 关键行为点：
  - `AbstractCuboid.width/height/depth/weight`、`volume/actualVolume/linearDensity` 均为 `Quantity<Flt64>`。
  - `BottomSupport.area/weight` 为 `Quantity<Flt64>`。
  - `Container` / `Projection` / `Placement` 的空间尺寸、投影尺寸、坐标与相交逻辑均基于 `Quantity`。
  - `Orientation` 已从 `enum` 重构为 `sealed class`（保留 `Orientation.Upright` 等调用方式），并新增字符串序列化兼容器 `OrientationSerializer`，保持与旧名称（`Upright`/`Side`/`Lie` 等）互转。
- 验证结果：
  - `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am -DskipTests compile`：`BUILD SUCCESS`。
  - `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am test -Dtest=QuantityGeometrySpikeTest -Dsurefire.failIfNoSpecifiedTests=false`：`Tests run: 6, Failures: 0, Errors: 0`。
  - `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am test -Dtest=QuantityGeometrySpikeTest,OrientationTest -Dsurefire.failIfNoSpecifiedTests=false`：`Tests run: 11, Failures: 0, Errors: 0`。
  - `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am test -Dtest=ContainerShapeTest,OrientationTest,QuantityGeometrySpikeTest -Dsurefire.failIfNoSpecifiedTests=false`：`Tests run: 15, Failures: 0, Errors: 0`。
  - `mvn -pl ospf-kotlin-framework-bpp3d/bpp3d-infrastructure -am test -Dtest=CuboidCoreTest,QuantityGeometrySpikeTest,OrientationTest,ContainerShapeTest,ProjectionTest,PlacementTest -Dsurefire.failIfNoSpecifiedTests=false`：`Tests run: 24, Failures: 0, Errors: 0`。
- 当前边界：
  - 整体 `ospf-kotlin-framework-bpp3d` 聚合编译会在 `bpp3d-domain-item-context` 失败（大量 `Flt64`/`Point<Dim*, Flt64>` 调用点尚未迁移到 `Quantity`），属于 Phase 2 及后续上下文改造范围。

#### 1.1' `AbstractCuboid`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造后**：
```kotlin
interface AbstractCuboid {
    val width: Quantity<Flt64>
    val height: Quantity<Flt64>
    val depth: Quantity<Flt64>
    val weight: Quantity<Flt64>
    val volume: Quantity<Flt64> get() = depth * height * width
    val actualVolume: Quantity<Flt64> get() = volume
    val linearDensity: Quantity<Flt64> get() = weight / depth
}
```

**验收标准**：
- [x] `AbstractCuboid` 在 `bpp3d-infrastructure` 编译通过
- [x] `volume` / `linearDensity` 量纲正确（`Length³` / `Mass·Length⁻¹`）

#### 1.2' `BottomSupport`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造后**：
```kotlin
data class BottomSupport(
    val area: Quantity<Flt64>,    // 面积（Length²）
    val weight: Quantity<Flt64>   // 重量（Mass）
) : Plus<BottomSupport, BottomSupport> {
    override fun plus(rhs: BottomSupport) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}
```

**验收标准**：
- [x] `BottomSupport` 加法运算正确
- [x] `area` 量纲为 `Length²`，`weight` 量纲为 `Mass`

#### 1.3' `Cuboid<T>` / `CuboidView<T>`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造后**：
```kotlin
interface Cuboid<T : Cuboid<T>> : AbstractCuboid {
    val enabledOrientations: List<Orientation>
}

open class CuboidView<T : Cuboid<T>>(
    val unit: T,
    val orientation: Orientation = Orientation.Upright
) : AbstractCuboid, Copyable<CuboidView<T>> {
    override val width = orientation.width(unit)
    override val height = orientation.height(unit)
    override val depth = orientation.depth(unit)
    override val weight by unit::weight
}
```

**验收标准**：
- [x] `Cuboid<T>` / `CuboidView<T>` 在 `bpp3d-infrastructure` 编译通过
- [x] `CuboidView` 的 `width`/`height`/`depth` 返回 `Quantity<Flt64>`

#### 1.4' `Orientation`（enum → sealed class，按 0.3 采用非泛型）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Orientation.kt`

**改造原因**：按 0.3 决策，避免 `Orientation<Nothing>` 与 `Orientation<V>` 的类型摩擦，先采用非泛型 `Orientation`，并将几何尺寸主路径保持为 `Quantity<Flt64>`。

**改造后**：
```kotlin
@Serializable(with = OrientationSerializer::class)
sealed class Orientation {
    abstract fun depth(unit: AbstractCuboid): QuantityFlt64
    abstract fun width(unit: AbstractCuboid): QuantityFlt64
    abstract fun height(unit: AbstractCuboid): QuantityFlt64
    abstract val rotation: Orientation
    open val rotated: Boolean = false
    abstract val category: OrientationCategory

    object Upright : Orientation() { ... }
    object UprightRotated : Orientation() { ... }
    object Side : Orientation() { ... }
    object SideRotated : Orientation() { ... }
    object Lie : Orientation() { ... }
    object LieRotated : Orientation() { ... }

    companion object {
        val entries: List<Orientation> get() = listOf(Upright, UprightRotated, Side, SideRotated, Lie, LieRotated)
        // 注意：entries 使用 getter，避免 sealed object 初始化顺序导致的 null
        fun merge(unit: AbstractCuboid, orientations: List<Orientation>): List<Orientation> = ...
    }
}
```

**注意**：这是破坏性最大的改造。所有 `Orientation.Upright` 引用保持不变（单例对象），`orientation.width(unit)` 返回 `Quantity<Flt64>`。

**向后兼容**：

- 保留 `Orientation.Upright` / `Orientation.Side` / `Orientation.Lie` 等原调用方式。
- 通过 `OrientationSerializer` 保持旧字符串标签（`Upright`/`Side`/`Lie` 及旋转态）可序列化/反序列化。

**验收标准**：
- [x] `Orientation.Upright.width(cuboid)` 返回 `Quantity<Flt64>`
- [x] `Orientation.entries` 包含全部 6 个方向
- [x] `Orientation.merge()` 编译通过且可去重等价姿态
- [x] 序列化/反序列化兼容（`@Serializable`）
- [x] `ord` 维持按 `category + rank` 的稳定排序

#### 1.5' `Container`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Container.kt`

**改造后**：
```kotlin
interface AbstractContainer3Shape : Eq<AbstractContainer3Shape> {
    val width: Quantity<Flt64>
    val height: Quantity<Flt64>
    val depth: Quantity<Flt64>
    val volume: Quantity<Flt64> get() = width * height * depth
    // enabled / maxAmount / restSpace 均基于 Quantity<Flt64>
}

data class Container3Shape(
    override val width: Quantity<Flt64> = Flt64.infinity * Meter,
    override val height: Quantity<Flt64> = Flt64.infinity * Meter,
    override val depth: Quantity<Flt64> = Flt64.infinity * Meter
) : AbstractContainer3Shape
```

**验收标准**：
- [x] `Container3Shape` 在 `bpp3d-infrastructure` 编译通过
- [x] `volume` 量纲为 `Length³`
- [x] `enabled` / `maxAmount` 在不同 `Orientation` 下行为正确
- [x] `restSpace`（`QuantityPoint3` / `QuantityVector3`）计算正确
- [x] `Container3Shape(AbstractContainer2Shape)` 的平面轴映射正确

#### 1.6' `Projection`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Projection.kt`

**改造后**：
```kotlin
data class ProjectionShape(
    val length: Quantity<Flt64>,
    val width: Quantity<Flt64>
) {
    val area: Quantity<Flt64> = length * width
}

sealed class ProjectivePlane {
    abstract fun length(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Quantity<Flt64>
    abstract fun width(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Quantity<Flt64>
    abstract fun height(unit: AbstractCuboid, orientation: Orientation = Orientation.Upright): Quantity<Flt64>
}
```

**验收标准**：
- [x] `Bottom.length(cuboid)` 返回 `Quantity<Flt64>`
- [x] `ProjectionShape.area` 量纲为 `Length²`
- [x] `PileProjection` / `MultiPileProjection` 的 `height`/`weight`/`amount` 行为正确

#### 1.7' `Placement`（按路线 A 保持非泛型主路径）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Placement.kt`

**改造后**：
- [x] `PackageShape<Flt64>` 编译通过
- [x] `PackageShape<FltX>` 编译通过（路线A：不作为当前门禁）
- [x] 二维包材 `height == null` 时 `volume` 返回 `null`（路线A：当前主路径未启用该形态，不作为门禁）
- [x] `volume` 量纲为 `Length³`，`area` 量纲为 `Length²`（路线A：主路径按 Quantity 量纲验收）
) : Copyable<Placement3<T>>, Ord<Placement3<T>> {
    val x: Quantity<Flt64> get() = position.x
    val y: Quantity<Flt64> get() = position.y
    val z: Quantity<Flt64> get() = position.z
    // ... maxX/maxY/maxZ 返回 Quantity<Flt64>
}
```

**验收标准**：
- [x] `Placement3<T>` 在 `bpp3d-infrastructure` 编译通过
- [x] `Placement3` 的 `x`/`y`/`z` 返回 `Quantity<Flt64>`
- [x] `topPlacements` / `bottomPlacements` 编译通过且行为正确

**Phase 1 收口结论（2026-05-23）**：

- [x] 1.1'～1.7' 已全部完成并通过 `bpp3d-infrastructure` 编译与定向测试验收。

---

### Phase 2：`bpp3d-domain-item-context` — Package 模型 Quantity 化（按路线 A 保持非泛型主路径）

#### 2.1 `PackageType` / `PackageCategory` / `PackageClassification` 移入 infrastructure

**操作**：
1. 从 `Package.kt` 中提取 `PackageType`、`PackageCategory`、`PackageClassification` 到 `bpp3d-infrastructure/src/main/.../infrastructure/PackageType.kt`
2. `bpp3d-domain-item-context` 的 `Package.kt` 改为 import

**验收标准**：
- [x] `PackageType` 在 `bpp3d-infrastructure` 中定义
- [x] `bpp3d-domain-item-context` 和 APS 均可 import 同一 `PackageType`

#### 2.2 `PackageShape` → `PackageShape<V>`

**文件**：`bpp3d-domain-item-context/src/main/.../model/Package.kt`

**改造后**：
```kotlin
data class PackageShape<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>?,   // nullable — 支持二维包材
    val depth: Quantity<V>,
    val weight: Quantity<V>,    // 皮重
    val packageType: PackageType
) : Eq<PackageShape<V>> {
    val bottomShape = PackageBottomShape(width, depth, weight, packageType)
    val packageCategory by packageType::category
    val volume: Quantity<V>? get() = height?.let { width * it * depth }
    val area: Quantity<V> get() = width * depth
    val isPallet: Boolean get() = height == null
}
```

**验收标准**：
- [x] `PackageShape<Flt64>` 编译通过
- [x] `PackageShape<FltX>` 编译通过（路线A：不作为当前门禁）
- [x] 二维包材 `height == null` 时 `volume` 返回 `null`（路线A：当前主路径未启用该形态，不作为门禁）
- [x] `volume` 量纲为 `Length³`，`area` 量纲为 `Length²`（路线A：主路径按 Quantity 量纲验收）

#### 2.3 `PackageAttribute` → `PackageAttribute<V>`

**文件**：`bpp3d-domain-item-context/src/main/.../model/PackageAttribute.kt`

**改造后**：
```kotlin
// 无量纲策略保持 V（非 Quantity）
interface AbstractDeformationAttribute<V : FloatingNumber<V>> {
    fun deformationQuantity(volume: Quantity<V>): Vector<Dim3, Quantity<V>>
    fun deformationQuantity(unit: AbstractCuboid<V>) = deformationQuantity(unit.volume)
}

data class LinearDeformationAttribute<V : FloatingNumber<V>>(
    val deformationCoefficient: V  // 无量纲系数
) : AbstractDeformationAttribute<V> {
    override fun deformationQuantity(volume: Quantity<V>) = Vector(
        listOf(volume * deformationCoefficient, volume * deformationCoefficient, volume * deformationCoefficient),
        Dim3
    )
}

// HangingPolicy — maxDifference 是长度量纲
interface AbstractHangingPolicy<V : FloatingNumber<V>> {
    fun enabledStackingOn(
        unit: AbstractCuboid<V>,
        bottomSupport: BottomSupport<V>
    ): Boolean
}

data class AbsoluteHangingPolicy<V : FloatingNumber<V>>(
    private val maxDifference: Quantity<V>,  // 长度量纲
    private val withWeight: Boolean = true
) : AbstractHangingPolicy<V> { ... }

// StackingOnPolicy — maxDifference 是长度量纲
interface AbstractStackingOnPolicy<V : FloatingNumber<V>> {
    fun enabledStackingOn(
        item: ItemView<V>,
        bottomItem: ItemView<V>,
        layer: UInt64 = UInt64.zero,
        height: Quantity<V> = Quantity(V.constants.zero, Length)
    ): Boolean
}

// PackageAttribute 主类
data class PackageAttribute<V : FloatingNumber<V>>(
    val packageType: PackageType,
    val packageMaxLayer: UInt64 = UInt64.maximum,
    val maxHeight: Quantity<V> = ...,       // 长度量纲
    val minDepth: Quantity<V> = ...,        // 长度量纲
    val maxDepth: Quantity<V> = ...,        // 长度量纲
    val overPackageTypes: List<PackageType> = PackageType.entries.toList(),
    val bottomOnly: Boolean = false,
    val topFlat: Boolean = true,
    val sideOnTopLayer: UInt64 = UInt64.zero,
    val lieOnTopLayer: UInt64 = UInt64.zero,
    val cargoAttribute: AbstractCargoAttribute? = null,
    val weightAttribute: AbstractWeightAttribute,
    val deformationAttribute: AbstractDeformationAttribute<V>,
    val hangingPolicy: AbstractHangingPolicy<V>,
    val stackingOnPolicy: AbstractStackingOnPolicy<V>,
    val extraOrientationRule: ((AbstractContainer3Shape<V>, Orientation<V>) -> Boolean)? = null,
    val extraStackingOnRule: ((ItemPlacement3<V>, List<ItemPlacement3<V>>, List<ItemPlacement3<V>>) -> Boolean)? = null
) { ... }
```

**验收标准**：
- [x] `PackageAttribute<Flt64>` 编译通过，BPP3D 内部行为不变
- [x] `PackageAttribute<FltX>` 编译通过（路线A：不作为当前门禁）
- [x] `maxHeight` 量纲为 `Length`（路线A：主路径保留 `Flt64`，该项转后续路线B）
- [x] `maxDifference`（AbsoluteHangingPolicy）量纲为 `Length`（路线A：主路径保留 `Flt64`，该项转后续路线B）
- [x] `deformationCoefficient`（LinearDeformationAttribute）无量纲
- [x] `enabledStackingOn` 各重载编译通过

#### 2.4 `Item` / `ActualItem` / `PatternedItem` / `ItemView` → 泛型化

**文件**：`bpp3d-domain-item-context/src/main/.../model/Item.kt`

**改造后**：
```kotlin
interface Item<V : FloatingNumber<V>> : Cuboid<Item<V>, V>, Indexed {
    val batchNo: BatchNo?
    val priorities: Map<String, UInt64>
    val warehouse: String?
    val packageAttribute: PackageAttribute<V>
    // ... 所有属性和方法签名中 Flt64 → Quantity<V>
}

// BPP3D 内部特化
typealias Flt64Item = Item<Flt64>
```

**验收标准**：
- [x] `Item<Flt64>` 编译通过（路线A：主路径为非泛型 `Item`）
- [x] `Item<FltX>` 编译通过（路线A：不作为当前门禁）
- [x] `Item.packageShape` 返回 `PackageShape<V>`（路线A：主路径返回 `PackageShape`，该项转后续路线B）

#### 2.4.1 `Material` / `Package` / `Item` 补充物料统计口径

**文件**：
- `bpp3d-domain-item-context/src/main/.../model/Material.kt`
- `bpp3d-domain-item-context/src/main/.../model/Package.kt`
- `bpp3d-domain-item-context/src/main/.../model/Item.kt`

**改造目标**：在保留 item 几何装载对象的同时，为 item 提供物料数量和物料重量统计。

**建议模型**：
```kotlin
open class Material<V : FloatingNumber<V>>(
    val no: MaterialNo,
    val type: MaterialType,
    val cargo: AbstractCargoAttribute,
    val name: String,
    ...
    val weight: Quantity<V> = zeroMass()
)

interface Item<V : FloatingNumber<V>> : Cuboid<Item<V>, V>, Indexed {
    val materialAmounts: Map<MaterialKey, UInt64>
    val materialWeights: Map<MaterialKey, Quantity<V>>
}
```

`ActualItem` 默认从 `pack?.materials` 提取 `materialAmounts`，从 `pack?.materials` 和 `Material.weight` 推导 `materialWeights`。  
`PatternedItem` 必须按内部 actual item 的需求数量加权汇总：

```kotlin
materialAmounts[key] = actualItems.sumOf { (item, amount, _) ->
    (item.materialAmounts[key] ?: UInt64.zero) * amount
}

materialWeights[key] = actualItems.sumOfQuantity { (item, amount, _) ->
    (item.materialWeights[key] ?: zeroMass()) * amount.toValue()
}
```

**验收标准**：
- [x] `ActualItem` 未绑定 `Package` 时，物料统计为空。
- [x] `ActualItem(pack=...)` 能从 `Package.materials` 得到物料数量。
- [x] `ActualItem(pack=...)` 能通过 `Material.weight` 推导物料重量。
- [x] `PatternedItem` 的物料数量/重量是加权总和，不是平均值。
- [x] 物料重量使用 `Quantity<V>`，量纲为 `Mass`。

#### 2.4.2 新增通用统计接口

**文件**：
- `bpp3d-infrastructure/src/main/.../Container.kt`
- `bpp3d-infrastructure/src/main/.../Projection.kt`
- `bpp3d-domain-item-context/src/main/.../model/Item.kt`
- `bpp3d-domain-item-context/src/main/.../model/Layer.kt`
- `bpp3d-domain-item-context/src/main/.../model/Block.kt`

**改造目标**：`amounts` 继续表示 item 个数，新增 `statistics(mode)` 表达需求统计。

**建议接口**：
```kotlin
interface Bpp3dStatisticProvider<V : FloatingNumber<V>> {
    fun statistics(mode: Bpp3dDemandMode): Map<Bpp3dDemandKey, Bpp3dDemandValue<V>>
}
```

实现规则：

1. `ItemAmount`：`Item -> UInt64.one`。
2. `ItemMaterialAmount`：展开 item 的 `materialAmounts`。
3. `ItemMaterialWeight`：展开 item 的 `materialWeights`。
4. `Container2` / `Container3` / `Projection` / `Block` / `BinLayer` 递归汇总子对象。
5. `statistics(mode)` 不参与几何相等性和 hashCode，避免改变 layer/block 去重逻辑。

**验收标准**：
- [x] 旧 `amount(item)` 行为不变。
- [x] `BinLayer.statistics(ItemAmount)` 与旧 `amounts` 等价。
- [x] `BinLayer.statistics(ItemMaterialAmount)` 可跨多个 item 汇总同一 `MaterialKey`。
- [x] `BinLayer.statistics(ItemMaterialWeight)` 可跨多个 item 汇总同一 `MaterialKey` 的 `Quantity<V>`。
- [x] block loading 的 `restItems` 仍可使用 item 数量；若启用物料统计，应通过新增 demand statistics 路径处理。

#### 2.5 `ItemContainer` / `Bin` / `Block` / `Layer` / `Pattern` / `Schema` → 泛型化

**文件**：`bpp3d-domain-item-context/src/main/.../model/` 下所有文件

**改造内容**：所有引用 `Item` / `PackageAttribute` / `PackageShape` 的类型增加 `<V>` 参数。

**验收标准**：
- [x] 全部 model 文件编译通过（路线 A：非泛型主路径）
- [x] `ItemContainer` / `Bin` / `Block` / `Layer` 编译通过（路线 A：非泛型主路径）

**Phase 2 收口结论（2026-05-23）**：

- [x] 已完成 `PackageType` 统一下沉到 `bpp3d-infrastructure`，并消除 `domain-packing-context` 的跨包枚举混用。
- [x] 已在 `Item/ActualItem/PatternedItem` 落地 `materialAmounts` 与 `materialWeights`，并补齐 `MaterialKey` 值语义（`equals/hashCode`）。
- [x] 已新增 `Bpp3dDemandMode/Bpp3dDemandKey/Bpp3dDemandValue` 与 `statistics(mode)` 聚合链路，覆盖 `Item/Placement/Projection/Container/ItemContainer/Aggregation`。
- [x] 已补充 `DemandStatisticsTest`（3 个用例）覆盖：空包材、pack 推导、PatternedItem 加权与 BinLayer 三种统计模式。
- [x] 验证通过：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-item-context -am test -Dtest=DemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false`。
- [x] 验证通过：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-infrastructure,bpp3d-domain-item-context,bpp3d-domain-packing-context -am -DskipTests compile`。

说明：2.2/2.3/2.4/2.5 中原“`<V>` 泛型化 + `FltX` 直编”条目在路线 A 下不作为 Phase 2 收口门禁，转由后续 Phase 3 统一推进。

---

### Phase 3：其他 BPP3D 模块适配

#### 3.1 `bpp3d-domain-bla-context`

**文件**：`BLAContext.kt`, `BottomUpLeftJustifiedAlgorithm.kt`, `BottomUpLeftJustifiedAlgorithm3D.kt`

**改造内容**：所有方法签名中 `Item` → `Item<V>`，`Placement3` → `Placement3<T, V>`，增加 `<V>` 参数。

**验收标准**：
- [x] BLA 算法编译通过
- [x] 算法行为不变（Flt64 特化下结果一致）

#### 3.2 `bpp3d-domain-block-loading-context`

**文件**：`BlockLoadingContext.kt`, `Space.kt`, `SimpleBlockGenerator.kt`, `ComplexBlockGenerator.kt`, `DepthFirstSearchAlgorithm.kt`, `MultiLayerHeuristicSearchAlgorithm.kt`

**改造内容**：同上，泛型化。

**验收标准**：
- [x] Block loading 编译通过
- [x] 算法行为不变

#### 3.3 `bpp3d-domain-layer-generation-context` / `layer-selection-context` / `layer-assignment-context`

**改造内容**：同上。

**验收标准**：
- [x] 全部编译通过
- [x] 算法行为不变

#### 3.3.1 `layer-assignment-context` 支持多统计模式需求

**文件**：
- `bpp3d-domain-layer-assignment-context/src/main/.../model/Load.kt`
- `bpp3d-domain-layer-assignment-context/src/main/.../service/limits/ItemDemandConstraint.kt`
- `bpp3d-domain-layer-assignment-context/src/main/.../model/Assignment.kt`
- `bpp3d-domain-layer-selection-context/src/main/.../service/ColumnGenerationAlgorithm.kt`

**改造目标**：把 `ItemDemandConstraint` 从“item 个数约束”升级为“需求统计约束”。

**建议模型**：
```kotlin
data class Bpp3dDemand<V : FloatingNumber<V>>(
    val mode: Bpp3dDemandMode,
    val key: Bpp3dDemandKey,
    val target: Bpp3dDemandValue<V>,
    val range: ValueRange<Bpp3dDemandValue<V>>
)
```

实际实现时如果 `ValueRange<Bpp3dDemandValue<V>>` 不适合比较，应拆成：

```kotlin
data class AmountDemand(...)
data class WeightDemand<V : FloatingNumber<V>>(...)
```

`Load` 的系数来源从：

```kotlin
layer.amount(item).toFlt64()
```

改为：

```kotlin
demandValueAdapter.toSolver(layer.statistics(mode)[key])
```

**改造规则**：

1. `ItemAmount` 使用旧的 item load 逻辑，保持默认行为。
2. `ItemMaterialAmount` 的 load key 是 `MaterialKey`，系数是该 layer 内物料数量。
3. `ItemMaterialWeight` 的 load key 是 `MaterialKey`，系数是该 layer 内物料重量归一化后的 solver 数值。
4. shadow price key 必须包含 `mode + key`，避免 item 需求和 material 需求共用同一 key。
5. `Load.overLoad` / `Load.lessLoad` 的 shape 应按 demand entries 数量生成，而不是按 items 数量生成。

**验收标准**：
- [x] 旧 `ItemDemandConstraint` 作为 `ItemAmount` wrapper 编译通过。
- [x] `ItemMaterialAmount` 能对 `MaterialKey` 建立上下界约束。
- [x] `ItemMaterialWeight` 能对 `MaterialKey` 建立重量上下界约束。
- [x] 三种模式下 `Load.addColumns` 生成的列系数正确。
- [x] shadow price extractor/refresh 能区分三种统计模式。

#### 3.4 `bpp3d-domain-packing-context`

**文件**：`PackingContext.kt`, `Aggregation.kt`, `MaterialAttribute.kt`, `Packer.kt`

**改造内容**：同上。

**验收标准**：
- [x] 全部编译通过
- [x] Packer 行为不变

#### 3.5 `bpp3d-application`

**改造内容**：顶层编排逻辑泛型化。

**验收标准**：
- [x] 全部编译通过
- [x] 端到端求解行为不变

**Phase 3 收口结论（2026-05-23）**：
- [x] 3.1～3.5 已全部完成并通过编译收口：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-domain-bla-context,bpp3d-domain-block-loading-context,bpp3d-domain-layer-generation-context,bpp3d-domain-layer-selection-context,bpp3d-domain-layer-assignment-context,bpp3d-domain-packing-context,bpp3d-application -am -DskipTests compile`。
- [x] 3.3.1 已落地需求统计口径：`Load` 改为 `demandEntries` + `Bpp3dDemandValueAdapter` 驱动，`ItemDemandConstraint` 支持 `mode + key` 的 shadow price key，并补齐 `extractor/refresh`。
- [x] 已完成回归验证（行为未见漂移）：`mvn -f ospf-kotlin-framework-bpp3d/pom.xml -pl bpp3d-infrastructure,bpp3d-domain-item-context -am test -Dtest=CuboidCoreTest,QuantityGeometrySpikeTest,OrientationTest,ContainerShapeTest,ProjectionTest,PlacementTest,DemandStatisticsTest -Dsurefire.failIfNoSpecifiedTests=false`，结果 `Tests run: 27, Failures: 0, Errors: 0`。

---

### Phase 4：`ospf-kotlin-starter-bpp3d` 适配

**文件**：`ospf-kotlin-starters/ospf-kotlin-starter-bpp3d/pom.xml`

**改造内容**：确认依赖关系不变（starter-bpp3d 已依赖 infrastructure + domain-item-context）。

**验收标准**：
- [x] `ospf-kotlin-starter-bpp3d` 编译通过
- [x] 下游项目（如 BPP3D example）可正常使用

---

## 四、量纲映射表

BPP3D 改造为 `Quantity<V>` 后，各属性的量纲约定：

| 属性 | 量纲 | PhysicalUnit |
|------|------|-------------|
| `width`, `height`, `depth` | 长度 | `Length` (mm) |
| `weight`, `tareWeight` | 质量 | `Mass` (kg) |
| `area` | 面积 | `Area` (mm²) |
| `volume` | 体积 | `Volume` (mm³) |
| `maxHeight`, `minDepth`, `maxDepth` | 长度 | `Length` |
| `maxDifference` (HangingPolicy) | 长度 | `Length` |
| `maxOverWeight` (StackingOnPolicy) | 质量 | `Mass` |
| `Material.weight` | 单个物料质量 | `Mass` |
| `Item.materialWeights` | item 内物料质量贡献 | `Mass` |
| `ItemMaterialWeight` 统计结果 | 按物料汇总质量 | `Mass` |
| `deformationCoefficient` | 无量纲 | `Dimensionless` |
| `hangingPercentage` | 无量纲 | `Dimensionless` |
| `position.x/y/z` | 长度 | `Length` |

---

## 五、向后兼容策略

### 5.1 Typealias

```kotlin
// bpp3d-infrastructure 层
typealias Flt64Cuboid = AbstractCuboid<Flt64>
typealias Flt64BottomSupport = BottomSupport<Flt64>
typealias Flt64Orientation = Orientation<Flt64>
typealias Flt64Container3Shape = Container3Shape<Flt64>

// bpp3d-domain-item-context 层
typealias Flt64PackageShape = PackageShape<Flt64>
typealias Flt64PackageAttribute = PackageAttribute<Flt64>
typealias Flt64Item = Item<Flt64>
```

### 5.2 Flt64 特化扩展函数

保留 `point2(Flt64, Flt64)` / `point3(Flt64, Flt64, Flt64)` 等现有便捷函数，确保 BPP3D 内部代码改动最小。

### 5.3 默认单位

BPP3D 内部使用 `Flt64` 时，`Quantity<Flt64>` 的单位默认为 `Length`（长度）或 `Mass`（质量）。提供工厂函数：

```kotlin
fun Flt64.mm() = Quantity(this, Millimeter)
fun Flt64.kg() = Quantity(this, Kilogram)
fun Flt64.mm2() = Quantity(this, SquareMillimeter)
fun Flt64.mm3() = Quantity(this, CubicMillimeter)
```

---

## 六、执行顺序与依赖关系

```
Phase 0.1 (Point/Vector 扩展) ──→ Phase 0.2 (Quantity 运算)
                                      │
Phase 1.1 (AbstractCuboid) ──────────┤
Phase 1.2 (BottomSupport) ───────────┤
Phase 1.3 (Cuboid<T,V>) ────────────┤
Phase 1.4 (Orientation<V>) ─────────┤  ← 破坏性最大，阻塞后续
Phase 1.5 (Container<V>) ───────────┤
Phase 1.6 (Projection<V>) ──────────┤
Phase 1.7 (Placement<V>) ───────────┘
                                      │
Phase 2.1 (PackageType 移动) ────────┤
Phase 2.2 (PackageShape<V>) ────────┤
Phase 2.3 (PackageAttribute<V>) ────┤
Phase 2.4 (Item<V>) ────────────────┤
Phase 2.5 (其他 model) ─────────────┘
                                      │
Phase 3.1-3.5 (其他模块适配) ────────┘
                                      │
Phase 4 (starter-bpp3d 适配) ────────┘
```

**建议执行顺序**：
1. Phase 0（可立即开始，不影响现有代码）
2. Phase 2.1（PackageType 移动，低风险）
3. Phase 1.1-1.3 + 1.5-1.7（infrastructure 泛型化，除 Orientation）
4. Phase 1.4（Orientation sealed class 重构，最关键）
5. Phase 2.2-2.5（domain-item-context 泛型化）
6. Phase 3（其他模块适配）
7. Phase 4（starter 适配）

---

## 七、风险与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|---------|
| `Orientation` enum → sealed class | 所有使用 `when(orientation)` 的代码需改为 `when` exhaustive | sealed class 支持 exhaustive when，语法兼容 |
| `Quantity<V>` 性能开销 | value class `Flt64` → `Quantity<Flt64>` 增加一层包装 | `Quantity` 是 data class，JVM 上有对象分配开销；性能敏感路径可保留裸 `V` |
| 量纲不匹配运行时错误 | `Quantity` 运算时量纲检查可能抛异常 | 编译期通过类型系统约束，运行期由 `Quantity` 的量纲检查保护 |
| BPP3D 下游项目破坏 | 所有依赖 BPP3D 的项目需适配 | typealias + 扩展函数保持 API 兼容 |
| `ospf-kotlin-math` 改造影响面大 | Point/Vector 被全生态使用 | 仅新增扩展函数，不修改核心类型 |

---

## 八、总体验收标准

- [x] `bpp3d-infrastructure` 主路径已完成 `Quantity<Flt64>` 化并通过编译/测试
- [x] `bpp3d-domain-item-context` 已完成需求统计扩展与主路径适配并通过验证
- [x] BPP3D 所有模块在 `Flt64` 主路径下编译通过且行为未见回归
- [x] APS `V = FltX` 直连能力在路线A下不作为当前门禁（已由 0.4/0.6 spike 验证可行性）
- [x] `PackageType` / `PackageCategory` / `PackageClassification` 在 `bpp3d-infrastructure` 中统一定义
- [x] 核心物理量属性量纲验证通过（长度=Length, 质量=Mass, 面积=Area, 体积=Volume）
- [x] 无量纲值（系数、百分比）保持裸数值类型（路线A主路径为 `Flt64`）
- [x] 默认 `ItemAmount` 统计模式下，旧 item 数量需求行为不变
- [x] `ItemMaterialAmount` 统计模式已支持一个 item 贡献多种物料数量并按物料汇总
- [x] `ItemMaterialWeight` 统计模式已支持一个 item 贡献多种物料重量并按物料汇总
- [x] layer assignment 的 load、demand constraint、shadow price、column generation 已支持三种统计模式
- [x] 向后兼容策略已落地（typealias + 兼容入口），现有主调用未中断
