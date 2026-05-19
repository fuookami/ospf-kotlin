package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

enum class AircraftType {
    B737,
    B757 {
        override val ballastNeeded = true
        override val mainDeckDoorEmptyPrefer = true
    },
    B767 {
        override val narrowBody = false
        override val wideBody = true
        override val ballastNeeded = true
        override val mainDeckDoorEmptyPrefer = true
    },
    B747 {
        override val narrowBody = false
        override val wideBody = true
    };

    open val narrowBody = true
    open val wideBody get() = !narrowBody
    open val ballastNeeded = false
    open val mainDeckDoorEmptyPrefer = false
}

data class AircraftModel(
    val type: AircraftType?,
    val model: String,
    val minorModel: AircraftMinorModel
) {
    companion object {
        operator fun invoke(minorModel: AircraftMinorModel): AircraftModel {
            val type = AircraftType.entries.find { minorModel.model.contains(it.name) }
            return AircraftModel(
                type = type,
                model = type?.name ?: "Other",
                minorModel = minorModel
            )
        }
    }

    val narrowBody get() = type?.narrowBody ?: true
    val wideBody get() = type?.wideBody ?: true
    val ballastNeeded get() = type?.ballastNeeded ?: false
    val mainDeckDoorEmptyPrefer get() = type?.mainDeckDoorEmptyPrefer ?: false

    val lengthUnit = Inch
    val areaUnit = SquareFoot
    val volumeUnit = CubicInch
    val weightUnit = Kilogram
    val forceUnit = KilogramForce
    val linearDensityUnit = weightUnit / lengthUnit
    val surfaceDensityUnit = weightUnit / areaUnit
    val torqueUnit = forceUnit * lengthUnit
    val fuelVolumeUnit = Liter
    val fuelDensityUnit = weightUnit / fuelVolumeUnit

    fun gravity(weight: Quantity<Flt64>): Quantity<Flt64> {
        val forcePerWeight = Flt64.one * (forceUnit / weightUnit)
        return weight * forcePerWeight
    }
}
