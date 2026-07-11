@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.symbol.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.ShadowPrice
import fuookami.ospf.kotlin.framework.model.ShadowPriceKey
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

/** 按机场和飞机子类型索引的车队平衡约束的影子价格键。Shadow price key for fleet balance constraints indexed by airport and aircraft minor type. */
private data class FleetBalanceShadowPriceKey(
    val airport: Airport,
    val aircraftMinorType: AircraftMinorType,
) : ShadowPriceKey(FleetBalanceShadowPriceKey::class) {
    override fun toString() = "Fleet Balance ($airport, $aircraftMinorType)"
}

/**
 * 实现列生成车队平衡约束和最小化的管线。Pipeline implementing fleet balance constraints and minimization for column generation.
 *
 * @property fleetBalance Fleet balance model / 车队平衡模型
 * @property coefficient Penalty coefficient function / 惩罚系数函数
*/
class FleetBalanceLimit(
    private val fleetBalance: FleetBalance,
    private val coefficient: (Airport, AircraftMinorType) -> Flt64,
    override val name: String = "fleet_balance_limit"
) : CGPipeline {

    /**
     * 向模型添加机队平衡约束和最小化目标。/ Adds fleet balance constraints and minimization objective to the model.
     *
     * @param model Linear meta model / 线性元模型
     * @return Invocation result / 调用结果
    */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((l, checkPoint) in fleetBalance.limits.withIndex()) {
            when (val result = model.addConstraint(
                relation = fleetBalance.slack[l] geq checkPoint.second.amount,
                name = "${name}_${l}",
                args = FleetBalanceShadowPriceKey(checkPoint.first.airport, checkPoint.first.aircraftMinorType)
            )) {
                is Ok -> {}

                is Failed -> {
                    return Failed(result.error)
                }

                is Fatal -> {
                    return Fatal(result.errors)
                }
            }
        }

        val poly = MutableLinearPolynomial()
        for ((l, checkPoint) in fleetBalance.limits.withIndex()) {
            poly += LinearMonomial(
                coefficient(checkPoint.first.airport, checkPoint.first.aircraftMinorType),
                fleetBalance.slack[l]
            )
        }
        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "fleet balance")
        ) {
            is Ok -> {}

            is Failed -> {
                return Failed(result.error)
            }

            is Fatal -> {
                return Fatal(result.errors)
            }
        }

        return ok
    }

    /**
     * 返回机队平衡约束的影子价格提取器。/ Returns the shadow price extractor for fleet balance constraints.
     *
     * @return Shadow price extractor / 影子价格提取器
    */
    override fun extractor(): ShadowPriceExtractor? {
        return { map, args: ShadowPriceArguments ->
            when (args) {
                is TaskShadowPriceArguments -> {
                    if (args.prevTask is FlightTask && args.task == null) {
                        map[FleetBalanceShadowPriceKey(
                            airport = (args.prevTask!! as FlightTask).arr,
                            aircraftMinorType = (args.prevTask!! as FlightTask).aircraft!!.minorType
                        )]?.price ?: Flt64.zero
                    } else {
                        Flt64.zero
                    }
                }

                else -> {
                    Flt64.zero
                }
            }
        }
    }

    /**
     * 用求解模型的对偶值刷新影子价格映射。/ Refreshes the shadow price map with dual values from the solved model.
     *
     * @param shadowPriceMap Shadow price map / 影子价格映射
     * @param model Solved linear meta model / 已求解的线性元模型
     * @param shadowPrices Dual solution values / 对偶解值
     * @return Refresh result / 刷新结果
    */
    override fun refresh(
        shadowPriceMap: ShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup(this)) {
            val key = constraint.args as? FleetBalanceShadowPriceKey ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key = key, price = price))
            }
        }

        return ok
    }
}
