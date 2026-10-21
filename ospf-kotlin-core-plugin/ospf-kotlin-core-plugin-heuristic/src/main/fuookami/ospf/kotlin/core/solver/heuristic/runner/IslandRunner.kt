/**
 * 岛模型搜索骨架 / Island-model search skeleton
*/
package fuookami.ospf.kotlin.core.solver.heuristic.runner

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.callback.AbstractCallBackModelInterface
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.*
import fuookami.ospf.kotlin.core.solver.heuristic.operator.archive.Archive
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.IslandMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.NoMigration

/**
 * 岛模型骨架：若干独立子种群各自演化，周期性交换个体。 / Island-model skeleton: several independent sub-populations evolve separately and exchange individuals periodically.
 *
 * 它是 [PopulationRunner] 的**组合**而非重写：每个岛屿就是一个 `PopulationRunner`，本类只负责
 * 「遍历岛屿」与「按周期触发迁移」两件事。因此新增岛屿数量、迁移策略都不需要改动单岛的搜索逻辑。 /
 * It **composes** [PopulationRunner] rather than reimplementing it: each island is one `PopulationRunner`, and this class only
 * owns two things—iterating the islands and triggering migration on schedule. Adding islands or changing the migration strategy
 * therefore never touches the single-island search logic.
 *
 * 岛模型的意义：多个较小的种群比一个大的种群更容易保住多样性，因此在多峰问题上更不容易整群陷入同一个局部最优；
 * 而迁移保证好解不会锁死在某个岛屿里。 / Why islands: several smaller populations preserve diversity better than one large one, so on multimodal problems the species is
 * less likely to collapse into a single local optimum, while migration keeps good solutions from being locked inside one island.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property islands 各岛屿的种群搜索骨架 / Population skeleton of each island
 * @property migrationPeriod 迁移周期（迭代数）；为零表示不迁移 / Migration period in iterations; zero means no migration
 * @property migration 迁移算子 / Migration operator
 * @property entropy 均匀分布熵源 / Uniform entropy source
*/
class IslandRunner<ObjValue, V>(
    val islands: List<PopulationRunner<ObjValue, V>>,
    val migrationPeriod: UInt64 = UInt64(5),
    val migration: IslandMigration<ObjValue, V> = NoMigration(),
    private val entropy: Generator<Flt64>,
    archive: Archive<ObjValue, V>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>
) : AbstractRunner<List<List<Individual<ObjValue, V>>>, ObjValue, V>(archive, stopping, converter)
    where V : RealNumber<V>, V : NumberField<V> {

    override suspend fun initialize(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration
    ): List<List<Individual<ObjValue, V>>> {
        return islands.map { island -> island.openIsland(model, iteration) }
    }

    override suspend fun advance(
        model: AbstractCallBackModelInterface<*, ObjValue, V>,
        iteration: Iteration,
        state: List<List<Individual<ObjValue, V>>>
    ): List<List<Individual<ObjValue, V>>> {
        if (state.isEmpty()) {
            return state
        }

        // 各岛屿独立推进一代；岛屿之间没有共享可变状态，因此可以并行。
        // Each island advances one generation independently; islands share no mutable state, so they can run in parallel.
        val advanced = state.mapIndexed { index, island ->
            val runner = islands.getOrNull(index)
            if (runner == null) {
                island
            } else {
                runner.stepIsland(model, iteration, island)
            }
        }

        if (migrationPeriod == UInt64.zero || iteration.iteration % migrationPeriod != UInt64.zero) {
            return advanced
        }
        return migration(
            iteration = iteration,
            islands = advanced,
            model = model,
            entropy = entropy
        )
    }

    override fun membersOf(state: List<List<Individual<ObjValue, V>>>): List<Individual<ObjValue, V>> {
        return state.flatten()
    }
}
