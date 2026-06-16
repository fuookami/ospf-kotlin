@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.*

import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** Enumerates the airport types by domestic/Regional/international classification. */
enum class AirportType {
    Domestic {
        override val isDomainType: Boolean get() = true
    },
    Regional,
    International;

    open val isDomainType: Boolean get() = false
}

/** An airport identified by ICAO code, with type, transfer times, and base flag. */
data class Airport(
    val icao: ICAO,
    val type: AirportType,
    val passengerTransferTime: Duration = Duration.ZERO,
    val cargoTransferTime: Duration = Duration.ZERO,
    val base: Boolean = false
) {
    companion object {
        private val pool = HashMap<ICAO, Airport>()
        val values by pool::values

        /** Retrieves an [Airport] by ICAO code from the pool. */
        operator fun invoke(icao: ICAO): Airport? {
            return pool[icao]
        }
    }

    init {
        pool[icao] = this
    }

    override fun hashCode(): Int {
        assert(icao.code.length == 4 && icao.code.all { it.isUpperCase() })

        var ret = 0
        for (ch in icao.code) {
            ret = ret shl 4
            ret = ret or (ch - 'A')
        }
        return ret
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Airport

        return icao == other.icao
    }

    override fun toString() = "$icao"
}

/** A route defined by departure and arrival airports. */
data class Route(
    val dep: Airport,
    val arr: Airport
)
