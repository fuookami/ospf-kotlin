package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

abstract class PhysicalUnit {
    abstract val name: String?
    abstract val symbol: String?

    abstract val system: UnitSystem
    abstract val quantity: DerivedQuantity
    abstract val scale: Scale

    fun to(system: UnitSystem): PhysicalUnit {
        if (system == this.system) {
            return this
        } else {
            TODO("not implemented yet")
        }
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
        return symbol ?: TODO("not implemented yet")
    }
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
        return symbol ?: TODO("not implemented yet")
    }
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
