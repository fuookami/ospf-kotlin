package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.service.limits


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.loading_effectiveness.model.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64

class SameSourceAdjacentLimit(
    private val adjacentPositions: List<PositionPair>,
    private val sources: List<FlightNo>,
    private val loading: TransferAdjacentLoading,
    private val coefficient: (FlightNo, Position, Position) -> Flt64 = { _, _, _ -> Flt64.one },
    override val name: String = "same_source_adjacent_limit",
) : Pipeline<AbstractLinearMetaModel<Flt64>> {
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        when (val result = model.maximize(
            sum(sources.flatMapIndexed { s, source ->
                adjacentPositions.mapIndexed { p, (position1, position2) ->
                    coefficient(source, position1, position2) * loading.sameSourceAdjacent[s, p]
                }
            }),
            name = "same source adjacent"
        )) {
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
}


















