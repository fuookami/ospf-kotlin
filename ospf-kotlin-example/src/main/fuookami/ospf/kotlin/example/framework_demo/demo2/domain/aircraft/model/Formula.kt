package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model


import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.adapter.flt64.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*

class Formula(
    val aircraftModel: AircraftModel,
    val lip: Quantity<Flt64>,
    val chord: Quantity<Flt64>,
    val standardDatum: Quantity<Flt64>,
    val forceDistanceCoefficient: Flt64,
    val doiCorrection: Quantity<Flt64>,
) {
    fun balancedArm(mac: MAC): Quantity<Flt64> {
        return mac.mac * chord + lip
    }

    @JvmName("balancedArm")
    fun balancedArm(index: Quantity<Flt64>, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return (index - doiCorrection) * forceDistanceCoefficient / aircraftModel.gravity(totalWeight) + standardDatum
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
        return index * forceDistanceCoefficient / aircraftModel.gravity(weight) + standardDatum
    }

    fun index(mac: MAC, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        return totalWeight * (balancedArm(mac) - standardDatum) / forceDistanceCoefficient + doiCorrection
    }

    fun index(weight: Quantity<Flt64>, arm: Quantity<Flt64>): Quantity<Flt64> {
        return aircraftModel.gravity(weight) * (arm - standardDatum) / forceDistanceCoefficient
    }

    fun index(weight: Quantity<Flt64>, position: Position): Quantity<Flt64> {
        return index(weight, position.coordinate.longitudinalArm)
    }

    fun mac(balancedArm: Quantity<Flt64>): MAC {
        return MAC(((balancedArm - lip) / chord).value)
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


