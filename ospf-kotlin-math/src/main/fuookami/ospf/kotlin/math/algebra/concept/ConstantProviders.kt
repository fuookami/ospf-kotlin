/**
 * 常量提供而
 * Constant Providers
 *
 * 定义数值常量提供者接口(HasZero, HasOne, HasTwo, HasThree, HasFive, HasTen, HasHalf, HasBounds, HasFixedPrecision, HasInfinity, HasNaN, HasTranscendentals) 及组合接口(ArithmeticConst, RealConst, FloatingConst)，并提供伴伴对象反射解析机制。
 * Defines numeric constant provider interfaces (HasZero, HasOne, HasTwo, HasThree, HasFive, HasTen, HasHalf, HasBounds, HasFixedPrecision, HasInfinity, HasNaN, HasTranscendentals) and composite interfaces (ArithmeticConst, RealConst, FloatingConst), with companion object reflection resolution mechanism.
*/
package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.reflect.full.companionObjectInstance
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 零常量提供而
 * Zero constant provider
*/
interface HasZero<T> {

    /** 零倌/ Zero value */
    val zero: T
}

/**
 * 一常量提供而
 * One constant provider
*/
interface HasOne<T> {

    /** 一倌/ One value */
    val one: T
}

/**
 * 二常量提供而
 * Two constant provider
*/
interface HasTwo<T> {

    /** 二倌/ Two value */
    val two: T
}

/**
 * 三常量提供而
 * Three constant provider
*/
interface HasThree<T> {

    /** 三倌/ Three value */
    val three: T
}

/**
 * 五常量提供而
 * Five constant provider
*/
interface HasFive<T> {

    /** 五倌/ Five value */
    val five: T
}

/**
 * 十常量提供而
 * Ten constant provider
*/
interface HasTen<T> {

    /** 十倌/ Ten value */
    val ten: T
}

/**
 * 半常量提供而
 * Half constant provider
*/
interface HasHalf<T> {

    /** 半倌(0.5) / Half value (0.5) */
    val half: T
}

/**
 * 边界常量提供而
 * Bounds constant provider
*/
interface HasBounds<T> {

    /** 最小倌/ Minimum value */
    val minimum: T

    /** 最大倌/ Maximum value */
    val maximum: T
}

/**
 * 定点精度常量提供而
 * Fixed precision constant provider
*/
interface HasFixedPrecision<T> {

    /** 小数位数 / Decimal digits */
    val decimalDigits: Int? get() = null

    /** 小数精度 / Decimal precision */
    val decimalPrecision: T? get() = null

    /** 精度误差 / Precision epsilon */
    val epsilon: T? get() = null
}

/**
 * 无穷常量提供而
 * Infinity constant provider
*/
interface HasInfinity<T> {

    /** 正无穌/ Positive infinity */
    val infinity: T? get() = null

    /** 负无穌/ Negative infinity */
    val negativeInfinity: T? get() = null
}

/**
 * NaN 常量提供而
 * NaN constant provider
*/
interface HasNaN<T> {

    /** 非数倌/ Not a Number */
    val nan: T? get() = null
}

/**
 * 超越数常量提供而
 * Transcendental numbers constant provider
*/
interface HasTranscendentals<T> {

    /** 圆周玌/ Pi */
    val pi: T

    /** 自然常数 / Euler's number e */
    val e: T

    /** 仌2 为底的对数常里/ Log base 2 constant */
    val lg2: T
}

/**
 * 算术常量组合接口
 * Arithmetic constants composite interface
*/
interface ArithmeticConst<T> : HasZero<T>, HasOne<T>

/**
 * 实数常量组合接口
 * Real number constants composite interface
*/
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

/**
 * 浮点数常量组合接口
 * Floating number constants composite interface
*/
interface FloatingConst<T> : RealConst<T>, HasHalf<T>, HasTranscendentals<T>

/**
 * 伴生对象常量提供者反射解析器
 * Companion object constant provider reflection resolver
*/
data object CompanionConstantProviderResolver {

    /** 反射回退启用属性名 / Reflection fallback enabled property name */
    const val reflectionFallbackEnabledProperty = "ospf.kotlin.math.enableCompanionReflectionFallback"

