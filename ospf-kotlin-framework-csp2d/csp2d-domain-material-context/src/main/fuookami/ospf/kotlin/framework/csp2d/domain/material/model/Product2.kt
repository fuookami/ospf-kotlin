/**
 * 产品与产品需求。 / Products and product demands.
 *
 * 需求口径用密封值类型表达（件数 / 面积 / 重量），避免用「量值 + 模式」两个字段互相约束：
 * 件数是整数、面积与重量带量纲，混在一个字段里会迫使泛型代码做 `toDouble()` 换算。
 * / The demand unit is expressed with a sealed value type (count / area / weight) instead of a "quantity plus mode" pair:
 * a count is an integer while area and weight are dimensional, and packing them into one field would force generic
 * code to call `toDouble()`.
 *
 * @property V 数值类型 / numeric type
*/
package fuookami.ospf.kotlin.framework.csp2d.domain.material.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.framework.csp2d.infrastructure.Csp2dValidationError

/**
 * 需求口径。 / Demand unit.
*/
enum class DemandMode2 {
    /** 件数 / piece count */
    Amount,

    /** 面积 / area */
    Area,

    /** 重量 / weight */
    Weight
}

/**
 * 需求量，按口径取值。 / A demanded quantity, valued according to its unit.
 *
 * @property V 数值类型 / numeric type
*/
sealed interface DemandValue2<V : FloatingNumber<V>> {

    /** 需求口径 / demand unit */
    val mode: DemandMode2

    /**
     * 件数口径的需求量。 / A demand expressed as a piece count.
     *
     * @property V 数值类型 / numeric type
     * @property count 件数 / piece count
    */
    data class Amount<V : FloatingNumber<V>>(
        val count: UInt64
    ) : DemandValue2<V> {
        override val mode: DemandMode2 get() = DemandMode2.Amount
    }

    /**
     * 面积口径的需求量。 / A demand expressed as an area.
     *
     * @property V 数值类型 / numeric type
     * @property area 面积 / area
    */
    data class Area<V : FloatingNumber<V>>(
        val area: Quantity<V>
    ) : DemandValue2<V> {
        override val mode: DemandMode2 get() = DemandMode2.Area
    }

    /**
     * 重量口径的需求量。 / A demand expressed as a weight.
     *
     * @property V 数值类型 / numeric type
     * @property weight 重量 / weight
    */
    data class Weight<V : FloatingNumber<V>>(
        val weight: Quantity<V>
    ) : DemandValue2<V> {
        override val mode: DemandMode2 get() = DemandMode2.Weight
    }
}

/**
 * 待裁产品（成品件）。 / A product to be cut (a finished part).
 *
 * 二维套裁只处理矩形件：宽、高、是否允许 90° 旋转。旋转限制在生产上很常见（木纹、纸纹、贴面方向）。
 * / 2D cutting stock only handles rectangles: width, height and whether a 90-degree rotation is allowed. Rotation
 * restrictions are common in production (wood grain, paper grain, facing direction).
 *
 * @property V 数值类型 / numeric type
 * @property id 产品编号 / product identifier
 * @property name 产品名称 / product name
 * @property width 宽度 / width
 * @property height 高度 / height
 * @property allowRotate 是否允许旋转 90° / whether a 90-degree rotation is allowed
 * @property unitWeight 单位重量 / unit weight
*/
data class Product2<V : FloatingNumber<V>>(
    override val id: ProductId2,
    override val name: String,
    override val width: Quantity<V>,
    override val height: Quantity<V>,
    val allowRotate: Boolean = true,
    override val unitWeight: Quantity<V>? = null
) : Production2<V> {

    /** 单件面积 / single-piece area */
    val area: Quantity<V> get() = width * height

    /**
     * 校验产品几何。 / Validate the product geometry.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        if (!quantityIsPositive(width) || !quantityIsPositive(height)) {
            return Failed(
                Csp2dValidationError(
                    detail = "产品 $id 的宽高必须为正：$width × $height / Product dimensions must be positive"
                )
            )
        }
        return ok
    }

    /**
     * 允许的摆放方向集合。 / The set of allowed orientations.
     *
     * @return 方向列表，第一个为不旋转 / orientation list, the first one being unrotated
    */
    fun orientations(): List<Boolean> {
        return if (allowRotate) listOf(false, true) else listOf(false)
    }
}

/**
 * 产品需求。 / A product demand.
 *
 * @property V 数值类型 / numeric type
 * @property product 产品 / product
 * @property demand 需求量 / demanded quantity
*/
data class ProductDemand2<V : FloatingNumber<V>>(
    val product: Product2<V>,
    val demand: DemandValue2<V>
) {

    /** 需求口径 / demand unit */
    val mode: DemandMode2 get() = demand.mode

    /**
     * 件数口径下的件数，其它口径返回 null。 / The piece count under the amount unit, null for other units.
     *
     * @return 件数 / piece count
    */
    fun piecesOrNull(): UInt64? {
        return when (val value = demand) {
            is DemandValue2.Amount -> value.count
            is DemandValue2.Area -> null
            is DemandValue2.Weight -> null
        }
    }

    /**
     * 校验需求量。 / Validate the demanded quantity.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        when (val value = demand) {
            is DemandValue2.Amount -> {
                if (value.count == UInt64.zero) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "产品 ${product.id} 的件数需求必须为正 / The demanded piece count must be positive"
                        )
                    )
                }
            }

            is DemandValue2.Area -> {
                if (!quantityIsPositive(value.area)) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "产品 ${product.id} 的面积需求必须为正：${value.area} / " +
                                    "The demanded area must be positive"
                        )
                    )
                }
            }

            is DemandValue2.Weight -> {
                if (!quantityIsPositive(value.weight)) {
                    return Failed(
                        Csp2dValidationError(
                            detail = "产品 ${product.id} 的重量需求必须为正：${value.weight} / " +
                                    "The demanded weight must be positive"
                        )
                    )
                }
            }
        }
        return ok
    }
}

/**
 * 需求条目集合，按产品编号索引。 / A demand entry set indexed by product identifier.
 *
 * @property V 数值类型 / numeric type
 * @property demands 需求列表 / demand list
*/
data class ProductDemands2<V : FloatingNumber<V>>(
    val demands: List<ProductDemand2<V>>
) {

    /**
     * 校验全部需求，并拒绝重复产品。 / Validate every demand and reject duplicate products.
     *
     * @return 校验结果 / validation result
    */
    fun validate(): Try {
        val duplicated = demands.groupingBy { it.product.id.value }.eachCount().filterValues { it > 1 }.keys
        if (duplicated.isNotEmpty()) {
            return Failed(
                Csp2dValidationError(
                    detail = "需求中存在重复产品：${duplicated.joinToString()} / Duplicate products in the demands"
                )
            )
        }
        for (demand in demands) {
            when (val result = demand.validate()) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }
        return ok
    }

    /**
     * 产品编号到需求的映射。 / Map from product identifier to demand.
     *
     * @return 需求映射 / demand map
    */
    fun byProduct(): Map<String, ProductDemand2<V>> {
        return demands.associateBy { it.product.id.value }
    }
}
