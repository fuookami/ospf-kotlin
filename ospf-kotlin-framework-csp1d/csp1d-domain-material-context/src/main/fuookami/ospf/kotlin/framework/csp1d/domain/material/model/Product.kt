package fuookami.ospf.kotlin.framework.csp1d.domain.material.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.concept.ManualIndexed

/**
 * 产品，描述物料的规格属性 / Product describing material specification properties
 *
 * @param V 数值类型 / Numeric value type
 * @property id 物料标识 / Material identifier
 * @property name 产品名称 / Product name
 * @property width 宽度列表 / List of widths
 * @property length 长度 / Length
 * @property unitWeight 单位重量 / Unit weight
 * @property maxOverProduceLength 最大超产长度 / Maximum overproduce length
 * @property dynamicLength 是否动态长度 / Whether length is dynamic
 */
open class Product<V : RealNumber<V>>(
    override val id: String,
    val name: String,
    override val width: List<Quantity<V>>,
    override val length: Quantity<V>? = null,
    override val unitWeight: Quantity<V>? = null,
    weight: Quantity<V>? = null,
    val maxOverProduceLength: Quantity<V>? = null,
    val dynamicLength: Boolean = false
) : Production<V>, ManualIndexed() {
    companion object {
        /**
         * 创建产品实例 / Create a product instance
         *
         * @param id 物料标识 / Material identifier
         * @param name 产品名称 / Product name
         * @param width 宽度列表 / List of widths
         * @param length 长度 / Length
         * @param unitWeight 单位重量 / Unit weight
         * @param weight 重量 / Weight
         * @param maxOverProduceLength 最大超产长度 / Maximum overproduce length
         * @return 产品实例 / Product instance
         */
        operator fun <V : RealNumber<V>> invoke(
            id: String,
            name: String,
            width: List<Quantity<V>>,
            length: Quantity<V>? = null,
            unitWeight: Quantity<V>? = null,
            weight: Quantity<V>? = null,
            maxOverProduceLength: Quantity<V>? = null
        ): Product<V> {
            return Product(
                id = id,
                name = name,
                width = width,
                length = length,
                unitWeight = unitWeight,
                weight = weight,
                maxOverProduceLength = maxOverProduceLength
            )
        }

        /**
         * 创建动态长度产品实例 / Create a dynamic-length product instance
         *
         * @param id 物料标识 / Material identifier
         * @param name 产品名称 / Product name
         * @param width 宽度列表 / List of widths
         * @param unitWeight 单位重量 / Unit weight
         * @return 动态长度产品实例 / Dynamic-length product instance
         */
        fun <V : RealNumber<V>> dynamicLengthOf(
            id: String,
            name: String,
            width: List<Quantity<V>>,
            unitWeight: Quantity<V>? = null
        ): Product<V> {
            return Product(
                id = id,
                name = name,
                width = width,
                unitWeight = unitWeight,
                dynamicLength = true
            )
        }

        /**
         * 兼容旧版 Flt64/FltX 入参创建产品实例 / Legacy factory from Flt64/FltX inputs
         *
         * @param id 物料标识 / Material identifier
         * @param name 产品名称 / Product name
         * @param width 宽度列表（无单位）/ Width list without unit
         * @param length 长度（无单位）/ Length without unit
         * @param unitWeight 单位重量（无单位）/ Unit weight without unit
         * @param weight 重量（无单位）/ Weight without unit
         * @param maxOverProduceLength 最大超产长度（无单位）/ Maximum overproduce length without unit
         * @param unit 统一单位 / Shared unit
         * @return FltX 产品实例 / FltX product instance
         */
        fun legacy(
            id: String,
            name: String,
            width: List<Flt64>,
            length: FltX? = null,
            unitWeight: FltX? = null,
            weight: FltX? = null,
            maxOverProduceLength: FltX? = null,
            unit: fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
        ): Product<FltX> {
            return Product(
                id = id,
                name = name,
                width = width.map { Quantity(it.toFltX(), unit) },
                length = length?.let { Quantity(it, unit) },
                unitWeight = unitWeight?.let { Quantity(it, unit) },
                weight = weight?.let { Quantity(it, unit) },
                maxOverProduceLength = maxOverProduceLength?.let { Quantity(it, unit) }
            )
        }
    }

    init {
        if (dynamicLength) {
            check(length == null && weight == null)
        }
    }

    private val explicitWeight = weight

    fun maxWidth(): Quantity<V>? {
        return width.reduceOrNull { lhs, rhs ->
            max(lhs, rhs) ?: lhs
        }
    }

    /** 产品重量（若未显式给出则按 max(width) * length * unitWeight 推导）/ Product weight (derived from max width when not explicitly provided) */
    val weight: Quantity<V>? by lazy {
        explicitWeight ?: run {
            val currentLength = length ?: return@run null
            val currentUnitWeight = unitWeight ?: return@run null
            val currentWidth = maxWidth() ?: return@run null
            currentLength.timesOrNull(currentUnitWeight)?.timesOrNull(currentWidth)
        }
    }

    /**
     * 根据指定宽度和长度计算重量 / Calculate weight for given width and length
     *
     * @param width 宽度 / Width
     * @param length 长度 / Length
     * @return 重量 / Weight
     */
    fun weight(
        width: Quantity<V>,
        length: Quantity<V>? = this.length
    ): Quantity<V>? {
        val currentLength = length ?: return null
        val currentUnitWeight = unitWeight ?: return null
        return currentLength.timesOrNull(currentUnitWeight)?.timesOrNull(width)
    }

    override fun toString(): String {
        return "$name${width.joinToString("x")}"
    }
}
