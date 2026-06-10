@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.concept.Indexed
import fuookami.ospf.kotlin.utils.concept.ManualIndexed
import fuookami.ospf.kotlin.utils.functional.Extractor
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.math.geometry.Dim3
import fuookami.ospf.kotlin.math.geometry.Point
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import kotlinx.coroutines.*







data class PriorityAttribute(
    val key: String,
    private val extractor: Extractor<String?, ActualItem>,
    val value: UInt64
) {
    private fun attribute(item: ActualItem) = extractor(item)

    operator fun invoke(item: ActualItem): UInt64? {
        return if (attribute(item) == key) {
            value
        } else {
            null
        }
    }
}

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

open class ItemPattern(
    val shape: PackageShape<InfraNumber>,
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

@Suppress("UNCHECKED_CAST")
private fun itemPackQuantityToInfra(value: Quantity<*>): Quantity<InfraNumber> {
    return when (value.value) {
        is InfraNumber -> value as Quantity<InfraNumber>
        else -> Quantity(InfraNumber(value.value.toString().toDouble()), value.unit)
    }
}

interface Item : Cuboid<Item>, Indexed {
    override val self: Item
        get() = this
    val explicitPackingShape: PackingShape3<InfraNumber>?
        get() = null
    val packingShapeSpec: PackageShapeSpec?
        get() = null
    val packingShape: PackingShape3<InfraNumber>
        get() = explicitPackingShape
            ?: packageShape.toPackingShapeOrNull()
            ?: view().asPackingShape3()
    /** 包围盒（从 packingShape 派生，不直接依赖 Cuboid 继承）。Bounding box derived from packingShape, not directly from Cuboid inheritance. */
    val shapeBoundingBox: ShapeBoundingBox3<InfraNumber>
        get() = packingShape.boundingBox
    /** 底面轮廓（从 packingShape 派生，不直接依赖 Cuboid 继承）。Bottom footprint derived from packingShape, not directly from Cuboid inheritance. */
    val shapeFootprint: ShapeFootprint2<InfraNumber>
        get() = packingShape.footprint()
    /** 形状实际体积（从 packingShape 派生，不直接依赖 Cuboid 继承）。Shape geometric volume derived from packingShape, not directly from Cuboid inheritance. */
    val shapeVolume: Quantity<InfraNumber>
        get() = packingShape.actualVolume
    val batchNo: BatchNo?
    val priorities: Map<String, UInt64>
    val warehouse: String?
    val packageAttribute: PackageAttribute
    val materialAmounts: Map<MaterialKey, UInt64>
        get() = emptyMap()
    val materialWeights: Map<MaterialKey, Quantity<InfraNumber>>
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

    fun enabledStackingOn(bottomSupport: BottomSupport): Boolean {
        return packageAttribute.enabledStackingOn(
            unit = this,
            bottomSupport = bottomSupport
        )
    }

    fun enabledStackingOn(
        bottomItem: Item,
        layer: UInt64 = UInt64.zero,
        height: Quantity<InfraNumber> = width * infraZero(),
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        return packageAttribute.enabledStackingOn(
            item = this,
            bottomItem = bottomItem,
            layer = layer,
            height = height,
            space = space
        )
    }

    override fun view(orientation: Orientation): ItemView {
        return ItemView(this, orientation)
    }
}

open class ActualItem(
    val id: String,
    val name: String,
    val packageCode: PackageCode? = null,
    val pack: Package<*>? = null,
    val priorityAttribute: List<PriorityAttribute> = emptyList(),
    // inherited from Cuboid<Item>
    override val width: Quantity<InfraNumber>,
    override val height: Quantity<InfraNumber>,
    override val depth: Quantity<InfraNumber>,
    override val weight: Quantity<InfraNumber>,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute,
    val shapeSpecOverride: PackageShapeSpec? = null,
    val packingShapeOverride: PackingShape3<InfraNumber>? = null,
    val materialAmountsOverride: Map<MaterialKey, UInt64>? = null,
    val materialWeightsOverride: Map<MaterialKey, Quantity<InfraNumber>>? = null
) : Item, ManualIndexed() {
    override val explicitPackingShape: PackingShape3<InfraNumber>?
        get() = packingShapeOverride
    override val packingShapeSpec: PackageShapeSpec?
        get() = shapeSpecOverride ?: (pack?.shape as? PackageShape<InfraNumber>)?.shapeSpec
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
    override val materialWeights: Map<MaterialKey, Quantity<InfraNumber>> by lazy {
        materialWeightsOverride?.let {
            return@lazy it
        }
        val counter = HashMap<MaterialKey, Quantity<InfraNumber>>()
        for ((material, amount) in pack?.materials ?: emptyMap()) {
            val key = material.key
            val weight = material.weight * InfraNumber(amount.toULong().toDouble())
            counter[key] = (counter[key] ?: (weight * infraZero())) + weight
        }
        counter
    }

    constructor(
        id: String,
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
        width = itemPackQuantityToInfra(pack.width),
        height = itemPackQuantityToInfra(pack.height),
        depth = itemPackQuantityToInfra(pack.depth),
        weight = itemPackQuantityToInfra(pack.weight),
        enabledOrientations = enabledOrientations,
        batchNo = batchNo,
        warehouse = warehouse,
        packageAttribute = packageAttribute
    )

    override fun toString(): String {
        return this.name
    }
}

open class PatternedItem(
    private val actualItems: List<Triple<ActualItem, UInt64, ValueRange<UInt64>>>,
    // inherited from Cuboid<Item>
    override val width: Quantity<InfraNumber>,
    override val height: Quantity<InfraNumber>,
    override val depth: Quantity<InfraNumber>,
    override val weight: Quantity<InfraNumber>,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val priorities: Map<String, UInt64> = emptyMap(),
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute
) : Item, ManualIndexed() {
    override val volume: Quantity<InfraNumber> = run {
        val totalAmount = actualItems.fold(infraZero()) { acc, (_, amount, _) -> acc + InfraNumber(amount.toULong().toDouble()) }
        if (totalAmount eq infraZero()) {
            width * height * depth * infraZero()
        } else {
            actualItems.sumOf { it.first.volume * InfraNumber(it.second.toULong().toDouble()) } / totalAmount
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
    override val materialWeights: Map<MaterialKey, Quantity<InfraNumber>> by lazy {
        val counter = HashMap<MaterialKey, Quantity<InfraNumber>>()
        for ((item, amount, _) in actualItems) {
            for ((material, weight) in item.materialWeights) {
                val thisWeight = weight * InfraNumber(amount.toULong().toDouble())
                counter[material] = (counter[material] ?: (thisWeight * infraZero())) + thisWeight
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
            val amountRange = actualItems.fold(ValueRange(UInt64.zero, UInt64.zero).value!!) { acc, triple -> acc + triple.second }
            val volume = actualItems.sumOf { it.first.volume * InfraNumber(it.second.toULong().toDouble()) } / InfraNumber(amount.toULong().toDouble())
            val deformation = pattern.packageAttribute.deformationAttribute.deformationQuantity(volume.value)
            return Triple(
                PatternedItem(
                    actualItems = actualItems,
                width = pattern.shape.width + deformation[0],
                height = pattern.shape.height + deformation[1],
                depth = pattern.shape.depth + deformation[2],
                weight = actualItems.sumOf { it.first.weight * InfraNumber(it.second.toULong().toDouble()) } / InfraNumber(amount.toULong().toDouble()),
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
        space: AbstractContainer2Shape<*>,
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
        space: AbstractContainer3Shape,
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
        actualOrientations.removeAll { packageAttribute.extraOrientationRule?.let { it1 -> it1(space, it) } == false }
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

open class ItemView(
    unit: Item,
    orientation: Orientation = Orientation.Upright
) : CuboidView<Item>(unit, orientation) {
    /** 放置级装载形状覆盖。Placement-level packing shape override. */
    open val placementPackingShape: PackingShape3<InfraNumber>? get() = null
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

    fun enabledStackingOn(bottomSupport: BottomSupport): Boolean {
        return unit.packageAttribute.enabledStackingOn(
            unit = this,
            bottomSupport = bottomSupport
        )
    }

    fun enabledStackingOn(
        bottomItem: ItemView?,
        layer: UInt64 = UInt64.zero,
        height: Quantity<InfraNumber> = this.height * infraZero(),
        space: AbstractContainer3Shape = Container3Shape()
    ): Boolean {
        return unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItem = bottomItem,
            layer = layer,
            height = height,
            space = space
        )
    }

    override val rotation: ItemView?
        get() {
            return super.rotation?.let { ItemView(it.unit, it.orientation) }
        }

    override fun rotationAt(space: AbstractContainer2Shape<*>): ItemView? {
        return super.rotationAt(space)?.let { ItemView(it.unit, it.orientation) }
    }

    override fun rotationAt(space: AbstractContainer3Shape): ItemView? {
        return super.rotationAt(space)?.let { ItemView(it.unit, it.orientation) }
    }

    override fun copy(): ItemView {
        return ItemView(
            unit,
            orientation
        )
    }
}

/** 货物投影别名，用于 item-domain 的二维装载语义。Item projection alias for item-domain 2D loading semantics. */
typealias ItemProjection<P> = Projection<Item, P>
/** 多层货物投影别名，用于 item-domain 的堆叠投影语义。Multi-pile item projection alias for item-domain stacking projection semantics. */
typealias MultipleItemProjection<P> = MultiPileProjection<Item, P>
/** 任意二维放置。Generic 2D placement. */
typealias AnyPlacement2<P> = QuantityPlacement2<*, P>
/** 任意侧视二维放置。Generic side-plane 2D placement. */
typealias AnySidePlacement2 = AnyPlacement2<Side>
/** 任意前视二维放置。Generic front-plane 2D placement. */
typealias AnyFrontPlacement2 = AnyPlacement2<Front>
/** 任意三维放置。Generic 3D placement. */
typealias AnyPlacement3 = QuantityPlacement3<*>
/** 货物二维放置别名，用于隐藏底层 QuantityPlacement2 泛型。Item 2D placement alias that hides the underlying QuantityPlacement2 generic. */
typealias ItemPlacement2<P> = QuantityPlacement2<Item, P>
/** 货物三维放置别名，用于隐藏底层 QuantityPlacement3 泛型。Item 3D placement alias that hides the underlying QuantityPlacement3 generic. */
typealias ItemPlacement3 = QuantityPlacement3<Item>

/**
 * 解析放置级真实装载形状，优先使用 ItemView 携带的候选几何。
 * Resolve placement-level packing shape, preferring candidate geometry carried by ItemView.
 *
 * @return 放置级装载形状 / placement-level packing shape
 */
fun AnyPlacement3.resolvedPackingShape(): PackingShape3<InfraNumber> {
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
val ItemProjection<*>.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemPlacement2Type")
val ItemPlacement2<*>.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemPlacement3Type")
val ItemPlacement3.type: ItemType
    get() {
        return (view as ItemView).type
    }

@get:JvmName("itemProjectionPackageType")
val ItemProjection<*>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemPlacement2PackageType")
val ItemPlacement2<*>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemPlacement3PackageType")
val ItemPlacement3.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemProjectionPackageCategory")
val ItemProjection<*>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement2PackageCategory")
val ItemPlacement2<*>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement3PackageCategory")
val ItemPlacement3.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemPlacement2BottomOnly")
val ItemPlacement2<*>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemPlacement3BottomOnly")
val ItemPlacement3.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemProjectionTopFlat")
val ItemProjection<*>.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

@get:JvmName("itemPlacement2TopFlat")
val ItemPlacement2<*>.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

@get:JvmName("itemPlacement3TopFlat")
val ItemPlacement3.topFlat: Boolean
    get() {
        return (view as ItemView).topFlat
    }

private fun AnyPlacement3.toHorizontalCylinderSupportGeometry(): HorizontalCylinderSupportGeometry {
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

private fun hasHorizontalCylinderStackingSupportCoverage(
    item: ItemPlacement3,
    bottomItems: List<AnyPlacement3>
): Boolean {
    val cylinder = item.resolvedPackingShape() as? CylinderPackingShape3 ?: return true
    val axis = cylinder.axis
    if (axis == Axis3.Y || item.absoluteY eq infraZero()) {
        return true
    }

    return horizontalCylinderCuboidSupportCoverage(
        cylinder = item.toHorizontalCylinderSupportGeometry(),
        axis = axis,
        supports = bottomItems.map { candidate -> candidate.toHorizontalCylinderSupportGeometry() }
    )
}

@JvmName("itemPlacement2SideEnabledStackingOn")
suspend fun ItemPlacement2<Side>.enabledStackingOn(
    bottomItems: List<AnySidePlacement2>,
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
@JvmName("itemPlacement2FrontEnabledStackingOn")
suspend fun ItemPlacement2<Front>.enabledStackingOn(
    bottomItems: List<AnyFrontPlacement2>,
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
@JvmName("itemPlacement3EnabledStackingOn")
suspend fun ItemPlacement3.enabledStackingOn(
    bottomItems: List<AnyPlacement3>,
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
    return if (absoluteY eq infraZero()) {
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

fun Map<Item, UInt64>.flatten(): List<Item> {
    return this.flatMap { (item, amount) ->
        if (amount == UInt64.zero) {
            emptyList()
        } else {
            (UInt64.zero until amount).map { _ -> item }
        }
    }
}

fun Iterable<Pair<Item, UInt64>>.flatten(): List<Item> {
    return this.flatMap { (item, amount) ->
        if (amount == UInt64.zero) {
            emptyList()
        } else {
            (UInt64.zero until amount).map { _ -> item }
        }
    }
}

fun Map<Item, UInt64>.totalCount(): UInt64 {
    return this.values.fold(UInt64.zero) { acc, value -> acc + value }
}

fun Iterable<Pair<Item, UInt64>>.totalCount(): UInt64 {
    return this.fold(UInt64.zero) { acc, (_, value) -> acc + value }
}

fun List<Item>.group(): Map<Item, UInt64> {
    return this.groupBy { it }.map { Pair(it.key, UInt64(it.value.size)) }.toMap()
}

fun List<AnyPlacement3>.dump(offset: Point<Dim3, InfraNumber>): List<ItemPlacement3> {
    return dump(point3(offset))
}

fun List<AnyPlacement3>.dump(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    val offsetVector = QuantityVector3(offset.x, offset.y, offset.z)
    val items = ArrayList<ItemPlacement3>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*> -> {
                items.addAll(unit.units.dump(placement.position + offsetVector))
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

fun List<AnyPlacement3>.dumpAbsolutely(offset: Point<Dim3, InfraNumber>): List<ItemPlacement3> {
    return dumpAbsolutely(point3(offset))
}

fun List<AnyPlacement3>.dumpAbsolutely(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    val offsetVector = QuantityVector3(offset.x, offset.y, offset.z)
    val items = ArrayList<ItemPlacement3>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*> -> {
                items.addAll(unit.units.dump(placement.absolutePosition + offsetVector))
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
val Projection<*, *>.bottomOnly: Boolean
    get() {
        val u = unit
        return when (u) {
            is Item -> u.bottomOnly
            is ItemContainer<*> -> u.bottomOnly
            else -> false
        }
    }

