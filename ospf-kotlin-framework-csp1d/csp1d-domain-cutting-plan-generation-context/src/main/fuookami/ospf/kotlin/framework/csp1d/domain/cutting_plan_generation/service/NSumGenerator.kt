package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.*
import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * N 数之和生成器，用于从给定集合中找出所有和为目标的子集组合。
 * N-sum generator, finds all subset combinations from a given collection that sum to a target value.
 *
 * @property source 源数据集合 / Source data collection
 * @property target 目标和值 / Target sum value
 * @property allowEmpty 是否允许空子集 / Whether to allow empty subsets
 * @property allowRepeat 是否允许重复选取元素 / Whether to allow repeated selection of elements
 */
class NSumGenerator<T : Number>(
    private val source: ICollection<T>,
    private val target: T,
    private val allowEmpty: Boolean,
    private val allowRepeat: Boolean,
) : IService {
    override fun invoke(): Result<ICollection<ICollection<T>>> {
        return try {
            val result = LinkedList<ICollection<T>>()
            backtrack(LinkedList(), BigDecimal(target.toDouble()).setScale(10, RoundingMode.HALF_UP), result)
            Result(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 回溯搜索所有满足条件的子集组合。
     * Backtrack search for all subset combinations satisfying the condition.
     *
     * @param current 当前正在构建的子集 / Current subset being constructed
     * @param remaining 剩余需要凑齐的目标值 / Remaining target value to achieve
     * @param result 结果集合 / Result collection
     */
    private fun backtrack(
        current: LinkedList<T>,
        remaining: BigDecimal,
        result: LinkedList<ICollection<T>>,
    ) {
        if (allowEmpty || current.isNotEmpty()) {
            if (BigDecimal(target.toDouble()).setScale(10, RoundingMode.HALF_UP)
                    .subtract(remaining)
                    .setScale(10, RoundingMode.HALF_UP)
                    .compareTo(BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP)) == 0
            ) {
                result.add(LinkedList(current))
            }
        }

        if (remaining.compareTo(BigDecimal.ZERO.setScale(10, RoundingMode.HALF_UP)) <= 0) {
            return
        }

        for (element in source) {
            val elementBD = BigDecimal(element.toDouble()).setScale(10, RoundingMode.HALF_UP)
            if (elementBD.compareTo(remaining) > 0) {
                continue
            }

            if (allowRepeat) {
                current.add(element)
                backtrack(current, remaining.subtract(elementBD), result)
                current.removeLast()
            } else {
                if (!current.contains(element)) {
                    current.add(element)
                    backtrack(current, remaining.subtract(elementBD), result)
                    current.removeLast()
                }
            }
        }
    }
}
