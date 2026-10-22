/**
 * CMA-ES 骨架 / CMA-ES skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import java.util.IdentityHashMap
import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.FitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.DefaultOnMissFitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.toIndividual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.linear.SymmetricEigen
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * CMA-ES 的状态：一整条多元正态分布。 / CMA-ES state: one multivariate normal distribution.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property mean 分布均值 / Distribution mean
 * @property sigma 全局步长 / Global step size
 * @property covariance 协方差矩阵 / Covariance matrix
 * @property pathSigma 步长累积路径 / Cumulative path for the step size
 * @property pathCovariance 协方差累积路径 / Cumulative path for the covariance
 * @property generation 已进行的代数 / Number of generations so far
 * @property best 迄今最优个体 / Best individual so far
*/
data class CmaEsState<ObjValue, V>(
    val mean: List<Double>,
    val sigma: Double,
    val covariance: List<List<Double>>,
    val pathSigma: List<Double>,
    val pathCovariance: List<Double>,
    val generation: Int,
    val best: Individual<ObjValue, V>?
) where V : RealNumber<V>, V : NumberField<V>

/**
 * CMA-ES（协方差矩阵自适应进化策略）。 / CMA-ES, covariance matrix adaptation evolution strategy.
 *
 * 与前四个骨架都不同：它是**分布型**算法，状态不是一群解，而是一个多元正态分布 `N(m, σ²C)`。 /
 * Unlike the previous four skeletons this is a **distribution-based** algorithm: the state is not a set of solutions but a
 * multivariate normal distribution `N(m, σ²C)`.
 *
 * 每一代： / Every generation:
 *
 * 1. 把 `C` 分解为 `B·D²·Bᵀ`，沿主方向采样 `λ` 个候选；
 *    decompose `C` into `B·D²·Bᵀ` and sample `λ` candidates along the principal axes;
 * 2. 按优度重加权，把均值移向优解；
 *    reweight by quality and shift the mean towards the better candidates;
 * 3. 用累积路径更新步长 `σ` 与协方差 `C`。
 *    update the step size `σ` and covariance `C` via cumulative paths.
 *
 * 因此它能自动学习变量之间的**相关性**与各方向的**尺度**——这是所有基于坐标的算法都做不到的，
 * 也是它在病态（强耦合）问题上远优于 GA/PSO 的原因。 / It therefore learns both the **correlations** between variables and the **scale** of each direction, which no
 * coordinate-wise method can do; that is why it dominates GA/PSO on ill-conditioned (strongly coupled) problems.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property initialSigma 初始步长 / Initial step size
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
 * @property numerics 数值及采样协议 / Numerical and sampling contract
*/
class CmaEsRunner<ObjValue, V>(
    val initialSigma: Double = 0.3,
    val fitnessEvaluator: FitnessEvaluator = DefaultOnMissFitnessEvaluator,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    val numerics: CmaNumerics = CmaNumerics.Native
) : AbstractRunner<CmaEsState<ObjValue, V>, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): CmaEsState<ObjValue, V> {
        val solution = model.initialSolutions(UInt64.one).firstOrNull()
            ?: return CmaEsState(emptyList(), initialSigma, emptyList(), emptyList(), emptyList(), 0, null)

        val mean = solution.map { it.asDouble() }
        val dimension = mean.size
        return CmaEsState(
            mean = mean,
            sigma = initialSigma,
            covariance = identity(dimension),
            pathSigma = List(dimension) { 0.0 },
            pathCovariance = List(dimension) { 0.0 },
            generation = 0,
            best = fitnessEvaluator.toIndividual(model, solution)
        )
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: CmaEsState<ObjValue, V>
    ): CmaEsState<ObjValue, V> {
        val dimension = state.mean.size
        if (dimension == 0 || !state.sigma.isFinite() || state.sigma <= 0.0) {
            return state
        }

        // —— 策略参数（Hansen 的标准取值）—— / Strategy parameters (Hansen's standard values)
        val lambda = 4 + floor(3.0 * numerics.ln(dimension.toDouble())).toInt()
        val mu = lambda / 2
        val rawWeights = List(mu) { numerics.ln(mu + 0.5) - numerics.ln(it + 1.0) }
        var weightTotal = 0.0
        for (weight in rawWeights) {
            weightTotal += weight
        }
        val weights = rawWeights.map { it / weightTotal }

        var squaredTotal = 0.0
        for (weight in weights) {
            squaredTotal += weight * weight
        }
        val effectiveMass = 1.0 / squaredTotal

        val n = dimension.toDouble()
        val cc = (4.0 + effectiveMass / n) / (n + 4.0 + 2.0 * effectiveMass / n)
        val cs = (effectiveMass + 2.0) / (n + effectiveMass + 5.0)
        val c1 = 2.0 / ((n + 1.3) * (n + 1.3) + effectiveMass)
        val cmu = min(
            1.0 - c1,
            2.0 * (effectiveMass - 2.0 + 1.0 / effectiveMass) / ((n + 2.0) * (n + 2.0) + effectiveMass)
        )
        val damps = 1.0 + 2.0 * max(0.0, numerics.sqrt((effectiveMass - 1.0) / (n + 1.0)) - 1.0) + cs
        val chiN = numerics.sqrt(n) * (1.0 - 1.0 / (4.0 * n) + 1.0 / (21.0 * n * n))

        val eigen = numerics.eigen(state.covariance)
        if (!eigen.isValid) {
            return state
        }
        val axes = eigen.values.map { numerics.sqrt(it.coerceAtLeast(1.0e-20)) }

        // —— 采样—— / Sampling
        data class Sample(val y: List<Double>, val individual: Individual<ObjValue, V>)

        val samples = ArrayList<Sample>(lambda)
        for (index in 0 until lambda) {
            val z = List(dimension) { gaussian() }
            val y = List(dimension) { row ->
                var total = 0.0
                for (column in 0 until dimension) {
                    total += eigen.vectors[column][row] * axes[column] * z[column]
                }
                total
            }
            val point = ArrayList<V>(dimension)
            for (axis in 0 until dimension) {
                point.add(
                    clampTo(
                        state.mean[axis] + state.sigma * y[axis],
                        model.lowerBoundOf(axis),
                        model.upperBoundOf(axis)
                    ).asValue(converter)
                )
            }
            val individual = fitnessEvaluator.toIndividual(model, point) ?: continue
            samples.add(Sample(y, individual))
        }

        if (samples.isEmpty()) {
            return state
        }

        val byIndividual = IdentityHashMap<Individual<ObjValue, V>, Sample>()
        samples.forEach { byIndividual[it.individual] = it }
        val ranked = rankIndividuals(model, samples.map { it.individual }).map { byIndividual.getValue(it) }

        // —— 重加权并移动均值—— / Reweighting and mean shift
        val newMean = DoubleArray(dimension)
        for (index in 0 until min(mu, ranked.size)) {
            val weight = weights[index]
            for (axis in 0 until dimension) {
                newMean[axis] += weight * ranked[index].individual.solution[axis].asDouble()
            }
        }

        val meanShift = List(dimension) { (newMean[it] - state.mean[it]) / state.sigma }

        // —— 步长路径与协方差路径—— / Step-size path and covariance path
        val sigmaCoefficient = numerics.sqrt(cs * (2.0 - cs) * effectiveMass)

        // B·y_w：把均值位移投到主方向后再变换回来。
        // B·y_w: project the mean shift onto the principal axes and transform back.
        val projected = List(dimension) { column ->
            var total = 0.0
            for (row in 0 until dimension) {
                total += eigen.vectors[column][row] * meanShift[row]
            }
            total
        }
        val normalizedShift = List(dimension) { row ->
            var total = 0.0
            for (column in 0 until dimension) {
                val axis = axes[column]
                if (axis > 1.0e-20) {
                    total += eigen.vectors[column][row] * projected[column] / axis
                }
            }
            total
        }

        val newPathSigma = List(dimension) {
            (1.0 - cs) * state.pathSigma[it] + sigmaCoefficient * normalizedShift[it]
        }

        var pathNorm = 0.0
        for (value in newPathSigma) {
            pathNorm += value * value
        }
        pathNorm = numerics.sqrt(pathNorm)

        val generation = state.generation + 1
        val hSigma = pathNorm /
            numerics.sqrt(1.0 - numerics.pow(1.0 - cs, 2.0 * generation)) < (1.4 + 2.0 / (n + 1.0)) * chiN

        val covCoefficient = numerics.sqrt(cc * (2.0 - cc) * effectiveMass) * (if (hSigma) 1.0 else 0.0)
        val newPathCovariance = List(dimension) {
            (1.0 - cc) * state.pathCovariance[it] + covCoefficient * meanShift[it]
        }

        // —— 协方差更新：rank-one + rank-μ—— / Covariance update: rank-one + rank-μ
        val newCovariance = ArrayList<List<Double>>(dimension)
        for (row in 0 until dimension) {
            val rowValues = ArrayList<Double>(dimension)
            for (column in 0 until dimension) {
                var value = (1.0 - c1 - cmu) * state.covariance[row][column]

                val rankOne = newPathCovariance[row] * newPathCovariance[column]
                val correction = (1.0 - (if (hSigma) 1.0 else 0.0)) * cc * (2.0 - cc) *
                    state.covariance[row][column]
                value += c1 * (rankOne + correction)

                var rankMu = 0.0
                for (index in 0 until min(mu, ranked.size)) {
                    rankMu += weights[index] * ranked[index].y[row] * ranked[index].y[column]
                }
                value += cmu * rankMu

                rowValues.add(value)
            }
            newCovariance.add(rowValues)
        }

        val newSigma = (state.sigma * numerics.exp((cs / damps) * (pathNorm / chiN - 1.0)))
            .coerceIn(1.0e-12, 1.0e12)

        val bestCandidate = ranked.firstOrNull()?.individual
        val best = when {
            bestCandidate == null -> state.best
            state.best == null -> bestCandidate
            compareIndividuals(
                model = model,
                lhs = bestCandidate,
                rhs = state.best
            ) < 0 -> bestCandidate
            else -> state.best
        }

        return CmaEsState(
            mean = newMean.toList(),
            sigma = newSigma,
            covariance = newCovariance,
            pathSigma = newPathSigma,
            pathCovariance = newPathCovariance,
            generation = generation,
            best = best
        )
    }

    override fun membersOf(state: CmaEsState<ObjValue, V>): List<Individual<ObjValue, V>> {
        return listOfNotNull(state.best)
    }

    /** 单位矩阵 / Identity matrix */
    private fun identity(dimension: Int): List<List<Double>> {
        return List(dimension) { row -> List(dimension) { column -> if (row == column) 1.0 else 0.0 } }
    }

    /** 由均匀熵源生成标准正态（Box–Muller）/ Standard normal from a uniform source (Box–Muller) */
    private fun gaussian(): Double {
        return numerics.gaussian(entropy)
    }
}
