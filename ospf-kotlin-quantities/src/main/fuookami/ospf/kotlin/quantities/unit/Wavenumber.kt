package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.quantities.dimension.WaveNumber

object ReciprocalMeter : DerivedPhysicalUnit(Meter.reciprocal()) {
    override val name = "reciprocal meter"
    override val symbol = "1/m"

    override val quantity = WaveNumber
}
