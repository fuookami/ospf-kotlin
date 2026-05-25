@file:Suppress("DEPRECATION")

package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.AbstractContainer2Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Container2Shape
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Cuboid
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Front
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PlaneProjection
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPlacement2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.ProjectivePlane
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.QuantityPoint2
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Side

private fun <T : Cuboid<T>, P : ProjectivePlane> PlaneProjection<T, P>.asSideProjectionOrNull(): PlaneProjection<T, Side>? {
    return if (plane == Side) {
        PlaneProjection(view = view, plane = Side)
    } else {
        null
    }
}

private fun <T : Cuboid<T>, P : ProjectivePlane> PlaneProjection<T, P>.asFrontProjectionOrNull(): PlaneProjection<T, Front>? {
    return if (plane == Front) {
        PlaneProjection(view = view, plane = Front)
    } else {
        null
    }
}

private fun <P : ProjectivePlane> QuantityPlacement2<*, P>.asSidePlacementOrNull(): QuantityPlacement2<*, Side>? {
    val sideProjection = (projection as? PlaneProjection<*, P>)?.asSideProjectionOrNull() ?: return null
    return QuantityPlacement2(
        projection = sideProjection,
        position = position
    )
}

private fun <P : ProjectivePlane> QuantityPlacement2<*, P>.asFrontPlacementOrNull(): QuantityPlacement2<*, Front>? {
    val frontProjection = (projection as? PlaneProjection<*, P>)?.asFrontProjectionOrNull() ?: return null
    return QuantityPlacement2(
        projection = frontProjection,
        position = position
    )
}

private fun QuantityPlacement2<*, Side>.asItemSidePlacementOrNull(): ItemPlacement2<Side>? {
    val item = unit as? Item ?: return null
    val itemView = item.view(orientation) ?: return null
    return QuantityPlacement2(
        projection = PlaneProjection(
            view = itemView,
            plane = Side
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, Front>.asItemFrontPlacementOrNull(): ItemPlacement2<Front>? {
    val item = unit as? Item ?: return null
    val itemView = item.view(orientation) ?: return null
    return QuantityPlacement2(
        projection = PlaneProjection(
            view = itemView,
            plane = Front
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, Side>.asBlockSidePlacementOrNull(): BlockPlacement2<Side>? {
    val block = unit as? Block ?: return null
    val blockView = block.view(orientation) ?: return null
    return QuantityPlacement2(
        projection = PlaneProjection(
            view = blockView,
            plane = Side
        ),
        position = position
    )
}

private fun QuantityPlacement2<*, Front>.asBlockFrontPlacementOrNull(): BlockPlacement2<Front>? {
    val block = unit as? Block ?: return null
    val blockView = block.view(orientation) ?: return null
    return QuantityPlacement2(
        projection = PlaneProjection(
            view = blockView,
            plane = Front
        ),
        position = position
    )
}

fun <P : ProjectivePlane> List<QuantityPlacement2<*, P>?>.toSidePlacements(): List<QuantityPlacement2<*, Side>> {
    return this.filterNotNull().mapNotNull { placement ->
        placement.asSidePlacementOrNull()
    }
}

fun <P : ProjectivePlane> List<QuantityPlacement2<*, P>?>.toFrontPlacements(): List<QuantityPlacement2<*, Front>> {
    return this.filterNotNull().mapNotNull { placement ->
        placement.asFrontPlacementOrNull()
    }
}

private fun <P : ProjectivePlane> AbstractContainer2Shape<P>.restSideSpace(position: QuantityPoint2): Container2Shape<Side> {
    val rest = restSpace(position)
    return Container2Shape(
        length = rest.length,
        width = rest.width,
        plane = Side
    )
}

private fun <P : ProjectivePlane> AbstractContainer2Shape<P>.restFrontSpace(position: QuantityPoint2): Container2Shape<Front> {
    val rest = restSpace(position)
    return Container2Shape(
        length = rest.length,
        width = rest.width,
        plane = Front
    )
}

suspend fun <P : ProjectivePlane> QuantityPlacement2<*, P>.enabledItemStackingOnPlane(
    bottomItems: List<QuantityPlacement2<*, P>?>,
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

suspend fun <P : ProjectivePlane> QuantityPlacement2<*, P>.enabledBlockStackingOnPlane(
    bottomItems: List<QuantityPlacement2<*, P>?>,
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
