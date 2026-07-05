package fuookami.ospf.kotlin.math.chaotic

import fuookami.ospf.kotlin.utils.math.*

/**
 * Sprott 吸引子模型，用于生成混沌时间序列数据。
 * Sprott attractor model for generating chaotic time series data.
 *
 * @property state 系统的当前状态向量 / The current state vector of the system
 * @property dt 时间步长 / The time step size
 * @property x 系统参数（默认值对应 Lorenz 系统中的 sigma）/ System parameter (default corresponds to sigma in Lorenz system)
 * @property y 系统参数（默认值对应 Lorenz 系统中的 rho）/ System parameter (default corresponds to rho in Lorenz system)
 * @property z 系统参数（默认值对应 Lorenz 系统中的 beta）/ System parameter (default corresponds to beta in Lorenz system)
 * @property initialConditions 初始条件向量 / The initial condition vector
 */
data class SprottAttractor(
    val state: Triple<Double, Double, Double>,
    val dt: Double,
    val x: Double = 10.0,
    val y: Double = 28.0,
    val z: Double = 8.0 / 3.0,
    val initialConditions: Triple<Double, Double, Double> = Triple(0.1, 0.0, 0.0),
) {
    operator fun invoke(): Sequence<Triple<Double, Double, Double>> = sequence {
        var (x0, y0, z0) = initialConditions
        yield(Triple(x0, y0, z0))
        while (true) {
            val dx = x * (y0 - x0)
            val dy = y0 * (z - x0) - y0
            val dz = x0 * y0 - z * z0
            x0 += dx * dt
            y0 += dy * dt
            z0 += dz * dt
            yield(Triple(x0, y0, z0))
        }
    }
}
