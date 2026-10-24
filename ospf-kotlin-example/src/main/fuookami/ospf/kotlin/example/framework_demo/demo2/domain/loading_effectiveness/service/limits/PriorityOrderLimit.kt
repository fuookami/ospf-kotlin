package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Ensures higher priority cargos get better (lower index) positions using big-M formulation.
 * 使用大 M 公式确保更高优先级的货物获得更好的位置。
 *
 * @property items 货物项列表 / The list of cargo items.
 * @property positions 配载位置列表 / The list of stowage positions.
 * @property stowage 配载分配模型 / The stowage assignment model.
 * @property bigM 约束公式中使用的大 M 常数 / The big-M constant used in constraint formulation.
*/
class PriorityOrderLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    private val bigM: Double = positions.size.toDouble(),
    override val name: String = "priority_order_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        // 相等位置保留原 associate 的末次排名；货物/位置索引则保留首次匹配。
        // Equal positions retain the original associate's last rank, unlike first-match indices.
        val order = positions.sortedBy { it.base.linearLoadingOrder }.withIndex().associate { it.value to it.index }
        // For cargo pairs where priority[i] > priority[j], ensure higher priority gets better position
        for (i in items.indices) {
            for (j in items.indices) {
                if (items[i].cargo.priority.priority <= items[j].cargo.priority.priority) {
                    continue
                }

                // sum((p + bigM) * x[i][p] for all p) + sum((-p + bigM) * x[j][p] for all p) <= 2 * bigM
                var lhs = LinearPolynomial()
                for (p in positions.indices) {
                    val rank = order.getValue(positions[p]).toDouble()
                    lhs += Flt64(rank + bigM) * stowage.stowage[i, p]
                    lhs += Flt64(-rank + bigM) * stowage.stowage[j, p]
                }
                val rhs = Flt64(2.0 * bigM)

                when (val result = model.addConstraint(
                    relation = lhs leq rhs,
                    name = "${name}_${i}_${j}_${items[i].cargo.priority.priority}"
                )) {
                    is Ok -> {}
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
        }

        return ok
    }
}
