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

import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.RtnX
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.DerivedQuantity
import fuookami.ospf.kotlin.quantities.dimension.QuantityDomain
import fuookami.ospf.kotlin.quantities.dimension.div
import fuookami.ospf.kotlin.quantities.dimension.times

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
     * 单位比例（相对于标准单位）
     * Unit scale (relative to standard unit)
     */
    abstract val scale: Scale

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
     * 转换到另一个单位
     * Convert to another unit
     *
     * @param unit 目标单位 / Target unit
     * @return 转换因子，如果量纲不同返回 null / Conversion factor, or null if dimensions differ
     */
    fun to(unit: PhysicalUnit): Scale? {
        return if (quantity == unit.quantity) {
            scale / unit.scale
        } else {
            null
        }
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
        if (scale != other.scale) return false
        if (domain != other.domain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + scale.hashCode()
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
    override val scale by unit::scale
}

/**
 * 匿名物理单位
 * Anonymous physical unit
 *
 * 用于动态创建的单位实例。
 * Used for dynamically created unit instances.
 *
 * @param quantity 单位量纲 / Unit quantity (dimension)
 * @param scale 单位比例 / Unit scale
 * @param name 单位名称（可选）/ Unit name (optional)
 * @param symbol 单位符号（可选）/ Unit symbol (optional)
 */
data class AnonymousPhysicalUnit(
    override val quantity: DerivedQuantity,
    override val scale: Scale,
    override val name: String? = null,
    override val symbol: String? = null,
    override val domain: QuantityDomain = quantity.domain
) : PhysicalUnit() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (quantity != other.quantity) return false
        if (scale != other.scale) return false
        if (domain != other.domain) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + scale.hashCode()
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
    override val scale = Scale()
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
    override val scale = Scale()
}

// ============================================================================
// 单位运算符 / Unit Operators
// ============================================================================

/**
 * 单位与整数比例相乘
 * Multiply unit by integer scale
 */
operator fun PhysicalUnit.times(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位除以整数比例
 * Divide unit by integer scale
 */
operator fun PhysicalUnit.div(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位与双精度浮点比例相乘
 * Multiply unit by double scale
 */
operator fun PhysicalUnit.times(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位除以双精度浮点比例
 * Divide unit by double scale
 */
operator fun PhysicalUnit.div(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位与扩展精度浮点比例相乘
 * Multiply unit by FltX scale
 */
operator fun PhysicalUnit.times(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位除以扩展精度浮点比例
 * Divide unit by FltX scale
 */
operator fun PhysicalUnit.div(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位与有理数比例相乘
 * Multiply unit by RtnX scale
 */
operator fun PhysicalUnit.times(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位除以有理数比例
 * Divide unit by RtnX scale
 */
operator fun PhysicalUnit.div(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale),
        domain = this.domain
    )
}

/**
 * 单位与比例相乘
 * Multiply unit by scale
 */
operator fun PhysicalUnit.times(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * scale,
        domain = this.domain
    )
}

/**
 * 单位除以比例
 * Divide unit by scale
 */
operator fun PhysicalUnit.div(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / scale,
        domain = this.domain
    )
}

/**
 * 两个单位相乘
 * Multiply two units
 */
operator fun PhysicalUnit.times(other: PhysicalUnit): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity * other.quantity,
        scale = this.scale * other.scale
    )
}

/**
 * 两个单位相除
 * Divide two units
 */
operator fun PhysicalUnit.div(other: PhysicalUnit): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity / other.quantity,
        scale = this.scale / other.scale
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
