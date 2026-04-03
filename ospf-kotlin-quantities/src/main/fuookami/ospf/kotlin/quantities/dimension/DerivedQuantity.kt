package fuookami.ospf.kotlin.quantities.dimension

/**
 * 导出量纲类
 * 由基础量纲的幂次组成，如 L²·M·T⁻²
 */
class DerivedQuantity(
    quantities: List<FundamentalQuantity>,
    val name: String? = null,
    val symbol: String? = null
) {
    val quantities = quantities.sortedBy { it.dimension.symbol }

    constructor(
        dimension: FundamentalQuantityDimension,
        name: String? = null,
        symbol: String? = null
    ) : this(
        quantities = listOf(FundamentalQuantity(dimension)),
        name = name,
        symbol = symbol
    )

    constructor(
        quantity: FundamentalQuantity,
        name: String? = null,
        symbol: String? = null
    ) : this(
        quantities = listOf(quantity),
        name = name,
        symbol = symbol
    )

    constructor(
        quantity: DerivedQuantity,
        name: String? = null,
        symbol: String? = null
    ) : this(
        quantities = quantity.quantities.toList(),
        name = name,
        symbol = symbol
    )

    /**
     * 取负（所有幂次取反）
     */
    operator fun unaryMinus(): DerivedQuantity {
        return DerivedQuantity(quantities.map { -it })
    }

    /**
     * 获取指定量纲的幂次
     */
    fun getPower(dimension: FundamentalQuantityDimension): Int {
        return quantities.find { it.dimension == dimension }?.index ?: 0
    }

    /**
     * 添加量纲幂次
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
            DerivedQuantity(newQuantities, name, symbol)
        } else {
            DerivedQuantity(quantities + FundamentalQuantity(dimension, power), name, symbol)
        }
    }

    /**
     * 检查是否为无量纲
     */
    fun isNone(): Boolean = quantities.isEmpty()

    /**
     * 获取量纲符号表示
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
     */
    fun pow(index: Int): DerivedQuantity {
        if (index == 0) return DerivedQuantity(emptyList(), name, "1")
        if (index == 1) return this
        return DerivedQuantity(quantities.map { it * index }, name, symbol)
    }

    /**
     * 倒数
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
 */
val DimLess = DerivedQuantity(emptyList(), "Dimensionless", "1")

operator fun FundamentalQuantity.times(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) + other.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun FundamentalQuantity.div(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) - other.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun FundamentalQuantity.times(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun FundamentalQuantity.div(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) - quantity.index
    }
    indexes[this.dimension] = indexes.getOrDefault(this.dimension, 0) + this.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.times(other: Int): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index * other
    }
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.div(other: Int): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index / other
    }
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.times(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) + other.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.div(other: FundamentalQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    indexes[other.dimension] = indexes.getOrDefault(other.dimension, 0) - other.index
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.times(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}

operator fun DerivedQuantity.div(other: DerivedQuantity): DerivedQuantity {
    val indexes = mutableMapOf<FundamentalQuantityDimension, Int>()
    for (quantity in this.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) + quantity.index
    }
    for (quantity in other.quantities) {
        indexes[quantity.dimension] = indexes.getOrDefault(quantity.dimension, 0) - quantity.index
    }
    // 过滤掉幂次为0的量纲
    return DerivedQuantity(indexes.filter { it.value != 0 }.map { FundamentalQuantity(it.key, it.value) })
}
