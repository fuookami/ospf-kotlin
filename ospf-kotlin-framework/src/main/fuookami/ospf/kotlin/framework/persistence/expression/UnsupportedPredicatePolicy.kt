/**
 * 不支持谓词策略 / Unsupported Predicate Policy
 *
 * 定义 translator 遇到无法下推的 predicate 时的处理策略。
 * Defines how translators handle predicates that cannot be pushed down.
*/
package fuookami.ospf.kotlin.framework.persistence.expression

import fuookami.ospf.kotlin.math.symbol.expression.BooleanExpression
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * 不支持谓词策略 / Unsupported predicate policy
*/
enum class UnsupportedPredicatePolicy {
    /**
     * 立即失败 / Fail immediately
    */
    FailFast,

    /**
     * 转为恒假条件 / Translate to an always-false condition
    */
    AlwaysFalse,

    /**
     * 客户端过滤 / Client-side filtering
    */
    ClientFilter
}

/**
 * 谓词翻译结果 / Predicate translation result
 *
 * @param T 翻译目标类型 / Translation target type
*/
sealed class PredicateTranslation<out T> {

    /**
     * 翻译成功 / Translation succeeded
     *
     * @property value 翻译后的值 / Translated value
     * @param T 翻译结果类型 / Translation result type
    */
    data class Translated<T>(val value: T) : PredicateTranslation<T>()

    /**
     * 不支持的谓词 / Unsupported predicate
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
 * 不支持谓词详情 / Unsupported predicate detail
 *
 * 保留不支持谓词的结构化信息，便于调用方按错误类型分支处理。 / Preserves structured unsupported predicate information for callers to branch by error type.
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
         * 创建 AlwaysFalse 策略的详情 / Create detail for the AlwaysFalse policy
         *
         * @param expressionType 表达式类型 / Expression type
         * @param reason 不支持的原因 / Reason for being unsupported
         * @param backendName 后端名称 / Backend name
         * @return AlwaysFalse 策略详情 / AlwaysFalse policy detail
        */
        fun alwaysFalse(
            expressionType: String,
            reason: String,
            backendName: String
        ): UnsupportedPredicateDetail {
            return UnsupportedPredicateDetail(
                expressionType = expressionType,
                reason = reason,
                policy = UnsupportedPredicatePolicy.AlwaysFalse,
                backendName = backendName
            )
        }

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
     * 转换为错误 / Convert to error
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

/**
 * 保留失败结果并转换其成功值类型 / Preserve a failure while changing the success value type
 *
 * @param T 原成功值类型 / Original success value type
 * @param U 目标成功值类型 / Target success value type
 * @return 失败或致命结果；成功时返回 null / Failed or fatal result, or null on success
 */
fun <U> Ret<*>.propagateFailure(): Ret<U>? {
    return when (this) {
        is Ok -> null
        is Failed -> Failed(error)
        is Fatal -> Fatal(errors)
    }
}

/**
 * 仅在根翻译结果为 AlwaysFalse unsupported 时生成恒假结果 / Resolve an AlwaysFalse unsupported result at the translation root
 *
 * @param createFalse 恒假结果构造函数 / Always-false result factory
 * @return 根级策略处理后的结果 / Result after applying the root policy
 */
fun <T> Ret<T>.withAlwaysFalseUnsupported(createFalse: () -> T): Ret<T> {
    return when (this) {
        is Ok -> this
        is Failed -> {
            val detail = error.value as? UnsupportedPredicateDetail
            if (detail?.policy == UnsupportedPredicatePolicy.AlwaysFalse) {
                Ok(createFalse())
            } else {
                this
            }
        }
        is Fatal -> this
    }
}

/**
 * 将外部持久化或翻译异常转换为结构化失败 / Convert an external persistence or translation exception to a structured failure
 *
 * @param operation 失败操作 / Failed operation
 * @param error 捕获的异常 / Caught exception
 * @return 持久化失败结果 / Persistence failure result
 */
fun <T> persistenceFailure(operation: String, error: Exception): Ret<T> {
    val reason = error.message ?: error::class.qualifiedName ?: "unknown error"
    return Failed(
        ErrorCode.ApplicationFailed,
        "持久化操作失败：$operation：$reason / Persistence operation failed: $operation: $reason"
    )
}
