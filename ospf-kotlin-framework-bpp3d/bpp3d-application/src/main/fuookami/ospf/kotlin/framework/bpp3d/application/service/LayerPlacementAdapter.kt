/**
 * 层放置适配器。
 * Layer placement adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.CirclePackingLayerGenerator

/**
 * 统一创建 BinLayer 的放置对象。
 * Create BinLayer placement via a unified adapter.
 */
internal fun BinLayer.toLayerPlacement(z: Quantity<FltX>? = null): QuantityPlacement3<BinLayer, FltX> {
    ensureGeneratedCylinderCandidatePath(
        layer = this
    )
    return toKnownCoordinateLayerPlacement(z)
}

/**
 * 已知坐标层放置构造，不执行默认候选路径的圆柱轴向门禁。
 * Known-coordinate layer placement builder, without the default candidate-path cylinder-axis guard.
 *
 * @param z 层深度坐标 / layer depth coordinate
 * @return 层放置 / layer placement
 */
internal fun BinLayer.toKnownCoordinateLayerPlacement(z: Quantity<FltX>? = null): QuantityPlacement3<BinLayer, FltX> {
    val position = if (z == null) {
        point3FltX()
    } else {
        point3FltX(z = z.value, unit = z.unit)
    }
    return binLayerPlacementOf(
        view = CuboidView<BinLayer, FltX>(copy()),
        position = position
    )
}

/**
 * 统一创建空层箱体，避免业务处散落 QuantityPlacement3 构造。
 * Build a bin with one placed layer via adapter.
 */
internal fun Bin<BinLayer, FltX>.withPlacedLayer(layer: BinLayer, z: Quantity<FltX>? = null): Bin<BinLayer, FltX> {
    return layerBinOf(
        shape = type,
        units = listOf(layer.toLayerPlacement(z)),
        batchNo = batchNo
    )
}

/**
 * 统一创建 item 放置对象，避免测试夹具散落构造。
 * Create item placement via a unified adapter for fixtures.
 */
internal fun Item.toItemPlacement(
    x: Quantity<FltX>? = null,
    y: Quantity<FltX>? = null,
    z: Quantity<FltX>? = null,
    orientation: Orientation = Orientation.Upright
): QuantityPlacement3<Item, FltX> {
    val origin = point3FltX()
    val position = point3(
        x = x ?: origin.x,
        y = y ?: origin.y,
        z = z ?: origin.z
    )
    return itemPlacement3Of(
        item = this,
        position = position,
        orientation = orientation
    )
}

/**
 * 应用层使用共享圆柱契约校验生成候选能力。
 * Application layer uses the shared cylinder contract to validate generated candidate capability.
 */
internal fun ensureGeneratedCylinderCandidatePath(layer: BinLayer) {
    for (placement in layer.units) {
        if (placement.unit !is Item) {
            continue
        }
        val shape = placement.resolvedPackingShape()
        requireVerifiedGeneratedCylinderCandidate(
            shape = shape,
            verifiedAxisAwareCandidate = layer.from == CirclePackingLayerGenerator::class,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )!!
    }
}
