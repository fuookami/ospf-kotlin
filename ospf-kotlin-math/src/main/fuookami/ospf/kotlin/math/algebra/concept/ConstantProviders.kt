/**
 * 常量提供而
 * Constant Providers
 *
 * 定义数值常量提供者接口(HasZero, HasOne, HasTwo, HasThree, HasFive, HasTen, HasHalf, HasBounds, HasFixedPrecision, HasInfinity, HasNaN, HasTranscendentals) 及组合接口(ArithmeticConst, RealConst, FloatingConst)，并提供伴伴对象反射解析机制。
 * Defines numeric constant provider interfaces (HasZero, HasOne, HasTwo, HasThree, HasFive, HasTen, HasHalf, HasBounds, HasFixedPrecision, HasInfinity, HasNaN, HasTranscendentals) and composite interfaces (ArithmeticConst, RealConst, FloatingConst), with companion object reflection resolution mechanism.
 */
package fuookami.ospf.kotlin.math.algebra.concept

import kotlin.reflect.full.companionObjectInstance

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

    private fun parseBoolean(value: String?): Boolean {
        return when (value?.trim()?.lowercase()) {
            "1", "true", "yes", "on" -> true
            else -> false
        }
    }
}

/**
 * 通过伴生对象反射解析常量提供而
 * Resolve constant provider through companion object reflection
 *
 * @param T 目标类型
 * @param T The target type
 * @param C 常量提供者类垌
 * @param C The constant provider type
 * @param caller 调用者名秌
 * @param caller The caller name
 * @param expectedTypeName 期望的类型名秌
 * @param expectedTypeName The expected type name
 * @return 解析到的常量提供而
 * @return The resolved constant provider
 */
@PublishedApi
internal inline fun <reified T, reified C : Any> resolveCompanionProvider(
    caller: String,
    expectedTypeName: String
): C {
    val typeName = T::class.qualifiedName ?: T::class.simpleName ?: "unknown type"
    if (!CompanionConstantProviderResolver.reflectionFallbackEnabled) {
        throw IllegalStateException(
            "Companion reflection fallback is disabled for $typeName in $caller. " +
                    "Pass explicit provider/constants or enable fallback by " +
                    "-D${CompanionConstantProviderResolver.reflectionFallbackEnabledProperty}=true."
        )
    }
    val companion = T::class.companionObjectInstance
        ?: throw IllegalStateException(
            "Type $typeName has no companion object in $caller, expected $expectedTypeName provider."
        )
    return companion as? C
        ?: throw IllegalStateException(
            "Companion object of $typeName does not implement $expectedTypeName in $caller."
        )
}

/**
 * 解析算术常量
 * Resolve arithmetic constants
 *
 * @param T 算术类型
 * @param T The arithmetic type
 * @param caller 调用者名秌
 * @param caller The caller name
 * @return 算术常量
 * @return The arithmetic constants
 */
inline fun <reified T> resolveArithmeticConstants(caller: String): ArithmeticConstants<T> where T : Arithmetic<T> {
    return resolveCompanionProvider<T, ArithmeticConstants<T>>(
        caller,
        "ArithmeticConstants<${T::class.simpleName}>"
    )
}

/**
 * 解析实数常量
 * Resolve real number constants
 *
 * @param T 实数类型
 * @param T The real number type
 * @param caller 调用者名秌
 * @param caller The caller name
 * @return 实数常量
 * @return The real number constants
 */
inline fun <reified T> resolveRealNumberConstants(caller: String): RealNumberConstants<T> where T : RealNumber<T> {
    return resolveCompanionProvider<T, RealNumberConstants<T>>(
        caller,
        "RealNumberConstants<${T::class.simpleName}>"
    )
}

/**
 * 解析浮点数常里
 * Resolve floating number constants
 *
 * @param T 浮点数类垌
 * @param T The floating number type
 * @param caller 调用者名秌
 * @param caller The caller name
 * @return 浮点数常里
 * @return The floating number constants
 */
inline fun <reified T> resolveFloatingNumberConstants(caller: String): FloatingNumberConstants<T> where T : FloatingNumber<T> {
    return resolveCompanionProvider<T, FloatingNumberConstants<T>>(
        caller,
        "FloatingNumberConstants<${T::class.simpleName}>"
    )
}