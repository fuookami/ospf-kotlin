package fuookami.ospf.kotlin.utils.math.algebra.concept

interface Invariant<T> {
    @Suppress("UNCHECKED_CAST")
    fun value(): T = this as T
}

interface Variant<T> {
    fun value(): T? = null
}

interface Bounded<Self> {
    val isBounded: Boolean
    val minBound: Self?
    val maxBound: Self?
}

interface Infinite<Self> {
    val supportsInfinity: Boolean
    val positiveInfinity: Self?
    val negativeInfinityValue: Self?
}

interface Fixed<Self> {
    val isFixed: Boolean
    val fixedDigits: Int?
    val fixedPrecision: Self?
}

interface Epsilon<Self> {
    val precisionEpsilon: Self?
}

