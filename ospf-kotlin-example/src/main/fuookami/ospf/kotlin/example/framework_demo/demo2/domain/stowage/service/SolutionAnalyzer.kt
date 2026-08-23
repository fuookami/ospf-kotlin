package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service

import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution as StowageSolution

/**
 * Analyzer that extracts a stowage domain solution from the solver's raw solution values.
 * 从求解器的原始解值中提取配载域方案的分析器。
 *
 * @property aggregation 配载聚合 / the stowage aggregation
*/
data class SolutionAnalyzer(
    private val aggregation: Aggregation
) {

    /**
     * Extracts the stowage solution from the solver's solution values.
     * 从求解器的解值中提取配载方案。
     *
     * @param solution 求解器解值 / the solver solution values
     * @param model 线性元模型 / the linear meta-model
     * @return 配载方案或失败 / the stowage solution or failure
    */
    operator fun invoke(
        solution: List<Flt64>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<StowageSolution> {
        val stowage = LinkedHashMap<Position, MutableList<Item>>()
        for (position in aggregation.positions) {
            if (position.loadedItems.isNotEmpty()) {
                stowage[position] = position.loadedItems.sortedBy { it.id }.toMutableList()
            }
        }

        for ((i, item) in aggregation.items.withIndex()) {
            for ((j, position) in aggregation.positions.withIndex()) {
                if (!Stowage.stowageNeeded(item, position)) {
                    continue
                }

                val value = when (val result = solverValue(
                    solution = solution,
                    model = model,
                    variable = aggregation.stowage.x[i, j],
                    name = "货物 ${item.id} 到舱位 ${position.spaceName} 的配载变量"
                )) {
                    is Ok -> result.value!!
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (value gr Flt64.half) {
                    stowage.getOrPut(position) { ArrayList() }.add(item)
                }
            }
        }

        val predicateLoadWeight = LinkedHashMap<Position, Quantity<Flt64>>()
        for ((j, position) in aggregation.positions.withIndex()) {
            if (position.status.predicateWeightNeeded) {
                val yi = aggregation.load.y[j]
                val value = when (val result = solverValue(
                    solution = solution,
                    model = model,
                    variable = yi.value,
                    name = "舱位 ${position.spaceName} 的谓词装载重量变量"
                )) {
                    is Ok -> result.value!!
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (value gr Flt64.zero) {
                    val loadWeight = Quantity(value, yi.unit).to(aggregation.aircraftModel.weightUnit)
                        ?: return Failed(
                            ErrorCode.ApplicationFailed,
                            "无法转换舱位 ${position.spaceName} 的谓词装载重量单位 / Cannot convert predicate load weight unit for position ${position.spaceName}"
                        )
                    predicateLoadWeight[position] = loadWeight
                }
            }
        }

        val recommendedLoadWeight = LinkedHashMap<Position, Quantity<Flt64>>()
        for ((j, position) in aggregation.positions.withIndex()) {
            if (position.status.recommendedWeightNeeded) {
                val zi = aggregation.load.z[j]
                val value = when (val result = solverValue(
                    solution = solution,
                    model = model,
                    variable = zi.value,
                    name = "舱位 ${position.spaceName} 的推荐装载重量变量"
                )) {
                    is Ok -> result.value!!
                    is Failed -> return Failed(result.error)
                    is Fatal -> return Fatal(result.errors)
                }
                if (value gr Flt64.zero) {
                    val loadWeight = Quantity(value, zi.unit).to(aggregation.aircraftModel.weightUnit)
                        ?: return Failed(
                            ErrorCode.ApplicationFailed,
                            "无法转换舱位 ${position.spaceName} 的推荐装载重量单位 / Cannot convert recommended load weight unit for position ${position.spaceName}"
                        )
                    recommendedLoadWeight[position] = loadWeight
                }
            }
        }

        return Ok<StowageSolution, ErrorCode, Error<ErrorCode>>(
            StowageSolution(
                stowage = stowage,
                predicateLoadWeight = predicateLoadWeight,
                recommendedLoadWeight = recommendedLoadWeight
            )
        )
    }

    private fun solverValue(
        solution: List<Flt64>,
        model: AbstractLinearMetaModel<Flt64>,
        variable: AbstractVariableItem<*, *>,
        name: String
    ): Ret<Flt64> {
        val tokenIndex = model.tokens.indexOf(variable)
            ?: return Failed(
                ErrorCode.ApplicationFailed,
                "求解器结果缺少 $name 的 token / Solver result is missing token for $name"
            )
        return solution.getOrNull(tokenIndex)?.let { Ok(it) } ?: Failed(
            ErrorCode.ApplicationFailed,
            "求解器结果缺少 $name 的值（索引 $tokenIndex） / Solver result is missing value for $name at index $tokenIndex"
        )
    }
}

