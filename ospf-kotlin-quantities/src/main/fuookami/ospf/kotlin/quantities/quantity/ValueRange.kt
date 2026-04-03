package fuookami.ospf.kotlin.quantities.quantity

import fuookami.ospf.kotlin.math.algebra.concept.NumberField
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.value_range.Bound
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.math.algebra.value_range.ValueWrapper

val <V> Quantity<ValueRange<V>>.lowerBound where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.lowerBound, unit)

val <V> Quantity<ValueRange<V>>.upperBound where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.upperBound, unit)

val <V> Quantity<ValueRange<V>>.diff where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.diff, unit)

val <V> Quantity<Bound<V>>.boundValue where V : RealNumber<V>, V : NumberField<V>
    get() = Quantity(value.value, unit)

fun <V> Quantity<ValueWrapper<V>>.unwrap(): Quantity<V> where V : RealNumber<V>, V : NumberField<V> {
    return Quantity(value.unwrap(), unit)
}

fun <V> Quantity<ValueWrapper<V>>.unwrapOrNull(): Quantity<V>? where V : RealNumber<V>, V : NumberField<V> {
    return value.unwrapOrNull()?.let {
        Quantity(it, unit)
    }
}


