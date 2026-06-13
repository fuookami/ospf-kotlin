@file:Suppress("DEPRECATION")

/**
 * 放置平面桥接。
 * Placement plane bridge.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractContainer2Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container2Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Front
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PlaneProjection
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ProjectivePlane
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Side
import fuookami.ospf.kotlin.math.algebra.number.FltX
private fun <P : ProjectivePlane> QuantityPlacement2<*, FltX, P>.asSidePlacementOrNull(): QuantityPlacement2<*, FltX, Side>? {
    return if (plane == Side && projection is PlaneProjection<*, FltX, *>) {
        @Suppress("UNCHECKED_CAST")
        this as QuantityPlacement2<*, FltX, Side>
    } else {
        null
    }
}

private fun <P : ProjectivePlane> QuantityPlacement2<*, FltX, P>.asFrontPlacementOrNull(): QuantityPlacement2<*, FltX, Front>? {
    return if (plane == Front && projection is PlaneProjection<*, FltX, *>) {
        @Suppress("UNCHECKED_CAST")
        this as QuantityPlacement2<*, FltX, Front>
    } else {
        null
    }
}

private fun QuantityPlacement2<*, FltX, Side>.asItemSidePlacementOrNull(): QuantityPlacement2<Item, FltX, Side>? {
    val item = unit as? Item ?: return null
    val itemView = item.view(orientation) ?: return null
    return itemPlacement2Of(
        projection = PlaneProjection(
            view = itemView,
            plane = Side
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, FltX, Front>.asItemFrontPlacementOrNull(): QuantityPlacement2<Item, FltX, Front>? {
    val item = unit as? Item ?: return null
    val itemView = item.view(orientation) ?: return null
    return itemPlacement2Of(
        projection = PlaneProjection(
            view = itemView,
            plane = Front
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, FltX, Side>.asBlockSidePlacementOrNull(): QuantityPlacement2<Block, FltX, Side>? {
    val block = unit as? Block ?: return null
    val blockView = block.view(orientation) ?: return null
    return blockPlacement2Of(
        projection = PlaneProjection(
            view = blockView,
            plane = Side
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, FltX, Front>.asBlockFrontPlacementOrNull(): QuantityPlacement2<Block, FltX, Front>? {
    val block = unit as? Block ?: return null
    val blockView = block.view(orientation) ?: return null
    return blockPlacement2Of(
        projection = PlaneProjection(
            view = blockView,
            plane = Front
        ),
        position = position
    )
}

fun <P : ProjectivePlane> List<QuantityPlacement2<*, FltX, P>?>.toSidePlacements(): List<QuantityPlacement2<*, FltX, Side>> {
    return this.filterNotNull().mapNotNull { placement ->
        placement.asSidePlacementOrNull()
    }
}

fun <P : ProjectivePlane> List<QuantityPlacement2<*, FltX, P>?>.toFrontPlacements(): List<QuantityPlacement2<*, FltX, Front>> {
    return this.filterNotNull().mapNotNull { placement ->
        placement.asFrontPlacementOrNull()
    }
}

private fun <P : ProjectivePlane> AbstractContainer2Shape<P>.restSideSpace(position: QuantityPoint2<FltX>): Container2Shape<Side> {
    val rest = restSpace(position)
    return Container2Shape(
        length = rest.length,
        width = rest.width,
        plane = Side
    )
}

private fun <P : ProjectivePlane> AbstractContainer2Shape<P>.restFrontSpace(position: QuantityPoint2<FltX>): Container2Shape<Front> {
    val rest = restSpace(position)
    return Container2Shape(
        length = rest.length,
        width = rest.width,
        plane = Front
    )
}

suspend fun <P : ProjectivePlane> QuantityPlacement2<*, FltX, P>.enabledItemStackingOnPlane(
    bottomItems: List<QuantityPlacement2<*, FltX, P>?>,
    space: AbstractContainer2Shape<P>
): Boolean {
    return when (plane) {
        Side -> {
            val sidePlacement = asSidePlacementOrNull() ?: return false
            val itemPlacement = sidePlacement.asItemSidePlacementOrNull() ?: return false
            itemPlacement.enabledStackingOn(
                bottomItems = bottomItems.toSidePlacements(),
                space = space.restSideSpace(position)
            )
        }

        Front -> {
            val frontPlacement = asFrontPlacementOrNull() ?: return false
            val itemPlacement = frontPlacement.asItemFrontPlacementOrNull() ?: return false
            itemPlacement.enabledStackingOn(
                bottomItems = bottomItems.toFrontPlacements(),
                space = space.restFrontSpace(position)
            )
        }

        else -> true
    }
}

suspend fun <P : ProjectivePlane> QuantityPlacement2<*, FltX, P>.enabledBlockStackingOnPlane(
    bottomItems: List<QuantityPlacement2<*, FltX, P>?>,
    space: AbstractContainer2Shape<P>
): Boolean {
    return when (plane) {
        Side -> {
            val sidePlacement = asSidePlacementOrNull() ?: return false
            val blockPlacement = sidePlacement.asBlockSidePlacementOrNull() ?: return false
            blockPlacement.enabledStackingOn(
                bottomItems = bottomItems.toSidePlacements(),
                space = space.restSideSpace(position)
            )
        }

        Front -> {
            val frontPlacement = asFrontPlacementOrNull() ?: return false
            val blockPlacement = frontPlacement.asBlockFrontPlacementOrNull() ?: return false
            blockPlacement.enabledStackingOn(
                bottomItems = bottomItems.toFrontPlacements(),
                space = space.restFrontSpace(position)
            )
        }

        else -> true
    }
}
