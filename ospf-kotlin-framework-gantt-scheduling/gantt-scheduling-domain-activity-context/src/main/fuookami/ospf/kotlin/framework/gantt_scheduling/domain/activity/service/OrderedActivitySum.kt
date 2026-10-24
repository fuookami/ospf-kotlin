package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import java.util.TreeMap
import fuookami.ospf.kotlin.math.algebra.number.Flt64

/** 按原 occurrence 顺序求和；仅严格后缀追加可直接扩展前缀。 / Sum in original occurrence order; only strict suffix additions can extend the cached prefix. */
internal class OrderedActivitySum(private val initial: Flt64 = Flt64.zero) {
    private val terms = TreeMap<Int, Flt64>()
    var value = initial
        private set

    fun update(added: List<Pair<Int, Flt64>>, removed: List<Int> = emptyList()) {
        if (added.isEmpty() && removed.isEmpty()) return
        val ordered = added.sortedBy { it.first }
        val append = removed.isEmpty() && (terms.isEmpty() || ordered.first().first > terms.lastKey())
        for (index in removed) terms.remove(index)
        for ((index, quantity) in ordered) terms[index] = quantity
        value = if (append) {
            ordered.fold(value) { sum, term -> sum + term.second }
        } else {
            // 不用减法撤销旧值，避免改变消去误差和溢出行为。 / Do not subtract removed values: cancellation and overflow must retain their original behavior.
            terms.values.fold(initial) { sum, quantity -> sum + quantity }
        }
    }
}
