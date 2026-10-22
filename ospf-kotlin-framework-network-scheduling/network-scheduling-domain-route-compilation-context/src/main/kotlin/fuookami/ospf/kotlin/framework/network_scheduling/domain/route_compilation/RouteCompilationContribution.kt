package fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.Route
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.pricing.RouteArcCoefficientProvider
import fuookami.ospf.kotlin.framework.network_scheduling.domain.route_compilation.model.RouteCompilation

/** 路线扩展主问题中的单列视图。 / A single-column view for route master-problem extensions.
 * @property route 路线 / Route
 * @property variable 对应的 RMP 列变量 / Corresponding RMP column variable
 * @property coefficients 按路线扩展行键汇总的领域系数 / Route coefficients in domain values, keyed by extra-row IDs
 * @property solverCoefficients 同一系数的 solver 边界值 / Solver-boundary values of the same coefficients
 */
class RouteCompilationColumn<V : RealNumber<V>>(
    val route: Route<V>,
    val variable: LinearPolynomial<Flt64>,
    coefficients: Map<String, V>,
    solverCoefficients: Map<String, Flt64>
) {
    val coefficients: Map<String, V> = coefficients.toMap()
    val solverCoefficients: Map<String, Flt64> = solverCoefficients.toMap()
}

/** VRPTW 额外主问题贡献的列生成生命周期扩展。 / Lifecycle extension for extra VRPTW master-problem contributions. */
interface RouteCompilationContribution<V : RealNumber<V>> : RouteArcCoefficientProvider<V> {
    /** 稳定扩展名称，同时用于提取结果键。 / Stable extension name, also used as its extraction-result key. */
    val name: String

    /** 注册静态主问题结构。 / Register static master-problem structure.
     * @param compilation 基础路线编译模型 / Base route compilation model
     * @param model 当前线性主问题 / Current linear master problem
     * @return 操作结果 / Operation result
     */
    fun register(compilation: RouteCompilation<V>, model: AbstractLinearMetaModel<Flt64>): Try = ok

    /** 为新增路线列更新扩展表达式。 / Update extra expressions for newly added route columns.
     * @param columns 新增路线列和共享系数定义 / New route columns and their shared coefficient definitions
     * @param model 当前线性主问题 / Current linear master problem
     * @return 操作结果 / Operation result
     */
    fun addColumns(columns: List<RouteCompilationColumn<V>>, model: AbstractLinearMetaModel<Flt64>): Try = ok

    /** 移除路线列对扩展表达式的贡献。 / Remove route-column contributions from extra expressions.
     * @param columns 被移除路线列和原系数 / Removed route columns and their original coefficients
     * @param model 当前线性主问题 / Current linear master problem
     * @return 操作结果 / Operation result
     */
    fun removeColumns(columns: List<RouteCompilationColumn<V>>, model: AbstractLinearMetaModel<Flt64>): Try = ok

    /** 从当前主问题 dual solution 提取扩展定价行的对偶快照。
     * Extract the current dual snapshot for extra pricing rows.
     * @param model 当前线性主问题 / Current linear master problem
     * @param shadowPrices solver 对偶解 / Solver dual solution
     * @return 行稳定键到 solver 有符号对偶的映射 / Stable row IDs to signed solver duals
     */
    fun refreshShadowPrices(
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Ret<Map<String, Flt64>> = ok(emptyMap())

    /** 提取扩展结果；调用方按 contribution.name 收集异构结果。
     * Extract extension results; callers collect heterogeneous values by contribution name.
     * @param model 最终线性主问题 / Final linear master problem
     * @param routeValues 路线列及其解值 / Route columns and their solution values
     * @return 扩展结果或提取失败 / Extension result or extraction failure
     */
    fun extract(
        model: AbstractLinearMetaModel<Flt64>,
        routeValues: List<Pair<Route<V>, Flt64>>
    ): Ret<Any?> = ok(Unit)
}
