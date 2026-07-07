/** 粒子群优化器实现 / Particle Swarm Optimizer implementation */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.core.solver.heuristic.pso

import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Duration.Companion.minutes
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.nextFlt64
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.core.model.basic.MultiObjectLocation
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.cleanupAfterSolverRun
import fuookami.ospf.kotlin.core.solver.cleanupOnSolverMemoryPressure
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue

private val flt64Converter = object : IntoValue<Flt64> {
    override fun intoValue(value: Flt64) = value
    override val zero get() = Flt64.zero
    override val one get() = Flt64.one
    override fun fromValue(value: Flt64) = value
}

/** 粒子群优化器策略接口 / Particle Swarm Optimizer policy interface */
interface AbstractPSOPolicy<ObjValue, V> :
    AbstractHeuristicPolicy where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    /**
     * 加速粒子 / Accelerate particle
     *
     * @param iteration 当前迭代 / current iteration
     * @param particle 当前粒子 / current particle
     * @param bestParticle 全局最优粒子 / global best particle
     * @param model 回调模型 / callback model
     * @return 加速后的粒子 / accelerated particle
     */
    fun accelerate(
        iteration: Iteration,
        particle: Particle<ObjValue, V>,
        bestParticle: Particle<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Particle<ObjValue, V>
}

/**
 * 粒子群优化器策略
 *
 * 实现粒子群优化的加速操作，使用惯性权重、局部学习因子和全局学习因子控制粒子运动。
 *
 * Particle Swarm Optimizer policy
 *
 * Implements acceleration operation for PSO, using inertia weight, local learning factor,
 * and global learning factor to control particle movement.
 *
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property w 惯性权重 / inertia weight
 * @property c1 局部学习因子 / local learning factor
 * @property c2 全局学习因子 / global learning factor
 * @property maxVelocity 最大速度 / maximum velocity
 * @property randomGenerator 随机数生成器 / random number generator
 */
open class PSOPolicy<ObjValue, V>(
    val w: Flt64 = Flt64(0.4),
    val c1: Flt64 = Flt64.two,
    val c2: Flt64 = Flt64.two,
    val maxVelocity: Flt64 = Flt64(10000.0),
    iterationLimit: UInt64 = UInt64.maximum,
    notBetterIterationLimit: UInt64 = UInt64.maximum,
    timeLimit: Duration = 30.minutes,
    val randomGenerator: Generator<Flt64> = { Random.nextFlt64() },
    private val converter: IntoValue<V>
) : HeuristicPolicy(
    iterationLimit = iterationLimit,
    notBetterIterationLimit = notBetterIterationLimit,
    timeLimit = timeLimit
), AbstractPSOPolicy<ObjValue, V> where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            w: Flt64 = Flt64(0.4),
            c1: Flt64 = Flt64.two,
            c2: Flt64 = Flt64.two,
            maxVelocity: Flt64 = Flt64(10000.0),
            iterationLimit: UInt64 = UInt64.maximum,
            notBetterIterationLimit: UInt64 = UInt64.maximum,
            timeLimit: Duration = 30.minutes,
            randomGenerator: Generator<Flt64> = { Random.nextFlt64() }
        ): PSOPolicy<Flt64, Flt64> {
            return PSOPolicy(
                w = w,
                c1 = c1,
                c2 = c2,
                maxVelocity = maxVelocity,
                iterationLimit = iterationLimit,
                notBetterIterationLimit = notBetterIterationLimit,
                timeLimit = timeLimit,
                randomGenerator = randomGenerator,
                converter = flt64Converter
            )
        }
    }

    /** 加速粒子 / Accelerate particle */
    override fun accelerate(
        iteration: Iteration,
        particle: Particle<ObjValue, V>,
        bestParticle: Particle<ObjValue, V>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>
    ): Particle<ObjValue, V> {
        return particle.new(
            newVelocity = (0 until particle.size).map {
                val posFlt64 = converter.fromValue(particle.solution[it])
                val personalBestFlt64 = particle.currentBest?.solution?.get(it)
                    ?.let { pos -> converter.fromValue(pos) }
                val globalBestFlt64 = converter.fromValue(bestParticle.solution[it])
                val newVelocity = w * particle.velocity[it] +
                        c1 * randomGenerator()!! * (personalBestFlt64?.let { pb -> pb - posFlt64 } ?: Flt64.zero) +
                        c2 * randomGenerator()!! * (globalBestFlt64 - posFlt64)
                if (newVelocity gr maxVelocity) {
                    maxVelocity
                } else if (newVelocity ls -maxVelocity) {
                    -maxVelocity
                } else {
                    newVelocity
                }
            },
            iteration = iteration,
            policy = this,
            model = model
        )
    }
}

