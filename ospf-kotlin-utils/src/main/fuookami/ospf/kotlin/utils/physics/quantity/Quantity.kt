package com.sfa.opf.kotlin.utils.physics.quantity

/*
class Quantity<T>(val value: T, val unit: PhysicsUnit): Arithmetic<T> where T: Arithmetic<T> {
}

operator fun <T, U> Quantity<T>.plus(rhs: Quantity<T>): Quantity<U> where T: Arithmetic<T>, T: Invariant<T>, U:  Arithmetic<U>, T: Plus<T, U> {
    return Quantity(value + rhs.value, this.unit + rhs.unit)
}

operator fun <T> Quantity<T>.minus(rhs: Quantity<T>): Quantity<T> where T: Arithmetic<T>, T: Invariant<T>, T: Minus<T, T> {
    return Quantity(value - rhs.value, this.unit - rhs.unit)
}

operator fun <T> Quantity<T>.times(rhs: Quantity<T>): Quantity<T> where T: Arithmetic<T>, T: Invariant<T>, T: Times<T, T> {
    return Quantity(value * rhs.value, this.unit * rhs.unit)
}

operator fun <T> Quantity<T>.div(rhs: Quantity<T>): Quantity<T> where T: Arithmetic<T>, T: Invariant<T>, T: Div<T, T> {
    return Quantity(value / rhs.value, this.unit / rhs.unit)
}
*/
