/**
 * 物品模型。
 * Item model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * 优先级属性，根据提取器从实际物品中获取匹配的优先级值。
 * Priority attribute that extracts a priority value from an actual item via an extractor.
 *
 * @property key 属性键 / attribute key
 * @property value 优先级值 / priority value
 */
data class PriorityAttribute(
    val key: String,
    private val extractor: Extractor<String?, ActualItem>,
    val value: UInt64
) {
    /**
     * 从实际物品中提取属性值。
     * Extract the attribute value from an actual item.
     *
     * @param item 实际物品 / actual item
     * @return 提取的属性值 / extracted attribute value
     */
    private fun attribute(item: ActualItem) = extractor(item)

    operator fun invoke(item: ActualItem): UInt64? {
        return if (attribute(item) == key) {
            value
        } else {
            null
        }
    }
}

/**
 * 物品类型，由包装类型和朝向类别组成。
 * Item type composed of a package type and an orientation category.
 *
 * @property packageType 包装类型 / package type
 * @property orientation 朝向类别 / orientation category
 */
open class ItemType(
    val packageType: PackageType,
    val orientation: OrientationCategory = OrientationCategory.Upright
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemType) return false

        if (packageType != other.packageType) return false
        if (orientation != other.orientation) return false

        return true
    }

    override fun hashCode(): Int {
        var result = packageType.hashCode()
        result = 31 * result + orientation.hashCode()
        return result
    }
}

/**
 * 物品模式，描述物品的形状、朝向和属性。
 * Item pattern describing the shape, orientation, and attributes of an item.
 *
 * @property shape 包装形状 / package shape
 * @property enabledOrientations 允许的朝向列表 / list of enabled orientations
 * @property batchNo 批次号 / batch number
 * @property priorities 优先级映射 / priority map
 * @property warehouse 仓库 / warehouse
 * @property packageAttribute 包装属性 / package attribute
 */
open class ItemPattern(
    val shape: PackageShape<FltX>,
    val enabledOrientations: List<Orientation>,
    val batchNo: BatchNo?,
    val priorities: Map<String, UInt64>,
    val warehouse: String?,
    val packageAttribute: PackageAttribute,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ItemPattern) return false

        if (shape != other.shape) return false
        if (!(enabledOrientations.toTypedArray() contentEquals other.enabledOrientations.toTypedArray())) return false
        if (batchNo != other.batchNo) return false
        if (!(priorities.entries.toTypedArray() contentEquals other.priorities.entries.toTypedArray())) return false
        if (warehouse != other.warehouse) return false
        if (packageAttribute != other.packageAttribute) return false

        return true
    }

    override fun hashCode(): Int {
        var result = shape.hashCode()
        result = 31 * result + enabledOrientations.hashCode()
        result = 31 * result + (batchNo?.hashCode() ?: 0)
        result = 31 * result + (priorities?.hashCode() ?: 0)
        result = 31 * result + (warehouse?.hashCode() ?: 0)
        result = 31 * result + packageAttribute.hashCode()
        return result
    }
}

/**
 * 将 Quantity<*> 转换为 Quantity<FltX>。
 * Convert Quantity<*> to Quantity<FltX>.
 *
 * @param value 待转换的值 / value to convert
 * @return 转换为 FltX 类型的数量值 / quantity value converted to FltX type
 */
@Suppress("UNCHECKED_CAST")
private fun itemPackQuantityToFltX(value: Quantity<*>): Quantity<FltX> {
    return when (value.value) {
        is FltX -> value as Quantity<FltX>
        else -> Quantity(FltX(value.value.toString().toDouble()), value.unit)
    }
}

/**
 * 将 Container3Geometry 转换为 AbstractContainer3Shape。
 * Convert Container3Geometry to AbstractContainer3Shape.
 *
 * @return 抽象容器 3D 形状 / abstract container 3D shape
 */
private fun Container3Geometry<FltX>.asContainer3Shape(): AbstractContainer3Shape {
    return this as? AbstractContainer3Shape ?: Container3Shape(
        width = width,
        height = height,
        depth = depth
    )
}

/** 货物合并结果单元。Item merge result unit. */
sealed interface ItemMergeUnit

/**
 * 物品接口，定义三维装箱中的物品基本行为。
 * Item interface defining the basic behavior of items in 3D bin packing.
 */
