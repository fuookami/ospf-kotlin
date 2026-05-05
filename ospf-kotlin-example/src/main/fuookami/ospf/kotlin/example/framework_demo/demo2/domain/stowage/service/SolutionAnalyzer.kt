package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*

data class SolutionAnalyzer(
    private val aggregation: Aggregation
) {
    operator fun invoke(
        solution: List<Flt64>,
        model: AbstractLinearMetaModel<Flt64>
    ): Ret<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution> {
        val stowage = HashMap<Position, MutableList<Item>>()
        for ((i, item) in aggregation.items.withIndex()) {
            for ((j, position) in aggregation.positions.withIndex()) {
                val xi = aggregation.stowage.x[i, j]
                val token = model.tokens.tokenList.find(xi)
                val tokenIdx = token?.let { model.tokens.indexOf(it) }
                if (tokenIdx != null && solution[tokenIdx] gr Flt64.zero) {
                    stowage.getOrPut(position) { ArrayList() }.add(item)
                }
            }
        }

        val predicateLoadWeight = HashMap<Position, Quantity<Flt64>>()
        for ((j, position) in aggregation.positions.withIndex()) {
            if (position.status.predicateWeightNeeded) {
                val yi = aggregation.load.y[j]
                val token = model.tokens.tokenList.find(yi.value)
                val tokenIdx = token?.let { model.tokens.indexOf(it) }
                if (tokenIdx != null && solution[tokenIdx] gr Flt64.zero) {
                    predicateLoadWeight[position] = Quantity(
                        solution[tokenIdx],
                        yi.unit
                    ).to(aggregation.aircraftModel.weightUnit)!!
                }
            }
        }

        val recommendedLoadWeight = HashMap<Position, Quantity<Flt64>>()
        for ((j, position) in aggregation.positions.withIndex()) {
            if (position.status.recommendedWeightNeeded) {
                val zi = aggregation.load.z[j]
                val token = model.tokens.tokenList.find(zi.value)
                val tokenIdx = token?.let { model.tokens.indexOf(it) }
                if (tokenIdx != null && solution[tokenIdx] gr Flt64.zero) {
                    recommendedLoadWeight[position] = Quantity(
                        solution[tokenIdx],
                        zi.unit
                    ).to(aggregation.aircraftModel.weightUnit)!!
                }
            }
        }

        return Ok<fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution, ErrorCode, Error<ErrorCode>>(
            fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Solution(
                stowage = stowage,
                predicateLoadWeight = predicateLoadWeight,
                recommendedLoadWeight = recommendedLoadWeight
            )
        )
    }
}


