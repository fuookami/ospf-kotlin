package fuookami.ospf.kotlin.utils.physics.dimension

/**
 * 基础量纲接口
 * 支持标准量纲和自定义量纲
 */
interface FundamentalQuantityDimension {
    val symbol: String
    val name: String
    
    override fun toString(): String
}

/**
 * 标准基础量纲枚举
 */
enum class StandardFundamentalQuantityDimension(
    override val symbol: String,
    val dimensionName: String
) : FundamentalQuantityDimension {
    Length("L", "length"),
    Mass("M", "mass"),
    Time("T", "time"),
    Current("I", "current"),
    Temperature("Θ", "temperature"),
    SubstanceAmount("N", "amount of substance"),
    LuminousIntensity("J", "luminous intensity"),
    PlaneAngle("rad", "plane angle"),
    SolidAngle("sr", "solid angle"),
    Information("B", "information");

    override fun toString() = symbol
}

/**
 * 自定义基础量纲
 * 用于支持用户定义的量纲类型
 */
data class CustomFundamentalQuantityDimension(
    override val symbol: String,
    override val name: String
) : FundamentalQuantityDimension {
    init {
        require(symbol.isNotBlank()) { "Symbol cannot be blank" }
        require(name.isNotBlank()) { "Name cannot be blank" }
    }

    override fun toString() = symbol
}

// ============================================================================
// 标准量纲实例（向后兼容）
// ============================================================================

val L = StandardFundamentalQuantityDimension.Length
val M = StandardFundamentalQuantityDimension.Mass
val T = StandardFundamentalQuantityDimension.Time
val I = StandardFundamentalQuantityDimension.Current
val Theta = StandardFundamentalQuantityDimension.Temperature
val N = StandardFundamentalQuantityDimension.SubstanceAmount
val J = StandardFundamentalQuantityDimension.LuminousIntensity
val rad = StandardFundamentalQuantityDimension.PlaneAngle
val sr = StandardFundamentalQuantityDimension.SolidAngle
val B = StandardFundamentalQuantityDimension.Information

// 注意：以下别名已移至 Dimensions.kt，以避免命名冲突
// 在 Dimensions.kt 中，这些名称被重新定义为 DerivedQuantity 类型

/**
 * 创建自定义基础量纲
 */
fun CustomDimension(symbol: String, name: String): FundamentalQuantityDimension {
    return CustomFundamentalQuantityDimension(symbol, name)
}

/**
 * 基础量纲值
 * 表示量纲的幂次
 */
data class FundamentalQuantity(val dimension: FundamentalQuantityDimension, val index: Int = 1) {
    operator fun plus(rhs: FundamentalQuantity): FundamentalQuantity {
        require(dimension == rhs.dimension) { 
            "Cannot add quantities with different dimensions: ${dimension.symbol} vs ${rhs.dimension.symbol}" 
        }
        return FundamentalQuantity(dimension, this.index + rhs.index)
    }

    operator fun minus(rhs: FundamentalQuantity): FundamentalQuantity {
        require(dimension == rhs.dimension) { 
            "Cannot subtract quantities with different dimensions: ${dimension.symbol} vs ${rhs.dimension.symbol}" 
        }
        return FundamentalQuantity(dimension, this.index - rhs.index)
    }

    operator fun unaryMinus(): FundamentalQuantity {
        return FundamentalQuantity(dimension, -index)
    }

    override fun toString() = "$dimension$index"
}

operator fun FundamentalQuantityDimension.times(index: Int) = FundamentalQuantity(this, index)
operator fun FundamentalQuantity.times(index: Int) = FundamentalQuantity(dimension, index * this.index)
operator fun FundamentalQuantity.div(index: Int) = FundamentalQuantity(dimension, this.index / index)
