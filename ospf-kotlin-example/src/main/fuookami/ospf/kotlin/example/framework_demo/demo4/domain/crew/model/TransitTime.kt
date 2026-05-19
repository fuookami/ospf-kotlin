@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.crew.model

import kotlin.time.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

enum class TransitTimeScene {
    SameAircraft,
    DomainNotSameAircraft,
    InternationNotSameAircraft;

    companion object {
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

data class TransitTime(
    val scene: TransitTimeScene,
    val duration: Duration
)

typealias TransitTimeMap = Map<TransitTimeScene, TransitTime>

operator fun TransitTimeMap.get(prevTask: FlightTask, nextTask: FlightTask): TransitTime? {
    return TransitTimeScene.invoke(prevTask, nextTask)?.let {
        this[it]
    }
}