package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.Scale
import fuookami.ospf.kotlin.utils.physics.dimension.AmountOfSubstance

object Mole : PhysicalUnit() {
    override val name = "mole"
    override val symbol = "mol"

    override val quantity = AmountOfSubstance
    override val scale = Scale()
}
