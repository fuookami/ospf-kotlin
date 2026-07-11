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
 * @property aggregation The bunch compilation aggregation / 批次编译聚合
 * @property configuration The selector configuration / 选择器配置
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
     * @param fixedBunches The current set of fixed bunches / 当前固定的批次集合
     * @param hiddenExecutors The current set of hidden executors / 当前隐藏的执行器集合
     * @param shadowPriceMap The shadow price map / 影子价格映射
     * @param model The solved linear model / 已求解的线性模型
     * @return The set of aircraft to free, or an error / 要释放的飞机集合，或错误
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

    /**
     * Frees aircraft whose bunches have bad (positive) reduced cost. / 释放其批次具有较差（正）检验成本的飞机。
     *
     * @param fixedBunches The mutable list of fixed bunches, sorted in place / 固定批次的可变列表，原地排序
     * @param freeAircrafts The mutable set of aircraft to free / 待释放飞机的可变集合
     * @param shadowPriceMap The shadow price map for computing reduced costs / 用于计算检验成本的影子价格映射
     * @return Success or failure / 成功或失败
    */
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

    /**
     * Frees aircraft whose bunches have the highest cost. / 释放其批次具有最高成本的飞机。
     *
     * @param fixedBunches The mutable list of fixed bunches, sorted in place / 固定批次的可变列表，原地排序
     * @param freeAircrafts The mutable set of aircraft to free / 待释放飞机的可变集合
     * @return Success or failure / 成功或失败
    */
    private fun freeHighCostAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>
    ): Try {
        fixedBunches.sortByDescending { it.cost.solverCostOrNull() ?: Flt64.zero }
        return freeByStrategy(fixedBunches, freeAircrafts, configuration.highCostAmount)
    }

    /**
     * Frees aircraft whose bunches have the highest aircraft change count. / 释放其批次具有最高换机次数的飞机。
     *
     * @param fixedBunches The mutable list of fixed bunches, sorted in place / 固定批次的可变列表，原地排序
     * @param freeAircrafts The mutable set of aircraft to free / 待释放飞机的可变集合
     * @return Success or failure / 成功或失败
    */
    private fun freeHighAircraftChangeAircrafts(
        fixedBunches: MutableList<FlightTaskBunch>,
        freeAircrafts: MutableSet<Aircraft>
    ): Try {
        fixedBunches.sortByDescending { it.aircraftChange }
        return freeByStrategy(fixedBunches, freeAircrafts, configuration.highAircraftChangeAmount)
    }

    /**
     * Frees aircraft randomly with tabu exclusion. / 随机释放飞机，带禁忌排除。
     *
     * @param fixedBunches The mutable list of fixed bunches / 固定批次的可变列表
     * @param freeAircrafts The mutable set of aircraft to free / 待释放飞机的可变集合
     * @return Success or failure / 成功或失败
    */
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

    /**
     * Frees aircraft from sorted bunches up to a maximum amount, respecting tabu. / 从已排序的批次中释放飞机，直到达到最大数量，遵守禁忌。
     *
     * @param fixedBunches The list of fixed bunches (pre-sorted by strategy) / 固定批次列表（已按策略排序）
     * @param freeAircrafts The mutable set of aircraft to free / 待释放飞机的可变集合
     * @param maximumAmount The maximum number of aircraft to free / 释放飞机的最大数量
     * @return Success or failure / 成功或失败
    */
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

    /**
     * Adds an aircraft to the tabu list, evicting the oldest entry if full. / 将飞机加入禁忌列表，若已满则淘汰最旧条目。
     *
     * @param aircraft The aircraft to add to the tabu list / 要加入禁忌列表的飞机
    */
    private fun taboo(aircraft: Aircraft) {
        if (tabuAircrafts.size == configuration.tabuAmount.toInt()) {
            tabuAircrafts.removeFirst()
        }
        tabuAircrafts.add(aircraft)
    }

    /**
     * Checks whether an aircraft is currently in the tabu list. / 检查飞机当前是否在禁忌列表中。
     *
     * @param aircraft The aircraft to check / 要检查的飞机
     * @return True if the aircraft is tabu, false otherwise / 如果飞机在禁忌列表中则返回 true，否则返回 false
    */
    private fun tabu(aircraft: Aircraft): Boolean {
        return tabuAircrafts.contains(aircraft)
    }
}
