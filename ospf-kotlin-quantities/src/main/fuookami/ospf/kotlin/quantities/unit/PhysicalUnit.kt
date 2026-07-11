/**
 * 物理单位抽象类
 * Physical unit abstract classes
 *
 * 提供物理单位的抽象定义，支持单位转换、量纲检查和单位运算。
 * Provides abstract definitions for physical units, supporting unit conversion, dimension checking, and unit operations.
 *
 * 核心概念 / Core concepts:
 * - [PhysicalUnit]: 物理单位抽象基类 / Physical unit abstract base class
 * - [DerivedPhysicalUnit]: 导出物理单位抽象类 / Derived physical unit abstract class
 * - [AnonymousPhysicalUnit]: 匿名物理单位（用于动态创建）/ Anonymous physical unit (for dynamic creation)
 * - [NoneUnit]: 无单位（无量纲）/ No unit (dimensionless)
 * - [QuantityUnit]: 量纲单位（无量纲）/ Quantity unit (dimensionless)
 *
 * 单位是独立的实体，不绑定到特定单位制。
 * Units are independent entities, not bound to any specific unit system.
 *
 * 支持的单位运算 / Supported unit operations:
 * - 乘法：单位 × 单位 / Multiplication: unit × unit
 * - 除法：单位 / 单位 / Division: unit / unit
 * - 幂运算：单位^n / Power operation: unit^n
 * - 倒数：单位.reciprocal() / Reciprocal: unit.reciprocal()
 * - 缩放：单位 × 比例 / Scaling: unit × scale
*/
package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.*

/**
 * 单位转换规则
 * Unit conversion rule
 *
 * 描述单位到标准单位的转换方式。
 * Describes how a unit converts to its standard unit.
 *
 * - [Linear]: 普通线性转换，standard = value * scale
 * - [Affine]: 仿射转换（绝对温标），standard = value * scale + offset
 *
 * 仿射单位参与加减乘除的语义不同于线性单位：
 * Affine units have different arithmetic semantics than linear units:
 * - 仿射单位不允许乘除幂运算
 *   Affine units cannot participate in multiplication, division, or power operations
 * - 仿射 + 仿射 不应作为普通加法
 *   Affine + Affine should not be valid as regular addition
 * - 仿射 - 仿射 得到线性温差
 *   Affine - Affine yields a linear temperature difference
 * - 仿射 ± 线性温差 得到新的仿射值
 *   Affine ± LinearDifference yields a new affine value
*/
sealed interface UnitConversionRule {

    /**
     * 转换规则中的线性比例因子
     * The linear scale factor within the conversion rule
    */
    val scale: Scale

    /**
     * 是否为仿射转换
     * Whether this is an affine conversion
    */
    val isAffine: Boolean get() = this is Affine

    /**
     * 线性转换规则
     * Linear conversion rule
     *
     * standard = value * scale
     * 目标值 = (标准值) / targetScale
     * Target value = (standard value) / targetScale
     *
     * @param scale 相对于标准单位的线性比例 / Linear scale relative to standard unit
    */
    data class Linear(
        override val scale: Scale
    ) : UnitConversionRule

    /**
     * 仿射转换规则
     * Affine conversion rule
     *
     * standard = value * scale + offset
     * 目标值 = (standard - targetOffset) / targetScale
     * Target value = (standard - targetOffset) / targetScale
     *
     * 用于摄氏度、华氏度等绝对温标。
     * Used for absolute temperature scales like Celsius, Fahrenheit.
     *
     * @param scale 相对于标准单位的线性比例 / Linear scale relative to standard unit
     * @param offset 转换到标准单位时的偏移量 / Offset when converting to standard unit
    */
    data class Affine(
        override val scale: Scale,
        val offset: FltX
    ) : UnitConversionRule
}

/**
 * 物理单位抽象类
 * Physical unit abstract class
 *
 * 支持单位转换、量纲检查。
 * Supports unit conversion and dimension checking.
 *
 * 单位是独立的实体，不绑定到特定单位制。
 * Units are independent entities, not bound to any specific unit system.
*/
abstract class PhysicalUnit {

