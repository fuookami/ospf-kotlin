package fuookami.ospf.kotlin.framework.network_scheduling.infrastructure

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/** 网络调度非法参数失败。 / Network scheduling illegal-argument failure. */
fun <T> networkSchedulingFailure(message: String): Ret<T> {
    return Failed(ErrorCode.IllegalArgument, message)
}
