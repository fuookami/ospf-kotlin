package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object Watt : PhysicalUnit() {
    private val unit = Newton * MeterPerSecond

    override val name = "watt"
    override val symbol = "W"

    override val quantity = Power
    override val system by unit::system
    override val scale by unit::scale
}
