@file:OptIn(kotlin.time.ExperimentalTime::class)

package fuookami.ospf.kotlin.example.framework_demo.demo4.domain.bunch_compilation.model

import fuookami.ospf.kotlin.example.exampleThresholdSlack
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.rule.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo4.domain.task.model.*

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

/**
 * Models flight link expressions tracking connections between consecutive flight tasks
 * and their associated slack variables for the column generation formulation.
 */
class FlightLink(
    val links: List<Link>,
    private val compilation: Compilation
) {
    init {
        ManualIndexed.flush<Link>()
        for (link in links) {
            link.setIndexed()
        }
    }

    lateinit var link: LinearExpressionSymbols1<Flt64>
    lateinit var slack: LinearIntermediateSymbols1<Flt64>

    /** Registers link and slack symbols with the model. */
    fun register(model: AbstractLinearMetaModel<Flt64>): Try {
        if (links.isNotEmpty()) {
            if (!::link.isInitialized) {
                link = LinearExpressionSymbols1<Flt64>(
                    "link",
                    Shape1(links.size)
                ) { k, _ ->
                    LinearExpressionSymbol(
                        MutableLinearPolynomial(),
                        name = "link_$k"
                    )
                }
            }
            when (val result = model.add(link)) {
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
                    "link_slack",
                    Shape1(links.size)
                ) { k, _ ->
                    val poly = MutableLinearPolynomial()
                    poly += LinearMonomial(Flt64.one, link[k])
                    poly += LinearMonomial(Flt64(0.5), compilation.y[links[k].prevTask])
                    poly += LinearMonomial(Flt64(0.5), compilation.y[links[k].succTask])
                    exampleThresholdSlack(
                        x = LinearPolynomial(poly.monomials, poly.constant),
                        threshold = Flt64.one,
                        withNegative = true,
                        withPositive = false,
                        name = "link_slack_$k"
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

    /** Adds columns for new bunches to the link expressions. */
    fun addColumns(
        iteration: UInt64,
        bunches: List<FlightTaskBunch>,
    ): Try {
        val xi = compilation.x[iteration.toInt()]

        for (link in links) {
            val thisBunches = bunches.filter { it.contains(link.prevTask, link.succTask) }
            if (thisBunches.isNotEmpty()) {
                val thisLink = this.link[link]
                thisLink.flush()
                for (bunch in thisBunches) {
                    thisLink.asMutable() += LinearMonomial(Flt64.one, xi[bunch])
                }
            }
        }

        return ok
    }
}
