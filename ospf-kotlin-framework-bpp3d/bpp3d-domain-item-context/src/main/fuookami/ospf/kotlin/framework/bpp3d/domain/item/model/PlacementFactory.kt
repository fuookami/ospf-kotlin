/**
 * Placement factory and bottom-overlap helpers.
 * 放置构造与底面重叠辅助。
*/
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

/**
 * 创建多态二维放置，仅供 BLA 多态投影搜索和多态工厂内部复用。
 * Create a polymorphic 2D placement only for BLA polymorphic projection search and polymorphic factory internals.
 *
 * 业务调用侧应优先使用 `itemPlacement2Of` 或 `blockPlacement2Of`，避免直接暴露底层 Cuboid 泛型。
 * Business callers should prefer `itemPlacement2Of` or `blockPlacement2Of` to avoid exposing the underlying Cuboid type parameter.
 *
 * @param projection 多态投影 / polymorphic projection
 * @param position 放置坐标 / placement position
 * @return 多态二维放置 / polymorphic 2D placement
*/
fun <T : Cuboid<T, FltX>, P : ProjectivePlane> placement2Of(
    projection: Projection<T, FltX, P>,
    position: QuantityPoint2<FltX>
): QuantityPlacement2<T, FltX, P> {
    return QuantityPlacement2(
        projection = projection,
        position = position
    )
}

/**
 * 创建 Item 二维放置，隐藏底层 Cuboid 泛型工厂。
 * Create an item 2D placement while hiding the underlying Cuboid polymorphic factory.
 *
 * @param projection item 投影 / item projection
 * @param position 放置坐标 / placement position
 * @return item 二维放置 / item 2D placement
*/
fun <P : ProjectivePlane> itemPlacement2Of(
    projection: Projection<Item, FltX, P>,
    position: QuantityPoint2<FltX>
): QuantityPlacement2<Item, FltX, P> {
    return placement2Of(
        projection = projection,
        position = position
    )
}

/**
 * 创建 Block 二维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a Block 2D placement while hiding the underlying Cuboid polymorphic factory.
 *
 * @param projection block 投影 / block projection
 * @param position 放置坐标 / placement position
 * @return Block 二维放置 / Block 2D placement
*/
fun <P : ProjectivePlane> blockPlacement2Of(
    projection: Projection<Block, FltX, P>,
    position: QuantityPoint2<FltX>
): QuantityPlacement2<Block, FltX, P> {
    return placement2Of(
        projection = projection,
        position = position
    )
}

private fun <T : Cuboid<T, FltX>> placement3Of(
    view: CuboidView<T, FltX>,
    position: QuantityPoint3<FltX>
): QuantityPlacement3<T, FltX> {
    return QuantityPlacement3(
        view = view,
        position = position
    )
}

/**
 * 创建 Item 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create an item 3D placement while hiding the underlying Cuboid polymorphic factory.
 *
 * @param view item 视图 / item view
 * @param position 放置坐标 / placement position
 * @return item 三维放置 / item 3D placement
*/
fun itemPlacement3Of(
    view: ItemView,
    position: QuantityPoint3<FltX>
): QuantityPlacement3<Item, FltX> {
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
    position: QuantityPoint3<FltX>,
    orientation: Orientation = Orientation.Upright
): QuantityPlacement3<Item, FltX> {
    return itemPlacement3Of(
        view = item.view(orientation),
        position = position
    )
}

/**
 * 创建 BinLayer 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a BinLayer 3D placement while hiding the underlying Cuboid polymorphic factory.
 *
 * @param view layer 视图 / layer view
 * @param position 放置坐标 / placement position
 * @return BinLayer 三维放置 / BinLayer 3D placement
*/
fun binLayerPlacementOf(
    view: CuboidView<BinLayer, FltX>,
    position: QuantityPoint3<FltX>
): QuantityPlacement3<BinLayer, FltX> {
    return placement3Of(
        view = view,
        position = position
    )
}

/**
 * 创建 Block 三维放置，隐藏底层 Cuboid 泛型工厂。
 * Create a Block 3D placement while hiding the underlying Cuboid polymorphic factory.
 *
 * @param view block 视图 / block view
 * @param position 放置坐标 / placement position
 * @return Block 三维放置 / Block 3D placement
*/
fun blockPlacement3Of(
    view: CuboidView<Block, FltX>,
    position: QuantityPoint3<FltX>
): QuantityPlacement3<Block, FltX> {
    return placement3Of(
        view = view,
        position = position
    )
}

private fun <T : Cuboid<T, FltX>> QuantityPlacement3<T, FltX>.bottomPlacement(): QuantityPlacement2<T, FltX, Bottom> {
    return QuantityPlacement2(this, Bottom)
}

/**
 * Checks whether this placement overlaps with another on the bottom projection plane.
 * 检查此放置是否与另一个放置在底面投影上重叠。
 *
 * @param other the placement to check overlap against / 待检查重叠的另一个放置
 * @return whether the two placements overlap on the bottom plane / 两个放置是否在底面投影上重叠
*/
fun QuantityPlacement3<*, FltX>.overlappedOnBottom(other: QuantityPlacement3<*, FltX>): Boolean {
    return bottomPlacement().overlapped(other.bottomPlacement()).value == true
}

/**
 * Iterable.
 * Iterable。
 * @param target target placement / 目标放置
 * @return placements overlapping the target on the bottom / 在底面与目标重叠的放置列表
*/
fun Iterable<QuantityPlacement3<*, FltX>>.filterBottomOverlapped(
    target: QuantityPlacement3<*, FltX>
): List<QuantityPlacement3<*, FltX>> {
    val targetBottomPlacement = target.bottomPlacement()
    return filter { placement ->
        placement.bottomPlacement().overlapped(targetBottomPlacement).value == true
    }
}
