/**
 * SHADE 骨架与自适应记忆 / SHADE skeleton and adaptive memory
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.FitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.DefaultOnMissFitnessEvaluator
import fuookami.ospf.kotlin.core.solver.heuristic.operator.fitness.toIndividual
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ParameterSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.evolution.ConstantSource
import fuookami.ospf.kotlin.core.solver.heuristic.operator.*

/**
 * 一次参数采样结果 / One sampled parameter pair
 *
 * @property memoryIndex 采样自记忆库的槽位 / Slot the sample came from
 * @property differentialWeight 缩放因子 F / Scaling factor F
 * @property crossRate 交叉率 CR / Crossover rate CR
*/
data class ShadeParameters(
    val memoryIndex: Int,
    val differentialWeight: Double,
    val crossRate: Double
)

/**
 * 成功历史自适应记忆（SHADE 的核心）。 / Success-history adaptive memory, the core of SHADE.
 *
 * DE 的难点在于 `F` 与 `CR` 的最优值与问题、与搜索阶段都相关，手工设定必然次优。
 * SHADE 的做法是：把每次**成功**（子代优于父代）的 `F`、`CR` 记下来，用它们的均值反过来调整采样分布的中心——
 * 参数因此由搜索自身的历史决定，而不是由使用者猜。 / The hard part of DE is that the optimal `F` and `CR` depend on both the problem and the search stage, so hand-tuned
 * values are necessarily suboptimal. SHADE records the `F` and `CR` of every **successful** trial (offspring better than its
 * parent) and uses their means to re-centre the sampling distributions, so the parameters are determined by the search's own
 * history rather than guessed by the user.
 *
 * `F` 用**莱默均值**（偏向较大的成功值，因为较大的步长更容易跨越长距离），`CR` 用算术均值。 /
 * `F` uses the **Lehmer mean** (biased towards larger successful values because larger steps cover long distances more
 * effectively) while `CR` uses the arithmetic mean.
 *
 * @property memoryF 缩放因子的记忆库 / Memory bank of the scaling factor
 * @property memoryCr 交叉率的记忆库 / Memory bank of the crossover rate
 * @property cursor 下一个写入槽位 / Next slot to write
*/
data class ShadeMemory(
    val memoryF: List<Double> = List(DEFAULT_SIZE) { 0.5 },
    val memoryCr: List<Double> = List(DEFAULT_SIZE) { 0.5 },
    val cursor: Int = 0
) {
    companion object {
        /** 默认记忆槽数量 / Default number of memory slots */
        const val DEFAULT_SIZE: Int = 5
    }

    /**
     * 采样一组 (F, CR) / Sample one (F, CR) pair
     *
     * `F` 取自柯西分布（重尾，偶尔产生大步长），`CR` 取自正态分布；两者都被裁剪到 `[0, 1]`。 /
     * `F` comes from a Cauchy distribution (heavy tailed, occasionally producing a long step) and `CR` from a normal
     * distribution; both are clamped to `[0, 1]`.
     *
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 参数与槽位 / Parameters and slot
    */
    fun sample(entropy: Generator<Flt64>): ShadeParameters {
        val index = (entropy.unit() * memoryF.size).toInt().coerceIn(0, memoryF.size - 1)
        var differentialWeight = memoryF[index] + 0.1 * kotlin.math.tan(PI * (entropy.unit() - 0.5))
        if (differentialWeight <= 0.0) {
            differentialWeight = memoryF[index]
        }
        differentialWeight = differentialWeight.coerceIn(0.0, 1.0)

        val crossRate = (memoryCr[index] + 0.1 * (entropy.unit() + entropy.unit() + entropy.unit() - 1.5) * 2.0)
            .coerceIn(0.0, 1.0)
        return ShadeParameters(index, differentialWeight, crossRate)
    }

    /**
     * 用本代的成功参数更新记忆 / Update the memory with this generation's successful parameters
     *
     * @param successfulF 成功个体的缩放因子 / Scaling factors of the successful individuals
     * @param successfulCr 成功个体的交叉率 / Crossover rates of the successful individuals
     * @return 更新后的记忆 / Updated memory
    */
    fun update(successfulF: List<Double>, successfulCr: List<Double>): ShadeMemory {
        if (successfulF.isEmpty()) {
            return this
        }

        // 项目内的 Iterable.sumOf 扩展会遮蔽标准库同名函数，这里用显式累加。
        // The project's Iterable.sumOf extension shadows the stdlib one, so accumulate explicitly.
        var weightedSum = 0.0
        var totalSum = 0.0
        for (value in successfulF) {
            weightedSum += value * value
            totalSum += value
        }
        var crossRateSum = 0.0
        for (value in successfulCr) {
            crossRateSum += value
        }

        val lehmerMean = if (totalSum > 0.0) (weightedSum / totalSum).coerceIn(0.0, 1.0) else memoryF[cursor]
        val arithmeticMeanCr = (crossRateSum / successfulCr.size).coerceIn(0.0, 1.0)

        val nextF = memoryF.toMutableList().also { it[cursor] = lehmerMean }
        val nextCr = memoryCr.toMutableList().also { it[cursor] = arithmeticMeanCr }
        return ShadeMemory(nextF, nextCr, (cursor + 1) % memoryF.size)
    }
}