interface Item : Cuboid<Item, FltX>, Indexed, ItemMergeUnit {
    override val self: Item
        get() = this
    val explicitPackingShape: PackingShape3<FltX>?
        get() = null
    val packingShapeSpec: PackageShapeSpec?
        get() = null
    val packingShape: PackingShape3<FltX>
        get() = explicitPackingShape
            ?: packageShape.toPackingShapeOrNull()
            ?: view().asPackingShape3()
    /** 包围盒（从 packingShape 派生，不直接依赖 Cuboid 继承）。Bounding box derived from packingShape, not directly from Cuboid inheritance. */
    val shapeBoundingBox: ShapeBoundingBox3<FltX>
        get() = packingShape.boundingBox
    /** 底面轮廓（从 packingShape 派生，不直接依赖 Cuboid 继承）。Bottom footprint derived from packingShape, not directly from Cuboid inheritance. */
    val shapeFootprint: ShapeFootprint2<FltX>
        get() = packingShape.footprint()
    /** 形状实际体积（从 packingShape 派生，不直接依赖 Cuboid 继承）。Shape geometric volume derived from packingShape, not directly from Cuboid inheritance. */
    val shapeVolume: Quantity<FltX>
        get() = packingShape.actualVolume
    val batchNo: BatchNo?
    val priorities: Map<String, UInt64>
    val warehouse: String?
    val packageAttribute: PackageAttribute
    val materialAmounts: Map<MaterialKey, UInt64>
        get() = emptyMap()
    val materialWeights: Map<MaterialKey, Quantity<FltX>>
        get() = emptyMap()

    val packageType get() = packageAttribute.packageType
    val packageBottomShape get() = packageShape.bottomShape
    val packageShape
        get() = PackageShape(
            width = width,
            height = height,
            depth = depth,
            weight = weight,
            packageType = packageType,
            shapeSpec = packingShapeSpec ?: PackageShapeSpec.Cuboid
        )
    val type: ItemType get() = ItemType(packageType)
    val packageCategory get() = packageType.category
    val maxLayer get() = packageAttribute.maxLayer
    val maxHeight get() = packageAttribute.maxHeight
    val minDepth get() = packageAttribute.minDepth
    val maxDepth get() = packageAttribute.maxDepth
    val overPackageTypes get() = packageAttribute.overPackageTypes
    val bottomOnly get() = packageAttribute.bottomOnly
    val topFlat get() = packageAttribute.topFlat
    val sideOnTopLayer get() = packageAttribute.sideOnTopLayer
    val lieOnTopLayer get() = packageAttribute.lieOnTopLayer
    val enabledSideOnTop get() = packageAttribute.enabledSideOnTop
    val enabledLieOnTop get() = packageAttribute.enabledLieOnTop

    val pattern: ItemPattern
        get() = ItemPattern(
            shape = packageShape,
            enabledOrientations = enabledOrientations,
            batchNo = batchNo,
            priorities = priorities,
            warehouse = warehouse,
            packageAttribute = packageAttribute
        )

    /**
     * 判断物品是否允许在指定的底部支撑上堆叠。
     * Check whether this item can be stacked on the specified bottom support.
     *
     * @param bottomSupport 底部支撑 / bottom support
     * @return 是否允许堆叠 / whether stacking is allowed
     */
    fun enabledStackingOn(bottomSupport: BottomSupport): Boolean {
        return packageAttribute.enabledStackingOn(
            item = view(),
            bottomSupport = bottomSupport
        )
    }

    /**
     * 判断物品是否允许堆叠在指定底部物品之上。
     * Check whether this item can be stacked on the specified bottom item.
     *
     * @param bottomItem 底部物品 / bottom item
     * @param layer 层数 / layer number
     * @param height 高度 / height
     * @param space 容器空间 / container space
     * @return 是否允许堆叠 / whether stacking is allowed
     */
    fun enabledStackingOn(
        bottomItem: Item,
        layer: UInt64 = UInt64.zero,
        height: Quantity<FltX> = width * FltX.zero,
        space: Container3Geometry<FltX> = Container3Shape()
    ): Boolean {
        return packageAttribute.enabledStackingOn(
            item = this,
            bottomItem = bottomItem,
            layer = layer,
            height = height,
            space = space.asContainer3Shape()
        )
    }

    override fun view(orientation: Orientation): ItemView {
        return ItemView(this, orientation)
    }
}

/**
 * 实际物品，包含具体的包装信息和属性。
 * An actual item containing specific packaging information and attributes.
 *
 * @property id 物品 ID / item id
 * @property name 物品名称 / item name
 * @property packageCode 包装代码 / package code
 * @property pack 包装信息 / package information
 * @property priorityAttribute 优先级属性列表 / list of priority attributes
 * @property shapeSpecOverride 形状规格覆盖 / shape spec override
 * @property packingShapeOverride 装载形状覆盖 / packing shape override
 * @property materialAmountsOverride 材料用量覆盖 / material amounts override
 * @property materialWeightsOverride 材料重量覆盖 / material weights override
 */
