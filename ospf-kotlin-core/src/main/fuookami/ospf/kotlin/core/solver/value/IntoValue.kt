/**
 * 值类型转换接口
 * Value type conversion interface
 */
package fuookami.ospf.kotlin.core.solver.value

import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*

/**
 * 值类型转换接口（与 Rust IntoValue<V> 对齐）。
 * 将源数值类型转换为统一的泛型值类型 V。
 * Value type conversion trait (aligns with Rust IntoValue<V>).
 * Converts source numeric types into a unified generic value type V.
 *
 * Rust: `pub trait IntoValue<V>: Clone + Debug + PartialOrd + Send + Sync + 'static`
 * Kotlin: V 受 RealNumber<V> 约束，提供所有算术和排序操作。
 * Kotlin: V is bounded by RealNumber<V> which provides all arithmetic + ordering.
 *
 * 主要用途：将 Flt64（求解器标准类型）转换为 V（泛型值类型），
 * 使函数符号能将字面量 f64 常量转换为 V。
 * The primary use case: converting Flt64 (solver standard) → V (generic value type).
 * This enables function symbols to convert literal f64 constants to V.
 *
 * 还提供 V 类型常量（零、一）和反向转换（fromValue），
 * 以消除不安全的 `Flt64.zero as V` / `Flt64.one as V` / `this as Flt64` 强制转换。
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
         * 从 Flt64ValueConverter<V> 适配为 IntoValue<V>
         * Adapt Flt64ValueConverter<V> to IntoValue<V>
         *
         * 允许直接使用数值类型的 companion 对象（如 Flt64、FltX、Rtn64、RtnX）
         * 作为 IntoValue<V> 的提供者，消除重复的 flt64Converter 样板代码。
         *
         * Allows using numeric type companion objects (e.g., Flt64, FltX, Rtn64, RtnX)
         * directly as IntoValue<V> providers, eliminating repetitive flt64Converter boilerplate.
         *
         * @param converter Flt64ValueConverter 提供者
         * @param converter The Flt64ValueConverter provider
         * @return IntoValue 适配器
         * @return The IntoValue adapter
         */
        fun <V : RealNumber<V>> fromConverter(converter: Flt64ValueConverter<V>): IntoValue<V> = Flt64ValueConverterAdapter(converter)
    }
}

/**
 * Flt64ValueConverter 到 IntoValue 的适配器
 * Adapter from Flt64ValueConverter to IntoValue
 *
 * 将 math 层的 Flt64ValueConverter<V> 接口适配为 core 层的 IntoValue<V> 接口。
 * Adapts the math layer's Flt64ValueConverter<V> interface to the core layer's IntoValue<V> interface.
 */
private class Flt64ValueConverterAdapter<V : RealNumber<V>>(
    private val converter: Flt64ValueConverter<V>
) : IntoValue<V> {
    override fun intoValue(value: Flt64): V = converter.intoValue(value)
    override val zero: V get() = converter.zero
    override val one: V get() = converter.one
    override fun fromValue(value: V): Flt64 = converter.fromValue(value)
}

/**
 * 将 Flt64ValueConverter<V> 转换为 IntoValue<V>
 * Convert Flt64ValueConverter<V> to IntoValue<V>
 *
 * 扩展函数，允许直接写 `Flt64.intoValue()` 或 `FltX.intoValue()` 等。
 * Extension function allowing direct usage like `Flt64.intoValue()` or `FltX.intoValue()`.
 */
fun <V : RealNumber<V>> Flt64ValueConverter<V>.toIntoValue(): IntoValue<V> = IntoValue.fromConverter(this)
