/**
 * Flt64 值转换接口
 * Flt64 Value Converter Interface
 *
 * 定义数值类型与 Flt64 之间的双向转换能力，使 companion 对象可直接充当 converter，
 * 消除 core/framework 中大量重复的 flt64Converter 样板代码。
 *
 * Defines bidirectional conversion capability between numeric types and Flt64,
 * enabling companion objects to serve as converters directly,
 * eliminating repetitive flt64Converter boilerplate in core/framework.
 *
 * 四种核心数值类型（Flt64、FltX、Rtn64、RtnX）的 companion 对象均实现此接口，
 * 因此可直接作为 IntoValue<V> 的等价提供者使用。
 *
 * The companion objects of the four core numeric types (Flt64, FltX, Rtn64, RtnX)
 * all implement this interface, and can thus be used directly as IntoValue<V>-equivalent providers.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/**
 * Flt64 值转换接口
 * Flt64 value converter interface
 *
 * 提供从 Flt64 到 V 的正向转换（intoValue）和从 V 到 Flt64 的反向转换（fromValue），
 * 以及 V 类型的零和一常量。
 * 与 core 层的 IntoValue<V> 接口语义完全对齐，但定义在 math 层以允许 companion 对象直接实现。
 *
 * Provides forward conversion from Flt64 to V (intoValue) and reverse conversion from V to Flt64 (fromValue),
 * along with zero and one constants in the target value type V.
 * Semantically aligned with core's IntoValue<V> interface, but defined in the math layer
 * to allow companion objects to implement it directly.
 *
 * infinity 和 negativeInfinity 不在此接口中声明（避免与 HasInfinity<V?> 的 nullable 签名冲突），
 * 由 core 层的 IntoValue<V> 通过 intoValue(Flt64.infinity) 默认实现提供。
 *
 * infinity and negativeInfinity are not declared in this interface
 * (to avoid conflict with HasInfinity<V?>'s nullable signature),
 * and are provided by core's IntoValue<V> via intoValue(Flt64.infinity) default implementation.
 *
 * @param V 目标数值类型，必须是实数且满足数域约束
 * @param V The target numeric type, must be a real number satisfying number field constraints
 */
interface Flt64ValueConverter<V : RealNumber<V>> : HasZero<V>, HasOne<V> {
    /**
     * 将 Flt64 值转换为 V 类型
     * Convert a Flt64 value to type V
     *
     * @param value Flt64 源值
     *              The Flt64 source value
     * @return V 类型目标值
     *         The target value of type V
     */
    fun intoValue(value: Flt64): V

    /**
     * 将 V 类型值转换为 Flt64
     * Convert a value of type V to Flt64
     *
     * 默认实现使用 RealNumber 的 toFlt64() 方法。
     * Default implementation uses RealNumber's toFlt64() method.
     *
     * @param value V 类型源值
     *              The source value of type V
     * @return Flt64 目标值
     *         The Flt64 target value
     */
    fun fromValue(value: V): Flt64 = value.toFlt64()
}

/**
 * 通过伴生对象反射解析 Flt64ValueConverter
 * Resolve Flt64ValueConverter through companion object reflection
 *
 * @param V 目标数值类型
 * @param V The target numeric type
 * @param caller 调用者名称
 * @param caller The caller name
 * @return 解析到的 Flt64ValueConverter 提供者
 * @return The resolved Flt64ValueConverter provider
 */
inline fun <reified V> resolveFlt64ValueConverter(caller: String): Ret<Flt64ValueConverter<V>> where V : RealNumber<V> {
    return resolveFlt64ValueConverterSafe(caller)
}

/**
 * 安全解析 Flt64ValueConverter
 * Safely resolve Flt64ValueConverter
 *
 * @param V 目标数值类型
 * @param V The target numeric type
 * @param caller 调用者名称
 * @param caller The caller name
 * @return Flt64ValueConverter 解析结果
 * @return The Flt64ValueConverter resolution result
 */
inline fun <reified V> resolveFlt64ValueConverterSafe(caller: String): Ret<Flt64ValueConverter<V>> where V : RealNumber<V> {
    return resolveCompanionProviderSafe<V, Flt64ValueConverter<V>>(
        caller = caller,
        expectedTypeName = "Flt64ValueConverter<${V::class.simpleName}>"
    )
}

/**
 * 尝试解析 Flt64ValueConverter，失败返回 null
 * Try resolving Flt64ValueConverter, returning null on failure
 *
 * @param V 目标数值类型
 * @param V The target numeric type
 * @param caller 调用者名称
 * @param caller The caller name
 * @return Flt64ValueConverter，失败时返回 null
 * @return The Flt64ValueConverter, or null on failure
 */
inline fun <reified V> resolveFlt64ValueConverterOrNull(caller: String): Flt64ValueConverter<V>? where V : RealNumber<V> {
    return resolveCompanionProviderOrNull<V, Flt64ValueConverter<V>>(
        caller = caller,
        expectedTypeName = "Flt64ValueConverter<${V::class.simpleName}>"
    )
}
