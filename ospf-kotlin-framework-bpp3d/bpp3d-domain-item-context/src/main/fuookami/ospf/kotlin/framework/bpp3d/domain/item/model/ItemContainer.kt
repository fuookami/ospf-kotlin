@file:Suppress("DEPRECATION")

/**
 * 货物容器模型。
 * Item container model.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraNegativeInfinity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraOne
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.utils.functional.minOfWithThreeWayComparator
import fuookami.ospf.kotlin.utils.functional.Eq
import fuookami.ospf.kotlin.utils.functional.ord
import kotlinx.coroutines.*







sealed interface ItemContainer<S : ItemContainer<S>> : Container3CuboidUnit<S>, Eq<ItemContainer<S>> {
    val items: List<ItemPlacement3> get() = dump()

    val packageType: PackageType
        get() = units.minOfWithThreeWayComparator({ lhs, rhs -> lhs ord rhs }) {
            when (val unit = it.unit) {
                is Item -> unit.packageType
                is ItemContainer<*> -> unit.packageType
                else -> PackageType.CartonContainer
            }
        }
    val packageCategory get() = packageType.category

    val bottomOnly: Boolean get() = bottomPlacements(units).any { (it.view as ItemView).bottomOnly }
    val bottomOnlyHeight: Quantity<InfraNumber>
        get() = items.maxOfOrNull { item ->
            if (item.bottomOnly) {
                item.maxY
            } else {
                shape.height * infraZero()
            }
        } ?: (shape.height * infraZero())

    val topFlat: Boolean get() = topPlacements(units).all { (it.view as ItemView).topFlat }

    // inherit from CuboidUnit<Block>
    override val enabledOrientations: List<Orientation> get() = listOf(Orientation.Upright)

    fun dump(offset: QuantityPoint3 = point3()) = units.dump(offset)
    fun dumpAbsolutely(offset: QuantityPoint3 = point3()) = units.dumpAbsolutely(offset)

    override fun view(orientation: Orientation): CuboidView<S>? = CuboidView(copy(), orientation)

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

@get:JvmName("itemContainerPackageType")
val <S : ItemContainer<S>> CuboidView<S>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPackageCategory")
val <S : ItemContainer<S>> CuboidView<S>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemContainerBottomOnly")
val <S : ItemContainer<S>> CuboidView<S>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemContainerTopFlat")
val <S : ItemContainer<S>> CuboidView<S>.topFlat: Boolean
    get() {
        return unit.topFlat
    }

fun <S : ItemContainer<S>> CuboidView<S>.dump(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dump(offset)
}

fun <S : ItemContainer<S>> CuboidView<S>.dumpAbsolutely(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dumpAbsolutely(offset)
}

@get:JvmName("itemContainerPlacementPackageType")
val <S : ItemContainer<S>> QuantityPlacement3<S>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPlacementPackageCategory")
val <S : ItemContainer<S>> QuantityPlacement3<S>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

fun <S : ItemContainer<S>> QuantityPlacement3<S>.dump(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dump(position + QuantityVector3(offset.x, offset.y, offset.z))
}

fun <S : ItemContainer<S>> QuantityPlacement3<S>.dumpAbsolutely(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dump(absolutePosition + QuantityVector3(offset.x, offset.y, offset.z))
}
@JvmName("itemContainerPlacement2SideEnabledStackingOn")
suspend fun <S : ItemContainer<S>> QuantityPlacement2<S, Side>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, Side>>,
    space: AbstractContainer2Shape<Side> = Container2Shape(plane = Side)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    val itemPlacement = item.toItemPlacementOrNull() ?: return@async false
                    itemPlacement.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(space).restSpace(itemPlacement.position)
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
suspend fun <S : ItemContainer<S>> QuantityPlacement2<S, Front>.enabledStackingOn(
    bottomItems: List<QuantityPlacement2<*, Front>>,
    space: AbstractContainer2Shape<Front> = Container2Shape(plane = Front)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    val itemPlacement = item.toItemPlacementOrNull() ?: return@async false
                    itemPlacement.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(space).restSpace(itemPlacement.position)
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
suspend fun <S : ItemContainer<S>> QuantityPlacement3<S>.enabledStackingOn(
    bottomItems: List<QuantityPlacement3<*>>,
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



