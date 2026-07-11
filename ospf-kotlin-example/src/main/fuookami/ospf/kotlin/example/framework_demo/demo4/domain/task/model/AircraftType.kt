@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model

import kotlin.time.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.infrastructure.*

/**
 * 通过代码标识的飞机类型（具有池化实例管理）。An aircraft type identified by code, with pooled instance management.
 *
 * @property code The aircraft type code / 飞机类型代码
*/
data class AircraftType(
    val code: AircraftTypeCode
) {
    companion object {
        private val pool = HashMap<AircraftTypeCode, AircraftType>()
        val values by pool::values

        /**
         * 按代码检索或创建 [AircraftType]。Retrieves or creates an [AircraftType] by code.
         *
         * @param code The aircraft type code to look up / 要查找的飞机类型代码
         * @return The aircraft type instance / 飞机类型实例
        */
        operator fun invoke(code: AircraftTypeCode): AircraftType {
            return pool.getOrPut(code){ AircraftType(code) }
        }
    }

    override fun hashCode(): Int {
        return code.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AircraftType

        return code == other.code
    }
}

/**
 * 从给定映射查找航线的时长。
 * Looks up the duration for a route from the given map.
 *
 * @param dep The departure airport / 出发机场
 * @param arr The arrival airport / 到达机场
 * @return The duration, or null if the route does not exist / 时长，如果航线不存在则返回null
*/
operator fun Map<Route, Duration>.get(dep: Airport, arr: Airport): Duration? {
    return this[Route(dep, arr)]
}

/**
 * 具有成本、航线飞行时间和连接时间的飞机子机型。An aircraft minor type with cost, route fly times, and connection times.
 *
 * @property type The aircraft type / 飞机类型
 * @property code The minor type code / 子机型代码
 * @property costPerHour The cost per flight hour / 每飞行小时成本
 * @property routeFlyTime The map of routes to flight durations / 航线到飞行时长的映射
 * @property connectionTime The map of airports to connection durations / 机场到过站时间的映射
 * @property maxFlyTime The maximum flight time / 最大飞行时间
*/
data class AircraftMinorType(
    val type: AircraftType,
    val code: AircraftMinorTypeCode,
    val costPerHour: FltX,
    val routeFlyTime: Map<Route, Duration>,
    val connectionTime: Map<Airport, Duration>,
    val maxFlyTime: Duration? = null
) {
    val maxRouteFlyTime: Duration by lazy { routeFlyTime.asSequence().maxOf { it.value } }
    val maxConnectionTime: Duration by lazy { connectionTime.asSequence().maxOf { it.value } }

    companion object {
        private val pool = HashMap<AircraftMinorTypeCode, AircraftMinorType>()
        val values by pool::values

        /**
         * 从池中按代码检索飞机子机型。Retrieves an [AircraftMinorType] by code from the pool.
         *
         * @param code The minor type code to look up / 要查找的子机型代码
         * @return The aircraft minor type instance, or null if not found / 飞机子机型实例，未找到则为null
        */
        operator fun invoke(code: AircraftMinorTypeCode): AircraftMinorType? {
            return pool[code]
        }
    }

    override fun hashCode(): Int {
        return type.hashCode().inv() or code.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AircraftMinorType

        if (type != other.type) return false
        if (code != other.code) return false

        return true
    }

    override fun toString() = "$code"
}
