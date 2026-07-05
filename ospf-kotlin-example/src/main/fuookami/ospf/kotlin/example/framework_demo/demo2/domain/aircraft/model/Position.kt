package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import java.time.Instant

/** 飞机位置信息 / Aircraft position information */
data class Position(
    val tailNumber: String,
    /** 纬度 / Latitude */
    val latitude: Double,
    /** 经度 / Longitude */
    val longitude: Double,
    /** 海拔高度（英尺） / Altitude (feet) */
    val altitude: Double,
    /** 速度（节） / Speed (knots) */
    val speed: Double,
    /** 航向（度） / Heading (degrees) */
    val heading: Double,
    /** 时间戳 / Timestamp */
    val timestamp: Instant
) {
    companion object {
        /** 地球半径（公里） / Earth radius (km) */
        private const val earthRadiusKm = 6371.0
    }

    /**
     * 获取以节为单位的当前速度
     * / Get the current speed in knots
     * @return 当前速度（节） / Current speed (knots)
     */
    fun speed(): Double {
        return speed
    }

    /**
     * 计算到目标位置的大圆距离（公里）
     * / Calculate the great-circle distance to the target position (km)
     * @param other 目标位置 / Target position
     * @return 距离（公里） / Distance (km)
     */
    fun distanceTo(other: Position): Double {
        val lat1 = Math.toRadians(latitude)
        val lon1 = Math.toRadians(longitude)
        val lat2 = Math.toRadians(other.latitude)
        val lon2 = Math.toRadians(other.longitude)

        val dlat = lat2 - lat1
        val dlon = lon2 - lon1

        val a = Math.sin(dlat / 2) * Math.sin(dlat / 2) +
                Math.cos(lat1) * Math.cos(lat2) *
                Math.sin(dlon / 2) * Math.sin(dlon / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadiusKm * c
    }

    /**
     * 计算到目标位置的初始方位角（度）
     * / Calculate the initial bearing to the target position (degrees)
     * @param other 目标位置 / Target position
     * @return 方位角（度） / Bearing (degrees)
     */
    fun bearingTo(other: Position): Double {
        val lat1 = Math.toRadians(latitude)
        val lat2 = Math.toRadians(other.latitude)
        val dlon = Math.toRadians(other.longitude - longitude)

        val y = Math.sin(dlon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(dlon)

        val bearing = Math.toDegrees(Math.atan2(y, x))
        return (bearing + 360) % 360
    }
}
