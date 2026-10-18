/**
 * 装箱几何守卫。 / Packing geometry guard.
 *
 * 对一批二维放置做三项校验：是否全部位于板材内、是否两两不重叠、旋转是否合法。重叠判定直接复用
 * `LayerPlacement2.overlapped`，因此矩形×矩形、圆×圆、圆×矩形的语义统一为「内部重叠」——**相切不算重叠**。
 * / Validates a batch of 2D placements on three points: all inside the sheet, pairwise disjoint, and legal
 * rotation. Overlap delegates to `LayerPlacement2.overlapped`, so rectangle-rectangle, circle-circle and
 * circle-rectangle all share the "interior overlap" semantics where **tangency is not an overlap**.
*/
package fuookami.ospf.kotlin.framework.bpp2d.domain.packing.service

import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.AbstractItem
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.ItemId

/**
 * 判断形状旋转 90 度后是否保持不变。 / Check whether a shape is unchanged by a 90-degree rotation.
 *
 * 包围宽高相等的形状（圆形件与正方形件）旋转后不变，因此任何旋转都是合法的；宽高不等时旋转会改变占用
 * 区域，需要物品显式允许。 / A shape whose bounding width equals its bounding height (circle or square) is
 * invariant under rotation, so any rotation is legal; otherwise rotation changes the occupied region and
 * needs the item's explicit allowance.
 *
 * @param shape 装载形状 / packing shape
 * @return 是否旋转不变 / whether rotation invariant
*/
private fun isRotationInvariant(shape: PackingShape2<FltX>): Boolean {
    return quantityEqual(shape.boundingWidth, shape.boundingHeight)
}

/**
 * 旋转合法性规则。 / Rotation legality rule.
 *
 * 几何层无法判断「物品是否允许旋转」，因此由调用方以本规则注入该知识。 / The geometry layer cannot know
 * whether an item allows rotation, so callers inject that knowledge through this rule.
*/
fun interface PackingRotationRule {

    /**
     * 判断单个放置的旋转是否合法。 / Check whether the rotation of one placement is legal.
     *
     * @param placement 待检查的放置 / placement to check
     * @return 是否合法 / whether legal
    */
    fun allow(placement: LayerPlacement2<FltX>): Boolean

    companion object {

        /** 始终允许旋转：调用方已在上游校验过。 / Always allow rotation: the caller already validated it upstream. */
        val AllowAll: PackingRotationRule = PackingRotationRule { true }

        /** 仅允许旋转不变的形状（圆形件、正方形件）。 / Allow only rotation-invariant shapes (circles, squares). */
        val SymmetricOnly: PackingRotationRule = PackingRotationRule { placement ->
            !placement.rotated || isRotationInvariant(placement.shape)
        }

        /**
         * 依据物品目录判断旋转合法性：目录中缺失的物品视为允许旋转。 / Build a rule from the item catalogue;
         * items missing from the catalogue are treated as allowing rotation.
         *
         * @param items 物品编号到物品的映射 / item identifier to item mapping
         * @return 旋转合法性规则 / rotation legality rule
        */
        fun byItemCatalog(items: Map<ItemId, AbstractItem<FltX>>): PackingRotationRule {
            return PackingRotationRule { placement ->
                if (!placement.rotated || isRotationInvariant(placement.shape)) {
                    return@PackingRotationRule true
                }
                val itemId = placement.itemId ?: return@PackingRotationRule true
                val item = items[ItemId(itemId)] ?: return@PackingRotationRule true
                item.allowRotate
            }
        }
    }
}

/**
 * 装箱几何守卫。 / Packing geometry guard.
 *
 * 所有方法都返回 `Try`，业务错误以 `Failed` 表达，不做抛出、不使用非空断言。 / Every method returns `Try`;
 * business errors are expressed as `Failed` with no throwing and no non-null assertions.
*/
object PackingGeometryGuard {

