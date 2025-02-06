package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.math.value_range.*
import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

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
    val shape: PackageShape,
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

interface Item : Cuboid<Item>, Indexed {
    val batchNo: BatchNo?
    val priorities: Map<String, UInt64>
    val warehouse: String?
    val packageAttribute: PackageAttribute

    val packageType get() = packageAttribute.packageType
    val packageBottomShape get() = packageShape.bottomShape
    val packageShape get() = PackageShape(width, height, depth, weight, packageType)
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
        height: Flt64 = Flt64.zero,
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
    val pack: Package? = null,
    val priorityAttribute: List<PriorityAttribute> = emptyList(),
    // inherited from Cuboid<Item>
    override val width: Flt64,
    override val height: Flt64,
    override val depth: Flt64,
    override val weight: Flt64,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute
) : Item, ManualIndexed() {
    override val priorities = priorityAttribute.mapNotNull { it(this)?.let { value -> Pair(it.key, value) } }.toMap()

    constructor(
        id: String,
        name: String,
        pack: Package,
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
        width = pack.width,
        height = pack.height,
        depth = pack.depth,
        weight = pack.weight,
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
    override val width: Flt64,
    override val height: Flt64,
    override val depth: Flt64,
    override val weight: Flt64,
    // inherited from CuboidItem<Item>
    override val enabledOrientations: List<Orientation>,
    // inherited from Item
    override val priorities: Map<String, UInt64> = emptyMap(),
    override val batchNo: BatchNo? = null,
    override val warehouse: String? = null,
    override val packageAttribute: PackageAttribute
) : Item, ManualIndexed() {
    override val volume = actualItems.sumOf { it.first.volume * it.second.toFlt64() } / actualItems.sumOf { it.second.toFlt64() }

    companion object {
        operator fun invoke(
            pattern: ItemPattern,
            actualItems: List<Triple<ActualItem, UInt64, ValueRange<UInt64>>>
        ): Triple<PatternedItem, UInt64, ValueRange<UInt64>> {
            val amount = actualItems.sumOf { it.second }
            val amountRange = actualItems.fold(ValueRange(UInt64.zero, UInt64.zero).value!!) { acc, triple -> acc + triple.second }
            val volume = actualItems.sumOf { it.first.volume * it.second.toFlt64() } / amount.toFlt64()
            val deformation = pattern.packageAttribute.deformationAttribute.deformationQuantity(volume)
            return Triple(PatternedItem(
                actualItems = actualItems,
                width = pattern.shape.width + deformation.x,
                height = pattern.shape.height + deformation.y,
                depth = pattern.shape.depth + deformation.z,
                weight = actualItems.sumOf { it.first.weight * it.second.toFlt64() } / amount.toFlt64(),
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
        height: Flt64 = Flt64.zero,
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

typealias ItemProjection<P> = Projection<Item, P>
typealias MultipleItemProjection<P> = MultiPileProjection<Item, P>
typealias ItemPlacement2<P> = Placement2<Item, P>
typealias ItemPlacement3 = Placement3<Item>

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
val ItemProjection<*,>.packageType: PackageType
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

@get:JvmName("itemProjectionBottomOnly")
val ItemProjection<*>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
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
        return view.topFlat
    }

@get:JvmName("itemPlacement2TopFlat")
val ItemPlacement2<*>.topFlat: Boolean
    get() {
        return view.topFlat
    }

@get:JvmName("itemPlacement3TopFlat")
val ItemPlacement3.topFlat: Boolean
    get() {
        return view.topFlat
    }

@Suppress("UNCHECKED_CAST")
@JvmName("itemPlacement2SideEnabledStackingOn")
suspend fun ItemPlacement2<Side>.enabledStackingOn(
    bottomItems: List<Placement2<*, Side>>,
    space: AbstractContainer2Shape<Side> = Container2Shape(plane = Side)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in toPlacement3()) {
                promises.add(async(Dispatchers.Default) {
                    val bottomPlacement = Placement2(item, Bottom)
                    val thisBottomPlacements = bottomPlacements.filter { Placement2(it, Bottom).overlapped(bottomPlacement) }
                    (view as ItemView).enabledStackingOn(
                        bottomSupport = bottomSupport(
                            unit = item,
                            bottomUnits = thisBottomPlacements
                        )
                    ) && unit.packageAttribute.enabledStackingOn(
                        item = item,
                        bottomItems = thisBottomPlacements.flatMap {
                            when (val unit = it.unit) {
                                is Item -> {
                                    listOf(it as ItemPlacement3)
                                }

                                is ItemContainer<*> -> {
                                    unit.dump(it.position)
                                }

                                else -> {
                                    emptyList()
                                }
                            }.filter { placement3 ->
                                it.maxY leq this@enabledStackingOn.y && Placement2(placement3, Bottom).overlapped(bottomPlacement)
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

@Suppress("UNCHECKED_CAST")
@JvmName("itemPlacement2FrontEnabledStackingOn")
suspend fun ItemPlacement2<Front>.enabledStackingOn(
    bottomItems: List<Placement2<*, Front>>,
    space: AbstractContainer2Shape<Front> = Container2Shape(plane = Front)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in toPlacement3()) {
                promises.add(async(Dispatchers.Default) {
                    val bottomPlacement = Placement2(item, Bottom)
                    val thisBottomPlacements = bottomPlacements.filter { Placement2(it, Bottom).overlapped(bottomPlacement) }
                    (view as ItemView).enabledStackingOn(
                        bottomSupport = bottomSupport(
                            unit = item,
                            bottomUnits = thisBottomPlacements
                        )
                    ) && unit.packageAttribute.enabledStackingOn(
                        item = item,
                        bottomItems = thisBottomPlacements.flatMap {
                            when (val unit = it.unit) {
                                is Item -> {
                                    listOf(it as ItemPlacement3)
                                }

                                is ItemContainer<*> -> {
                                    unit.dump(it.position)
                                }

                                else -> {
                                    emptyList()
                                }
                            }
                        }.filter {
                            it.maxY leq this@enabledStackingOn.y && Placement2(it, Bottom).overlapped(bottomPlacement)
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

@Suppress("UNCHECKED_CAST")
@JvmName("itemPlacement3EnabledStackingOn")
suspend fun ItemPlacement3.enabledStackingOn(
    bottomItems: List<Placement3<*>>,
    space: AbstractContainer3Shape = Container3Shape()
): Boolean {
    val bottomPlacement = Placement2(this, Bottom)
    return if (absoluteY eq Flt64.zero) {
        unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItems = emptyList(),
            space = space
        )
    } else {
        (view as ItemView).enabledStackingOn(
            bottomSupport = bottomSupport(
                unit = this,
                bottomUnits = bottomItems
            )
        ) && unit.packageAttribute.enabledStackingOn(
            item = this,
            bottomItems = bottomItems.filter {
                it.maxY leq this.y && Placement2(it, Bottom).overlapped(bottomPlacement)
            }.flatMap {
                when (val unit = it.unit) {
                    is Item -> {
                        listOf(it as ItemPlacement3)
                    }

                    is ItemContainer<*> -> {
                        unit.dump(it.position)
                    }

                    else -> {
                        emptyList()
                    }
                }
            }.filter {
                Placement2(it, Bottom).overlapped(bottomPlacement)
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
    return this.values.sumOf { it }
}

fun Iterable<Pair<Item, UInt64>>.totalCount(): UInt64 {
    return this.sumOf { it.second }
}

fun List<Item>.group(): Map<Item, UInt64> {
    return this.groupBy { it }.map { Pair(it.key, UInt64(it.value.size)) }.toMap()
}

fun List<Placement3<*>>.dump(offset: Point3 = point3()): List<ItemPlacement3> {
    val items = ArrayList<ItemPlacement3>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*> -> {
                items.addAll(unit.units.dump(placement.position + offset))
            }

            is Item -> {
                items.add(Placement3(placement.view as ItemView, placement.position + offset))
            }

            else -> {}
        }
    }
    return items
}

fun List<Placement3<*>>.dumpAbsolutely(offset: Point3 = point3()): List<ItemPlacement3> {
    val items = ArrayList<ItemPlacement3>()
    for (placement in this) {
        when (val unit = placement.unit) {
            is Container3<*> -> {
                items.addAll(unit.units.dump(placement.absolutePosition + offset))
            }

            is Item -> {
                items.add(Placement3(placement.view as ItemView, placement.absolutePosition + offset))
            }

            else -> {}
        }
    }
    return items
}

@get:JvmName("cuboidUnitPackageType")
val Cuboid<*>.packageType: PackageType
    get() {
        return when (this) {
            is Item -> packageType
            is ItemContainer<*> -> packageType
            else -> PackageType.CartonContainer
        }
    }

@get:JvmName("cuboidViewPackageType")
val CuboidView<*>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("cuboidProjectionPackageType")
val Projection<*, *>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("cuboidPlacement2PackageType")
val Placement2<*, *>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("cuboidPlacement3PackageType")
val Placement3<*>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("cuboidPackageCategory")
val AbstractCuboid.packageCategory: PackageCategory
    get() {
        return when (this) {
            is Item -> packageCategory
            is ItemContainer<*> -> packageCategory
            else -> PackageCategory.SoftBox
        }
    }

@get:JvmName("cuboidViewPackageCategory")
val CuboidView<*>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("cuboidProjectionPackageCategory")
val Projection<*, *>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("cuboidPlacement2PackageCategory")
val Placement2<*, *>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("cuboidPlacement3PackageCategory")
val Placement3<*>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("cuboidUnitBottomOnly")
val Cuboid<*>.bottomOnly: Boolean
    get() {
        return when (this) {
            is Item -> bottomOnly
            is ItemContainer<*> -> bottomOnly
            else -> false
        }
    }

@get:JvmName("cuboidViewBottomOnly")
val CuboidView<*>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("cuboidProjectionBottomOnly")
val Projection<*, *>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("cuboidPlacement2BottomOnly")
val Placement2<*, *>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("cuboidPlacement3BottomOnly")
val Placement3<*>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("cuboidUnitTopFlat")
val Cuboid<*>.topFlat: Boolean
    get() {
        return when (this) {
            is Item -> topFlat
            is ItemContainer<*> -> topFlat
            else -> false
        }
    }

@get:JvmName("cuboidViewTopFlat")
val CuboidView<*>.topFlat: Boolean
    get() {
        return when (unit) {
            is Item -> (this as ItemView).topFlat
            is ItemContainer<*> -> (unit as ItemContainer<*>).topFlat
            else -> false
        }
    }

@get:JvmName("cuboidProjectionTopFlat")
val Projection<*, *>.topFlat: Boolean
    get() {
        return unit.topFlat
    }

@get:JvmName("cuboidPlacement2TopFlat")
val Placement2<*, *>.topFlat: Boolean
    get() {
        return unit.topFlat
    }

@get:JvmName("cuboidPlacement3TopFlat")
val Placement3<*>.topFlat: Boolean
    get() {
        return unit.topFlat
    }
