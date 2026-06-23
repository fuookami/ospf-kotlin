package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * 从飞机重量和位置数据计算平衡臂、指数和 MAC 的气动公式。Aerodynamic formula for computing balanced arm, index, and MAC from aircraft weight and position data.
 *
 * @property aircraftModel 参数。
 * @property lip 参数。
 * @property chord 参数。
 * @property standardDatum 参数。
 * @property forceDistanceCoefficient 参数。
 * @property doiCorrection 参数。
 */
class Formula(
    val aircraftModel: AircraftModel,
    val lip: Quantity<Flt64>,
    val chord: Quantity<Flt64>,
    val standardDatum: Quantity<Flt64>,
    val forceDistanceCoefficient: Flt64,
    val doiCorrection: Quantity<Flt64>,
) {
    fun balancedArm(mac: MAC): Quantity<Flt64> {
        return ((mac.mac * chord)!! + lip)!!
    }

    @JvmName("balancedArm")
    fun balancedArm(index: Quantity<Flt64>, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        val correctedIndex = (index - doiCorrection)!!
        val correctedTorque = (correctedIndex * forceDistanceCoefficient)!!
        val armOffset = (correctedTorque / aircraftModel.gravity(totalWeight))!!
        return (armOffset + standardDatum)!!
    }

    @JvmName("balancedArmPolynomial")
    fun balancedArm(index: Quantity<LinearPolynomial<Flt64>>, totalWeight: Quantity<Flt64>): Quantity<LinearPolynomial<Flt64>> {
        val gravity = aircraftModel.gravity(totalWeight).value
        val scale = forceDistanceCoefficient / gravity
        val poly = LinearPolynomial(
            monomials = index.value.monomials.map { LinearMonomial(it.coefficient * scale, it.symbol) },
            constant = (index.value.constant - doiCorrection.value) * scale + standardDatum.value
        )
        return Quantity(poly, standardDatum.unit)
    }

    fun arm(index: Quantity<Flt64>, weight: Quantity<Flt64>): Quantity<Flt64> {
        val torque = (index * forceDistanceCoefficient)!!
        val armOffset = (torque / aircraftModel.gravity(weight))!!
        return (armOffset + standardDatum)!!
    }

    fun index(mac: MAC, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        val armOffset = (balancedArm(mac) - standardDatum)!!
        val torque = (totalWeight * armOffset)!!
        val index = (torque / forceDistanceCoefficient)!!
        return (index + doiCorrection)!!
    }

    fun index(weight: Quantity<Flt64>, arm: Quantity<Flt64>): Quantity<Flt64> {
        val armOffset = (arm - standardDatum)!!
        val torque = (aircraftModel.gravity(weight) * armOffset)!!
        return (torque / forceDistanceCoefficient)!!
    }

    fun index(weight: Quantity<Flt64>, position: Position): Quantity<Flt64> {
        return index(weight, position.coordinate.longitudinalArm)
    }

    fun mac(balancedArm: Quantity<Flt64>): MAC {
        return MAC(((balancedArm - lip)!! / chord)!!.value)
    }

    fun mac(index: Quantity<Flt64>, totalWeight: Quantity<Flt64>): MAC {
        return mac(balancedArm(index, totalWeight))
    }

    fun mac(index: Quantity<LinearPolynomial<Flt64>>, totalWeight: Quantity<Flt64>): LinearPolynomial<Flt64> {
        val balanced = balancedArm(index, totalWeight).value
        val invChord = Flt64.one / chord.value
        return LinearPolynomial(
            monomials = balanced.monomials.map { LinearMonomial(it.coefficient * invChord, it.symbol) },
            constant = (balanced.constant - lip.value) * invChord
        )
    }
}
