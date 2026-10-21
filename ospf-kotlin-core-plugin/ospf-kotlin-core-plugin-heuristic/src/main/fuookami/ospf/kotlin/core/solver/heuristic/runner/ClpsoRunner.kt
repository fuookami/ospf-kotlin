/**
 * 综合学习粒子群 / Comprehensive learning particle swarm
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
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.LinearSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * CLPSO 的粒子状态。 / Particle state of CLPSO.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property individual 当前位置 / Current position
 * @property velocity 当前速度 / Current velocity
 * @property best 个体历史最优 / Personal historical best
 * @property exemplars 每个维度各自的学习对象下标 / Exemplar index per dimension
 * @property stagnation 连续未改进的代数 / Generations without improvement
*/
data class ClpsoParticle<ObjValue, V>(
    val individual: Individual<ObjValue, V>,
    val velocity: List<Flt64>,
    val best: Individual<ObjValue, V>,
    val exemplars: List<Int>,
    val stagnation: Int
) where V : RealNumber<V>, V : NumberField<V>

/**
 * 综合学习粒子群（CLPSO）。 / Comprehensive learning particle swarm optimization (CLPSO).
 *
 * 标准 PSO 的每颗粒子只向**自己的历史最优**和**全局最优**学习，一旦这两者都在局部最优上，整群一起停滞。
 * CLPSO 的关键改动是：**每个维度各自选一个学习对象**，对象是别的粒子的历史最优，且定期刷新。 /
 * A standard PSO particle learns only from its own best and the global best, so once both sit on a local optimum the whole
 * swarm stalls together. CLPSO's key change is that **every dimension picks its own exemplar**, drawn from the personal bests
 * of other particles and refreshed periodically.
 *
 * v_d = w·v_d + c·r_d·(pbest_{f(d), d} − x_d)
 *
 * 于是同一颗粒子的不同维度可以朝不同方向学习，种群不会整体塌缩到一个点上；再加上「长期未改进就重置 pbest」的
 * 多样性机制，CLPSO 在多峰问题上明显优于标准 PSO。 /
 * Different dimensions of the same particle can therefore learn towards different targets, so the swarm does not collapse onto
 * one point; together with the "reset pbest after prolonged stagnation" diversification, CLPSO clearly outperforms standard
 * PSO on multimodal problems.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property size 粒子数量 / Particle amount
 * @property inertia 惯性权重源 / Inertia weight source
 * @property learningWeight 学习因子 c / Learning factor c
 * @property maxVelocity 最大速度 / Maximum velocity
 * @property refreshInterval 学习对象刷新间隔（按停滞代数）/ Exemplar refresh interval in stagnation generations
 * @property stagnationLimit 重置 pbest 的停滞阈值 / Stagnation threshold for resetting pbest
 * @property initializer 初始化算子 / Initialization operator
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
*/
class ClpsoRunner<ObjValue, V>(
    val size: Int,
    val inertia: ParameterSource = LinearSource(Flt64(0.9), Flt64(0.4), UInt64(100)),
    val learningWeight: Flt64 = Flt64(1.49445),
    val maxVelocity: Flt64 = Flt64(10000),
    val refreshInterval: Int = 7,
    val stagnationLimit: Int = 5,
    val initializer: Initializer = ModelInitializer,
    val fitnessEvaluator: FitnessEvaluator = DefaultOnMissFitnessEvaluator,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>
) : AbstractRunner<List<ClpsoParticle<ObjValue, V>>, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): List<ClpsoParticle<ObjValue, V>> {
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
                ClpsoParticle(
                    individual = individual,
                    velocity = List(individual.solution.size) { Flt64.zero },
                    best = individual,
                    exemplars = List(individual.solution.size) { randomIndex(size) },
                    stagnation = 0
                )
            }
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: List<ClpsoParticle<ObjValue, V>>
    ): List<ClpsoParticle<ObjValue, V>> {
        if (state.isEmpty()) {
            return state
        }

        val weight = inertia(iteration).toDouble()
        val coefficient = learningWeight.toDouble()
        val velocityLimit = maxVelocity.toDouble()
        // 学习对象来自「上一代」的历史最优，因此先快照，避免同代内互相污染。
        // Exemplars come from the previous generation's personal bests, so snapshot them first to avoid intra-generation contamination.
        val bests = state.map { it.best }

        return coroutineScope {
            state.map { particle ->
                async(Dispatchers.Default) {
                    val dimension = particle.individual.solution.size
                    val refresh = particle.stagnation >= refreshInterval
                    val velocity = DoubleArray(dimension)
                    val moved = ArrayList<V>(dimension)

                    for (index in 0 until dimension) {
                        val exemplarIndex = if (refresh) {
                            randomIndex(state.size)
                        } else {
                            particle.exemplars[index].coerceIn(0, state.size - 1)
                        }
                        val current = particle.individual.solution[index].asDouble()
                        val target = bests[exemplarIndex].solution[index].asDouble()
                        var speed = weight * particle.velocity[index].toDouble() +
                            coefficient * entropy.unit() * (target - current)
                        speed = speed.coerceIn(-velocityLimit, velocityLimit)
                        velocity[index] = speed
                        moved.add(
                            clampTo(
                                current + speed,
                                model.lowerBoundOf(index),
                                model.upperBoundOf(index)
                            ).asValue(converter)
                        )
                    }

                    val candidate = fitnessEvaluator.toIndividual(model, moved)
                        ?: return@async particle.copy(
                            velocity = velocity.map { Flt64(it) },
                            stagnation = particle.stagnation + 1
                        )

                    val improved = compareStrictly(model, candidate.fitness, particle.best.fitness) < 0
                    when {
                        improved -> ClpsoParticle(
                            individual = candidate,
                            velocity = velocity.map { Flt64(it) },
                            best = candidate,
                            exemplars = particle.exemplars,
                            stagnation = 0
                        )

                        // 长期停滞：把 pbest 重置到当前位置，迫使粒子换一个学习方向。
                        // Prolonged stagnation: reset pbest to the current position so the particle is forced to change direction.
                        particle.stagnation + 1 > stagnationLimit -> ClpsoParticle(
                            individual = candidate,
                            velocity = velocity.map { Flt64(it) },
                            best = candidate,
                            exemplars = List(dimension) { randomIndex(state.size) },
                            stagnation = 0
                        )

                        else -> ClpsoParticle(
                            individual = candidate,
                            velocity = velocity.map { Flt64(it) },
                            best = particle.best,
                            exemplars = if (refresh) {
                                List(dimension) { randomIndex(state.size) }
                            } else {
                                particle.exemplars
                            },
                            stagnation = particle.stagnation + 1
                        )
                    }
                }
            }.awaitAll()
        }
    }

    override fun membersOf(state: List<ClpsoParticle<ObjValue, V>>): List<Individual<ObjValue, V>> {
        return state.map { it.best }
    }

    /** 采样一个合法下标 / Sample a valid index */
    private fun randomIndex(amount: Int): Int {
        if (amount <= 1) {
            return 0
        }
        return (entropy.unit() * amount).toInt().coerceIn(0, amount - 1)
    }
}
