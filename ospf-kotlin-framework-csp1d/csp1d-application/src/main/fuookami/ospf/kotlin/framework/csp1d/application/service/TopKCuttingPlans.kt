package fuookami.ospf.kotlin.framework.csp1d.application.service

import java.util.PriorityQueue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

/**
 * 切割方案 Top-K 容器 / Cutting plan Top-K container
 *
 * @param V 数值类型 / Numeric value type
 * @property limit 保留上限 / Top-K limit
 */
class TopKCuttingPlans<V : RealNumber<V>>(
    private val limit: Int64
) {
    private val comparator = Comparator<CuttingPlan<V>> { left, right ->
        compareScore(left.usedWidth?.value?.toFlt64(), right.usedWidth?.value?.toFlt64())
    }
    private val heap = PriorityQueue(comparator)

    /**
     * 插入单个方案 / Offer one plan
     *
     * @param plan 切割方案 / Cutting plan
     */
    fun offer(plan: CuttingPlan<V>) {
        if (limit.toLong() <= 0L) {
            return
        }
        if (heap.size.toLong() < limit.toLong()) {
            heap.offer(plan)
            return
        }
        val worst = heap.peek() ?: return
        if (comparator.compare(plan, worst) > 0) {
            heap.poll()
            heap.offer(plan)
        }
    }

    /**
     * 批量插入方案 / Offer plans in batch
     *
     * @param plans 方案集合 / Plan collection
     */
    fun offerAll(plans: Iterable<CuttingPlan<V>>) {
        for (plan in plans) {
            offer(plan)
        }
    }

    /**
     * 导出排序结果 / Export sorted result
     *
     * @return 按评分降序的方案列表 / Plans sorted by score in descending order
     */
    fun toSortedList(): List<CuttingPlan<V>> {
        return heap.toList().sortedWith(comparator.reversed())
    }

    private fun compareScore(left: Flt64?, right: Flt64?): Int {
        return when {
            left == null && right == null -> 0
            left == null -> -1
            right == null -> 1
            left < right -> -1
            left > right -> 1
            else -> 0
        }
    }
}
