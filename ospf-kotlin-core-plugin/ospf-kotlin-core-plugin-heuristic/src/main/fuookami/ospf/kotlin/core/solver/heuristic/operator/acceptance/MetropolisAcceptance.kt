/**
 * Metropolis 接受判据 / Metropolis acceptance criterion
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.acceptance

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.GeometricSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * Metropolis 接受判据，模拟退火的核心。 / Metropolis acceptance criterion, the core of simulated annealing.
 *
 * 更优解无条件接受；更差的解以 exp(−Δ/T) 的概率接受，因此高温期能翻越局部最优、低温期趋于纯爬山。 / Better solutions are accepted unconditionally; worse ones are accepted with probability exp(−Δ/T), so a high temperature
 * escapes local optima while a low temperature degenerates into plain hill climbing.
 *
 * 目标值必须是 [Flt64]（需要做数值差），否则对更差解一律拒绝。 / The objective value must be [Flt64] (a numeric difference is required); otherwise worse solutions are always rejected.
 *
 * @property temperature 温度演化源 / Temperature evolution source
 * @property entropy 均匀分布熵源 / Uniform entropy source
*/
class MetropolisAcceptance(
    val temperature: ParameterSource = GeometricSource(Flt64(100.0), Flt64(0.95)),
    private val entropy: Generator<Flt64>
) : Acceptance {

    override fun <ObjValue, V, T> invoke(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        current: T,
        candidate: T
    ): Boolean where V : RealNumber<V>, V : NumberField<V>, T : Individual<ObjValue, V> {
        if (compareStrictly(model, candidate.fitness, current.fitness) < 0) {
            return true
        }

        val candidateValue = (candidate.fitness as? Flt64)?.toDouble() ?: return false
        val currentValue = (current.fitness as? Flt64)?.toDouble() ?: return false
        val currentTemperature = temperature(iteration).toDouble()
        if (currentTemperature <= 1.0e-12) {
            return false
        }

        val delta = candidateValue - currentValue
        if (delta <= 0.0) {
            return true
        }
        return entropy.unit() < exp(-delta / currentTemperature)
    }
}
