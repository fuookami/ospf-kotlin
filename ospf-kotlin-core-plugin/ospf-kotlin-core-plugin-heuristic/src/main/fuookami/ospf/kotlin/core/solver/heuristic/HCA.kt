/**
 * 爬山算法 / Hill Climbing
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.utils.functional.Generator
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.core.solver.heuristic.operator.stopping.StoppingCriterion
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.Neighborhood
import fuookami.ospf.kotlin.core.solver.heuristic.operator.neighborhood.RandomPerturbation
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.Initializer
import fuookami.ospf.kotlin.core.solver.heuristic.operator.initializer.ModelInitializer
import fuookami.ospf.kotlin.core.solver.heuristic.solver.ComposedSolvers

/**
 * 爬山算法，即轨迹骨架 + `BetterOnlyAcceptance`。 / Hill climbing: trajectory skeleton + `BetterOnlyAcceptance`.
 *
 * 全部参数与 `ComposedSolvers.hillClimbing` 一一对应，只是少了 `ComposedSolvers.` 前缀。 /
 * Every parameter mirrors `ComposedSolvers.hillClimbing`; only the `ComposedSolvers.` prefix is dropped.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @param stopping 终止判据 / Stopping criterion
 * @param converter 值转换器 / Value converter
 * @param entropy 均匀分布熵源 / Uniform entropy source
 * @param neighborhood 邻域算子 / Neighbourhood operator
 * @param initializer 初始化算子 / Initialization operator
 * @return 求解器 / Solver
*/
fun <ObjValue, V> hillClimbing(
    stopping: StoppingCriterion,
    converter: IntoValue<V>,
    entropy: Generator<Flt64>,
    neighborhood: Neighborhood = RandomPerturbation(1),
    initializer: Initializer = ModelInitializer
): HeuristicSolver<ObjValue, V>
    where V : RealNumber<V>, V : NumberField<V> = ComposedSolvers.hillClimbing(
        stopping = stopping,
        converter = converter,
        entropy = entropy,
        neighborhood = neighborhood,
        initializer = initializer
    )