    /**
     * 单位名称
     * Unit name
    */
    abstract val name: String?

    /**
     * 单位符号
     * Unit symbol
    */
    abstract val symbol: String?

    /**
     * 单位量纲
     * Unit quantity (dimension)
    */
    abstract val quantity: DerivedQuantity

    /**
     * 单位取值域
     * Unit value domain
    */
    open val domain: QuantityDomain get() = quantity.domain

    /**
     * 单位转换规则
     * Unit conversion rule
     *
     * 描述此单位到标准单位的转换方式。默认为线性转换。
     * Describes how this unit converts to the standard unit. Defaults to linear conversion.
    */
    abstract val conversionRule: UnitConversionRule

    /**
     * 单位比例（相对于标准单位）
     * Unit scale (relative to standard unit)
     *
     * 兼容属性，从转换规则中读取线性比例因子。
     * Compatibility property that reads the linear scale factor from the conversion rule.
    */
    val scale: Scale
        get() = conversionRule.scale

    /**
     * 是否为仿射单位
     * Whether this is an affine unit
    */
    val isAffine: Boolean
        get() = conversionRule.isAffine

    /**
     * 检查量纲是否相同
     * Check if dimensions are the same
     *
     * @param other 另一个单位 / Another unit
     * @return 如果量纲相同返回 true，否则返回 false / Returns true if dimensions are the same, false otherwise
    */
    fun sameDimension(other: PhysicalUnit): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 转换到另一个单位（线性比例因子）
     * Convert to another unit (linear scale factor)
     *
     * 仅当两端均为线性单位时返回纯比例因子；
     * 仿射单位间的转换不适用于纯比例因子，请使用 [convertValue]。
     * Returns a pure scale factor only when both units are linear;
     * for affine unit conversion, use [convertValue] instead.
     *
     * @param unit 目标单位 / Target unit
     * @return 转换因子，如果量纲不同返回 null / Conversion factor, or null if dimensions differ
    */
    fun to(unit: PhysicalUnit): Scale? {
        if (quantity != unit.quantity) return null
        // 如果任一端为仿射单位，纯比例因子不足以表达转换
        // If either end is affine, a pure scale factor is insufficient
        if (this.isAffine || unit.isAffine) return null
        return scale / unit.scale
    }

    /**
     * 从另一个单位转换过来
     * Convert from another unit
     *
     * @param unit 源单位 / Source unit
     * @return 转换因子，如果量纲不同返回 null / Conversion factor, or null if dimensions differ
    */
    fun from(unit: PhysicalUnit): Scale? {
        return unit.to(this)
    }

    /**
     * 将值从此单位转换到目标单位
     * Convert a value from this unit to the target unit
     *
     * 支持线性和仿射转换：
     * Supports both linear and affine conversions:
     * - 线性: target = value * (thisScale / targetScale)
     * - 仿射: standard = value * thisScale + thisOffset;
     *         target = (standard - targetOffset) / targetScale
     *
     * @param value 此单位的值 / Value in this unit
     * @param unit 目标单位 / Target unit
     * @return 转换后的值，如果量纲不同返回 null / Converted value, or null if dimensions differ
    */
    fun convertValue(value: FltX, unit: PhysicalUnit): FltX? {
        if (quantity != unit.quantity) return null

        val thisRule = conversionRule
        val targetRule = unit.conversionRule

        return when (thisRule) {
            is UnitConversionRule.Linear if targetRule is UnitConversionRule.Linear -> {
                // 线性 → 线性: target = value * thisScale / targetScale
                val factor = thisRule.scale / targetRule.scale
                value * (factor.value ?: return null)
            }

            is UnitConversionRule.Linear if targetRule is UnitConversionRule.Affine -> {
                // 线性 → 仿射: 先转到标准，再从标准转到仿射目标
                // standard = value * thisScale; target = (standard - targetOffset) / targetScale
                val standard = value * (thisRule.scale.value ?: return null)
                (standard - targetRule.offset) / (targetRule.scale.value ?: return null)
            }

            is UnitConversionRule.Affine if targetRule is UnitConversionRule.Linear -> {
                // 仿射 → 线性: standard = value * thisScale + thisOffset; target = standard / targetScale
                val standard = value * (thisRule.scale.value ?: return null) + thisRule.offset
                standard / (targetRule.scale.value ?: return null)
            }

            is UnitConversionRule.Affine if targetRule is UnitConversionRule.Affine -> {
                // 仿射 → 仿射: 通过标准单位中转
                // standard = value * thisScale + thisOffset; target = (standard - targetOffset) / targetScale
                val standard = value * (thisRule.scale.value ?: return null) + thisRule.offset
                (standard - targetRule.offset) / (targetRule.scale.value ?: return null)
            }

            else -> null
        }
    }

