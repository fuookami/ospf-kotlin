package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.QuantityDomain
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit

/**
 * 卷数离散单位 / Discrete unit for roll count
 */
object RollCountUnit : PhysicalUnit() {
    override val name = "roll"
    override val symbol = "roll"
    override val quantity = DerivedQuantity(
        quantities = emptyList(),
        name = "roll count",
        symbol = "roll",
        domain = QuantityDomain.Discrete
    )
    override val scale = Scale()
}

/**
 * 张数离散单位 / Discrete unit for sheet count
 */
object SheetCountUnit : PhysicalUnit() {
    override val name = "sheet"
    override val symbol = "sheet"
    override val quantity = DerivedQuantity(
        quantities = emptyList(),
        name = "sheet count",
        symbol = "sheet",
        domain = QuantityDomain.Discrete
    )
    override val scale = Scale()
}

/**
 * 产品需求模型，统一使用 Quantity<V> 表达需求值 / Product demand model using Quantity<V> as the single demand value
 *
 * @param V 数值类型 / Numeric value type
 * @property product 产品 / Product
 * @property quantity 需求值 / Demand quantity
 * @property mode 需求口径标签，仅用于语义标识 / Demand mode label for semantic tracing only
 */
data class ProductDemand<V : RealNumber<V>>(
    val product: Product<V>,
    val quantity: Quantity<V>,
    val mode: DemandMode? = null
) {
    companion object {
        /**
         * 按卷数口径创建需求 / Create demand with roll mode label
         *
         * @param product 产品 / Product
         * @param quantity 需求值 / Demand quantity
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> roll(
            product: Product<V>,
            quantity: Quantity<V>
        ): ProductDemand<V> {
            return ProductDemand(
                product = product,
                quantity = quantity,
                mode = DemandMode.Roll
            )
        }

        /**
         * 按重量口径创建需求 / Create demand with weight mode label
         *
         * @param product 产品 / Product
         * @param quantity 需求值 / Demand quantity
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> weight(
            product: Product<V>,
            quantity: Quantity<V>
        ): ProductDemand<V> {
            return ProductDemand(
                product = product,
                quantity = quantity,
                mode = DemandMode.Weight
            )
        }

        /**
         * 按张数口径创建需求 / Create demand with sheet mode label
         *
         * @param product 产品 / Product
         * @param quantity 需求值 / Demand quantity
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> sheet(
            product: Product<V>,
            quantity: Quantity<V>
        ): ProductDemand<V> {
            return ProductDemand(
                product = product,
                quantity = quantity,
                mode = DemandMode.Sheet
            )
        }

        /**
         * legacy 卷数输入转换 / Legacy roll-amount input adapter
         *
         * @param product 产品 / Product
         * @param rollAmount 卷数值 / Roll amount
         * @param unit 离散单位，默认 roll / Discrete unit, defaults to roll
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> legacyRoll(
            product: Product<V>,
            rollAmount: V,
            unit: PhysicalUnit = RollCountUnit
        ): ProductDemand<V> {
            return roll(
                product = product,
                quantity = Quantity(rollAmount, unit)
            )
        }

        /**
         * legacy 重量输入转换 / Legacy weight-amount input adapter
         *
         * @param product 产品 / Product
         * @param weightAmount 重量值 / Weight amount
         * @param unit 质量单位，默认 kg / Mass unit, defaults to kg
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> legacyWeight(
            product: Product<V>,
            weightAmount: V,
            unit: PhysicalUnit = Kilogram
        ): ProductDemand<V> {
            return weight(
                product = product,
                quantity = Quantity(weightAmount, unit)
            )
        }

        /**
         * legacy 张数输入转换 / Legacy sheet-amount input adapter
         *
         * @param product 产品 / Product
         * @param sheetAmount 张数值 / Sheet amount
         * @param unit 离散单位，默认 sheet / Discrete unit, defaults to sheet
         * @return 产品需求 / Product demand
         */
        fun <V : RealNumber<V>> legacySheet(
            product: Product<V>,
            sheetAmount: V,
            unit: PhysicalUnit = SheetCountUnit
        ): ProductDemand<V> {
            return sheet(
                product = product,
                quantity = Quantity(sheetAmount, unit)
            )
        }
    }

    /** 是否离散需求，仅基于 quantity.unit.domain / Whether discrete demand based only on quantity.unit.domain */
    val isDiscrete get() = quantity.unit.domain == QuantityDomain.Discrete

    /** 是否连续需求，仅基于 quantity.unit.domain / Whether continuous demand based only on quantity.unit.domain */
    val isContinuous get() = quantity.unit.domain == QuantityDomain.Continuous
}

