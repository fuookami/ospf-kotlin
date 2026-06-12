/** 粒子群优化器粒子定义 / Particle Swarm Optimizer particle definition */
package fuookami.ospf.kotlin.core.solver.heuristic.pso

import fuookami.ospf.kotlin.utils.functional.Order
import fuookami.ospf.kotlin.utils.functional.ifNull
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

private val flt64Converter = object : IntoValue<Flt64> {
        override fun intoValue(value: Flt64) = value
        override val zero get() = Flt64.zero
        override val one get() = Flt64.one
        override fun fromValue(value: Flt64) = value
    }

/** 粒子类，表示粒子群优化中的一个粒子 / Particle class, represents a particle in Particle Swarm Optimization */
data class Particle<ObjValue, V>(
    override val fitness: ObjValue,
    override val solution: List<V>,
    val velocity: List<Flt64>,
    val currentBest: Particle<ObjValue, V>? = null,
    // converter must be provided explicitly; use Particle.Flt64 companion for V=Flt64 convenience / 转换器必须显式提供；V=Flt64 时可使用 Particle.Flt64 伴生对象
    private val converter: IntoValue<V>
) : Individual<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            fitness: Flt64,
            solution: List<Flt64>,
            velocity: List<Flt64>,
            currentBest: Particle<Flt64, Flt64>? = null
        ): Particle<Flt64, Flt64> {
            return Particle(
                fitness = fitness,
                solution = solution,
                velocity = velocity,
                currentBest = currentBest,
                converter = flt64Converter
            )
        }
    }
    init {
        assert(solution.size == velocity.size)
    }

    val size by solution::size

    /**
     * 创建新粒子 / Create new particle
     *
     * @param newVelocity 新速度 / new velocity
     * @param iteration 当前迭代 / current iteration
     * @param policy 启发式策略 / heuristic policy
     * @param model 回调模型 / callback model
     * @return 新粒子 / new particle
     */
    fun new(
        newVelocity: List<Flt64>,
        iteration: Iteration,
        policy: AbstractHeuristicPolicy,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Particle<ObjValue, V> {
        val newPosition = (0..<size).map {
            val posFlt64 = converter.fromValue(solution[it])
            val newFlt64 = posFlt64 + velocity[it]
            converter.intoValue(policy.coerceIn(
                iteration = iteration,
                index = it,
                value = newFlt64,
                model = model
            ))
        }
        val newFitness = model.objective(newPosition).ifNull { model.defaultObjective }
        return if (currentBest != null) {
            Particle(
                fitness = newFitness,
                solution = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, currentBest.fitness) is Order.Less) {
                    null
                } else {
                    currentBest
                },
                converter = converter
            )
        } else {
            Particle(
                fitness = newFitness,
                solution = newPosition,
                velocity = newVelocity,
                currentBest = if (model.compareObjective(newFitness, fitness) is Order.Less) {
                    null
                } else {
                    this
                },
                converter = converter
            )
        }
    }
}


