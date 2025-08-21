package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Radian : PhysicalUnit() {
    override val name = "radian"
    override val symbol = "rad"

    override val quantity = PlaneAngle
    override val system = SI
    override val scale = Scale()
}

object Milliradian : DerivedPhysicalUnit(Radian * Scale.milli) {
    override val name = "milliradian"
    override val symbol = "mrad"

    override val quantity = PlaneAngle
}

object RoundAngle : DerivedPhysicalUnit(Radian * (FltX.two * FltX.pi)) {
    override val name = "round angle"
    override val symbol = "round angle"

    override val quantity = PlaneAngle
}

object RightAngle : DerivedPhysicalUnit(Radian / FltX(4.0)) {
    override val name = "right angle"
    override val symbol = "right angle"

    override val quantity = PlaneAngle
}

object Degree : DerivedPhysicalUnit(RoundAngle / FltX(360.0)) {
    override val name = "degree"
    override val symbol = "°"

    override val quantity = PlaneAngle
}

object MinuteAngle: DerivedPhysicalUnit(Degree / FltX(60.0)) {
    override val name = "minute angle"
    override val symbol = "′"

    override val quantity = PlaneAngle
}

object SecondAngle: DerivedPhysicalUnit(MinuteAngle / FltX(60.0)) {
    override val name = "second angle"
    override val symbol = "″"

    override val quantity = PlaneAngle
}

object Gradian: DerivedPhysicalUnit(RightAngle / FltX(100.0)) {
    override val name = "gradian"
    override val symbol = "gon"

    override val quantity = PlaneAngle
}
