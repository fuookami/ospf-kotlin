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

// Default concurrent amount for parallel operations / 并行操作的默认并发量
// Handle empty collection to avoid log(0) = -inf / 处理空集合避免 log(0) = -inf
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

@PublishedApi
internal fun MutableList<Error>.appendFrom(ret: Ret<*>) {
    when (ret) {
        is Ok -> {}
        is Failed -> add(ret.error)
        is Fatal -> addAll(ret.errors)
    }
}

@PublishedApi
internal fun <T> exResultOf(value: T, errors: List<Error>): ExRet<T> {
    return if (errors.isEmpty()) {
        Ok(value)
    } else {
        Fatal(errors)
    }
}
