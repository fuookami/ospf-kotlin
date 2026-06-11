@file:Suppress("DEPRECATION")

/**
 * 放置构造与底面重叠辅助。
 * Placement factory and bottom-overlap helpers.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Bottom
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CuboidView
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ProjectivePlane
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Projection
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement3
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint3

/**
 * 创建泛型二维放置，仅供 BLA 泛型投影搜索和 generic factory 内部复用。
 * Create a generic 2D placement only for BLA generic projection search and generic factory internals.
 *
 * 业务调用侧应优先使用 `itemPlacement2Of` 或 `blockPlacement2Of`，避免直接暴露底层 Cuboid 泛型。
 * Business callers should prefer `itemPlacement2Of` or `blockPlacement2Of` to avoid exposing the underlying Cuboid generic.
 *
 * @param projection 泛型投影 / generic projection
 * @param position 放置坐标 / placement position
 * @return 泛型二维放置 / generic 2D placement
 */
fun <T : Cuboid<T>, P : ProjectivePlane> placement2Of(
    projection: Projection<T, P>,
    position: QuantityPoint2
): QuantityPlacement2<T, P> {
    return QuantityPlacement2(
        projection = projection,
        position = position
    )
}

/**
 * 创建 Item 二维放置，隐藏底层 Cuboid 泛型工厂。
 * Create an item 2D placement while hiding the underlying Cuboid generic factory.
 *
 * @param projection item 投影 / item projection
 * @param position 放置坐标 / placement position
 * @return item 二维放置 / item 2D placement
 */
fun <P : ProjectivePlane> itemPlacement2Of(
    projection: ItemProjection<P>,
    position: QuantityPoint2
): ItemPlacement2<P> {
    return placement2Of(
        projection = projection,
        position = position
    )
}

/**
 * 创建 Block 二维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a Block 2D placement while hiding the underlying Cuboid generic factory.
 *
 * @param projection block 投影 / block projection
 * @param position 放置坐标 / placement position
 * @return Block 二维放置 / Block 2D placement
 */
fun <P : ProjectivePlane> blockPlacement2Of(
    projection: Projection<Block, P>,
    position: QuantityPoint2
): BlockPlacement2<P> {
    return placement2Of(
        projection = projection,
        position = position
    )
}

private fun <T : Cuboid<T>> placement3Of(
    view: CuboidView<T>,
    position: QuantityPoint3
): QuantityPlacement3<T> {
    return QuantityPlacement3(
        view = view,
        position = position
    )
}

/**
 * 创建 Item 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create an item 3D placement while hiding the underlying Cuboid generic factory.
 *
 * @param view item 视图 / item view
 * @param position 放置坐标 / placement position
 * @return item 三维放置 / item 3D placement
 */
fun itemPlacement3Of(
    view: ItemView,
    position: QuantityPoint3
): ItemPlacement3 {
    return placement3Of(
        view = view,
        position = position
    )
}

/**
 * 创建 Item 三维放置，使用指定姿态构造视图。
 * Create an item 3D placement by building the view from an orientation.
 *
 * @param item item 单元 / item unit
 * @param position 放置坐标 / placement position
 * @param orientation 放置姿态 / placement orientation
 * @return item 三维放置 / item 3D placement
 */
fun itemPlacement3Of(
    item: Item,
    position: QuantityPoint3,
    orientation: Orientation = Orientation.Upright
): ItemPlacement3 {
    return itemPlacement3Of(
        view = item.view(orientation),
        position = position
    )
}

/**
 * 创建 BinLayer 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a BinLayer 3D placement while hiding the underlying Cuboid generic factory.
 *
 * @param view layer 视图 / layer view
 * @param position 放置坐标 / placement position
 * @return BinLayer 三维放置 / BinLayer 3D placement
 */
fun binLayerPlacementOf(
    view: BinLayerView,
    position: QuantityPoint3
): BinLayerPlacement {
    return placement3Of(
        view = view,
        position = position
    )
}

/**
 * 创建 Block 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a Block 3D placement while hiding the underlying Cuboid generic factory.
 *
 * @param view block 视图 / block view
 * @param position 放置坐标 / placement position
 * @return Block 三维放置 / Block 3D placement
 */
fun blockPlacement3Of(
    view: BlockView,
    position: QuantityPoint3
): BlockPlacement3 {
    return placement3Of(
        view = view,
        position = position
    )
}

private fun <T : Cuboid<T>> QuantityPlacement3<T>.bottomPlacement(): QuantityPlacement2<T, Bottom> {
    return QuantityPlacement2(this, Bottom)
}

fun AnyPlacement3.overlappedOnBottom(other: AnyPlacement3): Boolean {
    return bottomPlacement().overlapped(other.bottomPlacement())
}

fun Iterable<AnyPlacement3>.filterBottomOverlapped(
    target: AnyPlacement3
): List<AnyPlacement3> {
    val targetBottomPlacement = target.bottomPlacement()
    return filter { placement ->
        placement.bottomPlacement().overlapped(targetBottomPlacement)
    }
}
