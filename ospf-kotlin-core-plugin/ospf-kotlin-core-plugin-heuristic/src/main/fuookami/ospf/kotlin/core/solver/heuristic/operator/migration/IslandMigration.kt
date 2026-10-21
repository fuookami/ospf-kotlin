/**
 * 岛屿迁移算子 / Island migration operators
*/
package fuookami.ospf.kotlin.core.solver.heuristic.operator.migration

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.heuristic.Individual
import fuookami.ospf.kotlin.core.solver.heuristic.Iteration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.compareStrictly

/**
 * 岛屿迁移算子，负责「多个子种群之间交换个体」。 / Island migration operator, responsible for "exchanging individuals between sub-populations".
 *
 * 迁移属于**骨架层**的职责而不是算子层的：它作用于整群解，而不是「若干父代 → 若干子代」，
 * 因此与 `SolutionUpdate` / `Mutation` 这类逐步算子不同类。把它抽象成算子，是为了让岛模型骨架
 * （`IslandRunner`）能像挑选交叉算子一样挑选迁移策略。 /
 * Migration belongs to the **skeleton layer** rather than the operator layer: it acts on whole populations instead of
 * "some parents → some offspring", so it is unlike step operators such as `SolutionUpdate` / `Mutation`. Abstracting it as an
 * operator lets the island-model skeleton (`IslandRunner`) pick a migration strategy the same way it picks a crossover.
 *
 * 所有实现都从传入的 `entropy` 取随机数，不使用全局随机源——否则同一份配置无法复现。 /
 * Every implementation draws randomness from the supplied `entropy` rather than a global source, otherwise the same
 * configuration could not be reproduced.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
interface IslandMigration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    /**
     * 执行迁移 / Perform migration
     *
     * @param T 个体类型 / Individual type
     * @param iteration 当前迭代 / Current iteration
     * @param islands 各岛屿的个体列表 / Individuals of each island
     * @param model 回调模型接口，用于比较优度 / Callback model interface used to compare quality
     * @param entropy 均匀分布熵源 / Uniform entropy source
     * @return 迁移后的各岛屿 / Islands after migration
    */
    operator fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        islands: List<List<T>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        entropy: Generator<Flt64>
    ): List<List<T>>
}

/**
 * 不迁移。 / No migration.
 *
 * 单独提供实现而不是用可空参数表示「不迁移」，是为了让「有迁移 / 无迁移」在类型上一致，
 * 骨架里不需要分支判断。 / A dedicated implementation instead of a nullable parameter keeps "migrating / not migrating" type-uniform, so the
 * skeleton needs no branch.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
*/
class NoMigration<ObjValue, V> : IslandMigration<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> {

    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        islands: List<List<T>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        entropy: Generator<Flt64>
    ): List<List<T>> {
        return islands
    }
}

/**
 * 环形迁移：每个岛屿把若干个最优个体送给下一个岛屿，替换对方最差的个体。 / Ring migration: each island sends several best individuals to the next island, replacing its worst.
 *
 * 这是岛模型最常用的迁移方式——单向、环形、只传优者。只传优者可以避免「好岛被坏岛污染」，
 * 而环形结构保证信息最终绕所有岛屿一圈。 /
 * This is the most common island-model migration: unidirectional, ring-shaped, best-only. Sending only the best avoids a good
 * island being polluted by a poor one, and the ring guarantees information eventually reaches every island.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property rate 每个岛屿外迁的个体比例 / Fraction of each island's individuals that migrate
*/
class RingMigration<ObjValue, V>(
    val rate: Double = 0.1
) : IslandMigration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        islands: List<List<T>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        entropy: Generator<Flt64>
    ): List<List<T>> {
        if (islands.size < 2) {
            return islands
        }

        val ranked = islands.map { island -> rankAscending(island, model) }

        return islands.indices.map { index ->
            val incoming = ranked[(index - 1 + islands.size) % islands.size]
            val local = ranked[index]
            val amount = (local.size * rate).toInt().coerceAtLeast(1)
                .coerceAtMost(minOf(local.size, incoming.size))
            if (amount <= 0) {
                return@map islands[index]
            }
            // 用迁入者替换本岛最差的若干个体，保持岛屿规模不变。
            // Replace the island's worst individuals with the migrants, keeping the island size unchanged.
            local.dropLast(amount) + incoming.take(amount)
        }
    }
}

