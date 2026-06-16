@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

/** Records an aircraft change from one aircraft to another. */
data class AircraftChange(
    val from: Aircraft,
    val to: Aircraft
)

/** Records an aircraft type change from one type to another. */
data class AircraftTypeChange(
    val from: AircraftType,
    val to: AircraftType
)

/** Records an aircraft minor type change from one minor type to another. */
data class AircraftMinorTypeChange(
    val from: AircraftMinorType,
    val to: AircraftMinorType
)

/** Records a route change from one route to another. */
data class RouteChange(
    val from: Route,
    val to: Route
)

/** Key pairing a flight task with its recovery policy for lookup purposes. */
data class RecoveryFlightTaskKey(
    val task: FlightTask,
    val policy: FlightTaskAssignment
)
