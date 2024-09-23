package fuookami.ospf.kotlin.utils.physics.quantity

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.physics.unit.*

data class Quantity<V : Arithmetic<V>>(
    val value: V,
    val unit: PhysicalUnit
) {
    fun to(unit: PhysicalUnit): Quantity<V> {
        TODO("not implemented yet")
    }
}

infix fun <V> Quantity<V>.eq(other: Quantity<V>): Boolean where V : Arithmetic<V>, V: Eq<V> {
    return if (this.unit == other.unit) {
        this.value eq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        false
    }
}

infix fun <V> Quantity<V>.partialOrd(other: Quantity<V>): Order? where V : Arithmetic<V>, V: PartialOrd<V> {
    return if (this.unit == other.unit) {
        this.value partialOrd other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        null
    }
}

operator fun <V : Arithmetic<V>> V.times(unit: PhysicalUnit): Quantity<V> {
    return Quantity(this, unit)
}

operator fun <V> Quantity<V>.plus(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Plus<V, V> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        TODO("not implemented yet")
    }
}

operator fun <V> Quantity<V>.minus(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Minus<V, V> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        TODO("not implemented yet")
    }
}

operator fun <V> Quantity<V>.times(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Times<V, V> {
    return if (this.unit.quantity == other.unit.quantity) {
        Quantity(this.value * other.value, this.unit * other.unit)
    } else {
        TODO("not implemented yet")
    }
}

operator fun <V> Quantity<V>.div(other: Quantity<V>): Quantity<V> where V : Arithmetic<V>, V : Div<V, V> {
    return if (this.unit.quantity == other.unit.quantity) {
        Quantity(this.value / other.value, this.unit / other.unit)
    } else {
        TODO("not implemented yet")
    }
}
