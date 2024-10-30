package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object MeterPerSquareSecond : PhysicalUnit() {
    private val unit = Meter / SquaredMeter

    override val name = "meter per square second"
    override val symbol = "mps2"

    override val quantity = Acceleration
    override val system by unit::system
    override val scale by unit::scale
}
