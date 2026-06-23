@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model

import fuookami.ospf.kotlin.utils.concept.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.multiarray.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.symbol.function.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.exampleThresholdSlack
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/**
 * 车队平衡约束模型，确保飞机在各机场的分布与每个检查点的预期车队组成相匹配。
 * Models fleet balance constraints ensuring aircraft distribution across airports
 * matches the expected fleet composition at each checkpoint.
 *
 * @property aircrafts 飞机列表。
 * @property originBunches 原始航班任务束列表。
 * @property compilation 编译信息。
  * @property aircrafts 参数。
  * @property originBunches 参数。
  * @property compilation 参数。
 */
class FleetBalance(
    aircrafts: List<Aircraft>,
    originBunches: List<FlightTaskBunch>,
    private val compilation: Compilation
) {
    /**
     * 表示机场和飞机子机型组合的检查点（用于车队平衡跟踪）。Checkpoint representing an airport and aircraft minor type combination for fleet balance tracking.
     *
     * @property airport 参数。
     * @property aircraftMinorType 参数。
     */
    data class CheckPoint(
        val airport: Airport,
        val aircraftMinorType: AircraftMinorType
    ) : ManualIndexed() {
        /**
         * 检查给定批次是否以匹配的飞机子机型到达此检查点。
         * Checks whether the given bunch arrives at this checkpoint with the matching aircraft minor type.
         *
         * @param bunch 航班任务束。
         * @return 是否匹配。
         */
        operator fun invoke(bunch: FlightTaskBunch): Boolean {
            return bunch.aircraft.minorType == aircraftMinorType && bunch.arr == airport
        }

        override fun hashCode(): Int {
            return airport.hashCode() xor aircraftMinorType.hashCode()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as CheckPoint

            if (airport != other.airport) return false
            if (aircraftMinorType != other.aircraftMinorType) return false

            return true
        }
    }

    /**
     * 指定检查点预期飞机数量和关联飞机列表的限制。Limit specifying the expected aircraft count and associated aircraft list at a checkpoint.
     *
     * @property amount 参数。
     * @property aircrafts 参数。
     */
    data class Limit(
        val amount: UInt64,
        val aircrafts: List<Aircraft>
    )

    /** 车队平衡约束的延迟计算的检查点-限制对列表。Lazily computed list of checkpoint-limit pairs for fleet balance constraints. */
    val limits: List<Pair<CheckPoint, Limit>> by lazy {
        ManualIndexed.flush<CheckPoint>()

        val limits = HashMap<CheckPoint, Pair<UInt64, MutableList<Aircraft>>>()
        for (aircraft in aircrafts) {
            val bunch = originBunches.find { it.aircraft == aircraft }
            val key = if (bunch != null && !bunch.empty) {
                CheckPoint(bunch.arr, aircraft.minorType)
            } else {
                CheckPoint(aircraft.usability.location, aircraft.minorType)
            }
            if (!limits.containsKey(key)) {
                key.setIndexed()
                limits[key] = UInt64.zero to mutableListOf()
            }
            limits[key] = (limits[key]!!.first + UInt64.one) to limits[key]!!.second
        }
        limits.entries.map { it.key to Limit(it.value.first, it.value.second) }
    }

    lateinit var fleet: LinearExpressionSymbols1<Flt64>
    lateinit var slack: LinearIntermediateSymbols1<Flt64>

    /**
     * 向模型注册车队平衡符号和松弛变量。
     * Registers fleet balance symbols and slack variables with the model.
     *
     * @param model 优化模型。
     * @return 执行结果。
     */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (limits.isNotEmpty()) {
            if (!::fleet.isInitialized) {
                fleet = LinearExpressionSymbols1<Flt64>(
                    "fleet",
                    Shape1(limits.size)
                ) { l, _ ->
                    val limit = limits[l]
                    val poly = MutableLinearPolynomial()
                    for (aircraft in limit.second.aircrafts) {
                        poly += LinearMonomial(Flt64.one, compilation.z[aircraft])
                    }
                    LinearExpressionSymbol(
                        poly,
                        name = "fleet_$l"
                    )
                }
            }
            when (val result = model.add(fleet)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }

            if (!::slack.isInitialized) {
                slack = LinearIntermediateSymbols1<Flt64>(
                    "fleet_slack",
                    Shape1(limits.size)
                ) { l, _ ->
                    val poly = MutableLinearPolynomial()
                    poly += LinearMonomial(Flt64.one, fleet[l])
                    exampleThresholdSlack(
                        x = LinearPolynomial(poly.monomials, poly.constant),
                        threshold = limits[l].second.amount.toFlt64(),
                        withNegative = true,
                        withPositive = false,
                        name = "fleet_slack_$l"
                    )
                }
            }
            when (val result = model.add(slack)) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        return ok
    }

    /**
     * 向车队平衡表达式添加新批次的列。
     * Adds columns for new bunches to the fleet balance expressions.
     *
     * @param iteration 迭代次数。
     * @param bunches 新批次列表。
     * @return 执行结果。
     */
    fun addColumns(
        iteration: UInt64,
        bunches: List<FlightTaskBunch>,
    ): Try {
        val xi = compilation.x[iteration.toInt()]

        for ((checkPoint, _) in limits) {
            val thisBunches = bunches.filter { checkPoint(it) }
            if (thisBunches.isNotEmpty()) {
                val thisFleet = fleet[checkPoint]
                thisFleet.flush()
                for (bunch in thisBunches) {
                    thisFleet.asMutable() += LinearMonomial(Flt64.one, xi[bunch])
                }
            }
        }

        return ok
    }
}
