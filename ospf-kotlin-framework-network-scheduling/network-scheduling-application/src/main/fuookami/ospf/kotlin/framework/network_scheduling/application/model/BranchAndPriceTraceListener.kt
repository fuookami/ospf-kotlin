package fuookami.ospf.kotlin.framework.network_scheduling.application.model

import fuookami.ospf.kotlin.utils.functional.*

/** Branch-and-Price 轨迹监听器。 / Branch-and-Price trace listener. */
fun interface BranchAndPriceTraceListener {
    /** 接收最新轨迹快照。 / Receive the latest trace snapshot. */
    fun onTrace(trace: BranchAndPriceTrace): Try

    companion object {
        /** 默认空监听器。 / Default no-op listener. */
        val None = BranchAndPriceTraceListener { ok }
    }
}
