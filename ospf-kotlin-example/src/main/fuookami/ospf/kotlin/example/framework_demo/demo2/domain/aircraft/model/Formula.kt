package fuookami.ospf.kotlin.example.framework_demo.demo2.domain.aircraft.model

import fuookami.ospf.kotlin.example.framework_demo.demo2.infrastructure.*
import fuookami.ospf.kotlin.math.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.symbol.monomial.*
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.polynomial.*
import fuookami.ospf.kotlin.quantities.quantity.*

/**
 * Aerodynamic formula for computing balanced arm, index, and MAC from aircraft weight and position data.
 * 从飞机重量和位置数据计算平衡臂、指数和 MAC 的气动公式。
 *
 * @property aircraftModel 用于单位定义和重力转换的飞机型号 / The aircraft model used for unit definitions and gravity conversion.
 * @property lip 前缘截距位置（LEMAC） / The leading edge intercept position (LEMAC).
 * @property chord 平均气动弦长 / The mean aerodynamic chord length.
 * @property standardDatum 标准基准参考位置 / The standard datum reference position.
 * @property forceDistanceCoefficient 将力矩转换为指数的系数 / The coefficient for converting torque to index.
 * @property doiCorrection 干操作指数修正值 / The dry operating index correction value.
*/
class Formula(
    val aircraftModel: AircraftModel,
    val lip: Quantity<Flt64>,
    val chord: Quantity<Flt64>,
    val standardDatum: Quantity<Flt64>,
    val forceDistanceCoefficient: Flt64,
    val doiCorrection: Quantity<Flt64>,
) {

    /**
     * Calculate the balanced arm from a MAC (Mean Aerodynamic Chord) value.
     * 从 MAC（平均气动弦）值计算平衡臂。
     *
     * @param mac 平均气动弦百分比 / The mean aerodynamic chord percentage.
     * @return 平衡臂位置 / The balanced arm position.
    */
    fun balancedArm(mac: MAC): Quantity<Flt64> {
        return ((mac.mac * chord)!! + lip)!!
    }

    /**
     * Calculate the balanced arm from an index value and total weight.
     * 从指数值和总重量计算平衡臂。
     *
     * @param index 指数值 / The index value.
     * @param totalWeight 飞机总重量 / The total weight of the aircraft.
     * @return 平衡臂位置 / The balanced arm position.
    */
    @JvmName("balancedArm")
    fun balancedArm(index: Quantity<Flt64>, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        val correctedIndex = (index - doiCorrection)!!
        val correctedTorque = (correctedIndex * forceDistanceCoefficient)!!
        val armOffset = (correctedTorque / aircraftModel.gravity(totalWeight))!!
        return (armOffset + standardDatum)!!
    }

    /**
     * Calculate the balanced arm as a linear polynomial from an index polynomial and total weight.
     * 从指数多项式和总重量计算平衡臂的线性多项式。
     *
     * @param index 线性多项式形式的指数 / The index as a linear polynomial.
     * @param totalWeight 飞机总重量 / The total weight of the aircraft.
     * @return 线性多项式形式的平衡臂 / The balanced arm as a linear polynomial.
    */
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

    /**
     * Calculate the arm from an index value and weight.
     * 从指数值和重量计算臂。
     *
     * @param index 指数值 / The index value.
     * @param weight 重量值 / The weight value.
     * @return 臂位置 / The arm position.
    */
    fun arm(index: Quantity<Flt64>, weight: Quantity<Flt64>): Quantity<Flt64> {
        val torque = (index * forceDistanceCoefficient)!!
        val armOffset = (torque / aircraftModel.gravity(weight))!!
        return (armOffset + standardDatum)!!
    }

    /**
     * Calculate the index from a MAC value and total weight.
     * 从 MAC 值和总重量计算指数。
     *
     * @param mac 平均气动弦百分比 / The mean aerodynamic chord percentage.
     * @param totalWeight 飞机总重量 / The total weight of the aircraft.
     * @return 指数值 / The index value.
    */
    fun index(mac: MAC, totalWeight: Quantity<Flt64>): Quantity<Flt64> {
        val armOffset = (balancedArm(mac) - standardDatum)!!
        val torque = (totalWeight * armOffset)!!
        val index = (torque / forceDistanceCoefficient)!!
        return (index + doiCorrection)!!
    }

    /**
     * Calculate the index from a weight and arm value.
     * 从重量和臂值计算指数。
     *
     * @param weight 重量值 / The weight value.
     * @param arm 臂位置 / The arm position.
     * @return 指数值 / The index value.
    */
    fun index(weight: Quantity<Flt64>, arm: Quantity<Flt64>): Quantity<Flt64> {
        val armOffset = (arm - standardDatum)!!
        val torque = (aircraftModel.gravity(weight) * armOffset)!!
        return (torque / forceDistanceCoefficient)!!
    }

    /**
     * Calculate the index from a weight and cargo position.
     * 从重量和货物位置计算指数。
     *
     * @param weight 重量值 / The weight value.
     * @param position 货物位置 / The cargo position.
     * @return 指数值 / The index value.
    */
    fun index(weight: Quantity<Flt64>, position: Position): Quantity<Flt64> {
        return index(weight, position.coordinate.longitudinalArm)
    }

    /**
     * Calculate the MAC (Mean Aerodynamic Chord) from a balanced arm.
     * 从平衡臂计算 MAC（平均气动弦）。
     *
     * @param balancedArm 平衡臂位置 / The balanced arm position.
     * @return The MAC value. / MAC 值
    */
    fun mac(balancedArm: Quantity<Flt64>): MAC {
        return MAC(((balancedArm - lip)!! / chord)!!.value)
    }

    /**
     * Calculate the MAC from an index value and total weight.
     * 从指数值和总重量计算 MAC。
     *
     * @param index 指数值 / The index value.
     * @param totalWeight 飞机总重量 / The total weight of the aircraft.
     * @return The MAC value. / MAC 值
    */
    fun mac(index: Quantity<Flt64>, totalWeight: Quantity<Flt64>): MAC {
        return mac(balancedArm(index, totalWeight))
    }

    /**
     * Calculate the MAC as a linear polynomial from an index polynomial and total weight.
     * 从指数多项式和总重量计算 MAC 的线性多项式。
     *
     * @param index 线性多项式形式的指数 / The index as a linear polynomial.
     * @param totalWeight 飞机总重量 / The total weight of the aircraft.
     * @return 线性多项式形式的 MAC / The MAC as a linear polynomial.
    */
    fun mac(index: Quantity<LinearPolynomial<Flt64>>, totalWeight: Quantity<Flt64>): LinearPolynomial<Flt64> {
        val balanced = balancedArm(index, totalWeight).value
        val invChord = Flt64.one / chord.value
        return LinearPolynomial(
            monomials = balanced.monomials.map { LinearMonomial(it.coefficient * invChord, it.symbol) },
            constant = (balanced.constant - lip.value) * invChord
        )
    }
}
