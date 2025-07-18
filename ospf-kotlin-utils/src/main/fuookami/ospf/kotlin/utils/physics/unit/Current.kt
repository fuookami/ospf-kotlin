package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Ampere : PhysicalUnit() {
    override val name = "amper"
    override val symbol = "A"

    override val quantity = Current
    override val system = SI
    override val scale = Scale()
}

object Microcurrent : DerivedPhysicalUnit(Ampere * Scale.micro) {
    override val name = "micro"
    override val symbol = "µA"

    override val quantity = Current
}

object Millicurrent : DerivedPhysicalUnit(Ampere * Scale.micro) {
    override val name = "micro"
    override val symbol = "mA"

    override val quantity = Current
}
