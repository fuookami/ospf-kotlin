package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

/**
 * Ensures at least one cargo from each source is assigned to an early position.
 * 确保每个来源的至少一个货物被分配到早期位置。
 *
 * @property items 货物项列表 / The list of cargo items.
 * @property positions 配载位置列表 / The list of stowage positions.
 * @property stowage 配载分配模型 / The stowage assignment model.
 * @property cargosBySource 来源标识到货物项索引的映射 / Mapping from source identifier to cargo item indices.
 * @property earlyEnd 最后一个早期位置的索引 / The index of the last early position.
*/
class SourceEarlyLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val stowage: Stowage,
    private val cargosBySource: Map<String, List<Int>>,
    private val earlyEnd: Int,
    override val name: String = "source_early_limit"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        // For each source with multiple cargos, at least one must be in early positions
        for ((source, cargos) in cargosBySource) {
            if (cargos.size <= 1) {
                continue
            }

            // sum(x[c][p] for c in cargos, p in 0..earlyEnd) >= 1
            val symbols = cargos.flatMap { c ->
                (0..earlyEnd).map { p -> stowage.stowage[c, p] }
            }
            val lhs = sum(symbols)
            val rhs = Flt64.one

            when (val result = model.addConstraint(
                relation = lhs geq rhs,
                name = "${name}_${source}"
            )) {
                is Ok -> {}
                is Failed -> return Failed(result.error)
                is Fatal -> return Fatal(result.errors)
            }
        }

        return ok
    }
}
