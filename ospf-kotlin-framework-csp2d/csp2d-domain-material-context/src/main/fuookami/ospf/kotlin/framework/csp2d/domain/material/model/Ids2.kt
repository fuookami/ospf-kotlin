/**
 * 二维套裁标识与生产对象契约。 / 2D cutting stock identifiers and production object contracts.
 *
 * 与 CSP1D 一致：产品、物料、设备、副产物都是「可被裁出的生产对象」，共享编号与单位重量口径；
 * 差异在于二维侧用「宽 × 高」而不是「宽 + 长」。
 * / Consistent with CSP1D: products, materials, machines and by-products are all production objects sharing an
 * identifier and a unit weight; the difference is that the 2D side uses width and height instead of width and length.
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.geometry2d.*

/**
 * 生产对象编号。 / Production object identifier.
*/
interface ProductionId2 {
    /** 裸值 / raw value */
    val value: String
}

/**
 * 产品编号。 / Product identifier.
*/
interface ProductId2 : ProductionId2

/**
 * 物料编号。 / Material identifier.
*/
interface MaterialId2 : ProductionId2

/**
 * 设备编号。 / Machine identifier.
*/
interface MachineId2 : ProductionId2

/**
 * 副产物编号。 / By-product identifier.
*/
interface CostarId2 : ProductionId2

/**
 * 裁切图案编号。 / Cutting pattern identifier.
*/
interface PatternId2 {
    /** 裸值 / raw value */
    val value: String
}

/**
 * 裁切方案编号。 / Cutting plan identifier.
*/
interface PlanId2 {
    /** 裸值 / raw value */
    val value: String
}

/**
 * 字符串编号的通用实现。 / Generic implementation of a string identifier.
 *
 * @property value 裸值 / raw value
*/
data class Csp2dId(
    override val value: String
) : ProductId2, MaterialId2, MachineId2, CostarId2

/**
 * 由字符串构造产品编号。 / Build a product identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 产品编号 / product identifier
*/
fun productId2Of(value: String): ProductId2 = Csp2dId(value)

/**
 * 由字符串构造物料编号。 / Build a material identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 物料编号 / material identifier
*/
fun materialId2Of(value: String): MaterialId2 = Csp2dId(value)

/**
 * 由字符串构造设备编号。 / Build a machine identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 设备编号 / machine identifier
*/
fun machineId2Of(value: String): MachineId2 = Csp2dId(value)

/**
 * 由字符串构造副产物编号。 / Build a by-product identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 副产物编号 / by-product identifier
*/
fun costarId2Of(value: String): CostarId2 = Csp2dId(value)

/**
 * 裁切图案编号实现。 / Cutting pattern identifier implementation.
 *
 * @property value 裸值 / raw value
*/
data class PatternId2Impl(
    override val value: String
) : PatternId2

/**
 * 由字符串构造裁切图案编号。 / Build a cutting pattern identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 裁切图案编号 / cutting pattern identifier
*/
fun patternId2Of(value: String): PatternId2 = PatternId2Impl(value)

/**
 * 裁切方案编号实现。 / Cutting plan identifier implementation.
 *
 * @property value 裸值 / raw value
*/
data class PlanId2Impl(
    override val value: String
) : PlanId2

/**
 * 由字符串构造裁切方案编号。 / Build a cutting plan identifier from a string.
 *
 * @param value 裸值 / raw value
 * @return 裁切方案编号 / cutting plan identifier
*/
fun planId2Of(value: String): PlanId2 = PlanId2Impl(value)

/**
 * 生产对象：可被套裁出的东西。 / A production object: something the cutting stock process can produce.
 *
 * @property V 数值类型 / numeric type
*/
interface Production2<V : FloatingNumber<V>> {

    /** 编号 / identifier */
    val id: ProductionId2

    /** 名称 / name */
    val name: String

    /** 宽度 / width */
    val width: Quantity<V>

    /** 高度；卷材物料为空 / height; null for a coil material */
    val height: Quantity<V>?

    /** 单位重量 / unit weight */
    val unitWeight: Quantity<V>?

    /**
     * 单件面积，高度为空时返回 null。 / Single-piece area, or null when the height is unknown.
     *
     * @return 面积 / area
    */
    fun area(): Quantity<V>? {
        val actualHeight = height ?: return null
        return width * actualHeight
    }

    /**
     * 单件包围形状。 / The enclosing shape of a single piece.
     *
     * 二维套裁只处理矩形件（异形件与圆件明确排除），因此这里固定为矩形形状。
     * / 2D cutting stock only handles rectangles (irregular parts and circles are explicitly out of scope), so this is
     * always a rectangle.
     *
     * @return 包围形状 / enclosing shape
    */
    fun sheetShape(): Sheet2Shape<V>? {
        val actualHeight = height ?: return null
        return Sheet2Shape(width = width, height = actualHeight)
    }
}