    /**
     * 检查是否可以转换到目标单位
     * Check if can convert to target unit
     *
     * @param unit 目标单位 / Target unit
     * @return 如果可以转换返回 true，否则返回 false / Returns true if convertible, false otherwise
    */
    fun canConvertTo(unit: PhysicalUnit): Boolean {
        return quantity == unit.quantity
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (quantity != other.quantity) return false
        if (conversionRule != other.conversionRule) return false
        if (domain != other.domain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + conversionRule.hashCode()
        result = 31 * result + domain.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: name ?: "${quantity.dimensionSymbol()}(${scale})"
    }
}

/**
 * 导出物理单位抽象类
 * Derived physical unit abstract class
 *
 * 通过现有单位组合创建的导出单位。
 * Derived units created by combining existing units.
 *
 * @param unit 基础单位（用于推导量纲和比例）/ Base unit (for deriving quantity and scale)
*/
abstract class DerivedPhysicalUnit(
    private val unit: PhysicalUnit,
) : PhysicalUnit() {
    override val quantity by unit::quantity
    override val domain by unit::domain
    override val conversionRule by unit::conversionRule
}

/**
 * 匿名物理单位
 * Anonymous physical unit
 *
 * 用于动态创建的单位实例。
 * Used for dynamically created unit instances.
 *
 * @param quantity 单位量纲 / Unit quantity (dimension)
 * @param conversionRule 单位转换规则 / Unit conversion rule
 * @param name 单位名称（可选）/ Unit name (optional)
 * @param symbol 单位符号（可选）/ Unit symbol (optional)
*/
data class AnonymousPhysicalUnit(
    override val quantity: DerivedQuantity,
    override val conversionRule: UnitConversionRule,
    override val name: String? = null,
    override val symbol: String? = null,
    override val domain: QuantityDomain = quantity.domain
) : PhysicalUnit() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (quantity != other.quantity) return false
        if (conversionRule != other.conversionRule) return false
        if (domain != other.domain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + conversionRule.hashCode()
        result = 31 * result + domain.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: ""
    }
}

/**
 * 无单位
 * No unit
 *
 * 表示无量纲的特殊单位。
 * Special unit representing dimensionless quantity.
*/
object NoneUnit : PhysicalUnit() {
    override val quantity = DerivedQuantity(emptyList())
    override val conversionRule = UnitConversionRule.Linear(Scale())
    override val name: String? = null
    override val symbol: String? = null
}

/**
 * 量纲单位（无量纲）
 * Quantity unit (dimensionless)
 *
 * 用于表示无量纲量的单位。
 * Unit for representing dimensionless quantities.
 *
 * @param name 单位名称（可选）/ Unit name (optional)
 * @param symbol 单位符号（可选）/ Unit symbol (optional)
*/
data class QuantityUnit(
    override val name: String? = null,
    override val symbol: String? = null
) : PhysicalUnit() {
    override val quantity = DerivedQuantity(emptyList())
    override val conversionRule = UnitConversionRule.Linear(Scale())
}

// ============================================================================
// 单位运算符 / Unit Operators
// ============================================================================

