package fuookami.ospf.kotlin.utils.physics.unit

import fuookami.ospf.kotlin.utils.math.*
import fuookami.ospf.kotlin.utils.physics.dimension.*

object Joule : DerivedPhysicalUnit(Watt * Second) {
    override val name = "joule"
    override val symbol = "J"

    override val quantity = Energy
}

object Kilojoule : DerivedPhysicalUnit(Joule * Scale.kilo) {
    override val name = "kilojoule"
    override val symbol = "kJ"

    override val quantity = Energy
}

object Calorie : DerivedPhysicalUnit(Joule * 4.1858518) {
    override val name = "calorie"
    override val symbol = "cal"

    override val quantity = Energy
}

object Kilocalorie : DerivedPhysicalUnit(Calorie * Scale.kilo) {
    override val name = "kilocalorie"
    override val symbol = "kcal"

    override val quantity = Energy
}

object WattSecond : DerivedPhysicalUnit(Joule) {
    override val name = "watt second"
    override val symbol = "Ws"

    override val quantity = Energy
}

object KilowattSecond : DerivedPhysicalUnit(Kilowatt * Second) {
    override val name = "kilowatt second"
    override val symbol = "KWs"

    override val quantity = Energy
}

object KilowattHour : DerivedPhysicalUnit(Kilowatt * Hour) {
    override val name = "kilowatt hour"
    override val symbol = "kWh"

    override val quantity = Energy
}

object HorsepowerHour : DerivedPhysicalUnit(Horsepower * Hour) {
    override val name = "horsepower hour"
    override val symbol = "hp·h"

    override val quantity = Energy
}

object UKHorsepowerHour : DerivedPhysicalUnit(UKHorsepower * Hour) {
    override val name = "uk horsepower hour"
    override val symbol = "uk.hp·h"

    override val quantity = Energy
}
