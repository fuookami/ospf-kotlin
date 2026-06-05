package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
import fuookami.ospf.kotlin.quantities.unit.UnitConversionRule
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PackingProgramMaterialValueTest {
    private object CargoAttr : AbstractCargoAttribute

    private object DiscreteCountUnit : PhysicalUnit() {
        @Suppress("unused")
        fun getDomain(): String = "Discrete"

        override val name = "count"
        override val symbol = "cnt"
        override val quantity = QuantityUnit(name = "count", symbol = "cnt").quantity
        override val conversionRule = UnitConversionRule.Linear(Scale())
    }

    private fun material(
        no: String,
        unitWeightKg: InfraNumber
    ): Material<InfraNumber> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(): PackageShape<InfraNumber> {
        return PackageShape(
            width = infraScalar(1.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(1.0) * Meter,
            weight = infraScalar(1.0) * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    private fun assertScalarEquals(expected: Double, actual: InfraNumber?) {
        assertEquals(expected, requireNotNull(actual).toDouble(), 1e-10)
    }

    @Test
    fun amountOnlyShouldExposeAmountAndDeriveWeightWhenCatalogProvided() {
        val material = material(
            no = "M-AMOUNT",
            unitWeightKg = InfraNumber(2.0)
        )
        val program = PackingProgram.innerPackage(
            shape = shape(),
            materials = mapOf(material.key to UInt64(3))
        )

        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        assertScalarEquals(6.0, program.materialWeights(mapOf(material.key to material))[material.key]?.value)
    }

    @Test
    fun weightOnlyShouldNotCreateAmountContribution() {
        val material = material(
            no = "M-WEIGHT",
            unitWeightKg = InfraNumber(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    weight = infraScalar(5.0) * Kilogram
                )
            )
        )

        assertTrue(program.materialAmounts().isEmpty())
        assertScalarEquals(5.0, program.materialWeights()[material.key]?.value)
    }

    @Test
    fun amountAndWeightShouldPreferExplicitWeight() {
        val material = material(
            no = "M-BOTH",
            unitWeightKg = InfraNumber(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    amount = UInt64(3),
                    weight = infraScalar(9.0) * Kilogram
                )
            )
        )

        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        assertScalarEquals(9.0, program.materialWeights(mapOf(material.key to material))[material.key]?.value)
    }

    @Test
    fun quantityMapShouldSupportDiscreteAndContinuousUnits() {
        val materialAmount = material(
            no = "M-QUANTITY-AMOUNT",
            unitWeightKg = InfraNumber.one
        )
        val materialWeight = material(
            no = "M-QUANTITY-WEIGHT",
            unitWeightKg = InfraNumber.one
        )
        val program = PackingProgram.innerPackageWithMaterialQuantities(
            shape = shape(),
            materials = mapOf(
                materialAmount.key to Quantity(InfraNumber(3), DiscreteCountUnit),
                materialWeight.key to Quantity(InfraNumber(5), Kilogram)
            )
        )

        assertEquals(UInt64(3), program.materialAmounts()[materialAmount.key])
        assertScalarEquals(5.0, program.materialWeights()[materialWeight.key]?.value)

        val quantities = program.materialQuantities()
        assertScalarEquals(3.0, quantities[materialAmount.key]?.value)
        assertScalarEquals(5.0, quantities[materialWeight.key]?.value)
    }

    @Test
    fun materialQuantityMapShouldAcceptFltXWeightsAndKeepFlt64CompatibilityView() {
        val material = material(
            no = "M-QUANTITY-FLTX",
            unitWeightKg = InfraNumber.one
        )
        val program = PackingProgram.innerPackageWithMaterialQuantities(
            shape = shape(),
            materials = mapOf(
                material.key to Quantity(FltX(2.5), Kilogram)
            )
        )

        assertTrue(program.materials[material.key]?.weight?.value is FltX)
        assertScalarEquals(2.5, program.materialWeights()[material.key]?.value)
        assertScalarEquals(2.5, program.materialQuantities()[material.key]?.value)
    }
}


