@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.service.limits

import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

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

/** Shadow price key for flight link constraints indexed by the link. */
private data class FlightLinkShadowPriceKey(
    val link: Link
) : ShadowPriceKey(FlightLinkShadowPriceKey::class) {
    override fun toString() = "Link ($link)"
}

/** Pipeline implementing flight link constraints and minimization for column generation. */
class FlightLinkLimit(
    private val flightLink: FlightLink,
    private val coefficient: (Link) -> Flt64,
    override val name: String = "flight_link_limit"
) : CGPipeline {
    /** Adds flight link constraints and minimization objective to the model. */
    override fun invoke(model: AbstractLinearMetaModel<Flt64>): Try {
        for ((k, _) in flightLink.links.withIndex()) {
            when (val result = model.addConstraint(
                relation = flightLink.slack[k] geq Flt64.one,
                name = "${name}_${k}",
                args = FlightLinkShadowPriceKey(flightLink.links[k])
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
        for ((k, link) in flightLink.links.withIndex()) {
            poly += LinearMonomial(coefficient(link), flightLink.slack[k])
        }

        when (val result = model.minimize(
            LinearExpressionSymbol(LinearPolynomial(poly.monomials, poly.constant)),
            name = "link"
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

    /** Returns the shadow price extractor for flight link constraints. */
    override fun extractor(): ShadowPriceExtractor? {
        return { map, args: ShadowPriceArguments ->
            when (args) {
                is TaskShadowPriceArguments -> {
                    if (args.prevTask is FlightTask && args.task is FlightTask) {
                        val link = flightLink.links.find { it.prevTask == (args.prevTask!! as FlightTask).originTask
                                && it.succTask == (args.task!! as FlightTask).originTask
                        }
                        if (link != null) {
                            map[FlightLinkShadowPriceKey(link)]?.price ?: Flt64.zero
                        } else {
                            Flt64.zero
                        }
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

    /** Refreshes the shadow price map with dual values from the solved model. */
    override fun refresh(
        shadowPriceMap: ShadowPriceMap,
        model: AbstractLinearMetaModel<Flt64>,
        shadowPrices: MetaDualSolution
    ): Try {
        for (constraint in model.constraintsOfGroup(this)) {
            val key = constraint.args as? FlightLinkShadowPriceKey ?: continue
            shadowPrices.constraints[constraint]?.let { price ->
                shadowPriceMap.put(ShadowPrice(key = key, price = price))
            }
        }

        return ok
    }
}
