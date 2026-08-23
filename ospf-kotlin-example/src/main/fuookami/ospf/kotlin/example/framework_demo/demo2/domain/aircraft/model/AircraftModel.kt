package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*

/**
 * Enumeration of supported aircraft types with properties for body type and loading preferences.
 * 支持的飞机类型枚举（具有机身类型和装载偏好属性）。
*/
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

/**
 * Aircraft model with type classification, physical unit definitions, and gravity conversion.
 * 飞机模型（具有类型分类、物理单位定义和重力转换）。
 *
 * @property type 飞机类型分类 / The aircraft type classification.
 * @property model 飞机型号名称 / The aircraft model name.
 * @property minorModel 飞机子型号 / The aircraft minor model variant.
*/
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

    /**
     * Calculate gravity force from weight.
     * 根据重量计算重力值。
     *
     * @param weight 重量值 / The weight value.
     * @return 重力值 / The gravity force value.
    */
    fun gravity(weight: Quantity<Flt64>): Quantity<Flt64> {
        val forcePerWeight = Flt64.one * (forceUnit / weightUnit)
        return (weight * forcePerWeight)!!
    }
}
