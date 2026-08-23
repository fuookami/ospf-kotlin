@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

/**
 * 记录从一架飞机到另一架飞机的变更。Records an aircraft change from one aircraft to another.
 *
 * @property from 原始飞机 / The original aircraft
 * @property to 新飞机 / The new aircraft
*/
data class AircraftChange(
    val from: Aircraft,
    val to: Aircraft
)

/**
 * 记录从一种类型到另一种类型的飞机类型变更。Records an aircraft type change from one type to another.
 *
 * @property from 原始飞机类型 / The original aircraft type
 * @property to 新飞机类型 / The new aircraft type
*/
data class AircraftTypeChange(
    val from: AircraftType,
    val to: AircraftType
)

/**
 * 记录从一个子类型到另一个子类型的飞机子类型变更。Records an aircraft minor type change from one minor type to another.
 *
 * @property from 原始飞机子机型 / The original aircraft minor type
 * @property to 新飞机子机型 / The new aircraft minor type
*/
data class AircraftMinorTypeChange(
    val from: AircraftMinorType,
    val to: AircraftMinorType
)

/**
 * 记录从一条航线到另一条航线的变更。Records a route change from one route to another.
 *
 * @property from 原始航线 / The original route
 * @property to 新航线 / The new route
*/
data class RouteChange(
    val from: Route,
    val to: Route
)

/**
 * 用于查找的航班任务与其恢复策略配对的键。Key pairing a flight task with its recovery policy for lookup purposes.
 *
 * @property task 航班任务 / The flight task
 * @property policy 恢复策略分配 / The recovery policy assignment
*/
data class RecoveryFlightTaskKey(
    val task: FlightTask,
    val policy: FlightTaskAssignment
)
