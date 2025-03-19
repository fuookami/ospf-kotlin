package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Newton : PhysicalUnit() {
    private val unit = Kilogram * MeterPerSquareSecond

    override val name = "newton"
    override val symbol = "N"

    override val quantity = Force
    override val system by unit::system
    override val scale by unit::scale
}

object KilogramForce : PhysicalUnit() {
    override val name = "kilogram force"
    override val symbol = "kgf"

    override val quantity = Force
    override val system = SI
    override val scale = Scale(Flt64(9.80665), Flt64.one)
}
