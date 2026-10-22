/**
 * 粒子群搜索 / Particle swarm search
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.math.*
import kotlinx.coroutines.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.FitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.DefaultOnMissFitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.toIndividual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ConstantSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 粒子状态，速度是跨迭代信息，必须由骨架持有。 / Particle state; the velocity is cross-iteration information and must be held by the skeleton.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property individual 当前位置 / Current position
 * @property velocity 当前速度 / Current velocity
 * @property best 个体历史最优 / Personal historical best
*/
data class ParticleState<ObjValue, V>(
    val individual: Individual<ObjValue, V>,
    val velocity: List<Flt64>,
    val best: Individual<ObjValue, V>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 粒子群搜索模板，第三步独立状态类型。 / Particle swarm search template, the third independent state type.
 *
 * 与种群骨架的区别只在状态：粒子需要携带**速度**与**个体历史最优**，二者都是跨迭代信息，
 * 无法写进无状态的解更新算子。这正是「两级骨架」需要第三种模板的原因——不是每个算法都能压成算子组合。 / The only difference from the population skeleton is the state: a particle carries a **velocity** and a **personal best**, both
 * of which are cross-iteration information that cannot be squeezed into a stateless solution-update operator. This is exactly
 * why the skeleton needs a third template: not every algorithm collapses into an operator composition.
 *
 * v' = w·v + c₁·r₁·(pBest − x) + c₂·r₂·(gBest − x)
 * x' = x + v'
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property size 粒子数量 / Particle amount
 * @property inertia 惯性权重 w / Inertia weight w
 * @property personalWeight 个体学习因子 c₁ / Personal learning factor c₁
 * @property socialWeight 社会学习因子 c₂ / Social learning factor c₂
 * @property maxVelocity 最大速度 / Maximum velocity
 * @property initialVelocity 初始速度生成器 / Initial velocity generator
 * @property initializer 初始化算子 / Initialization operator
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
*/
class ParticleSwarmRunner<ObjValue, V>(
    val size: Int,
    val inertia: ParameterSource = ConstantSource(Flt64(0.4)),
    val personalWeight: ParameterSource = ConstantSource(Flt64(2.0)),
    val socialWeight: ParameterSource = ConstantSource(Flt64(2.0)),
    val maxVelocity: Flt64 = Flt64(10000),
    val initialVelocity: (index: Int) -> Flt64 = { Flt64.zero },
    val initializer: Initializer = ModelInitializer,
    val fitnessEvaluator: FitnessEvaluator = DefaultOnMissFitnessEvaluator,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>
) : AbstractRunner<List<ParticleState<ObjValue, V>>, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): List<ParticleState<ObjValue, V>> {
        val solutions = initializer(
            iteration = iteration,
            model = model,
            amount = UInt64(size),
            converter = converter
        )
        return solutions
            .mapNotNull { fitnessEvaluator.toIndividual(model, it) }
            .take(size)
            .map { individual ->
                ParticleState(
                    individual = individual,
                    velocity = List(individual.solution.size) { initialVelocity(it) },
                    best = individual
                )
            }
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: List<ParticleState<ObjValue, V>>
    ): List<ParticleState<ObjValue, V>> {
        if (state.isEmpty()) {
            return state
        }

        val globalBest = state.minWithOrNull { lhs, rhs ->
            compareStrictly(model, lhs.individual.fitness, rhs.individual.fitness)
        } ?: return state

        val weight = inertia(iteration).toDouble()
        val personal = personalWeight(iteration).toDouble()
        val social = socialWeight(iteration).toDouble()
        val velocityLimit = maxVelocity.toDouble()

        return coroutineScope {
            state.map { particle ->
                async(SearchExecution.dispatcher()) {
                    val dimension = particle.individual.solution.size
                    val velocity = DoubleArray(dimension)
                    val moved = ArrayList<V>(dimension)

                    for (index in 0 until dimension) {
                        val current = particle.individual.solution[index].asDouble()
                        val personalTarget = particle.best.solution[index].asDouble()
                        val globalTarget = globalBest.individual.solution[index].asDouble()

                        var speed = weight * particle.velocity[index].toDouble() +
                            personal * entropy.unit() * (personalTarget - current) +
                            social * entropy.unit() * (globalTarget - current)
                        speed = speed.coerceIn(-velocityLimit, velocityLimit)
                        velocity[index] = speed

                        val lower = model.lowerBoundOf(index)
                        val upper = model.upperBoundOf(index)
                        moved.add(clampTo(current + speed, lower, upper).asValue(converter))
                    }

                    val candidate = fitnessEvaluator.toIndividual(model, moved)
                        ?: return@async ParticleState(
                            individual = particle.individual,
                            velocity = particle.velocity,
                            best = particle.best
                        )

                    val improved = compareStrictly(model, candidate.fitness, particle.best.fitness) < 0
                    ParticleState(
                        individual = candidate,
                        velocity = velocity.map { Flt64(it) },
                        best = if (improved) candidate else particle.best
                    )
                }
            }.awaitAll()
        }
    }

    override fun membersOf(state: List<ParticleState<ObjValue, V>>): List<Individual<ObjValue, V>> {
        return state.map { it.individual }
    }
}
