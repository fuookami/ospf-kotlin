package fuookami.ospf.kotlin.quantities.unit

import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.quantities.dimension.SolidAngle

object Steradian : PhysicalUnit() {
    override val name = "steradian"
    override val symbol = "sr"

    override val quantity = SolidAngle
    override val scale = Scale()
}

// Square degree: 1 sr = (180/π)² square degrees ≈ 0.00030461741978670857 sr
object SquareDegree : DerivedPhysicalUnit(Steradian * 0.00030461741978670857) {
    override val name = "square degree"
    override val symbol = "deg²"

    override val quantity = SolidAngle
}