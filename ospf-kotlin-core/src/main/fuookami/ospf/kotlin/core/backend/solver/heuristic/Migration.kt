package fuookami.ospf.kotlin.core.backend.solver.heuristic

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.frontend.model.callback.*

interface Migration<V> {
    operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>>
}

data class RandomMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }
}

data class BetterToWorseMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }
}

data class MoreToLessMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }
}

data class MigrationMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }

//    /**
//     * 迁移算子（岛屿模型）
//     * @param migrationRate 迁移率（0~1，迁移个体比例）
//     * @param topology 迁移拓扑（如环形、全连接）
//     */
//    class MigrationOperator(
//        private val migrationRate: Double,
//        private val topology: MigrationTopology = MigrationTopology.RING
//    ) {
//        init {
//            require(migrationRate in 0.0..1.0) { "迁移率必须在 [0,1] 之间" }
//        }
//
//        /**
//         * 执行迁移操作
//         * @param subPopulations 多个子种群（每个子种群为 List<T>）
//         * @return 迁移后的新子种群集合
//         */
//        fun <T> execute(subPopulations: List<List<T>>): List<List<T>> {
//            if (subPopulations.size < 2) return subPopulations // 无需迁移
//
//            // 1. 确定每个子种群的迁出个体
//            val migrants = subPopulations.map { population ->
//                val migrateCount = (population.size * migrationRate).toInt().coerceAtLeast(1)
//                population.shuffled().take(migrateCount) // 随机选择迁出个体
//            }
//
//            // 2. 根据拓扑分配迁入目标
//            return subPopulations.mapIndexed { index, population ->
//                val sources = topology.getSources(index, subPopulations.size)
//                val incoming = sources.flatMap { migrants[it] }
//                (population - migrants[index].toSet() + incoming).shuffled()
//            }
//        }
//    }
//
//    enum class MigrationTopology {
//        RING { // 环形拓扑：每个子种群迁移给下一个
//            override fun getSources(currentIndex: Int, total: Int): List<Int> {
//                val prev = (currentIndex - 1 + total) % total
//                return listOf(prev)
//            }
//        },
//        ALL_TO_ALL { // 全连接：迁移给所有其他子种群
//            override fun getSources(currentIndex: Int, total: Int): List<Int> {
//                return (0 until total).filter { it != currentIndex }
//            }
//        };
//
//        abstract fun getSources(currentIndex: Int, total: Int): List<Int>
//    }
}

data class RingExchangeMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }

//    /**
//     * 环形拓扑个体交换
//     * @param exchangeProb 交换概率（0~1）
//     */
//    class RingExchangeOperator(private val exchangeProb: Double) {
//        fun <T> execute(population: List<T>): List<T> {
//            val newPopulation = population.toMutableList()
//            for (i in population.indices) {
//                if (Random.nextDouble() > exchangeProb) continue
//
//                val neighbor = (i + 1) % population.size
//                // 交换个体
//                newPopulation[i] = population[neighbor]
//                newPopulation[neighbor] = population[i]
//            }
//            return newPopulation
//        }
//    }
}

data class RandomDiffusionMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T: Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }

//    /**
//     * 随机扩散交换
//     * @param diffusionRate 扩散率（0~1，被替换的个体比例）
//     */
//    class RandomDiffusionOperator(private val diffusionRate: Double) {
//        fun <T> execute(population: List<T>): List<T> {
//            val indicesToReplace = population.indices
//                .filter { Random.nextDouble() < diffusionRate }
//                .shuffled()
//
//            val candidates = population.shuffled().iterator()
//            return population.mapIndexed { index, individual ->
//                if (index in indicesToReplace) candidates.next() else individual
//            }
//        }
//    }
}

data class ElitistMigrationMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }

//    /**
//     * 精英迁移（替换最差个体）
//     * @param eliteCount 每个子种群保留的精英数量
//     */
//    class ElitistMigrationOperator(private val eliteCount: Int) {
//        fun <T : Comparable<T>> execute(
//            subPopulations: List<List<T>>,
//            fitnessFunc: (T) -> Double
//        ): List<List<T>> {
//            // 1. 提取每个子种群的精英和最差个体
//            val elites = subPopulations.map { pop ->
//                pop.sortedByDescending(fitnessFunc).take(eliteCount)
//            }
//            val worsts = subPopulations.map { pop ->
//                pop.sortedBy(fitnessFunc).take(eliteCount)
//            }
//
//            // 2. 将精英迁移到下一个子种群，替换最差个体
//            return subPopulations.mapIndexed { index, pop ->
//                val nextIndex = (index + 1) % subPopulations.size
//                val newPop = pop.toMutableList()
//                // 移除当前子种群的最差个体
//                newPop.removeAll(worsts[index])
//                // 添加来自前一个子种群的精英
//                newPop.addAll(elites[nextIndex])
//                newPop.shuffled()
//            }
//        }
//    }
}

data class PopulationMergeMigration<V>(
    private val randomGenerator: Generator<Flt64>,
) : Migration<V> {
    override operator fun <T : Individual<V>> invoke(
        iteration: Iteration,
        populations: List<Population<T, V>>,
        model: AbstractCallBackModelInterface<*, V>
    ): List<Pair<Population<T, V>, List<T>>> {
        TODO("not implemented yet")
    }

//    /**
//     * 混合种群合并（适用于周期性重启策略）
//     */
//    class PopulationMergeOperator {
//        fun <T> execute(subPopulations: List<List<T>>): List<List<T>> {
//            val merged = subPopulations.flatten().shuffled()
//            // 均等分割为与原数量相同的子种群
//            val chunkSize = merged.size / subPopulations.size
//            return merged.chunked(chunkSize)
//        }
//    }
}
