package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.concept.Flt64Bridge
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Value type conversion trait (aligns with Rust IntoValue<V>).
 * Converts source numeric types into a unified generic value type V.
 *
 * Rust: `pub trait IntoValue<V>: Clone + Debug + PartialOrd + Send + Sync + 'static`
 * Kotlin: V is bounded by RealNumber<V> which provides all arithmetic + ordering.
 *
 * The primary use case: converting Flt64 (solver standard) → V (generic value type).
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

        /**
         * 从 Flt64Bridge<V> 适配为 IntoValue<V>
         * Adapt Flt64Bridge<V> to IntoValue<V>
         *
         * 允许直接使用数值类型的 companion 对象（如 Flt64、FltX、Rtn64、RtnX）
         * 作为 IntoValue<V> 的提供者，消除重复的 flt64Converter 样板代码。
         *
         * Allows using numeric type companion objects (e.g., Flt64, FltX, Rtn64, RtnX)
         * directly as IntoValue<V> providers, eliminating repetitive flt64Converter boilerplate.
         *
         * @param bridge Flt64Bridge 提供者
         * @param bridge The Flt64Bridge provider
         * @return IntoValue 适配器
         * @return The IntoValue adapter
         */
        fun <V : RealNumber<V>> fromBridge(bridge: Flt64Bridge<V>): IntoValue<V> = Flt64BridgeAdapter(bridge)
    }
}

/**
 * Flt64Bridge 到 IntoValue 的适配器
 * Adapter from Flt64Bridge to IntoValue
 *
 * 将 math 层的 Flt64Bridge<V> 接口适配为 core 层的 IntoValue<V> 接口。
 * Adapts the math layer's Flt64Bridge<V> interface to the core layer's IntoValue<V> interface.
 */
private class Flt64BridgeAdapter<V : RealNumber<V>>(
    private val bridge: Flt64Bridge<V>
) : IntoValue<V> {
    override fun intoValue(value: Flt64): V = bridge.intoValue(value)
    override val zero: V get() = bridge.zero
    override val one: V get() = bridge.one
    override fun fromValue(value: V): Flt64 = bridge.fromValue(value)
}

/**
 * 将 Flt64Bridge<V> 转换为 IntoValue<V>
 * Convert Flt64Bridge<V> to IntoValue<V>
 *
 * 扩展函数，允许直接写 `Flt64.intoValue()` 或 `FltX.intoValue()` 等。
 * Extension function allowing direct usage like `Flt64.intoValue()` or `FltX.intoValue()`.
 */
fun <V : RealNumber<V>> Flt64Bridge<V>.toIntoValue(): IntoValue<V> = IntoValue.fromBridge(this)