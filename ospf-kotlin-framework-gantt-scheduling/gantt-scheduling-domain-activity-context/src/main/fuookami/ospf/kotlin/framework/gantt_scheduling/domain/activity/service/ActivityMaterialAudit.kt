@file:OptIn(kotlin.time.ExperimentalTime::class)
package fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.service

import kotlin.time.Instant
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.framework.gantt_scheduling.domain.activity.model.*

/** 由选中事件重放库存，不读取累计库存模型行。 / Replay selected inventory events without reading cumulative model rows. */
internal class ActivityMaterialAudit<V : RealNumber<V>>(private val data: CompiledActivityProblem<V>) {
    fun levels(selected: Set<Int>): Map<String, List<Pair<Instant, Flt64>>> {
        val events = data.materialEvents + selected.flatMap { data.choices[it].materialEvents }
        return data.materials.mapValues { (id, material) ->
            val changes = events.filter { it.material == id }.groupBy { it.time }
            val times = (listOf(data.problem.horizon.start, data.problem.horizon.end) + changes.keys).distinct().sorted()
            var stock = material.initial
            times.map { time ->
                stock += changes[time].orEmpty().fold(Flt64.zero) { sum, event -> sum + event.quantity }
                time to stock
            }
        }
    }

    fun violations(selected: Set<Int>): List<String> {
        val violations = mutableListOf<String>()
        val levels = levels(selected)
        for ((id, material) in data.materials) {
            fun invalid(stock: Flt64): Boolean = !stock.toDouble().isFinite() || (stock ls material.minimum) ||
                material.maximum?.let { stock gr it } == true
            if (invalid(material.initial)) violations.add("stock_opening:$id")
            for ((time, stock) in levels.getValue(id)) {
                if (invalid(stock)) violations.add("stock:$id:$time")
            }
        }
        return violations
    }

    fun extract(selected: Set<Int>, boundary: ActivityValueBoundary<V>): Ret<List<ActivityMaterialInventory<V>>> {
        val inventory = mutableListOf<ActivityMaterialInventory<V>>()
        for ((id, points) in levels(selected)) {
            val material = data.materials.getValue(id)
            val levels = mutableListOf<ActivityInventoryLevel<V>>()
            for ((time, stock) in points) {
                when (val result = boundary.quantity(stock, material.source.initial.unit)) {
                    is Ok -> levels.add(ActivityInventoryLevel(time, result.value))
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
            }
            inventory.add(ActivityMaterialInventory(
                material = id,
                initial = material.source.initial,
                levels = levels
            ))
        }
        return ok(inventory.toList())
    }
}
