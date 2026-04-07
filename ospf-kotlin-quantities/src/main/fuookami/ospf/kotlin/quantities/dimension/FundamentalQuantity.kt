/**
 * 基础量纲定义
 * Fundamental Quantity Dimension Definitions
 *
 * 定义物理量的基础量纲，包括国际单位制（SI）的基本量纲和辅助量纲。
 * Defines fundamental dimensions of physical quantities, including SI base dimensions and supplementary dimensions.
 */
package fuookami.ospf.kotlin.quantities.dimension

/**
 * 基础量纲接口
 * Fundamental quantity dimension interface
 *
 * 支持标准量纲和自定义量纲。
 * Supports standard dimensions and custom dimensions.
 */
interface FundamentalQuantityDimension {
    /** 量纲符号，如 "L"、"M"、"T" / Dimension symbol, e.g., "L", "M", "T" */
    val symbol: String
    /** 量纲名称（物理量名称，非枚举常量名） / Dimension name (physical quantity name, not enum constant name) */
    val dimensionName: String

    override fun toString(): String
}

/**
 * 标准基础量纲枚举
 * Standard fundamental quantity dimension enumeration
 *
 * 包含国际单位制（SI）定义的七个基本量纲和两个辅助量纲，以及信息量纲。
 * Contains the seven SI base dimensions, two supplementary dimensions, and the information dimension.
 */
enum class StandardFundamentalQuantityDimension(
    /** 量纲符号 / Dimension symbol */
    override val symbol: String,
    /** 量纲名称（物理量名称） / Dimension name (physical quantity name) */
    override val dimensionName: String
) : FundamentalQuantityDimension {
    /** 长度 / Length */
    Length("L", "length"),
    /** 质量 / Mass */
    Mass("M", "mass"),
    /** 时间 / Time */
    Time("T", "time"),
    /** 电流 / Electric current */
    Current("I", "current"),
    /** 温度 / Temperature */
    Temperature("Θ", "temperature"),
    /** 物质的量 / Amount of substance */
    SubstanceAmount("N", "amount of substance"),
    /** 发光强度 / Luminous intensity */
    LuminousIntensity("J", "luminous intensity"),
    /** 平面角 / Plane angle */
    PlaneAngle("rad", "plane angle"),
    /** 立体角 / Solid angle */
    SolidAngle("sr", "solid angle"),
    /** 信息 / Information */
    Information("B", "information");

    override fun toString() = symbol
}

/**
 * 自定义基础量纲
 * Custom fundamental quantity dimension
 *
 * 用于支持用户定义的量纲类型。
 * Used to support user-defined dimension types.
 *
 * @property symbol 量纲符号 / Dimension symbol
 * @property dimensionName 量纲名称 / Dimension name
 */
data class CustomFundamentalQuantityDimension(
    override val symbol: String,
    override val dimensionName: String
) : FundamentalQuantityDimension {
    init {
        require(symbol.isNotBlank()) { "Symbol cannot be blank" }
        require(dimensionName.isNotBlank()) { "Name cannot be blank" }
    }

    override fun toString() = symbol
}

// ============================================================================
// 标准量纲实例（向后兼容）
// Standard dimension instances (backward compatibility)
// ============================================================================

/** 长度量纲 / Length dimension */
val L = StandardFundamentalQuantityDimension.Length
/** 质量量纲 / Mass dimension */
val M = StandardFundamentalQuantityDimension.Mass
/** 时间量纲 / Time dimension */
val T = StandardFundamentalQuantityDimension.Time
/** 电流量纲 / Current dimension */
val I = StandardFundamentalQuantityDimension.Current
/** 温度量纲 / Temperature dimension */
val Theta = StandardFundamentalQuantityDimension.Temperature
/** 物质的量量纲 / Amount of substance dimension */
val N = StandardFundamentalQuantityDimension.SubstanceAmount
/** 发光强度量纲 / Luminous intensity dimension */
val J = StandardFundamentalQuantityDimension.LuminousIntensity
/** 平面角量纲 / Plane angle dimension */
val rad = StandardFundamentalQuantityDimension.PlaneAngle
/** 立体角量纲 / Solid angle dimension */
val sr = StandardFundamentalQuantityDimension.SolidAngle
/** 信息量纲 / Information dimension */
val B = StandardFundamentalQuantityDimension.Information

