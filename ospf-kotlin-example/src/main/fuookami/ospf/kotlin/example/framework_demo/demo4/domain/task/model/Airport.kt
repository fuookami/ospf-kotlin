@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/** 按国内/地区/国际分类枚举机场类型。Enumerates the airport types by domestic/Regional/international classification. */
enum class AirportType {
    Domestic {
        override val isDomainType: Boolean get() = true
    },
    Regional,
    International;

    open val isDomainType: Boolean get() = false
}

/**
 * 通过 ICAO 代码标识的机场（具有类型、中转时间和基地标志）。An airport identified by ICAO code, with type, transfer times, and base flag.
 *
 * @property icao 参数。
 * @property type 参数。
 * @property passengerTransferTime 参数。
 * @property cargoTransferTime 参数。
 * @property base 参数。
 */
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

        /**
         * Retrieves an [Airport] by ICAO code from the pool.
 *
         * @param icao 参数。
         * @return 返回结果。
         */
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

/**
 * 由出发和到达机场定义的航线。A route defined by departure and arrival airports.
 *
 * @property dep 参数。
 * @property arr 参数。
 */
data class Route(
    val dep: Airport,
    val arr: Airport
)
