package fuookami.ospf.kotlin.framework.bpp1d.domain.packing

import fuookami.ospf.kotlin.utils.error.ErrorCode
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp1d.domain.item.model.*

/** 箱内自由区间。 / Free interval in a bin.
 * @property binId 箱体 ID / Bin ID
 * @property start 起点 / Start
 * @property length 可用长度 / Available length
 */
data class Remnant<V : FloatingNumber<V>>(val binId: String, val start: Quantity<V>, val length: Quantity<V>)

/** 一维装箱物理结果。 / Physical packing result.
 * @property plans 放置结果 / Placements
 * @property remnants 尾端余量，不将物品间距算为可用余料 / Tail remnants, excluding spacing
 * @property materials 物料汇总 / Material totals
 * @property utilization 长度利用率 / Length utilization
 */
data class PackingResult<V : FloatingNumber<V>>(
    val plans: List<PackingPlan<V>>,
    val remnants: List<Remnant<V>>,
    val materials: Map<String, UInt64>,
    val utilization: V
)

/** 最终需求、库存、几何复核与物理结果提取。 / Final demand, stock, geometry verification and extraction. */
class PackingContext<V : FloatingNumber<V>> {
    /** 按确定顺序展开方案并复核精确需求。 / Expand patterns deterministically and verify exact demand.
     * @param problem 输入 / Input
     * @param usages 整数份数 / Integer usages
     * @param expansionLimit 输出展开预算 / Output expansion budget
     * @return 物理结果或失败 / Physical result or failure
     */
    fun pack(
        problem: PackingProblem<V>,
        usages: Map<PackingPattern<V>, UInt64>,
        expansionLimit: Int = 1000000
    ): Ret<PackingResult<V>> {
        val validation = problem.validate()
        if (validation is Failed) return Failed(validation.error)
        if (validation is Fatal) return Fatal(validation.errors)
        if (expansionLimit < 0) return Failed(ErrorCode.IllegalArgument, "展开预算不得为负 / Expansion budget must not be negative")
        val totals = problem.items.associate { it.id to 0uL }.toMutableMap()
        val stock = problem.binTypes.associate { it.id to 0uL }.toMutableMap()
        var expanded = 0uL
        for ((pattern, count) in usages) {
            val result = problem.validatePattern(pattern)
            if (result is Failed) return Failed(result.error)
            if (result is Fatal) return Fatal(result.errors)
            for ((id, amount) in pattern.amounts) {
                val copies = count.toULong()
                if (copies > 0uL && amount.toULong() > expansionLimit.toULong() / copies) return Failed(ErrorCode.IllegalArgument, "输出展开预算超限 / Output expansion budget exceeded")
                val pieces = amount.toULong() * copies
                expanded += pieces
                if (expanded > expansionLimit.toULong()) return Failed(ErrorCode.IllegalArgument, "输出展开预算超限 / Output expansion budget exceeded")
                totals[id] = totals.getValue(id) + pieces
            }
            stock[pattern.binTypeId] = stock.getValue(pattern.binTypeId) + count.toULong()
        }
        if (problem.items.any { totals.getValue(it.id) != it.amount.toULong() } || problem.binTypes.any { it.inventory != null && stock.getValue(it.id) > it.inventory!!.toULong() }) {
            return Failed(ErrorCode.ApplicationError, "需求或库存复核失败 / Demand or inventory verification failed")
        }
        val plans = arrayListOf<PackingPlan<V>>()
        val remnants = arrayListOf<Remnant<V>>()
        val materials = linkedMapOf<String, UInt64>()
        val sample = problem.binTypes.first().length.value
        var totalLength = sample.constants.zero
        var packedLength = sample.constants.zero
        for ((pattern, count) in usages) {
            val type = problem.binTypes.first { it.id == pattern.binTypeId }
            val gap = problem.spacing.convertTo(type.length.unit)!!.value
            repeat(count.toULong().toInt()) {
                var cursor = type.length.value.constants.zero
                val placements = arrayListOf<Placement<V>>()
                for ((id, amount) in pattern.amounts.toSortedMap()) {
                    val item = problem.items.first { it.id == id }
                    val length = item.length.convertTo(type.length.unit)!!
                    repeat(amount.toULong().toInt()) {
                        if (placements.isNotEmpty()) cursor += gap
                        placements.add(Placement(id, Quantity(cursor, type.length.unit), length))
                        cursor += length.value
                        packedLength += item.length.convertTo(Meter)!!.value
                        for ((material, quantity) in item.materials) {
                            val previous = materials[material] ?: UInt64.zero
                            if (quantity.toULong() > ULong.MAX_VALUE - previous.toULong()) return Failed(ErrorCode.IllegalArgument, "物料汇总溢出 / Material summary overflow")
                            materials[material] = previous + quantity
                        }
                    }
                }
                val bin = Bin("bin_${plans.size}", type)
                plans.add(PackingPlan(bin, placements))
                if (cursor < type.length.value) remnants.add(Remnant(
                    binId = bin.id,
                    start = Quantity(cursor, type.length.unit),
                    length = Quantity(type.length.value - cursor, type.length.unit)
                ))
                totalLength += type.length.convertTo(Meter)!!.value
            }
        }
        return Ok(PackingResult(
            plans = plans,
            remnants = remnants,
            materials = materials,
            utilization = if (totalLength == sample.constants.zero) sample.constants.zero else packedLength / totalLength
        ))
    }
}
