/**
 * 二维几何类型扩展。 / Extensions for the 2D quantity geometry types.
 *
 * `ospf-kotlin-quantities` 已提供 `QuantityRectangle2` / `QuantityCircle2` / `QuantityBox2` 及其矩形、圆、
 * 圆与矩形的重叠判定。本文件只补充装箱需要的派生量（角点、面积、包含），不重定义既有类型。
 * / The quantity geometry layer already ships rectangles, circles, boxes and their pairwise overlap tests.
 * This file only adds packing-oriented derived values (corners, area, containment) without redefining them.
*/
package fuookami.ospf.kotlin.framework.geometry2d

import fuookami.ospf.kotlin.utils.functional.Ret
import fuookami.ospf.kotlin.utils.functional.ok
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.geometry.QuantityBox2
import fuookami.ospf.kotlin.math.geometry.QuantityCircle2
import fuookami.ospf.kotlin.math.geometry.QuantityRectangle2
import fuookami.ospf.kotlin.quantities.quantity.Quantity

/**
 * 由矩形宽度与高度计算面积，量纲为 `Length²`（即 `Area`）。 / Area of a rectangle, with the `Length²` (`Area`) dimension.
 *
 * @param V 数值类型 / numeric type
 * @return 面积 / area
*/
fun <V : FloatingNumber<V>> QuantityRectangle2<V>.areaOf(): Quantity<V> {
    return width * height
}

/**
 * 由圆形半径计算面积 `πr²`，量纲为 `Area`。 / Area of a circle `πr²`, with the `Area` dimension.
 *
 * @param V 数值类型 / numeric type
 * @return 面积 / area
*/
fun <V : FloatingNumber<V>> QuantityCircle2<V>.areaOf(): Quantity<V> {
    val squared = radius * radius
    return squared * radius.value.constants.pi
}

/**
 * 包围盒宽度。 / Bounding-box width.
 *
 * @param V 数值类型 / numeric type
 * @return 宽度 / width
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.boundingWidth(): Quantity<V> {
    return width
}

/**
 * 包围盒高度。 / Bounding-box height.
 *
 * @param V 数值类型 / numeric type
 * @return 高度 / height
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.boundingHeight(): Quantity<V> {
    return height
}

/**
 * 包围盒左下角 x 坐标。 / x coordinate of the bounding-box bottom-left corner.
 *
 * @param V 数值类型 / numeric type
 * @return x 坐标 / x coordinate
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.minX(): Quantity<V> {
    return x
}

/**
 * 包围盒左下角 y 坐标。 / y coordinate of the bounding-box bottom-left corner.
 *
 * @param V 数值类型 / numeric type
 * @return y 坐标 / y coordinate
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.minY(): Quantity<V> {
    return y
}

/**
 * 包围盒右上角 x 坐标。 / x coordinate of the bounding-box top-right corner.
 *
 * @param V 数值类型 / numeric type
 * @return x 坐标或失败 / x coordinate, or failure
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.maxX(): Ret<Quantity<V>> {
    return quantityPlusSafe(x, width)
}

/**
 * 包围盒右上角 y 坐标。 / y coordinate of the bounding-box top-right corner.
 *
 * @param V 数值类型 / numeric type
 * @return y 坐标或失败 / y coordinate, or failure
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.maxY(): Ret<Quantity<V>> {
    return quantityPlusSafe(y, height)
}

/**
 * 包围盒面积（外接矩形面积，量纲为 `Area`）。 / Bounding-box area (the enclosing rectangle area, `Area` dimension).
 *
 * @param V 数值类型 / numeric type
 * @return 面积 / area
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.boundingArea(): Quantity<V> {
    return width * height
}

/**
 * 判断包围盒是否完全落在给定板材范围内。 / Check whether the bounding box lies entirely inside the given sheet.
 *
 * @param V 数值类型 / numeric type
 * @param sheetWidth 板材宽度 / sheet width
 * @param sheetHeight 板材高度 / sheet height
 * @return 是否在范围内或失败 / whether inside the sheet, or failure
*/
fun <V : FloatingNumber<V>> QuantityBox2<V>.insideSheet(
    sheetWidth: Quantity<V>,
    sheetHeight: Quantity<V>
): Ret<Boolean> {
    val zero = quantityZeroOf(x)
    return ok(
        quantityGreaterOrEqual(x, zero)
                && quantityGreaterOrEqual(y, zero)
                && quantityLessOrEqual(x + width, sheetWidth)
                && quantityLessOrEqual(y + height, sheetHeight)
    )
}