/**
 * 检查当前单位是否为线性单位，若为仿射单位则抛出异常
 * Check that the current unit is linear; throws if it is affine
 *
 * @param operation 触发检查的运算名称，用于错误信息 / Name of the operation triggering the check, used in the error message
*/
private fun PhysicalUnit.requireLinearForUnitOperation(operation: String) {
    require(!isAffine) {
        "Cannot $operation affine unit '$this'. Use a linear difference unit instead."
    }
}

/**
 * 单位与整数比例相乘
 * Multiply unit by integer scale
 *
 * @param scale 整数比例因子 / Integer scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.times(scale: Int): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位除以整数比例
 * Divide unit by integer scale
 *
 * @param scale 整数比例因子 / Integer scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.div(scale: Int): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位与双精度浮点比例相乘
 * Multiply unit by double scale
 *
 * @param scale 双精度浮点比例因子 / Double scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.times(scale: Double): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位除以双精度浮点比例
 * Divide unit by double scale
 *
 * @param scale 双精度浮点比例因子 / Double scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.div(scale: Double): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位与扩展精度浮点比例相乘
 * Multiply unit by FltX scale
 *
 * @param scale 扩展精度浮点比例因子 / Extended precision float scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.times(scale: FltX): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位除以扩展精度浮点比例
 * Divide unit by FltX scale
 *
 * @param scale 扩展精度浮点比例因子 / Extended precision float scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.div(scale: FltX): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位与有理数比例相乘
 * Multiply unit by RtnX scale
 *
 * @param scale 有理数比例因子 / Rational number scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.times(scale: RtnX): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位除以有理数比例
 * Divide unit by RtnX scale
 *
 * @param scale 有理数比例因子 / Rational number scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.div(scale: RtnX): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / Scale(scale)),
        domain = this.domain
    )
}

/**
 * 单位与比例相乘
 * Multiply unit by scale
 *
 * @param scale 比例因子 / Scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.times(scale: Scale): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * scale),
        domain = this.domain
    )
}

/**
 * 单位除以比例
 * Divide unit by scale
 *
 * @param scale 比例因子 / Scale factor
 * @return 缩放后的新单位 / New scaled unit
*/
operator fun PhysicalUnit.div(scale: Scale): PhysicalUnit {
    requireLinearForUnitOperation("scale")
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / scale),
        domain = this.domain
    )
}

/**
 * 两个单位相乘
 * Multiply two units
 *
 * @param other 另一个单位 / Another unit
 * @return 相乘后的新单位 / New unit resulting from multiplication
*/
operator fun PhysicalUnit.times(other: PhysicalUnit): PhysicalUnit {
    requireLinearForUnitOperation("multiply")
    other.requireLinearForUnitOperation("multiply")
    return AnonymousPhysicalUnit(
        quantity = this.quantity * other.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale * other.scale)
    )
}

/**
 * 两个单位相除
 * Divide two units
 *
 * @param other 另一个单位 / Another unit
 * @return 相除后的新单位 / New unit resulting from division
*/
operator fun PhysicalUnit.div(other: PhysicalUnit): PhysicalUnit {
    requireLinearForUnitOperation("divide")
    other.requireLinearForUnitOperation("divide")
    return AnonymousPhysicalUnit(
        quantity = this.quantity / other.quantity,
        conversionRule = UnitConversionRule.Linear(this.scale / other.scale)
    )
}

/**
 * 单位的幂运算
 * Power operation on unit
 *
 * @param index 幂指数 / Power index
 * @return 单位的幂次结果 / Result of unit power operation
*/
fun PhysicalUnit.pow(index: Int): PhysicalUnit {
    requireLinearForUnitOperation("raise")
    return if (index > 0) {
        pow(index - 1) * this
    } else if (index < 0) {
        pow(index + 1) / this
    } else {
        NoneUnit
    }
}

/**
 * 单位的倒数
 * Reciprocal of unit
 *
 * @return 单位的倒数 / Reciprocal of the unit
*/
fun PhysicalUnit.reciprocal(): PhysicalUnit {
    return NoneUnit / this
}
