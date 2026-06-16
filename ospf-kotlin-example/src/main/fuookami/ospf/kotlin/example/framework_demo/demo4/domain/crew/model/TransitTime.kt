@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import kotlin.time.*

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** Enumerates the transit time scenarios based on aircraft and airport relationships. */
enum class TransitTimeScene {
    SameAircraft,
    DomainNotSameAircraft,
    InternationNotSameAircraft;

    companion object {
        /** Determines the transit time scene for the given consecutive flight tasks. */
        operator fun invoke(prevTask: FlightTask, nextTask: FlightTask): TransitTimeScene? {
            return if (prevTask.aircraft == nextTask.aircraft) {
                SameAircraft
            } else if (prevTask.arr == nextTask.dep) {
                when (prevTask.arr.type) {
                    AirportType.Domestic -> DomainNotSameAircraft

                    else -> InternationNotSameAircraft
                }
            } else {
                null
            }
        }
    }
}

/** A transit time entry associating a scene with its required duration. */
data class TransitTime(
    val scene: TransitTimeScene,
    val duration: Duration
)

/** Type alias for a map of transit time scenes to their transit time entries. */
typealias TransitTimeMap = Map<TransitTimeScene, TransitTime>

/** Looks up the transit time for the given consecutive flight tasks. */
operator fun TransitTimeMap.get(prevTask: FlightTask, nextTask: FlightTask): TransitTime? {
    return TransitTimeScene.invoke(prevTask, nextTask)?.let {
        this[it]
    }
}
