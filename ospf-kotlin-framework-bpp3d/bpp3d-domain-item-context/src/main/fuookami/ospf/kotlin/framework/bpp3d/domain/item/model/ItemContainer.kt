/**
 * Item container model.
 * 货物容器模型。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
/**
 * ItemContainer interface.
 * ItemContainer接口。
*/
sealed interface ItemContainer<S : ItemContainer<S>> : Container3CuboidUnit<S, FltX>, ItemMergeUnit, Eq<ItemContainer<S>> {
    val items: List<QuantityPlacement3<Item, FltX>> get() = dump()

    val packageType: PackageType
        get() = units.minOfWithThreeWayComparatorOrNull({ lhs, rhs -> lhs ord rhs }) {
            when (val unit = it.unit) {
                is Item -> unit.packageType
                is ItemContainer<*> -> unit.packageType
                else -> PackageType.CartonContainer
            }
        } ?: PackageType.CartonContainer
    val packageCategory get() = packageType.category

    val bottomOnly: Boolean get() = bottomPlacements(units).any { (it.view as ItemView).bottomOnly }
    val bottomOnlyHeight: Quantity<FltX>
        get() = items.maxOfOrNullQuantity { item ->
            if (item.bottomOnly) {
                item.maxY
            } else {
                shape.height * FltX.zero
            }
        } ?: (shape.height * FltX.zero)

    val topFlat: Boolean get() = topPlacements(units).all { (it.view as ItemView).topFlat }

    // inherit from CuboidUnit<Block>
    override val enabledOrientations: List<Orientation> get() = listOf(Orientation.Upright)

/**
 * dump.
 * dump。
 * @param offset position offset for the dumped placements / 转储放置的位置偏移
*/
    fun dump(offset: QuantityPoint3<FltX> = point3FltX()) = units.dump(offset)

/**
 * Dumps absolutely.
 * 转储Absolutely。
 * @param offset position offset for the dumped placements / 转储放置的位置偏移
*/
    fun dumpAbsolutely(offset: QuantityPoint3<FltX> = point3FltX()) = units.dumpAbsolutely(offset)

    override fun view(orientation: Orientation): CuboidView<S, FltX>? = CuboidView(copy(), orientation)

    override fun partialEq(rhs: ItemContainer<S>): Boolean {
        if (shape neq rhs.shape) {
            return false
        }

        if (units.size != rhs.units.size) {
            return false
        }

        for (i in units.indices) {
            if (units[i].unit != rhs.units[i].unit || units[i].position neq rhs.units[i].position) {
                return false
            }
        }

        return true
    }
}

@get:JvmName("itemContainerPlacementPackageType")
val <S : ItemContainer<S>> QuantityPlacement3<S, FltX>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPlacementPackageCategory")
val <S : ItemContainer<S>> QuantityPlacement3<S, FltX>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

fun <S : ItemContainer<S>> QuantityPlacement3<S, FltX>.dump(offset: QuantityPoint3<FltX> = point3FltX()): List<QuantityPlacement3<Item, FltX>> {
    return unit.dump(position + QuantityVector3<FltX>(offset.x, offset.y, offset.z))
}

fun <S : ItemContainer<S>> QuantityPlacement3<S, FltX>.dumpAbsolutely(offset: QuantityPoint3<FltX> = point3FltX()): List<QuantityPlacement3<Item, FltX>> {
    return unit.dump(absolutePosition + QuantityVector3<FltX>(offset.x, offset.y, offset.z))
}
@JvmName("itemContainerPlacement2SideEnabledStackingOn")
suspend fun <S : ItemContainer<S>> QuantityPlacement2<S, FltX, Side>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, FltX, Side>>,
    space: AbstractContainer2Shape<Side> = Container2Shape(plane = Side)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    val itemPlacement = item.toItemPlacementOrNull() ?: return@async false
                    val restSpace = Container3Shape(space).restSpace(itemPlacement.position)
                    itemPlacement.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(
                            width = restSpace.width,
                            height = restSpace.height,
                            depth = restSpace.depth
                        )
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
@JvmName("itemContainerPlacement2FrontEnabledStackingOn")
suspend fun <S : ItemContainer<S>> QuantityPlacement2<S, FltX, Front>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, FltX, Front>>,
    space: AbstractContainer2Shape<Front> = Container2Shape(plane = Front)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    val itemPlacement = item.toItemPlacementOrNull() ?: return@async false
                    val restSpace = Container3Shape(space).restSpace(itemPlacement.position)
                    itemPlacement.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(
                            width = restSpace.width,
                            height = restSpace.height,
                            depth = restSpace.depth
                        )
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
@JvmName("itemContainerPlacement3EnabledStackingOn")
suspend fun <S : ItemContainer<S>> QuantityPlacement3<S, FltX>.enabledStackingOn(
    bottomItems: List<QuantityPlacement3<*, FltX>>,
    space: AbstractContainer3Shape = Container3Shape()
): Boolean {
    val thisBottomItems = bottomItemPlacements(this.dump())
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in thisBottomItems) {
                promises.add(async(Dispatchers.Default) {
                    item.enabledStackingOn(
                        bottomItems = bottomItems,
                        space = space
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
