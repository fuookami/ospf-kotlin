package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

/**
 * 物理单位抽象类
 * 支持单位转换、量纲检查和单位制
 */
abstract class PhysicalUnit {
    abstract val name: String?
    abstract val symbol: String?

    abstract val system: UnitSystem
    abstract val quantity: DerivedQuantity
    abstract val scale: Scale

    /**
     * 检查量纲是否相同
     */
    fun sameDimension(other: PhysicalUnit): Boolean {
        return this.quantity == other.quantity
    }

    /**
     * 转换到另一个单位
     * @param unit 目标单位
     * @return 转换因子，如果量纲不同返回 null
     */
    fun to(unit: PhysicalUnit): Scale? {
        return if (quantity == unit.quantity) {
            // 相同量纲，计算转换因子
            if (system == unit.system) {
                scale / unit.scale
            } else {
                // 不同单位制，需要找到标准单位进行转换
                convertAcrossSystems(unit)
            }
        } else {
            null
        }
    }

    /**
     * 转换到指定单位制的标准单位
     * @param system 目标单位制
     * @return 转换后的单位，如果无法转换返回 null
     */
    fun to(system: UnitSystem): PhysicalUnit? {
        if (system == this.system) {
            return this
        }
        // 尝试在目标单位制中找到等效单位
        return system.getUnitForQuantity(quantity)
    }

    /**
     * 从另一个单位转换过来
     */
    fun from(unit: PhysicalUnit): Scale? {
        return unit.to(this)
    }

    /**
     * 跨单位制转换
     */
    protected open fun convertAcrossSystems(other: PhysicalUnit): Scale? {
        // 获取两个单位制的比例
        val thisScaleFactor = system.getScaleFactor(quantity)
        val otherScaleFactor = other.system.getScaleFactor(quantity)
        
        // 计算转换因子 = (this.scale * thisScaleFactor) / (other.scale * otherScaleFactor)
        return (scale * thisScaleFactor) / (other.scale * otherScaleFactor)
    }

    /**
     * 检查是否可以转换到目标单位
     */
    fun canConvertTo(unit: PhysicalUnit): Boolean {
        return quantity == unit.quantity
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (system != other.system) return false
        if (quantity != other.quantity) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = system.hashCode()
        result = 31 * result + quantity.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: name ?: "${quantity.dimensionSymbol()}(${scale})"
    }
}

abstract class DerivedPhysicalUnit(
    private val unit: PhysicalUnit,
) : PhysicalUnit() {
    override val system by unit::system
    override val quantity by unit::quantity
    override val scale by unit::scale
}

data class AnonymousPhysicalUnit(
    override val system: UnitSystem,
    override val quantity: DerivedQuantity,
    override val scale: Scale,
    override val name: String? = null,
    override val symbol: String? = null
) : PhysicalUnit() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PhysicalUnit) return false

        if (system != other.system) return false
        if (quantity != other.quantity) return false
        if (scale != other.scale) return false

        return true
    }

    override fun hashCode(): Int {
        var result = system.hashCode()
        result = 31 * result + quantity.hashCode()
        result = 31 * result + scale.hashCode()
        return result
    }

    override fun toString(): String {
        return symbol ?: ""
    }
}

object NoneUnit : PhysicalUnit() {
    override val system = SI
    override val quantity = DerivedQuantity(emptyList())
    override val scale = Scale()
    override val name: String? = null
    override val symbol: String? = null
}

data class QuantityUnit(
    override val system: UnitSystem = SI,
    override val name: String? = null,
    override val symbol: String? = null
) : PhysicalUnit() {
    override val quantity = DerivedQuantity(emptyList())
    override val scale = Scale()
}

operator fun PhysicalUnit.times(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: Int): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: Double): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: FltX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale * Scale(scale)
    )
}

operator fun PhysicalUnit.div(scale: RtnX): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale / Scale(scale)
    )
}

operator fun PhysicalUnit.times(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale * scale,
    )
}

operator fun PhysicalUnit.div(scale: Scale): PhysicalUnit {
    return AnonymousPhysicalUnit(
        system = this.system,
        quantity = this.quantity,
        scale = this.scale / scale,
    )
}

operator fun PhysicalUnit.times(other: PhysicalUnit): PhysicalUnit {
    return if (this.system != other.system) {
        TODO("not implemented yet")
    } else {
        AnonymousPhysicalUnit(
            system = this.system,
            quantity = this.quantity * other.quantity,
            scale = this.scale * other.scale
        )
    }
}

operator fun PhysicalUnit.div(other: PhysicalUnit): PhysicalUnit {
    return if (this.system != other.system) {
        TODO("not implemented yet")
    } else {
        AnonymousPhysicalUnit(
            system = this.system,
            quantity = this.quantity / other.quantity,
            scale = this.scale / other.scale
        )
    }
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
