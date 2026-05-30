/**
 * 不支持谓词策略
 * Unsupported Predicate Policy
 *
 * 定义 translator 遇到无法下推的 predicate 时的处理策略。
 * Defines how translators handle predicates that cannot be pushed down.
 */
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression

/**
 * 不支持谓词策略
 * Unsupported predicate policy
 */
enum class UnsupportedPredicatePolicy {
    /**
     * 立即失败
     * Fail immediately
     */
    FailFast,

    /**
     * 转为恒假条件
     * Translate to an always-false condition
     */
    AlwaysFalse,

    /**
     * 客户端过滤
     * Client-side filtering
     */
    ClientFilter
}

/**
 * 谓词翻译结果
 * Predicate translation result
 */
/**
 * 谓词翻译结果
 * Predicate translation result
 *
 * @param T 翻译目标类型 / Translation target type
 */
sealed class PredicateTranslation<out T> {
    /**
     * 翻译成功
     * Translation succeeded
     */
    /**
     * 翻译成功
     * Translation succeeded
     *
     * @property value 翻译后的值 / Translated value
     * @param T 翻译结果类型 / Translation result type
     */
    data class Translated<T>(val value: T) : PredicateTranslation<T>()

    /**
     * 不支持的谓词
     * Unsupported predicate
     */
    /**
     * 不支持的谓词
     * Unsupported predicate
     *
     * @property reason 不支持的原因 / Reason for unsupported
     * @property expression 不可翻译的原始表达式，可能为 null / Original untranslatable expression, may be null
     */
    data class Unsupported(
        val reason: String,
        val expression: BooleanExpression? = null
    ) : PredicateTranslation<Nothing>()
}
