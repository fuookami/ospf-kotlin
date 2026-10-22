package fuookami.ospf.kotlin.framework.bpp.infrastructure

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp.domain.discrete_loading.model.PositionConflict

/** 舱位的纵向区间；空间 ID 隔离不同甲板。 / Longitudinal interval scoped to a physical space.
 * @property spaceId 甲板/独立空间 / Deck or independent space
 * @property start 起点 / Start
 * @property end 终点 / End
 */
data class LoadingInterval<V : FloatingNumber<V>>(
    val id: String,
    val spaceId: String,
    val start: Quantity<V>,
    val end: Quantity<V>
)

/** 扫描纵向候选，仅对活跃区间执行可选二维精确判交。 / Sweep intervals, refining active candidates with an optional 2D test.
 * @param intervals 区间 / Intervals
 * @param intersects 可选精确判交 / Optional exact intersection test
 * @return 内部重叠边或失败 / Interior-overlap edges or failure
 */
fun <V : FloatingNumber<V>> loadingConflicts(
    intervals: List<LoadingInterval<V>>,
    intersects: ((String, String) -> Ret<Boolean>)? = null
): Ret<Set<PositionConflict>> {
    if (intervals.map { it.id }.distinct().size != intervals.size) {
        return Failed(ErrorCode.IllegalArgument, "舱位 ID 重复 / Duplicate position IDs")
    }
    val normalized = arrayListOf<LoadingInterval<V>>()
    for (interval in intervals) {
        val start = interval.start.convertTo(Meter)
        val end = interval.end.convertTo(Meter)
        if (interval.id.isBlank() || interval.spaceId.isBlank() || start == null || end == null ||
            !start.value.isFinite() || !end.value.isFinite() || start.value >= end.value) {
            return Failed(ErrorCode.IllegalArgument, "舱位区间无效 / Invalid loading interval: ${interval.id}")
        }
        normalized.add(interval.copy(start = start, end = end))
    }
    val conflicts = linkedSetOf<PositionConflict>()
    for (space in normalized.groupBy { it.spaceId }.values) {
        val active = arrayListOf<LoadingInterval<V>>()
        for (current in space.sortedWith { a, b -> a.start.value.compareTo(b.start.value) }) {
            active.removeAll { it.end.value <= current.start.value }
            for (other in active) {
                val overlap = if (intersects == null) true else when (val result = intersects(other.id, current.id)) {
                    is Ok -> result.value
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (overlap) conflicts.add(PositionConflict(other.id, current.id))
            }
            active.add(current)
        }
    }
    return Ok(conflicts)
}
