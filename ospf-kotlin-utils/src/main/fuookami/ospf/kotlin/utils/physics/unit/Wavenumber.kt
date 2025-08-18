package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.physics.dimension.*

object ReciprocalMeter : DerivedPhysicalUnit(Meter.reciprocal()) {
    override val name = "reciprocal meter"
    override val symbol = "1/m"

    override val quantity = WaveNumber
}
