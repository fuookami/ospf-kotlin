@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*

enum class AircraftCategory {
    Passenger,
    Cargo
}

sealed class AircraftCapacity {
    class Passenger(
        private val capacity: Map<PassengerClass, UInt64>
    ) : AircraftCapacity() {
        val total = UInt64(capacity.values.sumOf { it.toInt() }.toULong())

        operator fun get(cls: PassengerClass) = capacity[cls] ?: UInt64.zero

        fun enabled(payload: Map<PassengerClass, UInt64>) = payload.asSequence().all { this[it.key] >= it.value }

        override val category get() = AircraftCategory.Passenger
    }

    class Cargo(
        val capacity: FltX
    ) : AircraftCapacity() {
        override val category get() = AircraftCategory.Cargo

        fun enabled(payload: FltX) = capacity geq payload
    }

    abstract val category: AircraftCategory
}