open class ActualItem(
    open val id: ItemId,
    val name: String,
    val packageCode: PackageCode? = null,
    val pack: Package<*>? = null,
    val priorityAttribute: List<PriorityAttribute> = emptyList(),
    // inherited from Cuboid<Item>
    override val width: Quantity<FltX>,
    override val height: Quantity<FltX>,
    override val depth: Quantity<FltX>,
    override val weight: Quantity<FltX>,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute,
    val shapeSpecOverride: PackageShapeSpec? = null,
    val packingShapeOverride: PackingShape3<FltX>? = null,
    val materialAmountsOverride: Map<MaterialKey, UInt64>? = null,
    val materialWeightsOverride: Map<MaterialKey, Quantity<FltX>>? = null
) : Item, ManualIndexed() {
    override val explicitPackingShape: PackingShape3<FltX>?
        get() = packingShapeOverride
    override val packingShapeSpec: PackageShapeSpec?
        get() = shapeSpecOverride ?: pack?.shape?.shapeSpec
    override val priorities = priorityAttribute.mapNotNull { it(this)?.let { value -> Pair(it.key, value) } }.toMap()
    override val materialAmounts: Map<MaterialKey, UInt64> by lazy {
        materialAmountsOverride?.let {
            return@lazy it
        }
        val counter = HashMap<MaterialKey, UInt64>()
        for ((material, amount) in pack?.materials ?: emptyMap()) {
            val key = material.key
            counter[key] = (counter[key] ?: UInt64.zero) + amount
        }
        counter
    }
    override val materialWeights: Map<MaterialKey, Quantity<FltX>> by lazy {
        materialWeightsOverride?.let {
            return@lazy it
        }
        val counter = HashMap<MaterialKey, Quantity<FltX>>()
        for ((material, amount) in pack?.materials ?: emptyMap()) {
            val key = material.key
            val weight = material.weight * FltX(amount.toULong().toDouble())
            counter[key] = (counter[key] ?: (weight * FltX.zero)) + weight
        }
        counter
    }

    constructor(
        id: ItemId,
        name: String,
        pack: Package<*>,
        priorityAttribute: List<PriorityAttribute> = emptyList(),
        enabledOrientations: List<Orientation>,
        batchNo: BatchNo,
        warehouse: String? = null,
        packageAttribute: PackageAttribute,
    ) : this(
        id = id,
        name = name,
        packageCode = pack.code,
        pack = pack,
        priorityAttribute = priorityAttribute,
        width = itemPackQuantityToFltX(pack.width),
        height = itemPackQuantityToFltX(pack.height),
        depth = itemPackQuantityToFltX(pack.depth),
        weight = itemPackQuantityToFltX(pack.weight),
        enabledOrientations = enabledOrientations,
        batchNo = batchNo,
        warehouse = warehouse,
        packageAttribute = packageAttribute
    )

    override fun toString(): String {
        return this.name
    }
}

/**
 * 由多个 ActualItem 按数量及范围组成的模式化物品。
 * A patterned item composed of multiple ActualItems with counts and ranges.
 *
 * @property actualItems 实际物品列表（物品、数量、数量范围） / list of actual items (item, count, count range)
 */
