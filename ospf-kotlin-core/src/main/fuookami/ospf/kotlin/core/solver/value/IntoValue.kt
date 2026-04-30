package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.concept.Field
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.utils.error.Err
import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Try
import fuookami.ospf.kotlin.utils.functional.ok

/**
 * Value type conversion trait (aligns with Rust IntoValue<V>).
 * Converts source numeric types into a unified generic value type V.
 *
 * Rust: `pub trait IntoValue<V>: Clone + Debug + PartialOrd + Send + Sync + 'static`
 * Kotlin: V is bounded by RealNumber<V> which provides all arithmetic + ordering.
 *
 * The primary use case: converting Flt64 (solver standard) → V (generic value type).
 * This enables function symbols to convert literal f64 constants to V.
 */
interface IntoValue<V : RealNumber<V>> {
    fun intoValue(value: Flt64): V

    companion object {
        val Flt64: IntoValue<Flt64> = object : IntoValue<Flt64> {
            override fun intoValue(value: Flt64): Flt64 = value
        }
    }
}

/**
 * Solve value trait (aligns with Rust SolveValue).
 * Converts generic value types to/from f64 for solver backends,
 * with configurable precision policy.
 *
 * Rust: `pub trait SolveValue: Clone + Debug + PartialOrd + Send + Sync + 'static`
 * In Kotlin, V is bounded by RealNumber<V>.
 */
interface SolveValue<V : RealNumber<V>> {
    fun typeName(): String

    fun fromFlt64WithPolicy(value: Double, policy: SolveValueConversionPolicy): Try
    fun toFlt64WithPolicy(value: V, policy: SolveValueConversionPolicy): Try
}

/**
 * Flt64 SolveValue implementation — identity-like conversion.
 */
object Flt64SolveValue : SolveValue<Flt64> {
    override fun typeName(): String = "Flt64"

    override fun fromFlt64WithPolicy(value: Double, policy: SolveValueConversionPolicy): Try {
        if (policy == SolveValueConversionPolicy.Strict) {
            if (value.isNaN()) return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected NaN"))
            if (value.isInfinite()) return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected infinity"))
        }
        return ok
    }

    override fun toFlt64WithPolicy(value: Flt64, policy: SolveValueConversionPolicy): Try {
        val raw = value.toDouble()
        if (policy == SolveValueConversionPolicy.Strict) {
            if (raw.isNaN()) return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected NaN"))
            if (raw.isInfinite()) return Failed(Err(ErrorCode.IllegalArgument, "Strict conversion rejected infinity"))
        }
        return ok
    }
}
