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

- [ ] 决定路线 A 或路线 B。
- [ ] 增加一个最小 spike：长度、面积、体积、质量、线密度四类 `Quantity<Flt64>` 运算全部通过。
- [ ] 增加一个坐标 spike：二维/三维位置可比较、可平移、可计算矩形相交面积。
- [ ] 明确 Flt64 兼容层：旧 API 是保留裸 `Flt64` 包装为默认单位，还是直接公开 `Quantity<Flt64>`。

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
- [ ] `Point<Dim3, Quantity<FltX>>` 可通过 `qpoint3(x, y, z)` 构造
- [ ] `.x` / `.y` / `.z` 扩展属性返回 `Quantity<FltX>`
- [ ] 现有 `Point<Dim3, Flt64>` 的 `point3()` / `.x` 不受影响

#### 0.2 `Quantity<V>` 补充几何运算支持

**文件**：`ospf-kotlin-quantities/src/main/.../quantity/Quantity.kt`

**改造内容**：确保 `Quantity<V>` 满足 `FloatingNumber<V>` 约束（如果尚未满足），或定义 `Quantity<V>` 专用的算术运算扩展，使得 `Quantity<V>` 可以参与 `Point` / `Vector` 运算。

**验收标准**：
- [ ] `Quantity<FltX>` 可作为 `Point<D, Quantity<FltX>>` 的坐标类型
- [ ] `Quantity<FltX>` 的加减乘除运算结果量纲正确

---

### Phase 1：`bpp3d-infrastructure` — 核心类型 Quantity 化

#### 1.1 `AbstractCuboid` → `AbstractCuboid<V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造前**：
```kotlin
interface AbstractCuboid {
    val width: Flt64
    val height: Flt64
    val depth: Flt64
    val weight: Flt64
    val volume: Flt64 get() = depth * height * width
    val actualVolume: Flt64 get() = volume
    val linearDensity: Flt64 get() = weight / depth
}
```

**改造后**：
```kotlin
interface AbstractCuboid<V : FloatingNumber<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>
    val weight: Quantity<V>
    val volume: Quantity<V> get() = depth * height * width
    val actualVolume: Quantity<V> get() = volume
    val linearDensity: Quantity<V> get() = weight / depth
}
```

**向后兼容**：
```kotlin
typealias Flt64Cuboid = AbstractCuboid<Flt64>
```

**验收标准**：
- [ ] `AbstractCuboid<Flt64>` 编译通过
- [ ] `AbstractCuboid<FltX>` 编译通过
- [ ] `volume` / `linearDensity` 量纲正确（`Length³` / `Mass·Length⁻¹`）

#### 1.2 `BottomSupport` → `BottomSupport<V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造后**：
```kotlin
data class BottomSupport<V : FloatingNumber<V>>(
    val area: Quantity<V>,    // 面积（Length²）
    val weight: Quantity<V>   // 重量（Mass）
) : Plus<BottomSupport<V>, BottomSupport<V>> {
    override fun plus(rhs: BottomSupport<V>) = BottomSupport(
        area = area + rhs.area,
        weight = weight + rhs.weight
    )
}
```

**验收标准**：
- [ ] `BottomSupport<Flt64>` 加法运算正确
- [ ] `area` 量纲为 `Length²`，`weight` 量纲为 `Mass`

#### 1.3 `Cuboid<T>` → `Cuboid<T, V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Cuboid.kt`

**改造后**：
```kotlin
interface Cuboid<T : Cuboid<T, V>, V : FloatingNumber<V>> : AbstractCuboid<V> {
    val enabledOrientations: List<Orientation<V>>
    // ... 方法签名中 AbstractCuboid → AbstractCuboid<V>
    // ... AbstractContainer2Shape → AbstractContainer2Shape<V>
    // ... AbstractContainer3Shape → AbstractContainer3Shape<V>
}

open class CuboidView<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val unit: T,
    val orientation: Orientation<V> = Orientation.Upright
) : AbstractCuboid<V>, Copyable<CuboidView<T, V>> {
    override val width = orientation.width(unit)
    override val height = orientation.height(unit)
    override val depth = orientation.depth(unit)
    override val weight by unit::weight
    // ...
}
```

