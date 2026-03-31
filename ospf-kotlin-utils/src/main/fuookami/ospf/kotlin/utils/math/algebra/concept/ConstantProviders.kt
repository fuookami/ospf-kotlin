package fuookami.ospf.kotlin.utils.math.algebra.concept

interface HasZero<T> {
    val zero: T
}

interface HasOne<T> {
    val one: T
}

interface HasTwo<T> {
    val two: T
}

interface HasThree<T> {
    val three: T
}

interface HasFive<T> {
    val five: T
}

interface HasTen<T> {
    val ten: T
}

interface HasHalf<T> {
    val half: T
}

interface HasBounds<T> {
    val minimum: T
    val maximum: T
}

interface HasFixedPrecision<T> {
    val decimalDigits: Int? get() = null
    val decimalPrecision: T? get() = null
    val epsilon: T? get() = null
}

interface HasInfinity<T> {
    val infinity: T? get() = null
    val negativeInfinity: T? get() = null
}

interface HasNaN<T> {
    val nan: T? get() = null
}

interface HasTranscendentals<T> {
    val pi: T
    val e: T
    val lg2: T
}

interface ArithmeticConst<T> : HasZero<T>, HasOne<T>

interface RealConst<T> :
    ArithmeticConst<T>,
    HasTwo<T>,
    HasThree<T>,
    HasFive<T>,
    HasTen<T>,
    HasBounds<T>,
    HasFixedPrecision<T>,
    HasInfinity<T>,
    HasNaN<T>

interface FloatingConst<T> : RealConst<T>, HasHalf<T>, HasTranscendentals<T>
