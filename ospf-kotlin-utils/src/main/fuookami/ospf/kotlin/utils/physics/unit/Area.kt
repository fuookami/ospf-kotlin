package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object SquaredMeter : PhysicalUnit() {
    private val unit = Meter * Meter

    override val name = "square meter"
    override val symbol = "m2"

    override val quantity = Area
    override val system by unit::system
    override val scale by unit::scale
}

object SquareInch : PhysicalUnit() {
    private val unit = Inch * Inch

    override val name = "square inch"
    override val symbol = "in2"

    override val quantity = Area
    override val system by unit::system
    override val scale by unit::scale
}

object SquareFoot : PhysicalUnit() {
    private val unit = Foot * Foot

    override val name = "square foot"
    override val symbol = "ft2"

    override val quantity = Area
    override val system by unit::system
    override val scale by unit::scale
}
