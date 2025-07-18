package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object KilogramPerSquareMeter : DerivedPhysicalUnit(Kilogram / SquareMeter) {
    override val name = "kilogram per square meter"
    override val symbol = "kgpm2"

    override val quantity = SurfaceDensity
}

object GramPerSquareMeter : DerivedPhysicalUnit(Gram / SquareMeter) {
    override val name = "gram per square meter"
    override val symbol = "gpm2"

    override val quantity = SurfaceDensity
}
