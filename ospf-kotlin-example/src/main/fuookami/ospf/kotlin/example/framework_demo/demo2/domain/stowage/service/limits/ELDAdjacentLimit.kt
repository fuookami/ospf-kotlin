package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.service.limits

import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.math.symbol.inequality.*
import fuookami.ospf.kotlin.core.model.basic.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.model.intermediate.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.domain.stowage.model.Position

class ELDAdjacentLimit(
    private val items: List<Item>,
    private val positions: List<Position>,
    private val neighbours: List<Neighbour>,
    private val stowage: Stowage,
    override val name: String = "eld_adjacent_limit"
): Pipeline<AbstractLinearMetaModelFlt64> {
    override fun invoke(model: AbstractLinearMetaModelFlt64): Try {
        for ((i1, item1) in items.withIndex()) {
            if (!item1.cargo.contains(CargoCode.ELD)) {
                continue
            }

            for (i2 in (i1 + 1) until items.size) {
                val item2 = items[i2]
                if (!item2.cargo.contains(CargoCode.ELD)) {
                    continue
                }

                for (neighbour in neighbours) {
                    val j1 = positions.indexOfFirst { it.base == neighbour.pair.first }
                    val position1 = positions[j1]
                    val j2 = positions.indexOfFirst { it.base == neighbour.pair.second }
                    val position2 = positions[j2]

                    if (Stowage.stowageNeeded(item1, position1) || Stowage.stowageNeeded(item2, position2)) {
                        val poly1 = MutableLinearPolynomial()
                        poly1 += LinearMonomial(Flt64.one, stowage.stowage[i1, j1])
                        poly1 += LinearMonomial(Flt64.one, stowage.stowage[i2, j2])
                        when (val result = model.addConstraint(
                            relation = LinearPolynomial(poly1.monomials, poly1.constant) leq Flt64.one,
                            name = "${name}_${item1}_${item2}_${position1}_${position2}"
                        )) {
                            is Ok -> {}
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                    if (Stowage.stowageNeeded(item1, position2) || Stowage.stowageNeeded(item2, position1)) {
                        val poly2 = MutableLinearPolynomial()
                        poly2 += LinearMonomial(Flt64.one, stowage.stowage[i1, j2])
                        poly2 += LinearMonomial(Flt64.one, stowage.stowage[i2, j1])
                        when (val result = model.addConstraint(
                            relation = LinearPolynomial(poly2.monomials, poly2.constant) leq Flt64.one,
                            name = "${name}_${item1}_${item2}_${position2}_${position1}"
                        )) {
                            is Ok -> {}
                            is Failed -> return Failed(result.error)
                            is Fatal -> return Fatal(result.errors)
                        }
                    }
                }
            }
        }

        return ok
    }
}
