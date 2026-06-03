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

/** 货物容器二维放置。2D placement of item container. */
typealias ItemContainerPlacement2<S, P> = QuantityPlacement2<S, P>
/** 货物容器侧视二维放置。Side-plane 2D placement of item container. */
typealias ItemContainerSidePlacement2<S> = ItemContainerPlacement2<S, Side>
/** 货物容器前视二维放置。Front-plane 2D placement of item container. */
typealias ItemContainerFrontPlacement2<S> = ItemContainerPlacement2<S, Front>
/** 货物容器三维放置。3D placement of item container. */
typealias ItemContainerPlacement3<S> = QuantityPlacement3<S>

@get:JvmName("itemContainerPackageType")
/** 货物容器视图包装类型属性（CuboidView-only compat）。CuboidView-only compat extension for container package type. */
val <S : ItemContainer<S>> CuboidView<S>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPackageCategory")
/** 货物容器视图包装类别属性（CuboidView-only compat）。CuboidView-only compat extension for container package category. */
val <S : ItemContainer<S>> CuboidView<S>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

@get:JvmName("itemContainerBottomOnly")
/** 货物容器视图仅底面放置属性（CuboidView-only compat）。CuboidView-only compat extension for container bottom-only placement. */
val <S : ItemContainer<S>> CuboidView<S>.bottomOnly: Boolean
    get() {
        return unit.bottomOnly
    }

@get:JvmName("itemContainerTopFlat")
/** 货物容器视图顶面平整属性（CuboidView-only compat）。CuboidView-only compat extension for container top-flat constraint. */
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
val <S : ItemContainer<S>> ItemContainerPlacement3<S>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPlacementPackageCategory")
val <S : ItemContainer<S>> ItemContainerPlacement3<S>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

fun <S : ItemContainer<S>> ItemContainerPlacement3<S>.dump(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dump(position + QuantityVector3(offset.x, offset.y, offset.z))
}

fun <S : ItemContainer<S>> ItemContainerPlacement3<S>.dumpAbsolutely(offset: QuantityPoint3 = point3()): List<ItemPlacement3> {
    return unit.dump(absolutePosition + QuantityVector3(offset.x, offset.y, offset.z))
}
@JvmName("itemContainerPlacement2SideEnabledStackingOn")
suspend fun <S : ItemContainer<S>> ItemContainerSidePlacement2<S>.enabledStackingOn(
    bottomItems: List<AnySidePlacement2>,
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
suspend fun <S : ItemContainer<S>> ItemContainerFrontPlacement2<S>.enabledStackingOn(
    bottomItems: List<AnyFrontPlacement2>,
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
suspend fun <S : ItemContainer<S>> ItemContainerPlacement3<S>.enabledStackingOn(
    bottomItems: List<AnyPlacement3>,
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



