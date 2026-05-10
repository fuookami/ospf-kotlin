package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Value type conversion trait (aligns with Rust IntoValue<V>).
 * Converts source numeric types into a unified generic value type V.
 *
 * Rust: `pub trait IntoValue<V>: Clone + Debug + PartialOrd + Send + Sync + 'static`
 * Kotlin: V is bounded by RealNumber<V> which provides all arithmetic + ordering.
 *
 * The primary use case: converting Flt64 (solver standard) â†?V (generic value type).
 * This enables function symbols to convert literal f64 constants to V.
 *
 * Also provides V-typed constants (zero, one) and reverse conversion (fromValue)
 * to eliminate unsafe `Flt64.zero as V` / `Flt64.one as V` / `this as Flt64` casts.
 */
interface IntoValue<V : RealNumber<V>> {
    fun intoValue(value: Flt64): V

    val zero: V
    val one: V

    val negativeInfinity: V get() = intoValue(Flt64.negativeInfinity)
    val infinity: V get() = intoValue(Flt64.infinity)

    fun fromValue(value: V): Flt64

    companion object {
        @JvmField
        val Identity: IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> = object : IntoValue<fuookami.ospf.kotlin.math.algebra.number.Flt64> {
            override fun intoValue(value: fuookami.ospf.kotlin.math.algebra.number.Flt64): fuookami.ospf.kotlin.math.algebra.number.Flt64 = value
            override val zero: fuookami.ospf.kotlin.math.algebra.number.Flt64 get() = fuookami.ospf.kotlin.math.algebra.number.Flt64.zero
            override val one: fuookami.ospf.kotlin.math.algebra.number.Flt64 get() = fuookami.ospf.kotlin.math.algebra.number.Flt64.one
            override fun fromValue(value: fuookami.ospf.kotlin.math.algebra.number.Flt64): fuookami.ospf.kotlin.math.algebra.number.Flt64 = value
        }
    }
}
