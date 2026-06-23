@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 从固定批次集合中选择要释放的飞机，使用多种策略。Selects aircraft to free from fixed bunches using multiple strategies.
 *
 * 已移植策略：badReducedCost、highCost、highAircraftChange、random+tabu。
 * 未移植策略（demo4 批次缺少对应指标）：noBusy、highCostDensity、highFlowControlCost、highDelay、airportClose。
 *
 * @property aggregation 批次编译聚合。
 * @property configuration 选择器配置。
 */
class FreeAircraftSelector(
    private val aggregation: Aggregation,
    private val configuration: FreeAircraftSelectorConfiguration = FreeAircraftSelectorConfiguration()
) {
    private val tabuAircrafts: MutableList<Aircraft> = ArrayList()
    private val random: kotlin.random.Random = kotlin.random.Random(configuration.randomSeed)

    /**
     * 从固定批次中选择要释放的飞机。Selects aircraft to free from the given fixed bunches.
     *
     * @param fixedBunches 当前固定的批次集合。
     * @param hiddenExecutors 当前隐藏的执行器。
     * @param shadowPriceMap 影子价格映射。
     * @param model 已求解的线性模型。
     * @return 要释放的飞机集合。
     */
    operator fun invoke(
        fixedBunches: Set<FlightTaskBunch>,
        hiddenExecutors: Set<Aircraft>,
        shadowPriceMap: ShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<Set<Aircraft>> {
        val bunches = fixedBunches.toMutableList()
        val freeAircrafts = hiddenExecutors.toMutableSet()

        when (val ret = freeBadReducedCostAircrafts(bunches, freeAircrafts, shadowPriceMap)) {
            is Ok -> {}
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        when (val ret = freeHighCostAircrafts(bunches, freeAircrafts)) {
            is Ok -> {}
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        when (val ret = freeHighAircraftChangeAircrafts(bunches, freeAircrafts)) {
            is Ok -> {}
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        when (val ret = freeRandAircrafts(bunches, freeAircrafts)) {
            is Ok -> {}
            is Failed -> return Failed(ret.error)
            is Fatal -> return Fatal(ret.errors)
        }

        return Ok(freeAircrafts)
    }

    private fun freeBadReducedCostAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>,
        shadowPriceMap: ShadowPriceMap
    ): Try {
        val reducedCosts = fixedBunches.associateWith { shadowPriceMap.reducedCost(it) }
        fixedBunches.sortByDescending { reducedCosts[it] ?: Flt64.zero }

        var amount = UInt64.zero
        for (bunch in fixedBunches) {
            val aircraft = bunch.aircraft
            val rc = reducedCosts[bunch] ?: Flt64.zero
            if (!freeAircrafts.contains(aircraft) && aircraft.indexed && rc.toDouble() > configuration.fixBar.toDouble()) {
                ++amount
                freeAircrafts.add(aircraft)
            }
            if (amount == configuration.badReducedAmount) {
                break
            }
        }
        return ok
    }

    private fun freeHighCostAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>
    ): Try {
        fixedBunches.sortByDescending { it.cost.solverCostOrNull() ?: Flt64.zero }
        return freeByStrategy(fixedBunches, freeAircrafts, configuration.highCostAmount)
    }

    private fun freeHighAircraftChangeAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>
    ): Try {
        fixedBunches.sortByDescending { it.aircraftChange }
        return freeByStrategy(fixedBunches, freeAircrafts, configuration.highAircraftChangeAmount)
    }

    private fun freeRandAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>
    ): Try {
        if (fixedBunches.isEmpty()) {
            return ok
        }

        val shuffled = fixedBunches.shuffled(random)
        var amount = UInt64.zero
        for (i in 0 until 10090) {
            val bunch = shuffled[shuffled.indices.random(random)]
            val aircraft = bunch.aircraft
            if (!freeAircrafts.contains(aircraft) && aircraft.indexed && amount != configuration.randAmount && !tabu(aircraft)) {
                ++amount
                freeAircrafts.add(aircraft)
                taboo(aircraft)
            }
            if (amount == configuration.randAmount) {
                break
            }
        }
        return ok
    }

    private fun freeByStrategy(
        fixedBunches: List<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>,
        maximumAmount: UInt64
    ): Try {
        var amount = UInt64.zero
        for (bunch in fixedBunches) {
            val aircraft = bunch.aircraft
            if (!freeAircrafts.contains(aircraft) && aircraft.indexed && !tabu(aircraft)) {
                ++amount
                freeAircrafts.add(aircraft)
                taboo(aircraft)
            }
            if (amount == maximumAmount) {
                break
            }
        }
        return ok
    }

    private fun taboo(aircraft: Aircraft) {
        if (tabuAircrafts.size == configuration.tabuAmount.toInt()) {
            tabuAircrafts.removeFirst()
        }
        tabuAircrafts.add(aircraft)
    }

    private fun tabu(aircraft: Aircraft): Boolean {
        return tabuAircrafts.contains(aircraft)
    }
}
