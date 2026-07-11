/**
 * 导出量纲类
 * Derived Quantity Class
 *
 * 由基础量纲的幂次组成，如 L²·M·T⁻²。导出量纲表示由基本物理量组合而成的复合物理量纲。
 * Composed of powers of fundamental dimensions, such as L²·M·T⁻². Derived quantities represent composite physical dimensions formed by combining fundamental physical quantities.
*/
package fuookami.ospf.kotlin.quantities.dimension

/**
 * 导出量纲类
 * Derived quantity class
 *
 * 由基础量纲的幂次组成，如 L^2*M*T^-2。
 * Composed of powers of fundamental dimensions, such as L^2*M*T^-2.
 *
 * @property quantities 基础量纲列表（构造时自动排序） / List of fundamental quantities (sorted on construction)
 * @property name 量纲名称 / Name of the quantity
 * @property symbol 量纲符号 / Symbol of the quantity
 * @property domain 取值域，默认为连续 / Value domain, defaults to Continuous
*/
class DerivedQuantity(
    quantities: List<FundamentalQuantity>,

    /** 量纲名称 / Name of the quantity */
    val name: String? = null,

    /** 量纲符号 / Symbol of the quantity */
    val symbol: String? = null,

    /** 取值域 / Value domain */
    val domain: QuantityDomain = QuantityDomain.Continuous
) {

    /**
     * 排序后的量纲列表
     * Sorted list of quantities
    */
    val quantities = quantities.sortedBy { it.dimension.symbol }

    /**
     * 从基础量纲构造导出量纲
     * Constructs a derived quantity from a fundamental dimension
     *
     * @param dimension 基础量纲 / The fundamental dimension
     * @param name 量纲名称 / Name of the quantity
     * @param symbol 量纲符号 / Symbol of the quantity
     * @param domain 取值域 / Value domain
    */
    constructor(
        dimension: FundamentalQuantityDimension,
        name: String? = null,
        symbol: String? = null,
        domain: QuantityDomain = QuantityDomain.Continuous
    ) : this(
        quantities = listOf(FundamentalQuantity(dimension)),
        name = name,
        symbol = symbol,
        domain = domain
    )

    /**
     * 从基础量纲值构造导出量纲
     * Constructs a derived quantity from a fundamental quantity value
     *
     * @param quantity 基础量纲值 / The fundamental quantity value
     * @param name 量纲名称 / Name of the quantity
     * @param symbol 量纲符号 / Symbol of the quantity
     * @param domain 取值域 / Value domain
    */
    constructor(
        quantity: FundamentalQuantity,
        name: String? = null,
        symbol: String? = null,
        domain: QuantityDomain = QuantityDomain.Continuous
    ) : this(
        quantities = listOf(quantity),
        name = name,
        symbol = symbol,
        domain = domain
    )

    /**
     * 从另一个导出量纲构造新的导出量纲
     * Constructs a new derived quantity from another derived quantity
     *
     * @param quantity 另一个导出量纲 / The source derived quantity
     * @param name 量纲名称 / Name of the quantity
     * @param symbol 量纲符号 / Symbol of the quantity
     * @param domain 取值域，默认继承源量纲 / Value domain, defaults to source quantity's domain
    */
    constructor(
        quantity: DerivedQuantity,
        name: String? = null,
        symbol: String? = null,
        domain: QuantityDomain = quantity.domain
    ) : this(
        quantities = quantity.quantities.toList(),
        name = name,
        symbol = symbol,
        domain = domain
    )

    /**
     * 取负（所有幂次取反）
     * Negation operator (inverts all powers)
     *
     * @return 幂次取反后的导出量纲 / The derived quantity with all powers inverted
    */
    operator fun unaryMinus(): DerivedQuantity {
        return DerivedQuantity(
            quantities = quantities.map { -it },
            domain = QuantityDomain.Continuous
        )
    }

    /**
     * 获取指定量纲的幂次
     * Gets the power of a specified dimension
     *
     * @param dimension 要查询的基础量纲 / The fundamental dimension to query
     * @return 该量纲的幂次，若不存在则返回0 / The power of the dimension, returns 0 if not present
    */
    fun getPower(dimension: FundamentalQuantityDimension): Int {
        return quantities.find { it.dimension == dimension }?.index ?: 0
    }

    /**
     * 添加量纲幂次
     * Adds power to a dimension
     *
     * @param dimension 要添加的基础量纲 / The fundamental dimension to add
     * @param power 要添加的幂次值 / The power value to add
     * @return 新的导出量纲 / The new derived quantity
    */
    fun addPower(dimension: FundamentalQuantityDimension, power: Int): DerivedQuantity {
        if (power == 0) return this

        val existingIndex = quantities.indexOfFirst { it.dimension == dimension }
        return if (existingIndex >= 0) {
            val newQuantities = quantities.toMutableList()
            val existing = newQuantities[existingIndex]
            val newIndex = existing.index + power
            if (newIndex == 0) {
                newQuantities.removeAt(existingIndex)
            } else {
                newQuantities[existingIndex] = FundamentalQuantity(dimension, newIndex)
            }
            DerivedQuantity(
                quantities = newQuantities,
                name = name,
                symbol = symbol,
                domain = QuantityDomain.Continuous
            )
        } else {
            DerivedQuantity(
                quantities = quantities + FundamentalQuantity(dimension, power),
                name = name,
                symbol = symbol,
                domain = QuantityDomain.Continuous
            )
        }
    }

    /**
     * 检查是否为无量纲
     * Checks if this is a dimensionless quantity
     *
     * @return 若无量纲则返回true / Returns true if dimensionless
    */
    fun isNone(): Boolean = quantities.isEmpty()

    /**
     * 获取量纲符号表示
     * Gets the dimension symbol representation
     *
     * @return 量纲符号字符串，如 "L²·M·T⁻²" / Dimension symbol string, e.g., "L²·M·T⁻²"
    */
    fun dimensionSymbol(): String {
        if (isNone()) return "1"
        return quantities.joinToString(separator = "·") { dim ->
            if (dim.index == 1) {
                dim.dimension.symbol
            } else {
                "${dim.dimension.symbol}^${dim.index}"
            }
        }
    }

    /**
     * 幂次运算
     * Power operation
     *
     * @param index 幂次 / The exponent
     * @return 幂运算后的导出量纲 / The derived quantity after power operation
    */
    fun pow(index: Int): DerivedQuantity {
        if (index == 0) {
            return DerivedQuantity(
                quantities = emptyList(),
                name = name,
                symbol = "1",
                domain = QuantityDomain.Continuous
            )
        }
        if (index == 1) return this
        return DerivedQuantity(
            quantities = quantities.map { it * index },
            name = name,
            symbol = symbol,
            domain = domain.pow(index)
        )
    }

    /**
     * 倒数
     * Reciprocal (inverse) of the quantity
     *
     * @return 倒数导出量纲 / The reciprocal derived quantity
    */
    fun reciprocal(): DerivedQuantity = -this

    override fun toString(): String {
        return symbol
            ?: name
            ?: if (isNone()) "1" else quantities.joinToString(separator = "·") { "${it.dimension}^${it.index}" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DerivedQuantity

        return quantities == other.quantities
    }

    override fun hashCode(): Int {
        return quantities.hashCode()
    }
}

/**
 * 无量纲常量
 * Dimensionless constant
*/
val DimLess = DerivedQuantity(emptyList(), "Dimensionless", "1")

/**
 * 两个基础量纲值相乘
 * Multiplies two fundamental quantities
 *
 * @param other 另一个基础量纲值 / The other fundamental quantity
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun FundamentalQuantity.times(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) + other.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 两个基础量纲值相除
 * Divides two fundamental quantities
 *
 * @param other 另一个基础量纲值（除数） / The other fundamental quantity (divisor)
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun FundamentalQuantity.div(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) - other.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 基础量纲值与导出量纲相乘
 * Multiplies a fundamental quantity by a derived quantity
 *
 * @param other 导出量纲 / The derived quantity
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun FundamentalQuantity.times(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 基础量纲值除以导出量纲
 * Divides a fundamental quantity by a derived quantity
 *
 * @param other 导出量纲（除数） / The derived quantity (divisor)
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun FundamentalQuantity.div(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) - quantity.index
    }
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 导出量纲乘以整数
 * Multiplies a derived quantity by an integer
 *
 * @param other 乘数 / The multiplier
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.times(other: Int): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index * other
    }
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(
        quantities = indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) },
        domain = this.domain.pow(other)
    )
}

/**
 * 导出量纲除以整数
 * Divides a derived quantity by an integer
 *
 * @param other 除数 / The divisor
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.div(other: Int): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index / other
    }
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 导出量纲乘以基础量纲值
 * Multiplies a derived quantity by a fundamental quantity
 *
 * @param other 基础量纲值 / The fundamental quantity
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.times(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) + other.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 导出量纲除以基础量纲值
 * Divides a derived quantity by a fundamental quantity
 *
 * @param other 基础量纲值（除数） / The fundamental quantity (divisor)
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.div(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) - other.index
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

/**
 * 两个导出量纲相乘
 * Multiplies two derived quantities
 *
 * @param other 另一个导出量纲 / The other derived quantity
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.times(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(
        quantities = indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) },
        domain = this.domain * other.domain
    )
}

/**
 * 两个导出量纲相除
 * Divides two derived quantities
 *
 * @param other 另一个导出量纲（除数） / The other derived quantity (divisor)
 * @return 导出量纲 / The resulting derived quantity
*/
operator fun DerivedQuantity.div(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) - quantity.index
    }
    // 过滤掉幂次为0的量纲 / Filter out dimensions with power 0
    return DerivedQuantity(
        quantities = indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) },
        domain = this.domain / other.domain
    )
}
