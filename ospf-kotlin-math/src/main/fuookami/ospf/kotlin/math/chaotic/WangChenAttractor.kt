package fuookami.ospf.kotlin.math.chaotic

import fuookami.ospf.kotlin.math.chaotic.state.*

/**
 * 王-陈吸引子混沌系统 / Wang-Chen attractor chaotic system
 *
 * @property state 系统状态向量 / System state vector
 */
data class WangChenAttractor(
    override val state: State4D = default
) : ChaoticSystem<State4D> {
    override fun evaluate(state: State4D): State4D {
        val (x, y, z, w) = state
        val dx = A * (y - x)
        val dy = B * x - C * x * z + w
        val dz = x * y - D * z
        val dw = -y * z
        return State4D(dx, dy, dz, dw)
    }

    companion object {
        /** 系统参数 a / System parameter a */
        const val a = 0.2
        /** 系统参数 b / System parameter b */
        const val b = 0.4
        /** 系统参数 c / System parameter c */
        const val c = 6.0
        /** 系统参数 d / System parameter d */
        const val d = 0.8

        /** 默认初始状态 / Default initial state */
        val default = State4D(1.0, 1.0, 1.0, 1.0)
    }
}

/** 系统参数 A（x 与 y 之间线性耦合系数）/ System parameter A (linear coupling coefficient between x and y) */
private const val A = 10.0
/** 系统参数 B（x 方向线性增益系数）/ System parameter B (linear gain coefficient in x direction) */
private const val B = 40.0
/** 系统参数 C（x 与 z 之间非线性耦合系数）/ System parameter C (nonlinear coupling coefficient between x and z) */
private const val C = 1.0
/** 系统参数 D（z 方向耗散系数）/ System parameter D (dissipation coefficient in z direction) */
private const val D = 2.5
