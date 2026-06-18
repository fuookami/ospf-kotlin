package fuookami.ospf.kotlin.framework.bpp2d.domain

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.convertTo
import fuookami.ospf.kotlin.quantities.quantity.partialOrd
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.utils.functional.Order

/** 二维矩形物料项 / 2D rectangle item
 * @param V 数值类型 / numeric type
 */
data class RectangleItem2<V : FloatingNumber<V>>(
    /** 物料标识 / Item identifier */
    val id: String,
    /** 宽度 / Width */
    val width: Quantity<V>,
    /** 高度 / Height */
    val height: Quantity<V>,
    /** 是否允许旋转 / Whether rotation is allowed */
    val allowRotate: Boolean = false
)

/** 二维板材 / 2D sheet
 * @param V 数值类型 / numeric type
 */
data class Sheet2<V : FloatingNumber<V>>(
    /** 板材标识 / Sheet identifier */
    val id: String,
    /** 宽度 / Width */
    val width: Quantity<V>,
    /** 高度 / Height */
    val height: Quantity<V>
)

/** 二维投影需求 / 2D projection need
 * @param V 数值类型 / numeric type
 */
data class Projection2Need<V : FloatingNumber<V>>(
    /** 宽度 / Width */
    val width: Quantity<V>,
    /** 高度 / Height */
    val height: Quantity<V>
) {
    /** 面积 / Area */
    val area: Quantity<V> get() = width * height
}

/** 二维放置需求 / 2D placement need
 * @param V 数值类型 / numeric type
 */
data class Placement2Need<V : FloatingNumber<V>>(
    /** X坐标 / X coordinate */
    val x: Quantity<V>,
    /** Y坐标 / Y coordinate */
    val y: Quantity<V>,
    /** 投影需求 / Projection need */
    val projection: Projection2Need<V>
) {
    /** 最大X坐标 / Maximum X coordinate */
    val maxX: Quantity<V> get() = quantityPlus(x, projection.width).value!!
    /** 最大Y坐标 / Maximum Y coordinate */
    val maxY: Quantity<V> get() = quantityPlus(y, projection.height).value!!

    /** 转换为盒体需求 / Convert to box need */
    fun toBox2Need(): Box2Need<V> {
        return Box2Need(
            minX = x,
            minY = y,
            maxX = maxX,
            maxY = maxY
        )
    }
}

/** 二维盒体需求 / 2D box need
 * @param V 数值类型 / numeric type
 */
data class Box2Need<V : FloatingNumber<V>>(
    /** 最小X坐标 / Minimum X coordinate */
    val minX: Quantity<V>,
    /** 最小Y坐标 / Minimum Y coordinate */
    val minY: Quantity<V>,
    /** 最大X坐标 / Maximum X coordinate */
    val maxX: Quantity<V>,
    /** 最大Y坐标 / Maximum Y coordinate */
    val maxY: Quantity<V>
) {
    /** 宽度 / Width */
    val width: Quantity<V> get() = quantityMinus(maxX, minX).value!!
    /** 高度 / Height */
    val height: Quantity<V> get() = quantityMinus(maxY, minY).value!!
    /** 面积 / Area */
    val area: Quantity<V> get() = width * height

    /** 判断是否与另一盒体重叠 / Check whether this box overlaps with another */
    fun overlaps(rhs: Box2Need<V>): Boolean {
        if (quantityOrd(maxX, rhs.minX, "maxX-rhsMinX").value!! !is Order.Greater) {
            return false
        }
        if (quantityOrd(minX, rhs.maxX, "minX-rhsMaxX").value!! !is Order.Less) {
            return false
        }
        if (quantityOrd(maxY, rhs.minY, "maxY-rhsMinY").value!! !is Order.Greater) {
            return false
        }
        if (quantityOrd(minY, rhs.maxY, "minY-rhsMaxY").value!! !is Order.Less) {
            return false
        }
        return true
    }

    /** 计算与另一盒体的交集 / Compute intersection with another box */
    fun intersect(rhs: Box2Need<V>): Box2Need<V>? {
        val left = quantityMax(minX, rhs.minX, "left")
        val right = quantityMin(maxX, rhs.maxX, "right")
        val bottom = quantityMax(minY, rhs.minY, "bottom")
        val top = quantityMin(maxY, rhs.maxY, "top")
        if (quantityOrd(left, right, "x-range").value!! !is Order.Less) {
            return null
        }
        if (quantityOrd(bottom, top, "y-range").value!! !is Order.Less) {
            return null
        }
        return Box2Need(
            minX = left,
            minY = bottom,
            maxX = right,
            maxY = top
        )
    }

    /** 判断是否完全位于另一盒体内部 / Check whether this box is entirely inside another */
    fun inside(sheet: Box2Need<V>): Boolean {
        val minXOrd = quantityOrd(minX, sheet.minX, "sheet-minX").value!!
        val minYOrd = quantityOrd(minY, sheet.minY, "sheet-minY").value!!
        val maxXOrd = quantityOrd(maxX, sheet.maxX, "sheet-maxX").value!!
        val maxYOrd = quantityOrd(maxY, sheet.maxY, "sheet-maxY").value!!
        return (minXOrd is Order.Greater || minXOrd is Order.Equal)
                && (minYOrd is Order.Greater || minYOrd is Order.Equal)
                && (maxXOrd is Order.Less || maxXOrd is Order.Equal)
                && (maxYOrd is Order.Less || maxYOrd is Order.Equal)
    }
}

