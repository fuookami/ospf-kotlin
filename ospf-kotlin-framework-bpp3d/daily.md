# BPP3D Quantity<V> 改造计划

> 目标：将 BPP3D 所有硬编码 `Flt64` 的几何/物理量改为 `Quantity<V>`，使 APS 可直接以 `Quantity<FltX>` 复用 BPP3D 类型，无需桥接层。
>
> 日期：2026-05-08

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
- [ ] 向后兼容：typealias + 扩展函数确保现有调用方式不中断
