package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bpp3dDemandValue
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.Quantity

private fun UInt64.toSolverInfraNumber(): InfraNumber = InfraNumber(this.toULong().toDouble())

private fun Quantity<InfraNumber>.toSolverInfraNumber(): InfraNumber = this.value

private fun ValueRange<UInt64>.toSolverRange(): ValueRange<InfraNumber> {
    val lower = this.lowerBound.value.unwrap().toSolverInfraNumber()
    val upper = this.upperBound.value.unwrap().toSolverInfraNumber()
    return ValueRange(
        lower,
        upper,
        this.lowerBound.interval,
        this.upperBound.interval,
        InfraNumber
    ).value!!
}

interface Bpp3dSolverValueAdapter {
    fun amountToSolver(value: UInt64): InfraNumber
    fun amountRangeToSolver(value: ValueRange<UInt64>): ValueRange<InfraNumber> = value.toSolverRange()
    fun lengthToSolver(value: Quantity<InfraNumber>): InfraNumber
    fun areaToSolver(value: Quantity<InfraNumber>): InfraNumber
    fun volumeToSolver(value: Quantity<InfraNumber>): InfraNumber
    fun depthToSolver(value: Quantity<InfraNumber>): InfraNumber = lengthToSolver(value)
    fun weightToSolver(value: Quantity<InfraNumber>): InfraNumber

    fun toSolver(value: Bpp3dDemandValue): InfraNumber {
        return when (value) {
            is Bpp3dDemandValue.Amount -> amountToSolver(value.value)
            is Bpp3dDemandValue.Weight -> weightToSolver(value.value)
        }
    }
}

typealias Bpp3dDemandValueAdapter = Bpp3dSolverValueAdapter

data object DefaultBpp3dDemandValueAdapter : Bpp3dSolverValueAdapter {
    override fun amountToSolver(value: UInt64): InfraNumber = value.toSolverInfraNumber()
    override fun lengthToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun areaToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun volumeToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
    override fun weightToSolver(value: Quantity<InfraNumber>): InfraNumber = value.toSolverInfraNumber()
}

val DefaultBpp3dSolverValueAdapter: Bpp3dSolverValueAdapter = DefaultBpp3dDemandValueAdapter