    /** 是否启用反射回退 / Whether reflection fallback is enabled */
    val reflectionFallbackEnabled: Boolean
        get() = parseBoolean(System.getProperty(reflectionFallbackEnabledProperty))

    /**
     * 解析字符串为布尔值
     * Parse a string to boolean value
     *
     * @param value 待解析的字符串
     * @return 解析后的布尔值
     * @return The parsed boolean value
    */
    private fun parseBoolean(value: String?): Boolean {
        return when (value?.trim()?.lowercase()) {
            "1", "true", "yes", "on" -> true
            else -> false
        }
    }
}

@PublishedApi
internal inline fun <T, R> Ret<T>.mapResolved(crossinline extractor: (T) -> R): Ret<R> {
    return when (this) {
        is Ok -> Ok(extractor(value))
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

@PublishedApi
internal inline fun <T, R> Ret<T>.flatMapResolved(crossinline extractor: (T) -> Ret<R>): Ret<R> {
    return when (this) {
        is Ok -> extractor(value)
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

/**
 * 通过伴生对象反射解析常量提供而
 * Resolve constant provider through companion object reflection
 *
 * @param T 目标类型
 * @param C 常量提供者类垌
 * @param caller 调用者名秌
 * @param expectedTypeName 期望的类型名秌
 * @return 解析到的常量提供而
 * @return The resolved constant provider
*/
@PublishedApi
internal inline fun <reified T, reified C : Any> resolveCompanionProvider(
    caller: String,
    expectedTypeName: String
): Ret<C> {
    return resolveCompanionProviderSafe<T, C>(
        caller = caller,
        expectedTypeName = expectedTypeName
    )
}

/**
 * 安全解析伴生对象常量提供者
 * Safely resolve constant provider through companion object reflection
 *
 * @param T 目标类型
 * @param C 常量提供者类型
 * @param caller 调用者名称
 * @param expectedTypeName 期望的类型名称
 * @return 常量提供者解析结果
 * @return The constant provider resolution result
*/
@PublishedApi
internal inline fun <reified T, reified C : Any> resolveCompanionProviderSafe(
    caller: String,
    expectedTypeName: String
): Ret<C> {
    val typeName = T::class.qualifiedName ?: T::class.simpleName ?: "unknown type"
    if (!CompanionConstantProviderResolver.reflectionFallbackEnabled) {
        return Failed(
            ErrorCode.IllegalArgument,
            "Companion reflection fallback is disabled for $typeName in $caller. " +
                    "Pass explicit provider/constants or enable fallback by " +
                    "-D${CompanionConstantProviderResolver.reflectionFallbackEnabledProperty}=true."
        )
    }
    val companion = T::class.companionObjectInstance
        ?: return Failed(
            ErrorCode.IllegalArgument,
            "Type $typeName has no companion object in $caller, expected $expectedTypeName provider."
        )
    return (companion as? C)?.let { Ok(it) }
        ?: Failed(
            ErrorCode.IllegalArgument,
            "Companion object of $typeName does not implement $expectedTypeName in $caller."
        )
}

/**
 * 尝试解析伴生对象常量提供者，失败返回 null
 * Try resolving constant provider through companion object reflection, returning null on failure
 *
 * @param T 目标类型
 * @param C 常量提供者类型
 * @param caller 调用者名称
 * @param expectedTypeName 期望的类型名称
 * @return 常量提供者，失败时返回 null
 * @return The constant provider, or null on failure
*/
@PublishedApi
internal inline fun <reified T, reified C : Any> resolveCompanionProviderOrNull(
    caller: String,
    expectedTypeName: String
): C? {
    return resolveCompanionProviderSafe<T, C>(
        caller = caller,
        expectedTypeName = expectedTypeName
    ).value
}

/**
 * 解析算术常量
 * Resolve arithmetic constants
 *
 * @param T 算术类型
 * @param caller 调用者名秌
 * @return 算术常量
 * @return The arithmetic constants
*/
inline fun <reified T> resolveArithmeticConstants(caller: String): Ret<ArithmeticConstants<T>> where T : Arithmetic<T> {
    return resolveArithmeticConstantsSafe(caller)
}

/**
 * 安全解析算术常量
 * Safely resolve arithmetic constants
 *
 * @param T 算术类型
 * @param caller 调用者名称
 * @return 算术常量解析结果
 * @return The arithmetic constants resolution result
*/
inline fun <reified T> resolveArithmeticConstantsSafe(caller: String): Ret<ArithmeticConstants<T>> where T : Arithmetic<T> {
    return resolveCompanionProviderSafe<T, ArithmeticConstants<T>>(
        caller = caller,
        expectedTypeName = "ArithmeticConstants<${T::class.simpleName}>"
    )
}

/**
 * 尝试解析算术常量，失败返回 null
 * Try resolving arithmetic constants, returning null on failure
 *
 * @param T 算术类型
 * @param caller 调用者名称
 * @return 算术常量，失败时返回 null
 * @return The arithmetic constants, or null on failure
*/
inline fun <reified T> resolveArithmeticConstantsOrNull(caller: String): ArithmeticConstants<T>? where T : Arithmetic<T> {
    return resolveCompanionProviderOrNull<T, ArithmeticConstants<T>>(
        caller = caller,
        expectedTypeName = "ArithmeticConstants<${T::class.simpleName}>"
    )
}

/**
 * 解析实数常量
 * Resolve real number constants
 *
 * @param T 实数类型
 * @param caller 调用者名秌
 * @return 实数常量
 * @return The real number constants
*/
inline fun <reified T> resolveRealNumberConstants(caller: String): Ret<RealNumberConstants<T>> where T : RealNumber<T> {
    return resolveRealNumberConstantsSafe(caller)
}

/**
 * 安全解析实数常量
 * Safely resolve real number constants
 *
 * @param T 实数类型
 * @param caller 调用者名称
 * @return 实数常量解析结果
 * @return The real number constants resolution result
*/
inline fun <reified T> resolveRealNumberConstantsSafe(caller: String): Ret<RealNumberConstants<T>> where T : RealNumber<T> {
    return resolveCompanionProviderSafe<T, RealNumberConstants<T>>(
        caller = caller,
        expectedTypeName = "RealNumberConstants<${T::class.simpleName}>"
    )
}

/**
 * 尝试解析实数常量，失败返回 null
 * Try resolving real number constants, returning null on failure
 *
 * @param T 实数类型
 * @param caller 调用者名称
 * @return 实数常量，失败时返回 null
 * @return The real number constants, or null on failure
*/
inline fun <reified T> resolveRealNumberConstantsOrNull(caller: String): RealNumberConstants<T>? where T : RealNumber<T> {
    return resolveCompanionProviderOrNull<T, RealNumberConstants<T>>(
        caller = caller,
        expectedTypeName = "RealNumberConstants<${T::class.simpleName}>"
    )
}

/**
 * 解析浮点数常里
 * Resolve floating number constants
 *
 * @param T 浮点数类垌
 * @param caller 调用者名秌
 * @return 浮点数常里
 * @return The floating number constants
*/
inline fun <reified T> resolveFloatingNumberConstants(caller: String): Ret<FloatingNumberConstants<T>> where T : FloatingNumber<T> {
    return resolveFloatingNumberConstantsSafe(caller)
}

/**
 * 安全解析浮点数常量
 * Safely resolve floating number constants
 *
 * @param T 浮点数类型
 * @param caller 调用者名称
 * @return 浮点数常量解析结果
 * @return The floating number constants resolution result
*/
inline fun <reified T> resolveFloatingNumberConstantsSafe(caller: String): Ret<FloatingNumberConstants<T>> where T : FloatingNumber<T> {
    return resolveCompanionProviderSafe<T, FloatingNumberConstants<T>>(
        caller = caller,
        expectedTypeName = "FloatingNumberConstants<${T::class.simpleName}>"
    )
}

/**
 * 尝试解析浮点数常量，失败返回 null
 * Try resolving floating number constants, returning null on failure
 *
 * @param T 浮点数类型
 * @param caller 调用者名称
 * @return 浮点数常量，失败时返回 null
 * @return The floating number constants, or null on failure
*/
inline fun <reified T> resolveFloatingNumberConstantsOrNull(caller: String): FloatingNumberConstants<T>? where T : FloatingNumber<T> {
    return resolveCompanionProviderOrNull<T, FloatingNumberConstants<T>>(
        caller = caller,
        expectedTypeName = "FloatingNumberConstants<${T::class.simpleName}>"
    )
}
