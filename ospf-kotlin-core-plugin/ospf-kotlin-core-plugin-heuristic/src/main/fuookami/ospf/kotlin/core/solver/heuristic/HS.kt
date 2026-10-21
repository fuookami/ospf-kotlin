/**
 * 和声搜索 / Harmony Search
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.SurvivorSelection
import fuookami.ospf.kotlin.core.solver.heuristic.operator.survivor.ElitistSurvivor
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.SolutionUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.operator.update.HarmonySearchUpdate
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 和声搜索，即种群骨架 + `HarmonySearchUpdate`。 / Harmony Search: population skeleton + `HarmonySearchUpdate`.
 *
 * 全部参数与 `ComposedSolvers.harmonySearch` 一一对应，只是少了 `ComposedSolvers.` 前缀。 /
 * Every parameter mirrors `ComposedSolvers.harmonySearch`; only the `ComposedSolvers.` prefix is dropped.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param size 种群规模 / Population size
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param update 解更新算子 / Solution update operator
 * @param survivor 环境选择算子 / Survivor selection operator
 * @param archiveAmount 档案容量 / Archive capacity
 * @param initializer 初始化算子 / Initialization operator
 * @return 求解器 / Solver
*/
fun <ObjValue, V> harmonySearch(
    size: Int,
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    update: SolutionUpdate = HarmonySearchUpdate(),
    survivor: SurvivorSelection = ElitistSurvivor,
    archiveAmount: Int = 1,
    initializer: Initializer = ModelInitializer
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.harmonySearch(
        size = size,
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        update = update,
        survivor = survivor,
        archiveAmount = archiveAmount,
        initializer = initializer
    )
