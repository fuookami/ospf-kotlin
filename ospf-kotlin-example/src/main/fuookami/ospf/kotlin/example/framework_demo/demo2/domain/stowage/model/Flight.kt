@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model

import kotlinx.datetime.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.IATA

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
