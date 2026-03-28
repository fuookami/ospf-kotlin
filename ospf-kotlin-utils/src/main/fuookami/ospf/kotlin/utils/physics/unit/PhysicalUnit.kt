package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.algebra.number.FltX
import fuookami.ospf.kotlin.utils.math.algebra.number.RtnX
import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.DerivedQuantity
import fuookami.ospf.kotlin.utils.physics.dimension.div
import fuookami.ospf.kotlin.utils.physics.dimension.times

/**
 * 物理单位抽象�?
 * 支持单位转换、量纲检�?
 * 
 * Physical unit abstract class
 * Supports unit conversion and dimension checking
 * 
 * 单位是独立的实体，不绑定到特定单位制�?
 * Units are independent entities, not bound to any specific unit system.
 */
abstract class PhysicalUnit {
    abstract val name: String?
    abstract val symbol: String?

    abstract val quantity: DerivedQuantity
    abstract val scale: Scale

    /**
     * 检查量纲是否相�?
     * Check if dimensions are the same
     */
    fun sameDimension(other: PhysicalUnit): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 转换到另一个单�?
     * Convert to another unit
     * @param unit 目标单位 / Target unit
     * @return 转换因子，如果量纲不同返�?null / Conversion factor, or null if dimensions differ
     */
    fun to(unit: PhysicalUnit): Scale? {
        return if (quantity == unit.quantity) {
            scale / unit.scale
        } else {
            null
        }
    }

    /**
     * 从另一个单位转换过�?
     * Convert from another unit
     */
    fun from(unit: PhysicalUnit): Scale? {
        return unit.to(this)
    }

    /**
     * 检查是否可以转换到目标单位
     * Check if can convert to target unit
     */
    fun canConvertTo(unit: PhysicalUnit): Boolean {
        return quantity == unit.quantity
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (quantity != other.quantity) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: name ?: "${quantity.dimensionSymbol()}(${scale})"
    }
}

/**
 * 导出物理单位抽象�?
 * Derived physical unit abstract class
 * 
 * 通过现有单位组合创建的导出单�?
 * Derived units created by combining existing units
 */
abstract class DerivedPhysicalUnit(
    private val unit: PhysicalUnit,
) : PhysicalUnit() {
    override val quantity by unit::quantity
    override val scale by unit::scale
}

/**
 * 匿名物理单位
 * Anonymous physical unit
 * 
 * 用于动态创建的单位实例
 * Used for dynamically created unit instances
 */
data class AnonymousPhysicalUnit(
    override val quantity: DerivedQuantity,
    override val scale: Scale,
    override val name: String? = null,
    override val symbol: String? = null
) : PhysicalUnit() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (quantity != other.quantity) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = quantity.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: ""
    }
}

/**
 * 无单�?
 * No unit
 */
object NoneUnit : PhysicalUnit() {
    override val quantity = DerivedQuantity(emptyList())
    override val scale = Scale()
    override val name: String? = null
    override val symbol: String? = null
}

/**
 * 量纲单位（无量纲�?
 * Quantity unit (dimensionless)
 */
data class QuantityUnit(
    override val name: String? = null,
    override val symbol: String? = null
) : PhysicalUnit() {
    override val quantity = DerivedQuantity(emptyList())
    override val scale = Scale()
}

// ============================================================================
// 单位运算�?/ Unit Operators
// ============================================================================

operator fun PhysicalUnit.times(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale * scale,
    )
}

operator fun PhysicalUnit.div(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity,
        scale = this.scale / scale,
    )
}

operator fun PhysicalUnit.times(other: PhysicalUnit): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity * other.quantity,
        scale = this.scale * other.scale
    )
}

operator fun PhysicalUnit.div(other: PhysicalUnit): PhysicalUnit {
    return AnonymousPhysicalUnit(
        quantity = this.quantity / other.quantity,
        scale = this.scale / other.scale
    )
}

fun PhysicalUnit.pow(index: Int): PhysicalUnit {
    return if (index > 0) {
        pow(index - 1) * this
    } else if (index < 0) {
        pow(index + 1) / this
    } else {
        NoneUnit
    }
}

fun PhysicalUnit.reciprocal(): PhysicalUnit {
    return NoneUnit / this
}


