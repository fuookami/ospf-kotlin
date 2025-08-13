package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Millisecond : PhysicalUnit() {
    override val name: String = "millisecond"
    override val symbol: String = "ms"

    override val quantity = Time
    override val system = SI
    override val scale: Scale = Scale(10, -3)
}

object Second : PhysicalUnit() {
    override val name: String = "second"
    override val symbol: String = "s"

    override val quantity = Time
    override val system = SI
    override val scale: Scale = Scale()
}

object Minute : PhysicalUnit() {
    private val unit = Second * 60

    override val name: String = "minute"
    override val symbol: String = "min"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Hour : PhysicalUnit() {
    private val unit = Minute * 60

    override val name: String = "hour"
    override val symbol: String = "h"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}

object Day : PhysicalUnit() {
    private val unit = Hour * 24

    override val name: String = "day"
    override val symbol: String = "d"

    override val quantity by unit::quantity
    override val system by unit::system
    override val scale by unit::scale
}