/**
 * SHADE 的状态：种群 + 记忆库 + 外部档案。 / SHADE state: population, memory bank, and external archive.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property population 当前种群 / Current population
 * @property memory 自适应记忆 / Adaptive memory
 * @property archive 被淘汰父代的外部档案 / External archive of replaced parents
*/
data class ShadeState<ObjValue, V>(
    val population: List<Individual<ObjValue, V>>,
    val memory: ShadeMemory,
    val archive: List<Solution<V>>
) where V : RealNumber<V>, V : NumberField<V>

/**
 * SHADE 搜索骨架。 / SHADE search skeleton.
 *
 * 这是第四个状态模板，也是对「两级骨架」设计的一次验证：SHADE 的自适应性要求跨迭代地累积成功历史，
 * 无状态的解更新算子做不到，因此把它建模为状态的一部分，循环本身仍然复用同一套结构。 /
 * This is the fourth state template and a validation of the "two-level skeleton" design: SHADE's adaptivity requires
 * accumulating success history across iterations, which a stateless solution-update operator cannot do, so it is modelled as
 * part of the state while the loop keeps the same structure.
 *
 * 变异用 `current-to-pbest/1`，`p` 取种群的前 `pBestRate` 比例： /
 * Mutation uses `current-to-pbest/1` with the top `pBestRate` fraction of the population:
 *
 * v_i = x_i + F_i·(x_pbest − x_i) + F_i·(x_r1 − x_r2)
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property size 初始种群规模 / Initial population size
 * @property memorySize 记忆槽数量 / Number of memory slots
 * @property pBestRate p-best 的比例 / Fraction used for p-best
 * @property sizeRatio 种群规模的缩减源；恒为 1 时即 SHADE，递减时即 L-SHADE / Shrinking source of the population size; constant 1 gives SHADE, decreasing gives L-SHADE
 * @property minimumSize 缩减后的种群规模下界 / Lower bound of the shrunk population size
 * @property fitnessEvaluator 适应度评估算子 / Fitness evaluation operator
*/
class ShadeRunner<ObjValue, V>(
    val size: Int,
    val memorySize: Int = ShadeMemory.DEFAULT_SIZE,
    val pBestRate: Double = 0.2,
    val sizeRatio: ParameterSource = ConstantSource(Flt64.one),
    val minimumSize: Int = 4,
    val fitnessEvaluator: FitnessEvaluator = DefaultOnMissFitnessEvaluator,
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>
) : AbstractRunner<ShadeState<ObjValue, V>, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): ShadeState<ObjValue, V> {
        val population = model
            .initialSolutions(UInt64(size))
            .mapNotNull { fitnessEvaluator.toIndividual(model, it) }
            .take(size)

        return ShadeState(
            population = population,
            memory = ShadeMemory(
                memoryF = List(memorySize) { 0.5 },
                memoryCr = List(memorySize) { 0.5 },
                cursor = 0
            ),
            archive = emptyList()
        )
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: ShadeState<ObjValue, V>
    ): ShadeState<ObjValue, V> {
        if (state.population.isEmpty()) {
            return state
        }

        val sorted = rankIndividuals(model, state.population)
        val pBestAmount = max(1, (sorted.size * pBestRate).toInt())
        val pBestPool = sorted.take(pBestAmount)

        val nextPopulation = ArrayList<Individual<ObjValue, V>>(state.population.size)
        val nextArchive = state.archive.toMutableList()
        val successfulF = ArrayList<Double>()
        val successfulCr = ArrayList<Double>()

        for (index in state.population.indices) {
            val target = state.population[index]
            val parameters = state.memory.sample(entropy)
            val pBest = pBestPool[(entropy.unit() * pBestPool.size).toInt().coerceIn(0, pBestPool.size - 1)]

            // r1 取当前种群，r2 取「种群 + 外部档案」的并集——外部档案保留了已被淘汰的父代位置，
            // 让差分向量在更长的历史里取点，这是 SHADE 相比经典 DE 的关键改动之一。
            // r1 comes from the current population and r2 from "population + external archive": the archive keeps replaced
            // parents so the difference vector can draw on a longer history, a key SHADE change over classic DE.
            var first = (entropy.unit() * state.population.size).toInt().coerceIn(0, state.population.size - 1)
            if (first == index) {
                first = (index + 1) % state.population.size
            }
            val secondPool = state.population.map { it.solution } + nextArchive
            val second = (entropy.unit() * secondPool.size).toInt().coerceIn(0, secondPool.size - 1)

            val dimension = target.solution.size
            val mutant = ArrayList<V>(dimension)
            for (dim in 0 until dimension) {
                val base = target.solution[dim].asDouble()
                val towardBest = pBest.solution[dim].asDouble() - base
                val difference = state.population[first].solution[dim].asDouble() -
                    secondPool[second][dim].asDouble()
                val value = base + parameters.differentialWeight * towardBest +
                    parameters.differentialWeight * difference
                mutant.add(
                    clampTo(
                        value,
                        model.lowerBoundOf(dim),
                        model.upperBoundOf(dim)
                    ).asValue(converter)
                )
            }

            // 二项式交叉：至少有一个维度取自变异向量，否则子代可能与父代完全相同。
            // Binomial crossover: at least one dimension must come from the mutant, otherwise the trial may equal its parent.
            val trial = ArrayList<V>(dimension)
            var forced = (entropy.unit() * dimension).toInt().coerceIn(0, dimension - 1)
            for (dim in 0 until dimension) {
                val takeMutant = entropy.unit() < parameters.crossRate || dim == forced
                trial.add(if (takeMutant) mutant[dim] else target.solution[dim])
            }

            val candidate = fitnessEvaluator.toIndividual(model, trial)
            if (candidate == null) {
                nextPopulation.add(target)
                continue
            }

            if (compareIndividuals(
    model = model,
    lhs = candidate,
    rhs = target
) <= 0) {
                nextPopulation.add(candidate)
                nextArchive.add(target.solution.toList())
                successfulF.add(parameters.differentialWeight)
                successfulCr.add(parameters.crossRate)
            } else {
                nextPopulation.add(target)
            }
        }

        // L-SHADE：种群规模随迭代线性缩减。缩减时保留最优的那些个体——这正是「规模缩减」而非「随机淘汰」的意义。
        // L-SHADE: the population size shrinks linearly with iterations, keeping the best individuals, which is what makes it a
        // size reduction rather than random culling.
        val targetSize = (size * sizeRatio(iteration).toDouble()).toInt()
            .coerceIn(minimumSize.coerceAtMost(size), size)
        val survived = if (nextPopulation.size > targetSize) {
            keepBest(model, nextPopulation, targetSize)
        } else {
            nextPopulation
        }

        // 外部档案与当前种群规模保持一致，保留最近被淘汰的父代。
        // Keep the external archive aligned with the current population size, retaining the most recently replaced parents.
        while (nextArchive.size > targetSize) {
            nextArchive.removeAt(0)
        }

        return ShadeState(
            population = survived,
            memory = state.memory.update(successfulF, successfulCr),
            archive = nextArchive
        )
    }

    /**
     * 选出最优的若干个个体 / Select the best individuals
     *
     * 与 `bestOf` 一致，用逐个挑选而不是 `sortedWith`：模型比较器可能是带容差的近似比较，不具备传递性，
     * 直接交给 TimSort 会抛 `Comparison method violates its general contract`。 /
     * Consistent with `bestOf`, this picks repeatedly instead of using `sortedWith`: the model comparator may be an approximate
     * one without transitivity, and handing it to TimSort throws `Comparison method violates its general contract`.
     *
     * @param model 回调模型接口 / Callback model interface
     * @param individuals 候选个体 / Candidate individuals
     * @param amount 保留数量 / Amount to keep
     * @return 保留的个体 / Kept individuals
    */
    private fun keepBest(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        individuals: List<Individual<ObjValue, V>>,
        amount: Int
    ): List<Individual<ObjValue, V>> {
        constraintHandling(model)?.let { return it.rank(individuals).take(amount) }
        val remaining = individuals.toMutableList()
        val kept = ArrayList<Individual<ObjValue, V>>(amount)
        repeat(amount.coerceAtMost(remaining.size)) {
            var bestIndex = 0
            for (index in 1 until remaining.size) {
                if (compareStrictly(model, remaining[index].fitness, remaining[bestIndex].fitness) < 0) {
                    bestIndex = index
                }
            }
            kept.add(remaining.removeAt(bestIndex))
        }
        return kept
    }

    override fun membersOf(state: ShadeState<ObjValue, V>): List<Individual<ObjValue, V>> {
        return state.population
    }
}
