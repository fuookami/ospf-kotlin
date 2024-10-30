package fuookami.ospf.kotlin.utils.physics.quantity

import java.math.BigDecimal
import java.math.BigInteger
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.operator.*
import fuookami.ospf.kotlin.utils.physics.unit.*

data class Quantity<out V>(
    val value: V,
    val unit: PhysicalUnit
)

infix fun <V> Quantity<V>.eq(other: Quantity<V>): Boolean where V : Arithmetic<V>, V : Eq<V> {
    return if (this.unit == other.unit) {
        this.value eq other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        false
    }
}

infix fun <V> Quantity<V>.partialOrd(other: Quantity<V>): Order? where V : Arithmetic<V>, V : PartialOrd<V> {
    return if (this.unit == other.unit) {
        this.value partialOrd other.value
    } else if (this.unit.quantity == other.unit.quantity) {
        TODO("not implemented yet")
    } else {
        null
    }
}

fun <V: RealNumber<V>> Quantity<V>.toInt64(): Quantity<Int64> {
    return Quantity(this.value.toInt64(), this.unit)
}

fun <V: RealNumber<V>> Quantity<V>.toUInt64(): Quantity<UInt64> {
    return Quantity(this.value.toUInt64(), this.unit)
}

fun <V: RealNumber<V>> Quantity<V>.toIntX(): Quantity<IntX> {
    return Quantity(this.value.toIntX(), this.unit)
}

fun <V: RealNumber<V>> Quantity<V>.toFlt64(): Quantity<Flt64> {
    return Quantity(this.value.toFlt64(), this.unit)
}

fun <V: RealNumber<V>> Quantity<V>.toFltX(): Quantity<FltX> {
    return Quantity(this.value.toFltX(), this.unit)
}

@JvmName("convertQuantityInt64")
fun Quantity<Int64>.to(unit: PhysicalUnit): Quantity<Int64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toInt64() * this.value, unit)
        }
    }
}

@JvmName("convertQuantityUInt64")
fun Quantity<UInt64>.to(unit: PhysicalUnit): Quantity<UInt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toUInt64() * this.value, unit)
        }
    }
}

fun Quantity<IntX>.to(unit: PhysicalUnit): Quantity<IntX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toIntX() * this.value, unit)
        }
    }
}

@JvmName("convertQuantityFlt64")
fun Quantity<Flt64>.to(unit: PhysicalUnit): Quantity<Flt64>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it * this.value, unit)
        }
    }
}

@JvmName("convertQuantityFltX")
fun Quantity<FltX>.to(unit: PhysicalUnit): Quantity<FltX>? {
    return if (this.unit == unit) {
        Quantity(this.value, unit)
    } else {
        this.unit.to(unit)?.value?.let {
            Quantity(it.toFltX() * this.value, unit)
        }
    }
}

operator fun <V : Arithmetic<V>> V.times(unit: PhysicalUnit): Quantity<V> {
    return Quantity(this, unit)
}

operator fun Int.times(unit: PhysicalUnit): Quantity<Int64> {
    return Quantity(Int64(this.toLong()), unit)
}

operator fun UInt.times(unit: PhysicalUnit): Quantity<UInt64> {
    return Quantity(UInt64(this.toULong()), unit)
}

operator fun BigInteger.times(unit: PhysicalUnit): Quantity<IntX> {
    return Quantity(IntX(this), unit)
}

operator fun Double.times(unit: PhysicalUnit): Quantity<Flt64> {
    return Quantity(Flt64(this), unit)
}

operator fun BigDecimal.times(unit: PhysicalUnit): Quantity<FltX> {
    return Quantity(FltX(this), unit)
}

@JvmName("plusQuantityInt64")
operator fun Quantity<Int64>.plus(other: Quantity<Int64>): Quantity<Int64> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("plusQuantityUInt64")
operator fun Quantity<UInt64>.plus(other: Quantity<UInt64>): Quantity<UInt64> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("plusQuantityIntX")
operator fun Quantity<IntX>.plus(other: Quantity<IntX>): Quantity<IntX> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("plusQuantityFlt64")
operator fun Quantity<Flt64>.plus(other: Quantity<Flt64>): Quantity<Flt64> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("plusQuantityFltX")
operator fun Quantity<FltX>.plus(other: Quantity<FltX>): Quantity<FltX> {
    return if (this.unit == other.unit) {
        Quantity(this.value + other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value + other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value + other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("minusQuantityInt64")
operator fun Quantity<Int64>.minus(other: Quantity<Int64>): Quantity<Int64> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("minusQuantityUInt64")
operator fun Quantity<UInt64>.minus(other: Quantity<UInt64>): Quantity<UInt64> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("minusQuantityIntX")
operator fun Quantity<IntX>.minus(other: Quantity<IntX>): Quantity<IntX> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("minusQuantityFlt64")
operator fun Quantity<Flt64>.minus(other: Quantity<Flt64>): Quantity<Flt64> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
    } else {
        TODO("not implemented yet")
    }
}

@JvmName("minusQuantityFltX")
operator fun Quantity<FltX>.minus(other: Quantity<FltX>): Quantity<FltX> {
    return if (this.unit == other.unit) {
        Quantity(this.value - other.value, this.unit)
    } else if (this.unit.quantity == other.unit.quantity) {
        if (this.unit.scale.value leq other.unit.scale.value) {
            Quantity(this.value - other.to(this.unit)!!.value, this.unit)
        } else {
            Quantity(this.to(other.unit)!!.value - other.value, other.unit)
        }
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

operator fun <V> Quantity<V>.unaryMinus(): Quantity<V> where V : Arithmetic<V>, V : Neg<V> {
    return Quantity(-this.value, this.unit)
}
