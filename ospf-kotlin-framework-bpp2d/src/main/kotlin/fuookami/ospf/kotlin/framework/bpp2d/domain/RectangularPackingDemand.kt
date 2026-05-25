package fuookami.ospf.kotlin.framework.bpp2d.domain

import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

data class RectangleItem2<V : FloatingNumber<V>>(
    val id: String,
    val width: Quantity<V>,
    val height: Quantity<V>,
    val allowRotate: Boolean = false
)

data class Sheet2<V : FloatingNumber<V>>(
    val id: String,
    val width: Quantity<V>,
    val height: Quantity<V>
)

data class Projection2Need<V : FloatingNumber<V>>(
    val width: Quantity<V>,
    val height: Quantity<V>
) {
    val area: Quantity<V> get() = width * height
}

data class Placement2Need<V : FloatingNumber<V>>(
    val x: Quantity<V>,
    val y: Quantity<V>,
    val projection: Projection2Need<V>
) {
    val maxX: Quantity<V> get() = quantityPlus(x, projection.width)
    val maxY: Quantity<V> get() = quantityPlus(y, projection.height)

    fun toBox2Need(): Box2Need<V> {
        return Box2Need(
            minX = x,
            minY = y,
            maxX = maxX,
            maxY = maxY
        )
    }
}

data class Box2Need<V : FloatingNumber<V>>(
    val minX: Quantity<V>,
    val minY: Quantity<V>,
    val maxX: Quantity<V>,
    val maxY: Quantity<V>
) {
    val width: Quantity<V> get() = quantityMinus(maxX, minX)
    val height: Quantity<V> get() = quantityMinus(maxY, minY)
    val area: Quantity<V> get() = width * height

    fun overlaps(rhs: Box2Need<V>): Boolean {
        if (quantityOrd(maxX, rhs.minX, "maxX-rhsMinX") !is Order.Greater) {
            return false
        }
        if (quantityOrd(minX, rhs.maxX, "minX-rhsMaxX") !is Order.Less) {
            return false
        }
        if (quantityOrd(maxY, rhs.minY, "maxY-rhsMinY") !is Order.Greater) {
            return false
        }
        if (quantityOrd(minY, rhs.maxY, "minY-rhsMaxY") !is Order.Less) {
            return false
        }
        return true
    }

    fun intersect(rhs: Box2Need<V>): Box2Need<V>? {
        val left = quantityMax(minX, rhs.minX, "left")
        val right = quantityMin(maxX, rhs.maxX, "right")
        val bottom = quantityMax(minY, rhs.minY, "bottom")
        val top = quantityMin(maxY, rhs.maxY, "top")
        if (quantityOrd(left, right, "x-range") !is Order.Less) {
            return null
        }
        if (quantityOrd(bottom, top, "y-range") !is Order.Less) {
            return null
        }
        return Box2Need(
            minX = left,
            minY = bottom,
            maxX = right,
            maxY = top
        )
    }

    fun inside(sheet: Box2Need<V>): Boolean {
        val minXOrd = quantityOrd(minX, sheet.minX, "sheet-minX")
        val minYOrd = quantityOrd(minY, sheet.minY, "sheet-minY")
        val maxXOrd = quantityOrd(maxX, sheet.maxX, "sheet-maxX")
        val maxYOrd = quantityOrd(maxY, sheet.maxY, "sheet-maxY")
        return (minXOrd is Order.Greater || minXOrd is Order.Equal)
                && (minYOrd is Order.Greater || minYOrd is Order.Equal)
                && (maxXOrd is Order.Less || maxXOrd is Order.Equal)
                && (maxYOrd is Order.Less || maxYOrd is Order.Equal)
    }
}

data class PlannedRectangle2<V : FloatingNumber<V>>(
    val item: RectangleItem2<V>,
    val x: Quantity<V>,
    val y: Quantity<V>,
    val rotated: Boolean = false
) {
    fun toPlacement2Need(): Placement2Need<V> {
        val projection = if (rotated && item.allowRotate) {
            Projection2Need(
                width = item.height,
                height = item.width
            )
        } else {
            Projection2Need(
                width = item.width,
                height = item.height
            )
        }
        return Placement2Need(
            x = x,
            y = y,
            projection = projection
        )
    }

    fun toBox2Need(): Box2Need<V> = toPlacement2Need().toBox2Need()
}

data class PackingScene2<V : FloatingNumber<V>>(
    val sheet: Sheet2<V>,
    val placements: List<PlannedRectangle2<V>>
) {
    val sheetArea: Quantity<V> get() = sheet.width * sheet.height

    fun sheetBox2Need(): Box2Need<V> {
        val zeroX = quantityZeroOf(sheet.width)
        val zeroY = quantityZeroOf(sheet.height)
        return Box2Need(
            minX = zeroX,
            minY = zeroY,
            maxX = sheet.width,
            maxY = sheet.height
        )
    }

    fun allInsideSheet(): Boolean {
        val sheetBox = sheetBox2Need()
        return placements.all { it.toBox2Need().inside(sheetBox) }
    }

    fun usedArea(): Quantity<V> {
        var acc = quantityZeroOf(sheetArea)
        for (placement in placements) {
            acc = quantityPlus(acc, placement.toPlacement2Need().projection.area)
        }
        return acc
    }

    fun remainingArea(): Quantity<V> {
        return quantityMinus(sheetArea, usedArea())
    }

    fun utilization(): V {
        val used = usedArea().convertTo(sheetArea.unit)
            ?: throw IllegalArgumentException("Cannot convert used area to sheet area unit.")
        return used.value / sheetArea.value
    }

    fun overlappedPairs(): List<Pair<String, String>> {
        val result = ArrayList<Pair<String, String>>()
        for (i in placements.indices) {
            val lhs = placements[i]
            val lhsBox = lhs.toBox2Need()
            for (j in i + 1 until placements.size) {
                val rhs = placements[j]
                val rhsBox = rhs.toBox2Need()
                if (lhsBox.overlaps(rhsBox)) {
                    result += lhs.item.id to rhs.item.id
                }
            }
        }
        return result
    }

    fun illegalOverlaps(): List<Pair<String, String>> {
        return overlappedPairs()
    }
}

private fun <V : FloatingNumber<V>> quantityPlus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value + rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value + converted.value, lhs.unit)
}

private fun <V : FloatingNumber<V>> quantityMinus(lhs: Quantity<V>, rhs: Quantity<V>): Quantity<V> {
    if (lhs.unit == rhs.unit) {
        return Quantity(lhs.value - rhs.value, lhs.unit)
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: throw IllegalArgumentException("Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return Quantity(lhs.value - converted.value, lhs.unit)
}

private fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Order {
    return lhs.partialOrd(rhs)
        ?: throw IllegalArgumentException("Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

private fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

private fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis)) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

private fun <V : FloatingNumber<V>> quantityZeroOf(quantity: Quantity<V>): Quantity<V> {
    return quantity.value.constants.zero * quantity.unit
}
