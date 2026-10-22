package fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.model

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.path_compilation.*

/** 注册边界规范化的单列；注册与定价共用此定义。 / Normalized column shared by registration and pricing.
 * @param V 领域数值类型 / Domain numeric type
 * @param C 完整路径载荷类型 / Complete path payload type
 * @property column 业务列 / Business column
 * @property cost 规范化费用 / Normalized cost
 * @property coefficients 规范化行系数 / Normalized row coefficients
 */
data class CompiledPathColumn<V : RealNumber<V>, C>(
    val column: PathColumn<V, C>,
    val cost: Flt64,
    val coefficients: Map<String, Flt64>
)

/** 路径变量与可增量更新的行组件。 / Path variables and incrementally updatable row component.
 * @param rowIds 稳定行标识 / Stable row IDs
 * @param name 命名空间 / Namespace
 * @property variables 当前列变量 / Current column variables
 * @property rows 行表达式 / Row expressions
 */
class PathCompilation(rowIds: List<String>, name: String) {
    val variables = linkedMapOf<String, BinVar>()
    val rows = rowIds.associateWith { LinearExpressionSymbol(Flt64, name = "${name}_row_$it") }

    internal fun add(id: String, coefficients: Map<String, Flt64>, name: String): BinVar {
        val variable = BinVar(name)
        variables[id] = variable
        for ((rowId, symbol) in rows) {
            symbol.flush()
            val polynomial = symbol.asMutable()
            polynomial += coefficients.getValue(rowId) * LinearPolynomial(variable)
        }
        return variable
    }

    internal fun remove(id: String) {
        val variable = variables.remove(id) ?: return
        for (symbol in rows.values) {
            symbol.flush()
            val polynomial = symbol.asMutable()
            val retained = polynomial.monomials.filterNot { it.symbol == variable }
            val constant = polynomial.constant
            polynomial.clear()
            retained.forEach(polynomial::addMonomial)
            polynomial.setConstant(constant)
        }
    }
}