// 注意：以下别名已移至 Dimensions.kt，以避免命名冲突
// 在 Dimensions.kt 中，这些名称被重新定义为 DerivedQuantity 类型
// Note: The following aliases have been moved to Dimensions.kt to avoid naming conflicts
// In Dimensions.kt, these names are redefined as DerivedQuantity types

/**
 * 创建自定义基础量纲
 * Creates a custom fundamental dimension
 *
 * @param symbol 量纲符号 / Dimension symbol
 * @param name 量纲名称 / Dimension name
 * @return 自定义基础量纲实例 / Custom fundamental dimension instance
 */
fun CustomDimension(symbol: String, name: String): FundamentalQuantityDimension {
    return CustomFundamentalQuantityDimension(symbol, name)
}

/**
 * 基础量纲值
 * Fundamental quantity value
 *
 * 表示量纲的幂次，用于构建导出量纲。
 * Represents the power of a dimension, used to build derived quantities.
 *
 * @property dimension 基础量纲 / The fundamental dimension
 * @property index 幂次指数，默认为1 / The power exponent, defaults to 1
 */
data class FundamentalQuantity(val dimension: FundamentalQuantityDimension, val index: Int = 1) {
    /**
     * 两个基础量纲值相加（幂次相加）
     * Adds two fundamental quantities (adds their powers)
     *
     * @param rhs 右操作数 / The right operand
     * @return 相加后的基础量纲值 / The resulting fundamental quantity
     * @throws IllegalArgumentException 如果量纲不同 / If dimensions are different
     */
    operator fun plus(rhs: FundamentalQuantity): FundamentalQuantity {
        require(dimension == rhs.dimension) {
            "Cannot add quantities with different dimensions: ${dimension.symbol} vs ${rhs.dimension.symbol}"
        }
        return FundamentalQuantity(dimension, this.index + rhs.index)
    }

    /**
     * 两个基础量纲值相减（幂次相减）
     * Subtracts two fundamental quantities (subtracts their powers)
     *
     * @param rhs 右操作数 / The right operand
     * @return 相减后的基础量纲值 / The resulting fundamental quantity
     * @throws IllegalArgumentException 如果量纲不同 / If dimensions are different
     */
    operator fun minus(rhs: FundamentalQuantity): FundamentalQuantity {
        require(dimension == rhs.dimension) {
            "Cannot subtract quantities with different dimensions: ${dimension.symbol} vs ${rhs.dimension.symbol}"
        }
        return FundamentalQuantity(dimension, this.index - rhs.index)
    }

    /**
     * 取负（幂次取反）
     * Negation operator (negates the power)
     *
     * @return 幂次取反后的基础量纲值 / The negated fundamental quantity
     */
    operator fun unaryMinus(): FundamentalQuantity {
        return FundamentalQuantity(dimension, -index)
    }

    override fun toString() = "$dimension$index"
}

/**
 * 基础量纲与整数相乘，创建基础量纲值
 * Multiplies a dimension by an integer to create a fundamental quantity
 */
operator fun FundamentalQuantityDimension.times(index: Int) = FundamentalQuantity(this, index)

/**
 * 基础量纲值与整数相乘
 * Multiplies a fundamental quantity by an integer
 */
operator fun FundamentalQuantity.times(index: Int) = FundamentalQuantity(dimension, index * this.index)

/**
 * 基础量纲值除以整数
 * Divides a fundamental quantity by an integer
 */
operator fun FundamentalQuantity.div(index: Int) = FundamentalQuantity(dimension, this.index / index)