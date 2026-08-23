@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Flight information including leg, current time, and estimated departure time.
 * 航班信息，包括航段、当前时间和预计出发时间。
 *
 * @property leg the IATA leg code / IATA 航段代码
 * @property now 当前时间 / the current time instant
 * @property etd 预计出发时间 / the estimated time of departure
*/
data class Flight(
    val leg: IATA,
    val now: kotlin.time.Instant,
    val etd: kotlin.time.Instant
) {
    companion object {
        private val reweighNeededTerminals = setOf<IATA>()
    }

    val department = IATA(leg.code.substring(0, 3))
    val arrival = IATA(leg.code.substring(4, 7))

    /**
     * Checks whether reweighing is needed for the given stowage mode.
     * 检查在给定装载模式下是否需要复称。
     *
     * @param stowageMode 装载模式 / the stowage mode
     * @return 是否需要复称 / true if reweighing is needed
    */
    fun reweighNeeded(stowageMode: StowageMode): Boolean {
        return when (stowageMode) {
            StowageMode.Predistribution -> {
                reweighNeededTerminals.contains(department)
            }

            else -> {
                false
            }
        }
    }
}