open class PatternedItem(
    private val actualItems: List<Triple<ActualItem, UInt64, ValueRange<UInt64>>>,
    // inherited from Cuboid<Item>
    override val width: Quantity<FltX>,
    override val height: Quantity<FltX>,
    override val depth: Quantity<FltX>,
    override val weight: Quantity<FltX>,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val priorities: Map<String, UInt64> = emptyMap(),
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute
) : Item, ManualIndexed() {
    override val volume: Quantity<FltX> = run {
        val totalAmount = actualItems.fold(FltX.zero) { acc, (_, amount, _) -> acc + FltX(amount.toULong().toDouble()) }
        if (totalAmount eq FltX.zero) {
            width * height * depth * FltX.zero
        } else {
            actualItems.sumOfQuantity { it.first.volume * FltX(it.second.toULong().toDouble()) } / totalAmount
        }
    }
    override val materialAmounts: Map<MaterialKey, UInt64> by lazy {
        val counter = HashMap<MaterialKey, UInt64>()
        for ((item, amount, _) in actualItems) {
            for ((material, thisAmount) in item.materialAmounts) {
                counter[material] = (counter[material] ?: UInt64.zero) + thisAmount * amount
            }
        }
        counter
    }
    override val materialWeights: Map<MaterialKey, Quantity<FltX>> by lazy {
        val counter = HashMap<MaterialKey, Quantity<FltX>>()
        for ((item, amount, _) in actualItems) {
            for ((material, weight) in item.materialWeights) {
                val thisWeight = weight * FltX(amount.toULong().toDouble())
                counter[material] = (counter[material] ?: (thisWeight * FltX.zero)) + thisWeight
            }
        }
        counter
    }

    companion object {
        operator fun invoke(
            pattern: ItemPattern,
            actualItems: List<Triple<ActualItem, UInt64, ValueRange<UInt64>>>
        ): Triple<PatternedItem, UInt64, ValueRange<UInt64>> {
            val amount = actualItems.fold(UInt64.zero) { acc, (_, thisAmount, _) -> acc + thisAmount }
            val amountRange = actualItems.fold(ValueRange(UInt64.zero, UInt64.zero).value!!) { acc, triple ->
                (acc + triple.second)!!
            }
            val volume = actualItems.sumOfQuantity {
                it.first.volume * FltX(it.second.toULong().toDouble())
            } / FltX(amount.toULong().toDouble())
            val deformation = pattern.packageAttribute.deformationAttribute.deformationQuantity(volume.value)
            return Triple(
                PatternedItem(
                    actualItems = actualItems,
                width = pattern.shape.width + deformation[0],
                height = pattern.shape.height + deformation[1],
                depth = pattern.shape.depth + deformation[2],
                weight = actualItems.sumOfQuantity {
                    it.first.weight * FltX(it.second.toULong().toDouble())
                } / FltX(amount.toULong().toDouble()),
                enabledOrientations = Orientation.merge(actualItems.first().first, pattern.enabledOrientations),
                batchNo = pattern.batchNo,
                priorities = pattern.priorities,
                warehouse = pattern.warehouse,
                packageAttribute = pattern.packageAttribute
            ), amount, amountRange)
        }
    }

    operator fun get(index: Int): ActualItem {
        assert(index >= 0)
        var i = index
        for (item in actualItems) {
            if (i >= item.second.toInt()) {
                i -= item.second.toInt()
            } else {
                return item.first
            }
        }
        return actualItems.last().first
    }

    // inherited from CuboidUnit<Item>
    override fun enabledOrientationsAt(
        space: Container2Geometry<*, FltX>,
        withRotation: Boolean
    ): List<Orientation> {
        val actualOrientations = super.enabledOrientationsAt(
            space = space,
            withRotation = withRotation
        ).toMutableSet()
        if (space.plane != Bottom) {
            if (space.width leq height && enabledSideOnTop) {
                actualOrientations.addAll(
                    Orientation
                        .entries
                        .filter {
                            it.category == OrientationCategory.Side
                                    && space.length geq space.plane.length(this, it)
                                    && space.width geq space.plane.width(this, it)
                        }
                )
            }
            if (space.width leq height && enabledLieOnTop) {
                actualOrientations.addAll(
                    Orientation
                        .entries
                        .filter {
                            it.category == OrientationCategory.Lie
                                    && space.length geq space.plane.length(this, it)
                                    && space.width geq space.plane.width(this, it)
                        }
                )
            }
        }
        return actualOrientations.toList()
    }

    override fun enabledOrientationsAt(
        space: Container3Geometry<FltX>,
        withRotation: Boolean
    ): List<Orientation> {
        val actualOrientations = super.enabledOrientationsAt(
            space = space,
            withRotation = withRotation
        ).toMutableSet()
        if (space.height leq height && enabledSideOnTop) {
            actualOrientations.addAll(
                Orientation
                    .entries
                    .filter {
                        it.category == OrientationCategory.Side
                                && space.width geq it.width(this)
                                && space.height geq it.height(this)
                                && space.depth geq it.depth(this)
                    }
            )
        }
        if (space.height leq height && enabledLieOnTop) {
            actualOrientations.addAll(
                Orientation
                    .entries
                    .filter {
                        it.category == OrientationCategory.Lie
                                && space.width geq it.width(this)
                                && space.height geq it.height(this)
                                && space.depth geq it.depth(this)
                    }
            )
        }
        val containerSpace = space.asContainer3Shape()
        actualOrientations.removeAll { packageAttribute.extraOrientationRule?.let { it1 -> it1(containerSpace, it) } == false }
        return actualOrientations.toList()
    }

    override fun toString(): String {
        return this
            .actualItems
            .groupBy { it.first.name }
            .keys
            .joinToString(";")
    }
}