/**
 * 优岛到劣岛迁移：表现最好的岛屿把最优个体直接注入表现最差的岛屿。 / Best-to-worst migration: the best-performing island injects its best individuals into the worst-performing island.
 *
 * 与环形迁移相比它更「激进」：不做全环传播，只针对最需要帮助的岛屿，因此收敛更快，
 * 但也更容易让劣岛过早同化、损失多样性。 /
 * Compared with ring migration this is more aggressive: instead of propagating around the ring it targets only the island that
 * needs the most help, converging faster but more likely to homogenise the poor island prematurely.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property amount 每次迁移的个体数量 / Number of individuals per migration
*/
class BestToWorstMigration<ObjValue, V>(
    val amount: Int = 1
) : IslandMigration<ObjValue, V> where V : RealNumber<V>, V : NumberField<V> {

    override fun <T : Individual<ObjValue, V>> invoke(
        iteration: Iteration,
        islands: List<List<T>>,
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        entropy: Generator<Flt64>
    ): List<List<T>> {
        if (islands.size < 2 || islands.any { it.isEmpty() }) {
            return islands
        }

        val ranked = islands.map { island -> rankAscending(island, model) }
        val leaders = ranked.map { it.first() }

        // 逐个挑选最优/最差的岛屿，避免依赖比较器的传递性。
        // Pick the best and worst islands one by one instead of relying on comparator transitivity.
        var source = 0
        var target = 0
        for (index in leaders.indices) {
            if (compareStrictly(model, leaders[index].fitness, leaders[source].fitness) < 0) {
                source = index
            }
            if (compareStrictly(model, leaders[index].fitness, leaders[target].fitness) > 0) {
                target = index
            }
        }
        if (source == target) {
            return islands
        }

        val migrants = ranked[source].take(minOf(amount, ranked[source].size))
        if (migrants.isEmpty()) {
            return islands
        }

        return islands.indices.map { index ->
            if (index != target) {
                islands[index]
            } else {
                ranked[target].dropLast(minOf(migrants.size, ranked[target].size)) + migrants
            }
        }
    }
}

/**
 * 按优度升序排列一个岛屿 / Rank one island by ascending quality
 *
 * 用逐个挑选而不是 `sortedWith`：模型比较器可能是带容差的近似比较，不具备传递性，
 * 直接交给 TimSort 会抛 `Comparison method violates its general contract`。 /
 * Uses repeated selection rather than `sortedWith`: the model comparator may be an approximate one without transitivity, and
 * handing it to TimSort throws `Comparison method violates its general contract`.
 *
 * @param T 个体类型 / Individual type
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param island 岛屿个体 / Island individuals
 * @param model 回调模型接口 / Callback model interface
 * @return 升序排列的个体 / Individuals in ascending order
*/
internal fun <T : Individual<ObjValue, V>, ObjValue, V> rankAscending(
    island: List<T>,
    model: AbstractCallBackModelInterface<*, ObjValue, V>
): List<T> where V : RealNumber<V>, V : NumberField<V> {
    val remaining = island.toMutableList()
    val ranked = ArrayList<T>(remaining.size)
    while (remaining.isNotEmpty()) {
        var bestIndex = 0
        for (index in 1 until remaining.size) {
            if (compareStrictly(model, remaining[index].fitness, remaining[bestIndex].fitness) < 0) {
                bestIndex = index
            }
        }
        ranked.add(remaining.removeAt(bestIndex))
    }
    return ranked
}
