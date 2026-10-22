package fuookami.ospf.kotlin.framework.bpp2d.domain.discrete_loading

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.geometry2d.Box2Geometry
import fuookami.ospf.kotlin.framework.bpp.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.DiscreteLoadingContext
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.DiscreteLoadingProblem

/** 显式二维舱位边界。 / Explicit two-dimensional position boundary.
 * @property spaceId 甲板/独立空间 / Deck or independent space
 * @property bounds 共享几何包围盒 / Shared geometry box
 */
data class LoadingRectangle<V : FloatingNumber<V>>(
    val id: String,
    val spaceId: String,
    val bounds: Box2Geometry<V>
)

/** 二维离散装载几何适配。 / Two-dimensional discrete-loading geometry adapter. */
data object Bpp2dDiscreteLoading {
    /** 纵向扫描加共享二维精确判交。 / Longitudinal sweep plus shared exact 2D intersection.
     * @param problem 离散输入 / Discrete input
     * @param geometry 每个舱位的显式矩形 / Explicit rectangle for every position
     * @param adapter 数值边界 / Numeric adapter
     * @return 上下文或失败 / Context or failure
     */
    fun <V : FloatingNumber<V>> context(
        problem: DiscreteLoadingProblem<V>,
        geometry: List<LoadingRectangle<V>>,
        adapter: PackingValueAdapter<V>
    ): Ret<DiscreteLoadingContext<V>> {
        if (geometry.map { it.id }.toSet() != problem.positions.map { it.id }.toSet()) {
            return Failed(ErrorCode.IllegalArgument, "舱位与几何不匹配 / Position/geometry mismatch")
        }
        val normalized = linkedMapOf<String, Box2Geometry<V>>()
        val intervals = arrayListOf<LoadingInterval<V>>()
        for (position in geometry) {
            val box = position.bounds
            val x = box.x.convertTo(box.width.unit)
            val y = box.y.convertTo(box.width.unit)
            val height = box.height.convertTo(box.width.unit)
            if (x == null || y == null || height == null || !x.value.isFinite() || !y.value.isFinite() ||
                !box.width.value.isFinite() || !height.value.isFinite() || box.width.value <= box.width.value.constants.zero || height.value <= height.value.constants.zero ||
                !(x.value + box.width.value).isFinite() || !(y.value + height.value).isFinite()) {
                return Failed(ErrorCode.IllegalArgument, "二维舱位边界无效 / Invalid 2D position bounds: ${position.id}")
            }
            val canonical = Box2Geometry.of(x = x, y = y, width = box.width, height = height)
            normalized[position.id] = canonical
            intervals.add(LoadingInterval(
                id = position.id,
                spaceId = position.spaceId,
                start = x,
                end = Quantity(x.value + box.width.value, x.unit)
            ))
        }
        val conflicts = when (val result = loadingConflicts(intervals) { a, b -> normalized.getValue(a).overlapped(normalized.getValue(b)) }) {
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