/**
 * 物品视图，通过特定朝向观察一个物品。
 * Item view, observing an item through a specific orientation.
 */
open class ItemView(
    unit: Item,
    orientation: Orientation = Orientation.Upright
) : CuboidView<Item, FltX>(unit, orientation) {
    /** 放置级装载形状覆盖。Placement-level packing shape override. */
    open val placementPackingShape: PackingShape3<FltX>? get() = null
    open val type get() = ItemType(unit.packageType, orientation.category)
    val packageType by unit::packageType
    val packageCategory by unit::packageCategory

    open val maxLayer: UInt64
        get() {
            return if (topFlat) {
                unit.maxLayer
            } else if (orientation.category == OrientationCategory.Side && unit.enabledSideOnTop) {
                unit.sideOnTopLayer
            } else if (orientation.category == OrientationCategory.Lie && unit.enabledLieOnTop) {
                unit.lieOnTopLayer
            } else {
                UInt64.one
            }
        }

    open val maxHeight by unit::maxHeight
    open val minDepth by unit::minDepth
    open val maxDepth by unit::maxDepth
    open val overPackageTypes by unit::overPackageTypes
    open val bottomOnly by unit::bottomOnly
    open val topFlat: Boolean
        get() {
            return if (orientation.category == OrientationCategory.Upright) {
                unit.topFlat
            } else {
                unit.enabledOrientations.contains(orientation)
            }
        }

    /**
     * 判断该视图物品是否允许在指定的底部支撑上堆叠。
     * Check whether this item view can be stacked on the specified bottom support.
     *
     * @param bottomSupport 底部支撑 / bottom support
     * @return 是否允许堆叠 / whether stacking is allowed
     */
    fun enabledStackingOn(bottomSupport: BottomSupport): Boolean {
        return unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomSupport = bottomSupport
        )
    }

    /**
     * 判断该视图物品是否允许堆叠在指定底部物品之上。
     * Check whether this item view can be stacked on the specified bottom item.
     *
     * @param bottomItem 底部物品视图 / bottom item view
     * @param layer 层数 / layer number
     * @param height 高度 / height
     * @param space 容器空间 / container space
     * @return 是否允许堆叠 / whether stacking is allowed
     */
    fun enabledStackingOn(
        bottomItem: ItemView?,
        layer: UInt64 = UInt64.zero,
        height: Quantity<FltX> = this.height * FltX.zero,
        space: Container3Geometry<FltX> = Container3Shape()
    ): Boolean {
        return unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItem = bottomItem,
            layer = layer,
            height = height,
            space = space.asContainer3Shape()
        )
    }

    override val rotation: ItemView?
        get() {
            return super.rotation?.let { ItemView(it.unit, it.orientation) }
        }

    override fun rotationAt(space: Container2Geometry<*, FltX>): ItemView? {
        return super.rotationAt(space)?.let { ItemView(it.unit, it.orientation) }
    }

    override fun rotationAt(space: Container3Geometry<FltX>): ItemView? {
        return super.rotationAt(space)?.let { ItemView(it.unit, it.orientation) }
    }

    override fun copy(): ItemView {
        return ItemView(
            unit,
            orientation
        )
    }
}

/**
 * 解析放置级真实装载形状，优先使用 ItemView 携带的候选几何。
 * Resolve placement-level packing shape, preferring candidate geometry carried by ItemView.
 *
 * @return 放置级装载形状 / placement-level packing shape
 */
fun QuantityPlacement3<*, FltX>.resolvedPackingShape(): PackingShape3<FltX> {
    val itemView = view as? ItemView
    itemView?.placementPackingShape?.let {
        return it
    }
    return when (val placementUnit = unit) {
        is Item -> placementUnit.packingShape
        else -> view.asPackingShape3()
    }
}

@get:JvmName("itemProjectionType")
val Projection<Item, FltX, *>.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemPlacement2Type")
val QuantityPlacement2<Item, FltX, *>.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemPlacement3Type")
val QuantityPlacement3<Item, FltX>.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemProjectionPackageType")
val Projection<Item, FltX, *>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemPlacement2PackageType")
val QuantityPlacement2<Item, FltX, *>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemPlacement3PackageType")
val QuantityPlacement3<Item, FltX>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemProjectionPackageCategory")
val Projection<Item, FltX, *>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement2PackageCategory")
val QuantityPlacement2<Item, FltX, *>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement3PackageCategory")
val QuantityPlacement3<Item, FltX>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement2BottomOnly")
val QuantityPlacement2<Item, FltX, *>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemPlacement3BottomOnly")
val QuantityPlacement3<Item, FltX>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemProjectionTopFlat")
val Projection<Item, FltX, *>.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

