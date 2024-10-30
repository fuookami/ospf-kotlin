package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object GramPerSquareMeter : PhysicalUnit() {
    private val unit = Gram / SquaredMeter

    override val name = "gram per square meter"
    override val symbol = "gpm2"

    override val quantity = SurfaceDensity
    override val system by unit::system
    override val scale by unit::scale
}

object KilogramPerSquareMeter : PhysicalUnit() {
    private val unit = Kilogram / SquaredMeter

    override val name = "kilogram per square meter"
    override val symbol = "kgpm2"

    override val quantity = SurfaceDensity
    override val system by unit::system
    override val scale by unit::scale
}