**验收标准**：
- [ ] `Cuboid<Item, Flt64>` 编译通过（BPP3D 内部继续用 Flt64）
- [ ] `CuboidView` 的 `width`/`height`/`depth` 返回 `Quantity<V>`

#### 1.4 `Orientation` → `Orientation<V>`（enum → sealed class）

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Orientation.kt`

**改造原因**：Kotlin `enum class` 不支持泛型参数，`Orientation.width(unit: AbstractCuboid): Flt64` 无法泛型化。

**改造后**：
```kotlin
sealed class Orientation<V : FloatingNumber<V>> {
    abstract fun depth(unit: AbstractCuboid<V>): Quantity<V>
    abstract fun width(unit: AbstractCuboid<V>): Quantity<V>
    abstract fun height(unit: AbstractCuboid<V>): Quantity<V>
    abstract val rotation: Orientation<V>
    open val rotated: Boolean = false
    abstract val category: OrientationCategory

    object Upright : Orientation<Nothing>() {
        // width/height/depth 不改变维度
    }
    object UprightRotated : Orientation<Nothing>() { ... }
    object Side : Orientation<Nothing>() { ... }
    object SideRotated : Orientation<Nothing>() { ... }
    object Lie : Orientation<Nothing>() { ... }
    object LieRotated : Orientation<Nothing>() { ... }

    companion object {
        val entries = listOf(Upright, UprightRotated, Side, SideRotated, Lie, LieRotated)
        // merge 等方法泛型化
    }
}
```

**注意**：这是破坏性最大的改造。所有 `Orientation.Upright` 引用不变（单例对象），但 `orientation.width(unit)` 返回类型从 `Flt64` 变为 `Quantity<V>`。

**向后兼容**：
```kotlin
typealias Flt64Orientation = Orientation<Flt64>
```

**验收标准**：
- [ ] `Orientation<Flt64>.Upright.width(cuboid)` 返回 `Quantity<Flt64>`
- [ ] `Orientation<FltX>.Upright.width(cuboid)` 返回 `Quantity<FltX>`
- [ ] `Orientation.entries` 包含全部 6 个方向
- [ ] `Orientation.merge()` 编译通过
- [ ] 序列化/反序列化兼容（`@Serializable`）

#### 1.5 `Container` → `Container<V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Container.kt`

**改造后**：
```kotlin
interface AbstractContainer3Shape<V : FloatingNumber<V>> : Eq<AbstractContainer3Shape<V>> {
    val width: Quantity<V>
    val height: Quantity<V>
    val depth: Quantity<V>
    val volume: Quantity<V> get() = width * height * depth
    // enabled / maxAmount / restSpace 方法签名中 Flt64 → Quantity<V>
}

data class Container3Shape<V : FloatingNumber<V>>(
    override val width: Quantity<V> = Quantity(V.constants.infinity, Length),
    override val height: Quantity<V> = Quantity(V.constants.infinity, Length),
    override val depth: Quantity<V> = Quantity(V.constants.infinity, Length)
) : AbstractContainer3Shape<V>
```

**验收标准**：
- [ ] `Container3Shape<Flt64>` 编译通过
- [ ] `Container3Shape<FltX>` 编译通过
- [ ] `volume` 量纲为 `Length³`

#### 1.6 `Projection` → `Projection<V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Projection.kt`

**改造后**：
```kotlin
data class ProjectionShape<V : FloatingNumber<V>>(
    val length: Quantity<V>,
    val width: Quantity<V>
) {
    val area: Quantity<V> = length * width
}

