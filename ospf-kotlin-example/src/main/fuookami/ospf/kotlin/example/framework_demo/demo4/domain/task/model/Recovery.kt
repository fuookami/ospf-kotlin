@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

data class AircraftChange(
    val from: Aircraft,
    val to: Aircraft
)

data class AircraftTypeChange(
    val from: AircraftType,
    val to: AircraftType
)

data class AircraftMinorTypeChange(
    val from: AircraftMinorType,
    val to: AircraftMinorType
)

data class RouteChange(
    val from: Route,
    val to: Route
)

data class RecoveryFlightTaskKey(
    val task: FlightTask,
    val policy: FlightTaskAssignment
)
