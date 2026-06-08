package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/**
 * 切割方案对需求的统一贡献值 / Unified demand contribution produced by a cutting plan
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property quantity 贡献值 / Contribution quantity
 */
data class CuttingPlanDemandContribution<V : RealNumber<V>>(
    val product: Product<V>,
    val quantity: Quantity<V>
) {
    companion object {
        /**
         * 按产品需求口径创建贡献 / Build contribution by product demand unit
         *
         * @param demand 产品需求 / Product demand
         * @param width 切片宽度 / Slice width
         * @param amount 切片份数 / Slice amount
         * @param arithmetic 物理量算术策略 / Quantity arithmetic strategy
         * @param length 贡献长度覆盖值 / Contribution length override
         * @return 需求贡献 / Demand contribution
         */
        fun <V : RealNumber<V>> fromDemand(
            demand: ProductDemand<V>,
            width: Quantity<V>,
            amount: UInt64,
            arithmetic: QuantityArithmetic<V>,
            length: Quantity<V>? = null
        ): CuttingPlanDemandContribution<V> {
            return of(
                product = demand.product,
                width = width,
                amount = amount,
                demandUnit = demand.quantity.unit,
                arithmetic = arithmetic,
                length = length
            )
        }

        /**
         * 按指定需求单位创建贡献 / Build contribution by a demand unit
         *
         * 离散单位按切片份数贡献；当需求单位等于产品单位重量单位且长度存在时，按 width * length * unitWeight 贡献。
         * Discrete units contribute by slice amount; when demand unit equals product unit-weight unit and length exists, contribution uses width * length * unitWeight.
         *
         * @param product 产品 / Product
         * @param width 切片宽度 / Slice width
         * @param amount 切片份数 / Slice amount
         * @param demandUnit 需求单位 / Demand unit
         * @param arithmetic 物理量算术策略 / Quantity arithmetic strategy
         * @param length 贡献长度覆盖值 / Contribution length override
         * @return 需求贡献 / Demand contribution
         */
        fun <V : RealNumber<V>> of(
            product: Product<V>,
            width: Quantity<V>,
            amount: UInt64,
            demandUnit: PhysicalUnit,
            arithmetic: QuantityArithmetic<V>,
            length: Quantity<V>? = null
        ): CuttingPlanDemandContribution<V> {
            return CuttingPlanDemandContribution(
                product = product,
                quantity = quantityOf(
                    product = product,
                    width = width,
                    amount = amount,
                    demandUnit = demandUnit,
                    arithmetic = arithmetic,
                    length = length
                )
            )
        }

        /**
         * 计算切片对需求的贡献值 / Calculate slice contribution quantity
         *
         * @param product 产品 / Product
         * @param width 切片宽度 / Slice width
         * @param amount 切片份数 / Slice amount
         * @param demandUnit 需求单位 / Demand unit
         * @param arithmetic 物理量算术策略 / Quantity arithmetic strategy
         * @param length 贡献长度覆盖值 / Contribution length override
         * @return 贡献值 / Contribution quantity
         */
        fun <V : RealNumber<V>> quantityOf(
            product: Product<V>,
            width: Quantity<V>,
            amount: UInt64,
            demandUnit: PhysicalUnit,
            arithmetic: QuantityArithmetic<V>,
            length: Quantity<V>? = null
        ): Quantity<V> {
            val contributionLength = length ?: product.length
            val unitContribution = product.unitWeight?.let { unitWeight ->
                contributionLength?.let { currentLength ->
                    if (unitWeight.unit == demandUnit) {
                        val areaValue = width.value * currentLength.value
                        val weightValue = areaValue * unitWeight.value
                        Quantity(weightValue, demandUnit)
                    } else {
                        null
                    }
                }
            }
            if (unitContribution != null) {
                return unitContribution.repeat(
                    amount = amount,
                    arithmetic = arithmetic
                )
            }
            val onePerPiece = Quantity(width.value.constants.one, demandUnit)
            return onePerPiece.repeat(
                amount = amount,
                arithmetic = arithmetic
            )
        }
    }
}

/**
 * 按当前需求口径创建切片贡献 / Build slice contribution by this demand
 *
 * @param width 切片宽度 / Slice width
 * @param amount 切片份数 / Slice amount
 * @param arithmetic 物理量算术策略 / Quantity arithmetic strategy
 * @param length 贡献长度覆盖值 / Contribution length override
 * @return 需求贡献 / Demand contribution
 */
fun <V : RealNumber<V>> ProductDemand<V>.contribution(
    width: Quantity<V>,
    amount: UInt64,
    arithmetic: QuantityArithmetic<V>,
    length: Quantity<V>? = null
): CuttingPlanDemandContribution<V> {
    return CuttingPlanDemandContribution.fromDemand(
        demand = this,
        width = width,
        amount = amount,
        arithmetic = arithmetic,
        length = length
    )
}
