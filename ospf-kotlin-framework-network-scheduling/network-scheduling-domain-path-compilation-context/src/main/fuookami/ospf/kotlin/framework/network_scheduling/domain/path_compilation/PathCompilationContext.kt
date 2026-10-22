package fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.model.*

/** 通用主问题行；相同上下界表示精确覆盖。 / Generic master row; equal bounds mean exact coverage.
 * @property id 稳定行标识 / Stable row identity
 * @property unit 系数与边界的计量单位 / Coefficient and bound unit
 * @property lower 可选下界 / Optional lower bound
 * @property upper 可选上界 / Optional upper bound
 */
data class PathCompilationRow<V : RealNumber<V>>(
    val id: String,
    val unit: PhysicalUnit,
    val lower: Quantity<V>? = null,
    val upper: Quantity<V>? = null
)

/** 一条不可拆候选路径，使用一个二元变量选择。 / An indivisible candidate path selected by one binary variable.
 * @property id 稳定列标识，相同 ID 必须代表相同载荷 / Stable identity; equal IDs must denote equal payloads
 * @property payload 完整业务路径，保留内部活动与时间 / Complete business path retaining activities and timing
 * @property cost 完整费用，允许起止及非可加贡献 / Full cost including start, terminal and non-additive contributions
 * @property coefficients 按行标识汇总的完整用量，缺省为零 / Complete usage by row identity, zero when omitted
 */
class PathColumn<V : RealNumber<V>, C>(
    val id: String,
    val payload: C,
    val cost: Quantity<V>,
    coefficients: Map<String, Quantity<V>>
) {
    val coefficients = coefficients.toMap()
}

/** 不持有模型状态的有限候选主问题。 / Finite candidate master problem without model state.
 * @property rows 需求、配额、容量或服务覆盖行 / Demand, quota, capacity or service-coverage rows
 * @property columns 初始候选 / Initial candidates
 * @property costUnit 费用口径 / Cost unit
 */
class PathCompilationProblem<V : RealNumber<V>, C>(
    rows: List<PathCompilationRow<V>>,
    columns: List<PathColumn<V, C>>,
    val costUnit: PhysicalUnit
) {
    val rows = rows.toList()
    val columns = columns.toList()
}

/** 提取的有限候选集结果，不包含全图最优证明。 / Extracted finite-pool result, without full-network optimality proof.
 * @property selected 选中的完整路径 / Selected complete paths
 * @property cost 总费用 / Total cost
 * @property usage 行用量 / Row usage
 * @property candidateIds 本次最终候选范围 / Final candidate scope
 */
data class PathCompilationSolution<V : RealNumber<V>, C>(
    val selected: List<PathColumn<V, C>>,
    val cost: Quantity<V>,
    val usage: Map<String, Quantity<V>>,
    val candidateIds: Set<String>
)

/** 每个模型独立的通用路径选择入口。 / Independent per-model entry for generic path selection.
 * @property problem 有限候选问题 / Finite candidate problem
 * @property adapter 数值与单位边界 / Numeric and unit boundary
 * @property name 命名空间 / Namespace
 * @property aggregation 列生命周期聚合 / Column-lifecycle aggregation
 */
class PathCompilationContext<V : RealNumber<V>, C>(
    val problem: PathCompilationProblem<V, C>,
    val adapter: NetworkSchedulingSolverValueAdapter<V>,
    val name: String = "path_compilation"
) {
    val aggregation = PathCompilationAggregation(problem, adapter, name)

    /** 注册共享主问题。 / Register the shared master.
     * @param model 当前模型 / Current model
     * @return 注册结果 / Registration result
     */
    fun register(model: LinearMetaModel<Flt64>): Try = aggregation.register(model)

    /** 添加列，同时更新行与目标。 / Add columns, updating rows and objective.
     * @param columns 完整候选 / Complete candidates
     * @param model 当前模型 / Current model
     * @return 实际新增的列 / Actually added columns
     */
    fun addColumns(columns: List<PathColumn<V, C>>, model: LinearMetaModel<Flt64>): Ret<List<PathColumn<V, C>>> =
        aggregation.addColumns(columns, model)

    /** 删除列并清除目标和符号项。 / Remove columns and clear objective and symbol terms.
     * @param ids 稳定列标识 / Stable column IDs
     * @param model 当前模型 / Current model
     * @return 删除结果 / Removal result
     */
    fun removeColumns(ids: Set<String>, model: LinearMetaModel<Flt64>): Try = aggregation.removeColumns(ids, model)

    /** 从已求解模型提取整条路径。 / Extract whole paths from a solved model.
     * @param model 当前模型 / Current model
     * @return 有限候选集结果 / Finite-pool result
     */
    fun extractSolution(model: LinearMetaModel<Flt64>): Ret<PathCompilationSolution<V, C>> = aggregation.extractSolution(model)
}
