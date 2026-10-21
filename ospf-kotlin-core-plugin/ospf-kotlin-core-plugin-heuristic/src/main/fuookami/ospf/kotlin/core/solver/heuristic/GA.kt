/**
 * 岛模型遗传算法 / Island-model genetic algorithm
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.SurvivorSelection
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.IslandMigration
import fuookami.ospf.kotlin.core.solver.heuristic.operator.migration.RingMigration
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.ArithmeticCross
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.GaussianMutation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.mutation.Mutation

/**
 * 单个岛屿的规格。 / Specification of one island.
 *
 * 只保留骨架真正读取的两项：岛内种群规模与变异率。旧 `PopulationBuilder` 还带 `eliteAmount` 与
 * `parentAmountRange`，但新骨架从不读它们——留着只会让人以为改了有用。 /
 * Only the two fields the skeleton actually reads are kept: island population size and mutation rate. The
 * old `PopulationBuilder` also carried `eliteAmount` and `parentAmountRange`, which the new skeleton never
 * reads — keeping them would suggest they still matter.
 *
 * @property size 岛内种群规模 / Population size inside the island
 * @property mutationRate 岛内变异率 / Mutation rate inside the island
*/
data class IslandSpec(
    val size: Int,
    val mutationRate: Flt64 = Flt64(0.1)
)

/**
 * 岛模型遗传算法，即 `IslandRunner` 骨架 + N 个种群骨架。 / Island-model genetic algorithm: the
 * `IslandRunner` skeleton plus N population skeletons.
 *
 * 岛模型把一个大种群拆成若干小岛：小种群更容易保住多样性，因此多峰问题上不容易整群陷入同一个局部最优；
 * 每隔 [migrationPeriod] 代按 [migration] 交换个体，好解又不会锁死在某个岛上。 /
 * The island model splits one large population into several islands: smaller populations preserve diversity
 * better, so on multimodal problems the species is less likely to collapse into one local optimum, while
 * exchanging individuals every `migrationPeriod` generations keeps good solutions from being locked inside
 * one island.
 *
 * 全部参数与 `ComposedSolvers.islandGenetic` 的异构岛屿重载一一对应。 /
 * Every parameter mirrors the heterogeneous-island overload of `ComposedSolvers.islandGenetic`.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param islands 各岛屿规格 / Specification of each island
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param migrationPeriod 迁移周期；为零表示不迁移 / Migration period; zero means no migration
 * @param migration 迁移算子 / Migration operator
 * @param update 解更新算子 / Solution update operator
 * @param mutation 变异算子 / Mutation operator
 * @param survivor 环境选择算子 / Survivor selection operator
 * @param archiveAmount 档案容量 / Archive capacity
 * @param initializer 初始化算子 / Initialization operator
 * @return 求解器 / Solver
*/
fun <ObjValue, V> islandGenetic(
    islands: List<IslandSpec>,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    migrationPeriod: UInt64 = UInt64(5),
    migration: IslandMigration<ObjValue, V> = RingMigration(),
    update: SolutionUpdate = ArithmeticCross,
    mutation: Mutation = GaussianMutation(),
    survivor: SurvivorSelection = ElitistSurvivor,
    archiveAmount: Int = 1,
    initializer: Initializer = ModelInitializer
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.islandGenetic(
        islands = islands,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        migrationPeriod = migrationPeriod,
        migration = migration,
        update = update,
        mutation = mutation,
        survivor = survivor,
        archiveAmount = archiveAmount,
        initializer = initializer
    )