@get:JvmName("itemPlacement2TopFlat")
val QuantityPlacement2<Item, FltX, *>.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

@get:JvmName("itemPlacement3TopFlat")
val QuantityPlacement3<Item, FltX>.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

/**
 * 将放置转换为水平圆柱支撑几何体。
 * Convert a placement to a horizontal cylinder support geometry.
 *
 * @return 水平圆柱支撑几何体 / horizontal cylinder support geometry
 */
private fun QuantityPlacement3<*, FltX>.toHorizontalCylinderSupportGeometry(): HorizontalCylinderSupportGeometry {
    val shape = resolvedPackingShape()
    val minX = absoluteX.toDouble()
    val minY = absoluteY.toDouble()
    val minZ = absoluteZ.toDouble()
    return HorizontalCylinderSupportGeometry(
        minX = minX,
        maxX = minX + shape.boundingWidth.toDouble(),
        minY = minY,
        maxY = minY + shape.boundingHeight.toDouble(),
        minZ = minZ,
        maxZ = minZ + shape.boundingDepth.toDouble(),
        isCylinder = shape is CylinderPackingShape3
    )
}

/**
 * 检查水平圆柱物品在底部物品上是否有足够的支撑覆盖。
 * Check whether a horizontal cylinder item has sufficient support coverage on bottom items.
 *
 * @param item 水平圆柱物品放置 / horizontal cylinder item placement
 * @param bottomItems 底部物品放置列表 / list of bottom item placements
 * @return 是否有足够的支撑覆盖 / whether there is sufficient support coverage
 */
private fun hasHorizontalCylinderStackingSupportCoverage(
    item: QuantityPlacement3<Item, FltX>,
    bottomItems: List<QuantityPlacement3<*, FltX>>
): Boolean {
    val cylinder = item.resolvedPackingShape() as? CylinderPackingShape3 ?: return true
    val axis = cylinder.axis
    if (axis == Axis3.Y || item.absoluteY eq FltX.zero) {
        return true
    }

    return horizontalCylinderCuboidSupportCoverage(
        cylinder = item.toHorizontalCylinderSupportGeometry(),
        axis = axis,
        supports = bottomItems.map { candidate -> candidate.toHorizontalCylinderSupportGeometry() }
    )
}

/**
 * 判断物品在 Side 平面中是否允许堆叠在底部物品之上。
 * Check whether the item can be stacked on bottom items in the Side plane.
 *
 * @param bottomItems 底部物品列表 / list of bottom items
 * @param space 容器空间 / container space
 * @return 是否允许堆叠 / whether stacking is allowed
 */
@JvmName("itemPlacement2SideEnabledStackingOn")
suspend fun QuantityPlacement2<Item, FltX, Side>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, FltX, Side>>,
    space: AbstractContainer2Shape<Side> = Container2Shape(plane = Side)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in toPlacement3()) {
                promises.add(async(Dispatchers.Default) {
                    val thisBottomPlacements = bottomPlacements.filterBottomOverlapped(item)
                    (view as ItemView).enabledStackingOn(
                        bottomSupport = bottomSupport(
                            unit = item,
                            bottomUnits = thisBottomPlacements,
                            shapeResolver = { placement ->
                                when (val placementUnit = placement.unit) {
                                    is Item -> placementUnit.packingShape
                                    else -> placement.view.asPackingShape3()
                                }
                            }
                        )
                    ) && unit.packageAttribute.enabledStackingOn(
                        item = item,
                        bottomItems = thisBottomPlacements.flatMap {
                            when (val unit = it.unit) {
                                is Item -> {
                                    it.toItemPlacementOrNull()?.let { itemPlacement ->
                                        listOf(itemPlacement)
                                    } ?: emptyList()
                                }

                                is ItemContainer<*> -> {
                                    unit.dump(it.position)
                                }

                                else -> {
                                    emptyList()
                                }
                            }.filter { bottomItem ->
                                bottomItem.maxY leq this@enabledStackingOn.y && bottomItem.overlappedOnBottom(item)
                            }
                        },
                        space = Container3Shape(space)
                    )
                })
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        return false
    }
}
/**
 * 判断物品在 Front 平面中是否允许堆叠在底部物品之上。
 * Check whether the item can be stacked on bottom items in the Front plane.
 *
 * @param bottomItems 底部物品列表 / list of bottom items
 * @param space 容器空间 / container space
 * @return 是否允许堆叠 / whether stacking is allowed
 */
