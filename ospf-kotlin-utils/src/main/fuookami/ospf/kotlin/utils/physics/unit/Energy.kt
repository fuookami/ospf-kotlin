package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object Joule : PhysicalUnit() {
    private val unit = Watt * Second

    override val name = "joule"
    override val symbol = "J"

    override val quantity = Energy
    override val system by unit::system
    override val scale by unit::scale
}
