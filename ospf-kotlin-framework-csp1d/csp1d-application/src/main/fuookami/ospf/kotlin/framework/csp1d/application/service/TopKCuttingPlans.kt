package fuookami.ospf.kotlin.framework.csp1d.application.service

import java.util.PriorityQueue
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.toFlt64
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.CuttingPlan

/**
 * 切割方案 Top-K 容器 / Cutting plan Top-K container
 *
 * @param V 数值类型 / Numeric value type
 * @property limit 保留上限 / Top-K limit
 */
class TopKCuttingPlans<V : RealNumber<V>>(
    private val limit: Int
) {
    private val comparator = compareBy<CuttingPlan<V>> { plan ->
        plan.usedWidth?.value?.toFlt64()?.toDouble() ?: Double.NEGATIVE_INFINITY
    }
    private val heap = PriorityQueue(comparator)

    /**
     * 插入单个方案 / Offer one plan
     *
     * @param plan 切割方案 / Cutting plan
     */
    fun offer(plan: CuttingPlan<V>) {
        if (limit <= 0) {
            return
        }
        if (heap.size < limit) {
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
}