@JvmName("itemPlacement2FrontEnabledStackingOn")
suspend fun QuantityPlacement2<Item, FltX, Front>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, FltX, Front>>,
    space: AbstractContainer2Shape<Front> = Container2Shape(plane = Front)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in toPlacement3()) {
                promises.add(async(Dispatchers.Default) {
                    val thisBottomPlacements = bottomPlacements.filterBottomOverlapped(item)
                    (view as ItemView).enabledStackingOn(
                        bottomSupport = bottomSupport(
                            unit = item,
                            bottomUnits = thisBottomPlacements,
                            shapeResolver = { placement ->
                                when (val placementUnit = placement.unit) {
                                    is Item -> placementUnit.packingShape
                                    else -> placement.view.asPackingShape3()
                                }
                            }
                        )
                    ) && unit.packageAttribute.enabledStackingOn(
                        item = item,
                        bottomItems = thisBottomPlacements.flatMap {
                            when (val unit = it.unit) {
                                is Item -> {
                                    it.toItemPlacementOrNull()?.let { itemPlacement ->
                                        listOf(itemPlacement)
                                    } ?: emptyList()
                                }

                                is ItemContainer<*> -> {
                                    unit.dump(it.position)
                                }

                                else -> {
                                    emptyList()
                                }
                            }
                        }.filter {
                            it.maxY leq this@enabledStackingOn.y && it.overlappedOnBottom(item)
                        },
                        space = Container3Shape(space)
                    )
                })
            }
            for (promise in promises) {
                if (!promise.await()) {
                    cancel()
                }
            }
            true
        }
    } catch (e: CancellationException) {
        return false
    }
}
/**
 * 判断物品在 3D 空间中是否允许堆叠在底部物品之上。
 * Check whether the item can be stacked on bottom items in 3D space.
 *
 * @param bottomItems 底部物品列表 / list of bottom items
 * @param space 容器空间 / container space
 * @return 是否允许堆叠 / whether stacking is allowed
 */
@JvmName("itemPlacement3EnabledStackingOn")
suspend fun QuantityPlacement3<Item, FltX>.enabledStackingOn(
    bottomItems: List<QuantityPlacement3<*, FltX>>,
    space: AbstractContainer3Shape = Container3Shape()
): Boolean {
    val shape = resolvedPackingShape()
    if (shape is CylinderPackingShape3 && shape.axis != Axis3.Y) {
        if (!hasHorizontalCylinderStackingSupportCoverage(
                item = this,
                bottomItems = bottomItems
            )
        ) {
            return false
        }
        return unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItems = bottomItems.filter {
                it.maxY leq this.y && it.overlappedOnBottom(this)
            }.flatMap {
                when (val unit = it.unit) {
                    is Item -> {
                        it.toItemPlacementOrNull()?.let { itemPlacement ->
                            listOf(itemPlacement)
                        } ?: emptyList()
                    }

                    is ItemContainer<*> -> {
                        unit.dump(it.position)
                    }

                    else -> {
                        emptyList()
                    }
                }
            },
            space = space
        )
    }
    return if (absoluteY eq FltX.zero) {
        unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItems = emptyList(),
            space = space
        )
    } else {
        (view as ItemView).enabledStackingOn(
            bottomSupport = bottomSupport(
                unit = this,
                bottomUnits = bottomItems,
                shapeResolver = { placement ->
                    when (val placementUnit = placement.unit) {
                        is Item -> placementUnit.packingShape
                        else -> placement.view.asPackingShape3()
                    }
                }
            )
        ) && unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItems = bottomItems.filter {
                it.maxY leq this.y && it.overlappedOnBottom(this)
            }.flatMap {
                when (val unit = it.unit) {
                    is Item -> {
                        it.toItemPlacementOrNull()?.let { itemPlacement ->
                            listOf(itemPlacement)
                        } ?: emptyList()
                    }

                    is ItemContainer<*> -> {
                        unit.dump(it.position)
                    }

                    else -> {
                        emptyList()
                    }
                }
            }.filter {
                it.overlappedOnBottom(this)
            },
            space = space
        )
    }
}

/** 将 Map<Item, UInt64> 扁平化为按数量重复的 Item 列表。Flatten Map<Item, UInt64> into a list of Items repeated by count. */
fun Map<Item, UInt64>.flatten(): List<Item> {
    return this.flatMap { (item, amount) ->
        if (amount == UInt64.zero) {
            emptyList()
        } else {
            (UInt64.zero until amount).map { _ -> item }
        }
    }
}

