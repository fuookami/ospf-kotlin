package fuookami.ospf.kotlin.math.chaotic

/**
 * Windmi 混沌吸引子 / Windmi chaotic attractor
 * @property a 系统参数 a / System parameter a
 * @property b 系统参数 b / System parameter b
 * @property c 系统参数 c / System parameter c
 * @property d 系统参数 d / System parameter d
 * @property e 系统参数 e / System parameter e
 * @property f 系统参数 f / System parameter f
 * @property g 系统参数 g / System parameter g
 * @property h 系统参数 h / System parameter h
*/
class WindmiAttractor(
    val a: Double = 0.2,
    val b: Double = 0.2,
    val c: Double = 2.2,
    val d: Double = 0.5,
    val e: Double = 0.2,
    val f: Double = 5.7,
    val g: Double = 0.5,
    val h: Double = 0.1,
) {

    /**
     * 计算 Windmi 吸引子的导数 / Compute the derivative of the Windmi attractor
     * @param state 当前状态 / Current state
     * @return 导数状态 / Derivative state
    */
    fun derive(state: WindmiState): WindmiState {
        val x = state.x
        val y = state.y
        val z = state.z

        val dx = a * (y - x) + d * x * z
        val dy = b * x - x * z + f * y
        val dz = c * z + x * y - e * x * x

        return WindmiState(dx, dy, dz)
    }

    /**
     * 使用欧拉法积分 Windmi 吸引子 / Integrate the Windmi attractor using Euler's method
     * @param state 当前状态 / Current state
     * @param dt 时间步长 / Time step
     * @return 积分后的状态 / Integrated state
    */
    fun integrate(state: WindmiState, dt: Double = 0.01): WindmiState {
        val derivative = derive(state)
        return WindmiState(
            state.x + derivative.x * dt,
            state.y + derivative.y * dt,
            state.z + derivative.z * dt,
        )
    }
}

/**
 * Windmi 吸引子状态 / Windmi attractor state
 * @property x X 坐标 / X coordinate
 * @property y Y 坐标 / Y coordinate
 * @property z Z 坐标 / Z coordinate
*/
data class WindmiState(
    val x: Double,
    val y: Double,
    val z: Double,
)