/** 已规划的矩形放置 / Planned rectangle placement
 * @param V 数值类型 / numeric type
 */
data class PlannedRectangle2<V : FloatingNumber<V>>(
    /** 矩形物料项 / Rectangle item */
    val item: RectangleItem2<V>,
    /** X坐标 / X coordinate */
    val x: Quantity<V>,
    /** Y坐标 / Y coordinate */
    val y: Quantity<V>,
    /** 是否已旋转 / Whether rotated */
    val rotated: Boolean = false
) {
    /** 转换为放置需求 / Convert to placement need */
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

    /** 转换为盒体需求 / Convert to box need */
    fun toBox2Need(): Box2Need<V> = toPlacement2Need().toBox2Need()
}

/** 二维装箱场景 / 2D packing scene
 * @param V 数值类型 / numeric type
 */
data class PackingScene2<V : FloatingNumber<V>>(
    /** 板材 / Sheet */
    val sheet: Sheet2<V>,
    /** 放置列表 / List of placements */
    val placements: List<PlannedRectangle2<V>>
) {
    /** 板材面积 / Sheet area */
    val sheetArea: Quantity<V> get() = sheet.width * sheet.height

    /** 获取板材对应的盒体需求 / Get box need for the sheet */
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

    /** 检查所有放置是否都在板材内 / Check whether all placements are inside the sheet */
    fun allInsideSheet(): Boolean {
        val sheetBox = sheetBox2Need()
        return placements.all { it.toBox2Need().inside(sheetBox) }
    }

    /** 计算已使用面积 / Compute used area */
    fun usedArea(): Quantity<V> {
        var acc = quantityZeroOf(sheetArea)
        for (placement in placements) {
            acc = quantityPlus(acc, placement.toPlacement2Need().projection.area).value!!
        }
        return acc
    }

    /** 计算剩余面积 / Compute remaining area */
    fun remainingArea(): Quantity<V> {
        return quantityMinus(sheetArea, usedArea()).value!!
    }

    /** 计算板材利用率 / Compute sheet utilization ratio */
    fun utilization(): V {
        val used = usedArea().convertTo(sheetArea.unit)
            ?: throw IllegalArgumentException("Cannot convert used area to sheet area unit.")
        return used.value / sheetArea.value
    }

    /** 计算板材利用率（Safe） / Compute sheet utilization ratio (Safe) */
    fun utilizationSafe(): Ret<V> {
        val used = usedArea().convertTo(sheetArea.unit)
            ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert used area to sheet area unit.")
        return Ok(used.value / sheetArea.value)
    }

    /** 获取所有重叠的物料对 / Get all overlapping item pairs */
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

    /** 获取非法重叠的物料对 / Get illegally overlapping item pairs */
    fun illegalOverlaps(): List<Pair<String, String>> {
        return overlappedPairs()
    }
}

/** 量值加法 / Quantity addition */
private fun <V : FloatingNumber<V>> quantityPlus(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (lhs.unit == rhs.unit) {
        return ok(Quantity(lhs.value + rhs.value, lhs.unit))
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return ok(Quantity(lhs.value + converted.value, lhs.unit))
}

/** 量值减法 / Quantity subtraction */
private fun <V : FloatingNumber<V>> quantityMinus(lhs: Quantity<V>, rhs: Quantity<V>): Ret<Quantity<V>> {
    if (lhs.unit == rhs.unit) {
        return ok(Quantity(lhs.value - rhs.value, lhs.unit))
    }
    require(lhs.unit.quantity == rhs.unit.quantity) {
        "Dimension mismatch: ${lhs.unit.quantity.dimensionSymbol()} vs ${rhs.unit.quantity.dimensionSymbol()}"
    }
    val converted = rhs.convertTo(lhs.unit)
        ?: return Failed(ErrorCode.IllegalArgument, "Cannot convert unit ${rhs.unit} to ${lhs.unit}.")
    return ok(Quantity(lhs.value - converted.value, lhs.unit))
}

/** 量值比较 / Quantity comparison */
private fun <V : FloatingNumber<V>> quantityOrd(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Ret<Order> {
    return lhs.partialOrd(rhs)?.let { ok(it) }
        ?: Failed(ErrorCode.IllegalArgument, "Incomparable quantity on axis $axis: ${lhs.unit} vs ${rhs.unit}")
}

/** 量值取最大 / Quantity max */
private fun <V : FloatingNumber<V>> quantityMax(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis).value!!) {
        is Order.Greater, Order.Equal -> lhs
        is Order.Less -> rhs
    }
}

/** 量值取最小 / Quantity min */
private fun <V : FloatingNumber<V>> quantityMin(lhs: Quantity<V>, rhs: Quantity<V>, axis: String): Quantity<V> {
    return when (quantityOrd(lhs, rhs, axis).value!!) {
        is Order.Greater -> rhs
        is Order.Equal, is Order.Less -> lhs
    }
}

/** 获取量值的零值 / Get zero value for a quantity */
private fun <V : FloatingNumber<V>> quantityZeroOf(quantity: Quantity<V>): Quantity<V> {
    return quantity.value.constants.zero * quantity.unit
}
