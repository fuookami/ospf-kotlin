package fuookami.ospf.kotlin.math.chaotic

/**
 * Rabinovich-Fabrikant 吸引子 / Rabinovich-Fabrikant attractor.
 *
 * 一个描述非平衡介质中波调制不稳定性的混沌吸引子系统。
 * A chaotic attractor system describing wave modulation instability in non-equilibrium media.
 *
 * @param alpha 系统参数 alpha / System parameter alpha
 * @param gamma 系统参数 gamma / System parameter gamma
 */
class RabinovichFabrikantAttractor(
    override val alpha: Double,
    override val gamma: Double
) : Chaos {
    override fun invoke(
        t: Double,
        x: Double,
        y: Double,
        z: Double
    ): Result<ChaosState> {
        return Result.success(
            ChaosState(
                dx = gamma * (y - x + x * z),
                dy = alpha * x - y + x * z,
                dz = -x * y - z
            )
        )
    }
}
