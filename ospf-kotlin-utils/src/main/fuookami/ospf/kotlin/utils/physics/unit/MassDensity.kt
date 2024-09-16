package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object KilogramPerLiter : PhysicalUnit() {
    private val unit = Kilogram / Liter

    override val name = "kilogram per liter"
    override val symbol = "kgpL"

    override val quantity = MassDensity
    override val system by unit::system
    override val scale by unit::scale
}

object KilogramPerCubicMeter : PhysicalUnit() {
    private val unit = Kilogram / CubicMeter

    override val name = "kilogram per cubic meter"
    override val symbol = "kgpm3"

    override val quantity = MassDensity
    override val system by unit::system
    override val scale by unit::scale
}
