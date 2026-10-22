package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.heuristic.*

/** 异构边界的类型化状态桥，绝不伪造成功参数历史 / Typed heterogeneous state bridge, never fabricating successful parameter history.
 * @property problem 目标语义 / Objective semantics
 * @property maxVelocity 速度夹紧 / Velocity limit
 * @property initialSigma 重启分布尺度 / Restart distribution scale
 */
class ContextStateSynchronizer<O, V>(
    private val problem: SearchProblem<O, V>,
    val maxVelocity: Double = 10000.0,
    val initialSigma: Double = 0.3
) where V : RealNumber<V>, V : NumberField<V> {
    private fun compare(a: Individual<O, V>, b: Individual<O, V>): Int = compareIndividuals(
        model = problem.model,
        lhs = a,
        rhs = b
    )

    private fun valid(incoming: List<Individual<O, V>>): Boolean = incoming.all {
        problem.validSolution(it.solution) && problem.valid(it.fitness) && problem.admissible(it.solution) &&
            it.solution.all { value -> value.toFlt64().toDouble().isFinite() }
    }

    /** 从显式相邻位置建立速度，不对无对应维度猜测 / Build velocity from explicit adjacent positions, never guessing dimensions.
     * @param previous 上次位置 / Previous position
     * @param current 当前位置 / Current position
     * @return 夹紧速度 / Clamped velocity
     */
    fun velocity(previous: List<V>, current: List<V>): Ret<List<Flt64>> {
        if (previous.size != current.size || !maxVelocity.isFinite() || maxVelocity <= 0 ||
            (previous + current).any { !it.toFlt64().toDouble().isFinite() }) return searchInvalid("invalid bridge positions/velocity scale")
        return Ok(current.indices.map { index -> Flt64(
            (current[index].toFlt64().toDouble() - previous[index].toFlt64().toDouble()).coerceIn(-maxVelocity, maxVelocity)
        ) })
    }

    /** 导入者与最近目的粒子匹配，由位置差热启动速度并保留历史最优 / Match nearest destination particle, warm-start displacement velocity and retain personal best.
     * @param state 原粒子 / Existing particles
     * @param incoming 已重评估导入解 / Re-evaluated imports
     * @return 新粒子状态 / New particle state
     */
    fun particles(state: List<ParticleState<O, V>>, incoming: List<Individual<O, V>>): Ret<List<ParticleState<O, V>>> {
        if (!valid(incoming) || state.any { !problem.validSolution(it.individual.solution) }) return searchInvalid("invalid bridge imports")
        if (incoming.isEmpty() || state.isEmpty()) return Ok(state)
        val replacements = state.sortedWith { a, b -> compare(a.best, b.best) }.toMutableList()
        for ((index, candidate) in incoming.take(state.size).withIndex()) {
            val source = state.minBy { particle -> candidate.solution.indices.map { i ->
                val delta = candidate.solution[i].toFlt64().toDouble() - particle.individual.solution[i].toFlt64().toDouble()
                delta * delta
            }.sum() }
            val motion = velocity(source.individual.solution, candidate.solution)
            if (motion !is Ok) return searchFailure(motion)
            replacements[state.lastIndex - index] = ParticleState(
                individual = candidate,
                velocity = motion.value,
                best = if (compare(source.best, candidate) < 0) source.best else candidate
            )
        }
        return Ok(replacements)
    }

    /** CLPSO 导入后重建合法示范索引，并保留速度/历史优解 / Rebuild valid CLPSO exemplar indices while retaining velocity/personal history.
     * @param state 原 CLPSO 粒子 / Previous CLPSO particles
     * @param incoming 已重评估导入解 / Re-evaluated imports
     * @return 同步后的粒子 / Synchronized particles
     */
    fun clpso(state: List<ClpsoParticle<O, V>>, incoming: List<Individual<O, V>>): Ret<List<ClpsoParticle<O, V>>> {
        if (incoming.isEmpty()) return Ok(state)
        return particles(state.map { ParticleState(
            individual = it.individual, velocity = it.velocity, best = it.best
        ) }, incoming).map { particles -> particles.mapIndexed { index, particle -> ClpsoParticle(
            individual = particle.individual,
            velocity = particle.velocity,
            best = particle.best,
            exemplars = List(particle.individual.solution.size) { index },
            stagnation = 0
        ) } }
    }

    /** 在种群变更时保留真实 SHADE 记忆，淘汰的解进入有界外部档案 / Retain genuine SHADE memory and archive displaced population members.
     * @param state 原 SHADE 状态 / Previous SHADE state
     * @param incoming 已重评估导入解 / Re-evaluated imports
     * @return 对齐后的 SHADE 状态 / Aligned SHADE state
     */
    fun shade(state: ShadeState<O, V>, incoming: List<Individual<O, V>>): Ret<ShadeState<O, V>> {
        if (!valid(incoming)) return searchInvalid("invalid SHADE imports")
        val population = rankIndividuals(problem.model, (state.population + incoming).distinctBy { it.solution }).take(state.population.size)
        val displaced = state.population.filter { old -> population.none { it.solution == old.solution } }
        return Ok(state.copy(
            population = population,
            archive = (state.archive + displaced.map { it.solution }).distinct().takeLast(state.population.size)
        ))
    }

    /** 从导入群体估计完整协方差；单解重定心时保留原协方差 / Estimate full covariance from imported population; a single seed retains previous covariance.
     * @param state 原分布 / Previous distribution
     * @param incoming 已重评估导入群体 / Re-evaluated imported population
     * @return 重启后的分布 / Restarted distribution
     */
    fun cmaEs(state: CmaEsState<O, V>, incoming: List<Individual<O, V>>): Ret<CmaEsState<O, V>> {
        if (!valid(incoming) || !initialSigma.isFinite() || initialSigma <= 0) return searchInvalid("invalid CMA bridge imports/scale")
        if (incoming.isEmpty()) return Ok(state)
        val ranked = rankIndividuals(problem.model, incoming)
        val dimension = ranked.first().solution.size
        if (ranked.any { it.solution.size != dimension }) return searchInvalid("inconsistent CMA bridge dimensions")
        val mean = (0 until dimension).map { axis -> ranked.map { it.solution[axis].toFlt64().toDouble() / ranked.size }.sum() }
        if (mean.any { !it.isFinite() }) return searchInvalid("invalid CMA bridge mean")
        val covariance = if (ranked.size < 2) state.covariance else List(dimension) { row -> List(dimension) { column ->
            val sum = ranked.map {
                (it.solution[row].toFlt64().toDouble() - mean[row]) * (it.solution[column].toFlt64().toDouble() - mean[column])
            }.sum()
            sum / (ranked.size - 1) / (initialSigma * initialSigma) + if (row == column) 1e-10 else 0.0
        } }
        if (covariance.size != dimension || covariance.any { it.size != dimension || it.any { v -> !v.isFinite() } }) return searchInvalid("invalid CMA bridge covariance")
        val best = state.best?.takeIf { compare(it, ranked.first()) < 0 } ?: ranked.first()
        return Ok(CmaEsState(
            mean = mean,
            sigma = initialSigma,
            covariance = covariance,
            pathSigma = List(dimension) { 0.0 },
            pathCovariance = List(dimension) { 0.0 },
            generation = 0,
            best = best
        ))
    }
}
