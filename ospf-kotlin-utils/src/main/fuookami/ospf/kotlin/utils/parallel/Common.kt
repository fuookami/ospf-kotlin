package fuookami.ospf.kotlin.utils.parallel

import fuookami.ospf.kotlin.utils.error.Error
import fuookami.ospf.kotlin.utils.functional.ExRet
import fuookami.ospf.kotlin.utils.functional.Failed
import fuookami.ospf.kotlin.utils.functional.Fatal
import fuookami.ospf.kotlin.utils.functional.Ok
import fuookami.ospf.kotlin.utils.functional.Ret
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

/**
 * 并行操作工具模块
 *
 * Utility module for parallel operations.
 */

/**
 * 默认并发量
 *
 * 根据集合大小和可用处理器计算合理的并发量。
 * 对于空集合返回 1 以避免 log(0) = -inf。
 *
 * Default concurrent amount based on collection size and available processors.
 * Returns 1 for empty collections to avoid log(0) = -inf.
 *
 * @receiver 集合 / Collection
 * @return 默认并发量 / Default concurrent amount
 */
val Collection<*>.defaultConcurrentAmount: ULong
    get() = if (this.isEmpty()) {
        1uL
    } else {
        maxOf(
            minOf(
                log2(this.size.toDouble()).toInt(),
                Runtime.getRuntime().availableProcessors()
            ),
            1
        ).toULong()
    }

/**
 * 从 Ret 结果中提取错误并添加到列表
 *
 * Extract errors from a Ret result and append to the list.
 *
 * @receiver 错误列表 / Error list
 * @param ret 结果对象 / Result object
 */
@PublishedApi
internal fun MutableList<Error>.appendFrom(ret: Ret<*>) {
    when (ret) {
        is Ok -> {}
        is Failed -> add(ret.error)
        is Fatal -> addAll(ret.errors)
    }
}

/**
 * 根据错误列表创建结果
 *
 * Create result based on error list.
 *
 * @param value 成功时的值 / Value for success case
 * @param errors 错误列表 / Error list
 * @return 如果无错误返回 Ok，否则返回 Fatal / Ok if no errors, Fatal otherwise
 */
@PublishedApi
internal fun <T> exResultOf(value: T, errors: List<Error>): ExRet<T> {
    return if (errors.isEmpty()) {
        Ok(value)
    } else {
        Fatal(errors)
    }
}
