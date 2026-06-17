@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

/**
 * 记录从一架飞机到另一架飞机的变更。Records an aircraft change from one aircraft to another.
 *
 * @property from 参数。
 * @property to 参数。
 */
data class AircraftChange(
    val from: Aircraft,
    val to: Aircraft
)

/**
 * 记录从一种类型到另一种类型的飞机类型变更。Records an aircraft type change from one type to another.
 *
 * @property from 参数。
 * @property to 参数。
 */
data class AircraftTypeChange(
    val from: AircraftType,
    val to: AircraftType
)

/**
 * 记录从一个子类型到另一个子类型的飞机子类型变更。Records an aircraft minor type change from one minor type to another.
 *
 * @property from 参数。
 * @property to 参数。
 */
data class AircraftMinorTypeChange(
    val from: AircraftMinorType,
    val to: AircraftMinorType
)

/**
 * 记录从一条航线到另一条航线的变更。Records a route change from one route to another.
 *
 * @property from 参数。
 * @property to 参数。
 */
data class RouteChange(
    val from: Route,
    val to: Route
)

/**
 * 用于查找的航班任务与其恢复策略配对的键。Key pairing a flight task with its recovery policy for lookup purposes.
 *
 * @property task 参数。
 * @property policy 参数。
 */
data class RecoveryFlightTaskKey(
    val task: FlightTask,
    val policy: FlightTaskAssignment
)