sealed class ProjectivePlane<V : FloatingNumber<V>> {
    abstract fun length(unit: AbstractCuboid<V>, orientation: Orientation<V> = Orientation.Upright): Quantity<V>
    abstract fun width(unit: AbstractCuboid<V>, orientation: Orientation<V> = Orientation.Upright): Quantity<V>
    abstract fun height(unit: AbstractCuboid<V>, orientation: Orientation<V> = Orientation.Upright): Quantity<V>
    // ...
}
```

**验收标准**：
- [ ] `Bottom.length(cuboid)` 返回 `Quantity<V>`
- [ ] `ProjectionShape.area` 量纲为 `Length²`

#### 1.7 `Placement` → `Placement<V>`

**文件**：`bpp3d-infrastructure/src/main/.../infrastructure/Placement.kt`

**改造后**：
```kotlin
data class Placement3<T : Cuboid<T, V>, V : FloatingNumber<V>>(
    val view: CuboidView<T, V>,
    val position: Point<Dim3, Quantity<V>>
) : Copyable<Placement3<T, V>>, Ord<Placement3<T, V>> {
    val x: Quantity<V> get() = position[0]
    val y: Quantity<V> get() = position[1]
    val z: Quantity<V> get() = position[2]
    // ... maxX/maxY/maxZ 返回 Quantity<V>
}
```

**验收标准**：
- [ ] `Placement3<Item, Flt64>` 编译通过
- [ ] `Placement3` 的 `x`/`y`/`z` 返回 `Quantity<V>`
- [ ] `topPlacements` / `bottomPlacements` 编译通过

---

### Phase 2：`bpp3d-domain-item-context` — Package 模型 Quantity 化

#### 2.1 `PackageType` / `PackageCategory` / `PackageClassification` 移入 infrastructure

**操作**：
1. 从 `Package.kt` 中提取 `PackageType`、`PackageCategory`、`PackageClassification` 到 `bpp3d-infrastructure/src/main/.../infrastructure/PackageType.kt`
2. `bpp3d-domain-item-context` 的 `Package.kt` 改为 import

**验收标准**：
- [ ] `PackageType` 在 `bpp3d-infrastructure` 中定义
- [ ] `bpp3d-domain-item-context` 和 APS 均可 import 同一 `PackageType`

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
- [ ] `PackageShape<Flt64>` 编译通过
- [ ] `PackageShape<FltX>` 编译通过
- [ ] 二维包材 `height == null` 时 `volume` 返回 `null`
- [ ] `volume` 量纲为 `Length³`，`area` 量纲为 `Length²`

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
- [ ] `PackageAttribute<Flt64>` 编译通过，BPP3D 内部行为不变
- [ ] `PackageAttribute<FltX>` 编译通过
- [ ] `maxHeight` 量纲为 `Length`
- [ ] `maxDifference`（AbsoluteHangingPolicy）量纲为 `Length`
- [ ] `deformationCoefficient`（LinearDeformationAttribute）无量纲
- [ ] `enabledStackingOn` 各重载编译通过

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
- [ ] `Item<Flt64>` 编译通过
- [ ] `Item<FltX>` 编译通过
- [ ] `Item.packageShape` 返回 `PackageShape<V>`

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
- [ ] `ActualItem` 未绑定 `Package` 时，物料统计为空。
- [ ] `ActualItem(pack=...)` 能从 `Package.materials` 得到物料数量。
- [ ] `ActualItem(pack=...)` 能通过 `Material.weight` 推导物料重量。
- [ ] `PatternedItem` 的物料数量/重量是加权总和，不是平均值。
- [ ] 物料重量使用 `Quantity<V>`，量纲为 `Mass`。

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
- [ ] 旧 `amount(item)` 行为不变。
- [ ] `BinLayer.statistics(ItemAmount)` 与旧 `amounts` 等价。
- [ ] `BinLayer.statistics(ItemMaterialAmount)` 可跨多个 item 汇总同一 `MaterialKey`。
- [ ] `BinLayer.statistics(ItemMaterialWeight)` 可跨多个 item 汇总同一 `MaterialKey` 的 `Quantity<V>`。
- [ ] block loading 的 `restItems` 仍可使用 item 数量；若启用物料统计，应通过新增 demand statistics 路径处理。

#### 2.5 `ItemContainer` / `Bin` / `Block` / `Layer` / `Pattern` / `Schema` → 泛型化

**文件**：`bpp3d-domain-item-context/src/main/.../model/` 下所有文件

**改造内容**：所有引用 `Item` / `PackageAttribute` / `PackageShape` 的类型增加 `<V>` 参数。

**验收标准**：
- [ ] 全部 model 文件编译通过
- [ ] `ItemContainer<V>` / `Bin<V>` / `Block<V>` / `Layer<V>` 编译通过

---

### Phase 3：其他 BPP3D 模块适配

#### 3.1 `bpp3d-domain-bla-context`

**文件**：`BLAContext.kt`, `BottomUpLeftJustifiedAlgorithm.kt`, `BottomUpLeftJustifiedAlgorithm3D.kt`

**改造内容**：所有方法签名中 `Item` → `Item<V>`，`Placement3` → `Placement3<T, V>`，增加 `<V>` 参数。

**验收标准**：
- [ ] BLA 算法编译通过
- [ ] 算法行为不变（Flt64 特化下结果一致）

#### 3.2 `bpp3d-domain-block-loading-context`

**文件**：`BlockLoadingContext.kt`, `Space.kt`, `SimpleBlockGenerator.kt`, `ComplexBlockGenerator.kt`, `DepthFirstSearchAlgorithm.kt`, `MultiLayerHeuristicSearchAlgorithm.kt`

**改造内容**：同上，泛型化。

**验收标准**：
- [ ] Block loading 编译通过
- [ ] 算法行为不变

#### 3.3 `bpp3d-domain-layer-generation-context` / `layer-selection-context` / `layer-assignment-context`

**改造内容**：同上。

**验收标准**：
- [ ] 全部编译通过
- [ ] 算法行为不变

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
- [ ] 旧 `ItemDemandConstraint` 作为 `ItemAmount` wrapper 编译通过。
- [ ] `ItemMaterialAmount` 能对 `MaterialKey` 建立上下界约束。
- [ ] `ItemMaterialWeight` 能对 `MaterialKey` 建立重量上下界约束。
- [ ] 三种模式下 `Load.addColumns` 生成的列系数正确。
- [ ] shadow price extractor/refresh 能区分三种统计模式。

#### 3.4 `bpp3d-domain-packing-context`

**文件**：`PackingContext.kt`, `Aggregation.kt`, `MaterialAttribute.kt`, `Packer.kt`

**改造内容**：同上。

**验收标准**：
- [ ] 全部编译通过
- [ ] Packer 行为不变

#### 3.5 `bpp3d-application`

**改造内容**：顶层编排逻辑泛型化。

**验收标准**：
- [ ] 全部编译通过
- [ ] 端到端求解行为不变

---

### Phase 4：`ospf-kotlin-starter-bpp3d` 适配

**文件**：`ospf-kotlin-starters/ospf-kotlin-starter-bpp3d/pom.xml`

**改造内容**：确认依赖关系不变（starter-bpp3d 已依赖 infrastructure + domain-item-context）。

**验收标准**：
- [ ] `ospf-kotlin-starter-bpp3d` 编译通过
- [ ] 下游项目（如 BPP3D example）可正常使用

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

- [ ] `bpp3d-infrastructure` 全部类型支持 `Quantity<V>` 泛型
- [ ] `bpp3d-domain-item-context` 全部类型支持 `Quantity<V>` 泛型
- [ ] BPP3D 所有模块在 `V = Flt64` 特化下编译通过且行为不变
- [ ] APS 可以 `V = FltX` 直接使用 BPP3D 类型，无需桥接层
- [ ] `PackageType` / `PackageCategory` / `PackageClassification` 在 `bpp3d-infrastructure` 中统一定义
- [ ] 所有物理量属性量纲正确（长度=Length, 质量=Mass, 面积=Area, 体积=Volume）
- [ ] 无量纲值（系数、百分比）保持裸 `V` 类型
- [ ] 默认 `ItemAmount` 统计模式下，旧 item 数量需求行为不变
- [ ] 新增 `ItemMaterialAmount` 统计模式，支持一个 item 贡献多种物料数量并按物料汇总
- [ ] 新增 `ItemMaterialWeight` 统计模式，支持一个 item 贡献多种物料重量并按物料汇总
- [ ] layer assignment 的 load、demand constraint、shadow price、column generation 都支持三种统计模式
- [ ] 向后兼容：typealias + 扩展函数确保现有调用方式不中断
