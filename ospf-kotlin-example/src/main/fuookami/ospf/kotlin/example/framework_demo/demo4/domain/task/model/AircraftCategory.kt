@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** Enumerates the aircraft categories (passenger or cargo). */
enum class AircraftCategory {
    Passenger,
    Cargo
}

/** Sealed class representing aircraft capacity, specialized for passenger or cargo. */
sealed class AircraftCapacity {
    /** Passenger capacity mapping each class to a seat count. */
    class Passenger(
        private val capacity: Map<PassengerClass, UInt64>
    ) : AircraftCapacity() {
        val total = UInt64(capacity.values.sumOf { it.toInt() }.toULong())

        operator fun get(cls: PassengerClass) = capacity[cls] ?: UInt64.zero

        /** Checks whether the aircraft can carry the given payload per class. */
        fun enabled(payload: Map<PassengerClass, UInt64>) = payload.asSequence().all { this[it.key] >= it.value }

        override val category get() = AircraftCategory.Passenger
    }

    /** Cargo capacity as a weight/volume value. */
    class Cargo(
        val capacity: FltX
    ) : AircraftCapacity() {
        override val category get() = AircraftCategory.Cargo

        /** Checks whether the aircraft can carry the given payload weight. */
        fun enabled(payload: FltX) = capacity geq payload
    }

    abstract val category: AircraftCategory
}
