package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.math.geometry.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

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
    val bottomOnlyHeight: Flt64
        get() = items.maxOfOrNull { item ->
            if (item.bottomOnly) {
                item.maxY
            } else {
                Flt64.zero
            }
        } ?: Flt64.zero

    val topFlat: Boolean get() = topPlacements(units).all { (it.view as ItemView).topFlat }

    // inherit from CuboidUnit<Block>
    override val enabledOrientations: List<Orientation> get() = listOf(Orientation.Upright)

    fun dump(offset: Point3 = point3()) = units.dump(offset)
    fun dumpAbsolutely(offset: Point3 = point3()) = units.dumpAbsolutely(offset)

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

fun <S : ItemContainer<S>> CuboidView<S>.dump(offset: Point3 = point3()): List<ItemPlacement3> {
    return unit.dump(offset)
}

fun <S : ItemContainer<S>> CuboidView<S>.dumpAbsolutely(offset: Point3 = point3()): List<ItemPlacement3> {
    return unit.dumpAbsolutely(offset)
}

@get:JvmName("itemContainerPlacementPackageType")
val <S : ItemContainer<S>> Placement3<S>.packageType: PackageType
    get() {
        return unit.packageType
    }

@get:JvmName("itemContainerPlacementPackageCategory")
val <S : ItemContainer<S>> Placement3<S>.packageCategory: PackageCategory
    get() {
        return unit.packageCategory
    }

fun <S : ItemContainer<S>> Placement3<S>.dump(offset: Point3 = point3()): List<ItemPlacement3> {
    return unit.dump(position + offset)
}

fun <S : ItemContainer<S>> Placement3<S>.dumpAbsolutely(offset: Point3 = point3()): List<ItemPlacement3> {
    return unit.dump(absolutePosition + offset)
}

@Suppress("UNCHECKED_CAST")
@JvmName("itemContainerPlacement2SideEnabledStackingOn")
suspend fun <S : ItemContainer<S>> Placement2<S, Side>.enabledStackingOn(
    bottomItems: List<Placement2<*, Side>>,
    space: AbstractContainer2Shape<Side> = Container2Shape(plane = Side)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    item as ItemPlacement3
                    item.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(space).restSpace(item.position)
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
@JvmName("itemContainerPlacement2FrontEnabledStackingOn")
suspend fun <S : ItemContainer<S>> Placement2<S, Front>.enabledStackingOn(
    bottomItems: List<Placement2<*, Front>>,
    space: AbstractContainer2Shape<Front> = Container2Shape(plane = Front)
): Boolean {
    val bottomPlacements = bottomItems.flatMap { it.toPlacement3() }
    return try {
        coroutineScope {
            val promises = ArrayList<Deferred<Boolean>>()
            for (item in bottomPlacements(toPlacement3().dump())) {
                promises.add(async(Dispatchers.Default) {
                    item as ItemPlacement3
                    item.enabledStackingOn(
                        bottomItems = bottomPlacements,
                        space = Container3Shape(space).restSpace(item.position)
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
@JvmName("itemContainerPlacement3EnabledStackingOn")
suspend fun <S : ItemContainer<S>> Placement3<S>.enabledStackingOn(
    bottomItems: List<Placement3<*>>,
    space: AbstractContainer3Shape = Container3Shape()
): Boolean {
    val thisBottomItems = bottomPlacements(this.dump()) as List<ItemPlacement3>
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
