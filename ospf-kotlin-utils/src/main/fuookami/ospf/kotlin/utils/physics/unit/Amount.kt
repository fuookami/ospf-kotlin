package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Mole : PhysicalUnit() {
    override val name = "mole"
    override val symbol = "mol"

    override val quantity = AmountOfSubstance
    override val system = SI
    override val scale = Scale()
}
