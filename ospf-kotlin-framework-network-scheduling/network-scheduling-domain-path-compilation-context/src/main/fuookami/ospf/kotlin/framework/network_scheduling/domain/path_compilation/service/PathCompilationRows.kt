package fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.LinearExpressionSymbol

/** 单行边界管线，行键同时用于对偶提取。 / Row-bound pipeline whose key is also used for dual extraction.
 * @property id 稳定行键 / Stable row key
 * @property symbol 可增量更新的表达式 / Incremental expression
 * @property lower 可选下界 / Optional lower bound
 * @property upper 可选上界 / Optional upper bound
 */
class PathCompilationRows(
    val id: String,
    val symbol: LinearExpressionSymbol<Flt64>,
    val lower: Flt64?,
    val upper: Flt64?
) {
    internal val constraints = mutableListOf<MathConstraint>()

    /** 注册边界。 / Register bounds.
     * @param model 当前模型 / Current model
     * @return 注册结果 / Registration result
     */
    operator fun invoke(model: LinearMetaModel<Flt64>): Try {
        val start = model.constraints.size
        val result = if (lower != null && upper != null && lower == upper) {
            model.addConstraint(symbol eq lower, name = "${symbol.name}_eq")
        } else {
            run(
                { lower?.let { model.addConstraint(symbol geq it, name = "${symbol.name}_lower") } ?: ok },
                { upper?.let { model.addConstraint(symbol leq it, name = "${symbol.name}_upper") } ?: ok }
            )
        }
        if (result.ok) constraints.addAll(model.constraints.drop(start))
        return result
    }
}
