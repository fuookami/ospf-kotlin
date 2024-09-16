package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object Newton : PhysicalUnit() {
    private val unit = Kilogram * MeterPerSquareSecond

    override val name = "newton"
    override val symbol = "N"

    override val quantity = Force
    override val system by unit::system
    override val scale by unit::scale
}
