package fuookami.ospf.kotlin.framework.gantt_scheduling.model

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.concept.*

data class CostItem(
    val tag: String,
    val value: Flt64? = null,
    val message: String? = null
) : Copyable<CostItem> {
    val valid get() = value != null

    override fun copy() = CostItem(tag, value, message)
}

data class Cost(
    private val _items: MutableList<CostItem> = ArrayList(),
    var sum: Flt64? = Flt64.zero
) : Iterable<CostItem>, Copyable<Cost> {
    val items: List<CostItem> get() = _items
    val valid: Boolean get() = sum != null

    operator fun plusAssign(rhs: CostItem) {
        if (!rhs.valid || rhs.value!! neq Flt64.zero) {
            _items.add(rhs)
        }

        if (rhs.valid) {
            sum = sum?.plus(rhs.value!!)
        }
    }

    operator fun plusAssign(rhs: Cost) {
        _items.addAll(rhs._items.asIterable().filter { !it.valid || it.value!! neq Flt64.zero })

        sum = if (this.valid && rhs.valid) {
            sum!! + rhs.sum!!
        } else {
            null
        }
    }

    override fun iterator() = _items.iterator()

    override fun copy() = Cost(_items.map { it.copy() }.toMutableList(), sum)
}
