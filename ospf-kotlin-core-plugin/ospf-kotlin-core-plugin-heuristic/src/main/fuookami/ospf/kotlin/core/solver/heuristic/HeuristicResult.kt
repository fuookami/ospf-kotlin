/**
 * 启发式求解结果 / Heuristic solve result
*/
package fuookami.ospf.kotlin.core.solver.heuristic

import fuookami.ospf.kotlin.math.algebra.concept.*
import fuookami.ospf.kotlin.core.model.basic.Solution

/**
 * 启发式求解状态。 / Heuristic solution status.
*/
enum class HeuristicSolutionStatus {

    /** 可行 / Feasible */
    Feasible,

    /** 不可行 / Infeasible */
    Infeasible
}

/**
 * 启发式求解结果，所有 [HeuristicSolver] 的统一返回。 / Heuristic solve result, the uniform return of every [HeuristicSolver].
 *
 * 与原实现同处一个文件不同：这里只保留「结果」这一件事。原先同文件的 `BasicHeuristicPolicy`
 * 与 `ParticleSwarmHeuristicSolver` 是骨架出现之前的旧入口，已随骨架化删除。 /
 * Unlike the original layout, this file holds only the result. The `BasicHeuristicPolicy` and
 * `ParticleSwarmHeuristicSolver` that shared the file were pre-skeleton entry points and were removed
 * once the skeleton covered them.
 *
 * @param ObjValue 目标值类型 / Objective value type
 * @param V 值类型 / Value type
 * @property bestSolution 最优解（可选）/ Best solution (optional)
 * @property bestObjective 最优目标值（可选）/ Best objective value (optional)
 * @property status 求解状态 / Solution status
 * @property iteration 迭代信息 / Iteration info
*/
data class HeuristicResult<ObjValue, V>(
    val bestSolution: Solution<V>?,
    val bestObjective: ObjValue?,
    val status: HeuristicSolutionStatus,
    val iteration: Iteration
) where V : RealNumber<V>, V : NumberField<V>