/**
 * 将 Pair<Item, UInt64> 的可迭代对象扁平化为按数量重复的 Item 列表。
 * Flatten an Iterable of Pair<Item, UInt64> into a list of Items repeated by count.
 *
 * @return 扁平化后的 Item 列表 / flattened list of Items
 */
fun Iterable<Pair<Item, UInt64>>.flatten(): List<Item> {
    return this.flatMap { (item, amount) ->
        if (amount == UInt64.zero) {
            emptyList()
        } else {
            (UInt64.zero until amount).map { _ -> item }
        }
    }
}

/**
 * 计算物品总数量。
 * Calculate total item count.
 *
 * @return 物品总数量 / total item count
 */
fun Map<Item, UInt64>.totalCount(): UInt64 {
    return this.values.fold(UInt64.zero) { acc, value -> acc + value }
}

/**
 * 计算物品总数量。
 * Calculate total item count.
 *
 * @return 物品总数量 / total item count
 */
fun Iterable<Pair<Item, UInt64>>.totalCount(): UInt64 {
    return this.fold(UInt64.zero) { acc, (_, value) -> acc + value }
}

/**
 * 将物品列表按其相等性分组并计数。
 * Group items by identity and count occurrences.
 *
 * @return 分组后的物品映射 / grouped item map
 */
fun List<Item>.group(): Map<Item, UInt64> {
    return this.groupBy { it }.map { Pair(it.key, UInt64(it.value.size)) }.toMap()
}

/**
 * 将容器树扁平化为物品放置列表。
 * Flatten the container tree into a list of item placements.
 *
 * @param offset 偏移量 / offset
 * @return 物品放置列表 / list of item placements
 */
fun List<QuantityPlacement3<*, FltX>>.dump(offset: Point<Dim3, FltX>): List<QuantityPlacement3<Item, FltX>> {
    return dump(point3FltX(offset))
}

/**
 * 将容器树扁平化为物品放置列表。
 * Flatten the container tree into a list of item placements.
 *
 * @param offset 偏移量 / offset
 * @return 物品放置列表 / list of item placements
 */
fun List<QuantityPlacement3<*, FltX>>.dump(offset: QuantityPoint3<FltX> = point3FltX()): List<QuantityPlacement3<Item, FltX>> {
    val offsetVector = QuantityVector3<FltX>(offset.x, offset.y, offset.z)
    val items = ArrayList<QuantityPlacement3<Item, FltX>>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                items.addAll((unit.units as List<QuantityPlacement3<*, FltX>>).dump(placement.position + offsetVector))
            }

            is Item -> {
                items.add(
                    itemPlacement3Of(
                        view = placement.view as ItemView,
                        position = placement.position + offsetVector
                    )
                )
            }

            else -> {}
        }
    }
    return items
}

/**
 * 将容器树扁平化为物品放置列表，使用绝对位置。
 * Flatten the container tree into a list of item placements using absolute positions.
 *
 * @param offset 偏移量 / offset
 * @return 物品放置列表 / list of item placements
 */
fun List<QuantityPlacement3<*, FltX>>.dumpAbsolutely(offset: Point<Dim3, FltX>): List<QuantityPlacement3<Item, FltX>> {
    return dumpAbsolutely(point3FltX(offset))
}

/**
 * 将容器树扁平化为物品放置列表，使用绝对位置。
 * Flatten the container tree into a list of item placements using absolute positions.
 *
 * @param offset 偏移量 / offset
 * @return 物品放置列表 / list of item placements
 */
fun List<QuantityPlacement3<*, FltX>>.dumpAbsolutely(offset: QuantityPoint3<FltX> = point3FltX()): List<QuantityPlacement3<Item, FltX>> {
    val offsetVector = QuantityVector3<FltX>(offset.x, offset.y, offset.z)
    val items = ArrayList<QuantityPlacement3<Item, FltX>>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                items.addAll((unit.units as List<QuantityPlacement3<*, FltX>>).dump(placement.absolutePosition + offsetVector))
            }

            is Item -> {
                items.add(
                    itemPlacement3Of(
                        view = placement.view as ItemView,
                        position = placement.absolutePosition + offsetVector
                    )
                )
            }

            else -> {}
        }
    }
    return items
}

@get:JvmName("projectionBottomOnly")
/** 投影底部限定属性。Projection bottom-only constraint. */
val Projection<*, FltX, *>.bottomOnly: Boolean
    get() {
        val u = unit
        return when (u) {
            is Item -> u.bottomOnly
            is ItemContainer<*> -> u.bottomOnly
            else -> false
        }
    }
