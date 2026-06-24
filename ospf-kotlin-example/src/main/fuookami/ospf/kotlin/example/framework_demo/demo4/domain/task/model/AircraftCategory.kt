@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*

/** 枚举飞机类别（客机或货机）。Enumerates the aircraft categories (passenger or cargo). */
enum class AircraftCategory {
    Passenger,
    Cargo
}

/** 表示飞机容量的密封类（专门用于客运或货运）。Sealed class representing aircraft capacity, specialized for passenger or cargo. */
sealed class AircraftCapacity {
    /**
     * 将每个舱位映射到座位数的乘客容量。Passenger capacity mapping each class to a seat count.
     *
     * @property capacity 参数。
     */
    class Passenger(
        private val capacity: Map<PassengerClass, UInt64>
    ) : AircraftCapacity() {
        val total = UInt64(capacity.values.sumOf { it.toInt() }.toULong())

        operator fun get(cls: PassengerClass) = capacity[cls] ?: UInt64.zero

        /**
         * 检查飞机是否能承载给定舱位的有效载荷。/ Checks whether the aircraft can carry the given payload per class.
 *
         * @param payload 参数。
         */
        fun enabled(payload: Map<PassengerClass, UInt64>) = payload.asSequence().all { this[it.key] >= it.value }

        override val category get() = AircraftCategory.Passenger
    }

    /**
     * 作为重量/体积值的货物容量。Cargo capacity as a weight/volume value.
     *
     * @property capacity 参数。
     */
    class Cargo(
        val capacity: FltX
    ) : AircraftCapacity() {
        override val category get() = AircraftCategory.Cargo

        /**
         * 检查飞机是否能承载给定重量的有效载荷。/ Checks whether the aircraft can carry the given payload weight.
 *
         * @param payload 参数。
         */
        fun enabled(payload: FltX) = capacity geq payload
    }

    abstract val category: AircraftCategory
}
