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
 * Also provides V-type constants (zero, one) and reverse conversion (fromValue)
 * to eliminate unsafe `Flt64.zero as V` / `Flt64.one as V` / `this as Flt64` casts.
 */
interface IntoValue<V : RealNumber<V>> {
    /**
     * 将 Flt64 转换为泛型值类型 V。
     * Convert Flt64 to generic value type V.
     *
     * @param value Flt64 值 / Flt64 value
     * @return 转换后的 V 值 / Converted V value
     */
    fun intoValue(value: Flt64): V

    /** 零值 / Zero value */
    val zero: V
    /** 单值 / One value */
    val one: V

    /** 负无穷 / Negative infinity */
    val negativeInfinity: V get() = intoValue(Flt64.negativeInfinity)
    /** 正无穷 / Positive infinity */
    val infinity: V get() = intoValue(Flt64.infinity)

    /**
     * 将泛型值类型 V 转换回 Flt64。
     * Convert generic value type V back to Flt64.
     *
     * @param value V 值 / V value
     * @return 转换后的 Flt64 值 / Converted Flt64 value
     */
    fun fromValue(value: V): Flt64

    companion object {
        /** Flt64 恒等转换器 / Flt64 identity converter */
        @JvmField
        val Identity: IntoValue<Flt64> = object : IntoValue<Flt64> {
            override fun intoValue(value: Flt64): Flt64 = value
            override val zero: Flt64 get() = Flt64.zero
            override val one: Flt64 get() = Flt64.one
            override fun fromValue(value: Flt64): Flt64 = value
        }

        /**
         * 从 Flt64ValueConverter 适配为 IntoValue。
         * Adapt Flt64ValueConverter to IntoValue.
         *
         * @param V 值类型 / Value type
         * @param converter Flt64ValueConverter 提供者 / Flt64ValueConverter provider
         * @return IntoValue 适配器 / IntoValue adapter
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
 *
 * @property converter 被适配的 Flt64ValueConverter / The adapted Flt64ValueConverter
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
 * 将 Flt64ValueConverter 转换为 IntoValue。
 * Convert Flt64ValueConverter to IntoValue.
 *
 * @param V 值类型 / Value type
 * @return IntoValue 适配器 / IntoValue adapter
 */
fun <V : RealNumber<V>> Flt64ValueConverter<V>.toIntoValue(): IntoValue<V> = IntoValue.fromConverter(this)