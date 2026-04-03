package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.AmountOfSubstance

object Mole : PhysicalUnit() {
    override val name = "mole"
    override val symbol = "mol"

    override val quantity = AmountOfSubstance
    override val scale = Scale()
}
