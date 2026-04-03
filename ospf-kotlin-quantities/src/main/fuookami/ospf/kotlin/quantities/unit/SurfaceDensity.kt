package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.SurfaceDensity

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
