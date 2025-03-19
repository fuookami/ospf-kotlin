package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object CubicDecimeter : PhysicalUnit() {
    private val unit = Decimeter * Decimeter * Decimeter

    override val name = "cubic decimeter"
    override val symbol = "dm3"

    override val quantity = Volume
    override val system by unit::system
    override val scale by unit::scale
}

object Liter : PhysicalUnit() {
    private val unit = CubicDecimeter

    override val name = "liter"
    override val symbol = "l"

    override val quantity = Volume
    override val system by unit::system
    override val scale by unit::scale
}

object CubicMeter : PhysicalUnit() {
    private val unit = Meter * Meter * Meter

    override val name = "cubic meter"
    override val symbol = "m3"

    override val quantity = Volume
    override val system by unit::system
    override val scale by unit::scale
}

object CubicInch : PhysicalUnit() {
    private val unit = Inch * Inch * Inch

    override val name = "cubic inch"
    override val symbol = "in3"

    override val quantity = Volume
    override val system by unit::system
    override val scale by unit::scale
}

object CubicFoot : PhysicalUnit() {
    private val unit = Foot * Foot * Foot

    override val name = "cubic foot"
    override val symbol = "ft3"

    override val quantity = Volume
    override val system by unit::system
    override val scale by unit::scale
}
