/**
 * N 体系统
 * N-Body System
 *
 * N 体系统是经典力学中的多体引力相互作用模型。
 * 该系统通过牛顿万有引力定律计算多个天体之间的引力，产生复杂的混沌轨道。
 * 常用于天体力学研究、星系模拟和混沌轨道分析。
 *
 * The N-body system is a multi-body gravitational interaction model in classical mechanics.
 * This system calculates gravitational forces between multiple celestial bodies using Newton's law of universal gravitation, producing complex chaotic orbits.
 * Commonly used for celestial mechanics research, galaxy simulation, and chaotic orbit analysis.
 */
package fuookami.ospf.kotlin.math.chaotic

import kotlin.random.Random
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.geometry.*
import fuookami.ospf.kotlin.math.nextFlt64

/**
 * N 体系统
 * N-Body System
 *
 * 公式 / Formula:
 * dx_i/dt = v_i
 * dv_i/dt = sum_{j!=i} G * m_j * (x_j - x_i) / |x_j - x_i|^3
 *
 * @property m 各天体质量列表 / List of body masses
 * @property G 万有引力常数 / Gravitational constant
 * @property h 时间步长 / Time step size
 */
data class NBodySystem(
    val m: List<Flt64>,
    val G: Flt64 = Flt64(6.67e-11),
    val h: Flt64 = Flt64(0.001)
) : Extractor<List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>>, List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>>> {
    override operator fun invoke(
        state: List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>>
    ): List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>> {
        val n = state.size
        return List(n) { i ->
            val (posI, velI) = state[i]
            // 计算引力加速度 / Calculate gravitational acceleration
            var ax = Flt64.zero
            var ay = Flt64.zero
            var az = Flt64.zero
            for (j in 0 until n) {
                if (i != j) {
                    val (posJ, _) = state[j]
                    val dx = posJ[0] - posI[0]
                    val dy = posJ[1] - posI[1]
                    val dz = posJ[2] - posI[2]
                    val distSq = dx * dx + dy * dy + dz * dz
                    val dist = distSq.sqrt()
                    val distCubed = distSq * dist
                    if (distCubed gr Flt64.zero) {
                        val force = G * m[j] / distCubed
                        ax = ax + force * dx
                        ay = ay + force * dy
                        az = az + force * dz
                    }
                }
            }
            // 更新位置和速度 / Update position and velocity
            val newPos = point3(
                posI[0] + h * velI[0],
                posI[1] + h * velI[1],
                posI[2] + h * velI[2]
            )
            val newVel = point3(
                velI[0] + h * ax,
                velI[1] + h * ay,
                velI[2] + h * az
            )
            Pair(newPos, newVel)
        }
    }

    companion object {
        /**
         * 创建二维 N 体系统
         * Create a 2D N-body system
         */
        fun plane(
            m: List<Flt64>,
            G: Flt64 = Flt64(6.67e-11),
            h: Flt64 = Flt64(0.001)
        ): NBodySystemPlane {
            return NBodySystemPlane(m, G, h)
        }
    }
}

/**
 * 二维 N 体系统
 * 2D N-Body System
 *
 * @property m 各天体质量列表 / List of body masses
 * @property G 万有引力常数 / Gravitational constant
 * @property h 时间步长 / Time step size
 */
data class NBodySystemPlane(
    val m: List<Flt64>,
    val G: Flt64 = Flt64(6.67e-11),
    val h: Flt64 = Flt64(0.001)
) : Extractor<List<Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>>>, List<Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>>>> {
    override operator fun invoke(
        state: List<Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>>>
    ): List<Pair<Point<Dim2, Flt64>, Point<Dim2, Flt64>>> {
        val n = state.size
        return List(n) { i ->
            val (posI, velI) = state[i]
            var ax = Flt64.zero
            var ay = Flt64.zero
            for (j in 0 until n) {
                if (i != j) {
                    val (posJ, _) = state[j]
                    val dx = posJ[0] - posI[0]
                    val dy = posJ[1] - posI[1]
                    val distSq = dx * dx + dy * dy
                    val dist = distSq.sqrt()
                    val distCubed = distSq * dist
                    if (distCubed gr Flt64.zero) {
                        val force = G * m[j] / distCubed
                        ax = ax + force * dx
                        ay = ay + force * dy
                    }
                }
            }
            val newPos = point2(posI[0] + h * velI[0], posI[1] + h * velI[1])
            val newVel = point2(velI[0] + h * ax, velI[1] + h * ay)
            Pair(newPos, newVel)
        }
    }
}

/**
 * N 体系统生成器
 * N-Body System Generator
 */
data class NBodySystemGenerator(
    val nBodySystem: NBodySystem = NBodySystem(listOf(Flt64.one, Flt64.one, Flt64.one)),
    private var _state: List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>> = nBodySystem.m.map {
        Pair(
            point3(
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one),
                Random.nextFlt64(-Flt64.one, Flt64.one)
            ),
            point3(Flt64.zero, Flt64.zero, Flt64.zero)
        )
    }
) : Generator<List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>>> {
    companion object {
        operator fun invoke(
            m: List<Flt64>,
            G: Flt64 = Flt64(6.67e-11),
            h: Flt64 = Flt64(0.001),
            state: List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>> = m.map {
                Pair(
                    point3(
                        Random.nextFlt64(-Flt64.one, Flt64.one),
                        Random.nextFlt64(-Flt64.one, Flt64.one),
                        Random.nextFlt64(-Flt64.one, Flt64.one)
                    ),
                    point3(Flt64.zero, Flt64.zero, Flt64.zero)
                )
            }
        ): NBodySystemGenerator {
            return NBodySystemGenerator(
                NBodySystem(m, G, h),
                state
            )
        }
    }

    val state by ::_state

    override operator fun invoke(): List<Pair<Point<Dim3, Flt64>, Point<Dim3, Flt64>>> {
        val s = _state.map { (p, v) -> Pair(p.copy(), v.copy()) }
        _state = nBodySystem(_state)
        return s
    }
}
