/**
 * 不支持谓词策略
 * Unsupported Predicate Policy
 *
 * 定义 translator 遇到无法下推的 predicate 时的处理策略。
 * Defines how translators handle predicates that cannot be pushed down.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.utils.error.*

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
 *
 * @param T 翻译目标类型 / Translation target type
*/
sealed class PredicateTranslation<out T> {

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
     *
     * @property reason 不支持的原因 / Reason for unsupported
     * @property expression 不可翻译的原始表达式，可能为 null / Original untranslatable expression, may be null
    */
    data class Unsupported(
        val reason: String,
        val expression: BooleanExpression? = null
    ) : PredicateTranslation<Nothing>()
}

/**
 * 不支持谓词详情
 * Unsupported predicate detail
 *
 * 保留不支持谓词的结构化信息，便于调用方按错误类型分支处理。
 * Preserves structured unsupported predicate information for callers to branch by error type.
 *
 * @property expressionType 表达式类型 / Expression type
 * @property reason 不支持的原因 / Reason for unsupported
 * @property policy 不支持谓词策略 / Unsupported predicate policy
 * @property backendName 后端名称（如 MyBatis、MongoDB、Ktorm）/ Backend name (e.g., MyBatis, MongoDB, Ktorm)
*/
data class UnsupportedPredicateDetail(
    val expressionType: String,
    val reason: String,
    val policy: UnsupportedPredicatePolicy,
    val backendName: String
) {

    /** 工厂方法 / Factory methods */
    companion object {
        /**
         * 创建 FailFast 策略的详情
         * Create detail for FailFast policy
         *
         * @param expressionType 表达式类型 / Expression type
         * @param reason 不支持的原因 / Reason for unsupported
         * @param backendName 后端名称 / Backend name
         * @return FailFast 策略详情 / FailFast policy detail
        */
        fun failFast(
            expressionType: String,
            reason: String,
            backendName: String
        ): UnsupportedPredicateDetail {
            return UnsupportedPredicateDetail(
                expressionType = expressionType,
                reason = reason,
                policy = UnsupportedPredicatePolicy.FailFast,
                backendName = backendName
            )
        }

        /**
         * 创建 ClientFilter 策略的详情
         * Create detail for ClientFilter policy
         *
         * @param expressionType 表达式类型 / Expression type
         * @param reason 不支持的原因 / Reason for unsupported
         * @param backendName 后端名称 / Backend name
         * @return ClientFilter 策略详情 / ClientFilter policy detail
        */
        fun clientFilter(
            expressionType: String,
            reason: String,
            backendName: String
        ): UnsupportedPredicateDetail {
            return UnsupportedPredicateDetail(
                expressionType = expressionType,
                reason = reason,
                policy = UnsupportedPredicatePolicy.ClientFilter,
                backendName = backendName
            )
        }
    }

    /**
     * 转换为错误
     * Convert to error
     *
     * @return 错误对象 / Error object
    */
    fun toError(): Error<ErrorCode> {
        return when (policy) {
            UnsupportedPredicatePolicy.FailFast -> ExErr(
                ErrorCode.IllegalArgument,
                "Unsupported predicate [$expressionType]: $reason (backend=$backendName, policy=FailFast)",
                this
            )
            UnsupportedPredicatePolicy.ClientFilter -> ExErr(
                ErrorCode.ApplicationFailed,
                "Unsupported predicate [$expressionType]: $reason (backend=$backendName, policy=ClientFilter)",
                this
            )
            UnsupportedPredicatePolicy.AlwaysFalse -> ExErr(
                ErrorCode.ApplicationError,
                "Unsupported predicate [$expressionType]: $reason (backend=$backendName, policy=AlwaysFalse)",
                this
            )
        }
    }
}