@OptIn(ExperimentalTime::class)
/**
 * 粒子群优化算法
 *
 * 实现基于粒子群的优化算法，每个粒子根据个体最优和全局最优更新位置和速度。
 *
 * Particle Swarm Optimization algorithm
 *
 * Implements swarm-based optimization algorithm, where each particle updates its position
 * and velocity based on personal best and global best.
 *
 * @param Obj 目标类型 / objective type
 * @param ObjValue 目标值类型 / objective value type
 * @param V 值类型 / value type
 * @property particleAmount 粒子数量 / particle amount
 * @property solutionAmount 期望解的数量 / desired number of solutions
 * @property policy 粒子群策略 / PSO policy
 */
class ParticleSwarmOptimizationAlgorithm<Obj, ObjValue, V>(
    val particleAmount: UInt64 = UInt64(100UL),
    val solutionAmount: UInt64 = UInt64.one,
    val policy: AbstractPSOPolicy<ObjValue, V>,
    // converter must be provided explicitly; use PSOPolicy.Flt64 companion for V=Flt64 convenience / 转换器必须显式提供；V=Flt64 时可使用 PSOPolicy.Flt64 伴生对象
    private val converter: IntoValue<V>
) where V : fuookami.ospf.kotlin.math.algebra.concept.RealNumber<V>, V : fuookami.ospf.kotlin.math.algebra.concept.NumberField<V> {
    companion object {
        operator fun invoke(
            particleAmount: UInt64 = UInt64(100UL),
            solutionAmount: UInt64 = UInt64.one,
            policy: AbstractPSOPolicy<Flt64, Flt64> = PSOPolicy()
        ): ParticleSwarmOptimizationAlgorithm<Flt64, Flt64, Flt64> {
            return ParticleSwarmOptimizationAlgorithm(
                particleAmount = particleAmount,
                solutionAmount = solutionAmount,
                policy = policy,
                converter = flt64Converter
            )
        }
    }

    /**
     * 执行粒子群优化算法 / Execute Particle Swarm Optimization
     *
     * @param model 回调模型 / callback model
     * @param initialVelocityGenerator 初始速度生成器 / initial velocity generator
     * @param runningCallBack 运行回调函数 / running callback function
     * @return 最优个体列表 / best individual list
     */
    operator fun invoke(
        model: AbstractCallBackModelInterface<Obj, ObjValue, V>,
        initialVelocityGenerator: Extractor<Flt64, UInt64> = { Random.nextFlt64(Flt64.two) - Flt64.one },
        runningCallBack: ((Iteration, Particle<ObjValue, V>, List<Particle<ObjValue, V>>) -> Try)? = null
    ): List<Individual<ObjValue, V>> {
        val iteration = Iteration()
        val initialSolutions = model.initialSolutions(particleAmount)
        var particles = initialSolutions
            .map {
                Particle(
                    fitness = model.objective(it).ifNull { model.defaultObjective },
                    solution = it,
                    velocity = it.indices.map { index -> initialVelocityGenerator(UInt64(index)) },
                    converter = converter
                )
            }
            .sortedWithPartialThreeWayComparator { lhs, rhs -> model.compareObjective(lhs.fitness, rhs.fitness) }
        var bestParticle = particles.first()
        val goodParticles = particles.take(solutionAmount.toInt()).toMutableList()

        try {
            while (!policy.finished(iteration)) {
                var globalBetter = false

                val newParticles = particles
                    .map {
                        policy.accelerate(
                            iteration = iteration,
                            particle = it,
                            bestParticle = bestParticle,
                            model = model
                        )
                    }
                    .sortedWithPartialThreeWayComparator { lhs, rhs ->
                        model.compareObjective(lhs.fitness, rhs.fitness)
                    }
                val newBestParticle = newParticles.first()
                particles = newParticles
                if (model.compareObjective(newBestParticle.fitness, bestParticle.fitness) is Order.Less) {
                    bestParticle = newBestParticle
                    globalBetter = true
                }
                refreshGoodIndividuals(
                    goodIndividuals = goodParticles,
                    newIndividuals = newParticles,
                    model = model,
                    solutionAmount = solutionAmount
                )

                model.flush()
                policy.update(
                    iteration = iteration,
                    better = globalBetter,
                    bestIndividual = bestParticle,
                    goodIndividuals = goodParticles,
                    populations = listOf(particles),
                    model = model
                )
                iteration.next(globalBetter)
                cleanupOnSolverMemoryPressure()

                if (runningCallBack?.invoke(iteration, bestParticle, goodParticles) is Failed) {
                    break
                }
            }

            return goodParticles
                .take(solutionAmount.toInt())
                .map {
                    SolutionWithFitness(
                        solution = it.solution,
                        fitness = it.fitness
                    )
                }
        } finally {
            cleanupAfterSolverRun()
        }
    }
}

/** 单目标粒子群优化器类型 / Single-objective Particle Swarm Optimization type */
typealias PSO = ParticleSwarmOptimizationAlgorithm<Flt64, Flt64, Flt64>
/** 多目标粒子群优化器类型 / Multi-objective Particle Swarm Optimization type */
typealias MulObjPSO = ParticleSwarmOptimizationAlgorithm<List<Pair<MultiObjectLocation<Flt64>, Flt64>>, List<Flt64>, Flt64>
