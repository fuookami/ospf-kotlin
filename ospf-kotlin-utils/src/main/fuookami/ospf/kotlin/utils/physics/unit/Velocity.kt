package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object MeterPerSecond : PhysicalUnit() {
    private val unit = Meter / Second

    override val name: String = "meter per second"
    override val symbol: String = "mps"

    override val quantity = Velocity
    override val system by unit::system
    override val scale by unit::scale
}

object KilometerPerHour : PhysicalUnit() {
    private val Unit = Kilometer / Hour

    override val name: String = "kilometer per hour"
    override val symbol: String = "kmph"

    override val quantity = Velocity
    override val system by Unit::system
    override val scale by Unit::scale
}
