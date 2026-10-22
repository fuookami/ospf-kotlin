package fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.mechanism.AbstractLinearMetaModel
import fuookami.ospf.kotlin.framework.model.Pipeline
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.*

/** 分配唯一性、容量及整体占用联动。 / Assignment, capacity and whole-position occupancy links.
 * @property problem 领域输入 / Domain input
 * @property aggregation 注册构件 / Registration artifacts
 * @property capacities 已统一单位的重量上限 / Normalized weight capacities
 */
class LoadingCapacityLimit<V : FloatingNumber<V>>(
    private val problem: DiscreteLoadingProblem<V>,
    private val aggregation: LoadingAggregation,
    private val capacities: Map<String, Flt64>,
    override val name: String = "loading_capacity"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for (item in problem.items) {
            val loaded = aggregation.loaded.getValue(item.id)
            val result = model.addConstraint(
                relation = if (item.required || item.fixedPositionId != null) loaded eq Flt64.one else loaded leq Flt64.one,
                name = "${name}_item_${item.id}"
            )
            if (result !is Ok) return result
        }
        for (position in problem.positions) {
            val active = aggregation.occupied.getValue(position.id)
            val countBound = minOf(position.maxCount.toULong(), aggregation.assignments.keys.count { it.positionId == position.id }.toULong())
            var result = model.addConstraint(
                relation = aggregation.counts.getValue(position.id) leq Flt64(countBound.toDouble()) * active,
                name = "${name}_count_${position.id}"
            )
            if (result !is Ok) return result
            result = model.addConstraint(
                relation = aggregation.weights.getValue(position.id) leq capacities.getValue(position.id) * active,
                name = "${name}_weight_${position.id}"
            )
            if (result !is Ok) return result
        }
        return ok
    }
}

/** 重叠舱位及布局选择约束。 / Overlap and layout-selection constraints.
 * @property problem 输入 / Input
 * @property aggregation 注册构件 / Registration artifacts
 * @property index 已验证团覆盖 / Verified clique cover
 */
class LoadingConflictLimit<V : FloatingNumber<V>>(
    private val problem: DiscreteLoadingProblem<V>,
    private val aggregation: LoadingAggregation,
    private val index: ConflictIndex,
    override val name: String = "loading_conflict"
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((i, clique) in index.cliques.withIndex()) {
            val result = model.addConstraint(
                relation = sum(clique.map { aggregation.occupied.getValue(it) }) leq Flt64.one,
                name = "${name}_clique_$i"
            )
            if (result !is Ok) return result
        }
        for (group in problem.layoutGroups) {
            var result = model.addConstraint(
                relation = sum(group.layouts.map { aggregation.layouts.getValue(group.id to it.id) }) eq Flt64.one,
                name = "${name}_layout_${group.id}"
            )
            if (result !is Ok) return result
            for (positionId in group.positionIds) {
                val choices = group.layouts.filter { positionId in it.positionIds }
                result = model.addConstraint(
                    relation = LinearPolynomial(aggregation.occupied.getValue(positionId)) leq
                        sum(choices.map { aggregation.layouts.getValue(group.id to it.id) }),
                    name = "${name}_layout_${group.id}_$positionId"
                )
                if (result !is Ok) return result
            }
        }
        return ok
    }
}
