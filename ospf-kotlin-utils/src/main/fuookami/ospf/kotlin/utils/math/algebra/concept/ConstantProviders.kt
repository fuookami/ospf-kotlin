package fuookami.ospf.kotlin.utils.math.algebra.concept

import kotlin.reflect.full.companionObjectInstance

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

data object CompanionConstantProviderResolver {
    const val reflectionFallbackEnabledProperty = "ospf.kotlin.math.enableCompanionReflectionFallback"

    val reflectionFallbackEnabled: Boolean
        get() = parseBoolean(System.getProperty(reflectionFallbackEnabledProperty))

    private fun parseBoolean(value: String?): Boolean {
        return when (value?.trim()?.lowercase()) {
            "1", "true", "yes", "on" -> true
            else -> false
        }
    }
}

@Suppress("UNCHECKED_CAST")
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

inline fun <reified T> resolveArithmeticConstants(caller: String): ArithmeticConstants<T> where T : Arithmetic<T> {
    return resolveCompanionProvider<T, ArithmeticConstants<T>>(
        caller,
        "ArithmeticConstants<${T::class.simpleName}>"
    )
}

inline fun <reified T> resolveRealNumberConstants(caller: String): RealNumberConstants<T> where T : RealNumber<T> {
    return resolveCompanionProvider<T, RealNumberConstants<T>>(
        caller,
        "RealNumberConstants<${T::class.simpleName}>"
    )
}

inline fun <reified T> resolveFloatingNumberConstants(caller: String): FloatingNumberConstants<T> where T : FloatingNumber<T> {
    return resolveCompanionProvider<T, FloatingNumberConstants<T>>(
        caller,
        "FloatingNumberConstants<${T::class.simpleName}>"
    )
}
