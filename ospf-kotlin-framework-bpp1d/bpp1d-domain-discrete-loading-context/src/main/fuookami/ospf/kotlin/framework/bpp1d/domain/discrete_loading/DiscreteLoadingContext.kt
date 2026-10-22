package fuookami.ospf.kotlin.framework.bpp1d.domain.discrete_loading

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.DiscreteLoadingProblem

/** 一维离散装载几何适配。 / One-dimensional discrete-loading geometry adapter. */
data object Bpp1dDiscreteLoading {
    /** 合并真实区间冲突和业务互斥边。 / Merge physical interval conflicts and business conflicts.
     * @param problem 离散输入 / Discrete input
     * @param geometry 每个舱位的显式区间 / Explicit interval for every position
     * @param adapter 数值边界 / Numeric adapter
     * @return 上下文或失败 / Context or failure
     */
    fun <V : FloatingNumber<V>> context(
        problem: DiscreteLoadingProblem<V>,
        geometry: List<LoadingInterval<V>>,
        adapter: PackingValueAdapter<V>
    ): Ret<DiscreteLoadingContext<V>> {
        if (geometry.map { it.id }.toSet() != problem.positions.map { it.id }.toSet()) {
            return Failed(ErrorCode.IllegalArgument, "舱位与几何不匹配 / Position/geometry mismatch")
        }
        val conflicts = when (val result = loadingConflicts(geometry)) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }
        val context = DiscreteLoadingContext(problem.copy(conflicts = problem.conflicts + conflicts), adapter)
        return when (val result = context.validate()) {
            is Ok -> Ok(context)
            is Failed -> Failed(result.error)
            is Fatal -> Fatal(result.errors)
        }
    }
}