    /**
     * 校验放置集合的纯几何约束：全部位于板材内且两两不重叠。 / Validate the pure geometric constraints of a
     * placement list: all inside the sheet and pairwise disjoint.
     *
     * @param sheet 板材几何 / sheet geometry
     * @param placements 放置列表 / placement list
     * @param sheetName 板材实例名称，用于错误信息 / sheet instance name used in error messages
     * @param source 调用来源 / call source
     * @param kerf 刀缝间距，非空时要求相邻件间隙不小于该值 / kerf spacing; when present, neighbours must keep at
     * least this gap
     * @return 校验结果 / validation result
    */
    fun requireSheetGeometry(
        sheet: Sheet2Geometry<FltX>,
        placements: List<LayerPlacement2<FltX>>,
        sheetName: String = "sheet",
        source: String = "PackingGeometryGuard.requireSheetGeometry",
        kerf: Quantity<FltX>? = null
    ): Try {
        val mixedDiagnostic = mixedShapeDiagnostic(
            placements.groupingBy { it.shape.shapeType }.eachCount()
        )
        for ((index, placement) in placements.withIndex()) {
            val inside = when (val result = placement.insideSheet(sheet)) {
                is Ok -> result.value
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
            if (!inside) {
                return Failed(
                    Bpp2dValidationError(
                        unsupportedOutsideSheetGeometryMessage(
                            source = source,
                            sheetName = sheetName,
                            itemIndex = index,
                            diagnostic = placementShapeDiagnostic(placement.shape, placement.rotated),
                            mixedDiagnostic = mixedDiagnostic
                        )
                    )
                )
            }
        }
        for (lhsIndex in placements.indices) {
            for (rhsIndex in (lhsIndex + 1) until placements.size) {
                // 刀缝：把左侧件按 kerf 膨胀后再与右侧件比较，真实间隙小于 kerf 时判为重叠。
                // Kerf: inflate the left placement by the kerf before comparing, so a real gap below the kerf
                // counts as an overlap.
                // 膨胀必须双向检查：只有把任一侧膨胀后相交，才说明真实间隙小于 kerf。
                // Inflating must be checked in both directions: only an intersection after inflating either side
                // proves that the real gap is below the kerf.
                val overlapped = if (kerf != null && quantityIsPositive(kerf)) {
                    when (val forward = placements[lhsIndex].inflatedBy(kerf).overlapped(placements[rhsIndex])) {
                        is Ok -> if (forward.value) {
                            true
                        } else {
                            when (val backward = placements[lhsIndex].overlapped(placements[rhsIndex].inflatedBy(kerf))) {
                                is Ok -> backward.value
                                is Failed -> return Failed(backward.error)
                                is Fatal -> return Fatal(backward.errors)
                            }
                        }

                        is Failed -> return Failed(forward.error)
                        is Fatal -> return Fatal(forward.errors)
                    }
                } else {
                    when (val result = placements[lhsIndex].overlapped(placements[rhsIndex])) {
                        is Ok -> result.value
                        is Failed -> return Failed(result.error)
                        is Fatal -> return Fatal(result.errors)
                    }
                }
                if (overlapped) {
                    return Failed(
                        Bpp2dValidationError(
                            unsupportedPlacementOverlapMessage(
                                source = source,
                                sheetName = sheetName,
                                lhsIndex = lhsIndex,
                                lhsDiagnostic = placementShapeDiagnostic(
                                    shape = placements[lhsIndex].shape,
                                    rotated = placements[lhsIndex].rotated
                                ),
                                rhsIndex = rhsIndex,
                                rhsDiagnostic = placementShapeDiagnostic(
                                    shape = placements[rhsIndex].shape,
                                    rotated = placements[rhsIndex].rotated
                                ),
                                mixedDiagnostic = mixedDiagnostic
                            )
                        )
                    )
                }
            }
        }
        return ok
    }

    /**
     * 完整校验入口：先做几何校验，再按旋转规则校验旋转合法性。 / Full validation entry point: the geometry
     * check first, then the rotation legality check under the given rule.
     *
     * @param sheet 板材几何 / sheet geometry
     * @param placements 放置列表 / placement list
     * @param sheetName 板材实例名称，用于错误信息 / sheet instance name used in error messages
     * @param source 调用来源 / call source
     * @param rotationRule 旋转合法性规则 / rotation legality rule
     * @param kerf 刀缝间距 / kerf spacing
     * @return 校验结果 / validation result
    */
    fun validate(
        sheet: Sheet2Geometry<FltX>,
        placements: List<LayerPlacement2<FltX>>,
        sheetName: String = "sheet",
        source: String = "PackingGeometryGuard.validate",
        rotationRule: PackingRotationRule = PackingRotationRule.AllowAll,
        kerf: Quantity<FltX>? = null
    ): Try {
        val geometry = requireSheetGeometry(
            sheet = sheet,
            placements = placements,
            sheetName = sheetName,
            source = source,
            kerf = kerf
        )
        when (geometry) {
            is Ok -> {}
            is Failed -> return Failed(geometry.error)
            is Fatal -> return Fatal(geometry.errors)
        }
        val mixedDiagnostic = mixedShapeDiagnostic(
            placements.groupingBy { it.shape.shapeType }.eachCount()
        )
        for ((index, placement) in placements.withIndex()) {
            if (rotationRule.allow(placement)) {
                continue
            }
            return Failed(
                Bpp2dValidationError(
                    unsupportedIllegalRotationMessage(
                        source = source,
                        sheetName = sheetName,
                        itemIndex = index,
                        diagnostic = placementShapeDiagnostic(placement.shape, placement.rotated),
                        itemId = placement.itemId,
                        mixedDiagnostic = mixedDiagnostic
                    )
                )
            )
        }
        return ok
    }
}
