/**
 * 层放置适配器。
 * Layer placement adapter.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.CirclePackingLayerGenerator

/**
 * 统一创建 BinLayer 的放置对象。
 * Create BinLayer placement via a unified adapter.
 *
 * @param z 层深度坐标，默认为原点 / layer depth coordinate, defaults to origin
 * @return 层放置 / layer placement
 */
internal fun BinLayer.toLayerPlacement(z: Quantity<FltX>? = null): Ret<QuantityPlacement3<BinLayer, FltX>> {
    when (val result = ensureGeneratedCylinderCandidatePath(
        layer = this
    )) {
        is Ok -> {}
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return Ok(toKnownCoordinateLayerPlacement(z))
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
 *
 * @param layer 待放置的箱层 / bin layer to place
 * @param z 层深度坐标，默认为原点 / layer depth coordinate, defaults to origin
 * @return 包含已放置层的箱体 / bin with the placed layer
 */
internal fun Bin<BinLayer, FltX>.withPlacedLayer(layer: BinLayer, z: Quantity<FltX>? = null): Ret<Bin<BinLayer, FltX>> {
    val placement = when (val result = layer.toLayerPlacement(z)) {
        is Ok -> result.value
        is Failed -> return Failed(result.error)
        is Fatal -> return Fatal(result.errors)
    }
    return Ok(layerBinOf(
        shape = type,
        units = listOf(placement),
        batchNo = batchNo
    ))
}

/**
 * 统一创建 item 放置对象，避免测试夹具散落构造。
 * Create item placement via a unified adapter for fixtures.
 *
 * @param x X 坐标，默认为原点 / X coordinate, defaults to origin
 * @param y Y 坐标，默认为原点 / Y coordinate, defaults to origin
 * @param z Z 坐标，默认为原点 / Z coordinate, defaults to origin
 * @param orientation 放置朝向 / placement orientation
 * @return 物品放置 / item placement
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
 *
 * @param layer 待校验的箱层 / bin layer to validate
 * @return 校验结果 / validation result
 */
internal fun ensureGeneratedCylinderCandidatePath(layer: BinLayer): Try {
    for (placement in layer.units) {
        if (placement.unit !is Item) {
            continue
        }
        val shape = placement.resolvedPackingShape()
        when (val result = requireVerifiedGeneratedCylinderCandidate(
            shape = shape,
            verifiedAxisAwareCandidate = layer.from == CirclePackingLayerGenerator::class,
            path = CylinderCapabilityPath.ApplicationLayerPlacementCandidate
        )) {
            is Ok -> {}
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
    }
    return ok
}
