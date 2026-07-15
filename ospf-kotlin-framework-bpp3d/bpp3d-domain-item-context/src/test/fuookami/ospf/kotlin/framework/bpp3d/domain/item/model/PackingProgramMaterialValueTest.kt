/**
 * 装箱方案物料值测试。
 * Packing program material value test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*

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
        unitWeightKg: FltX
    ): Material<FltX> {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(): PackageShape<FltX> {
        return PackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(1.0) * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    private fun assertScalarEquals(expected: Double, actual: FltX?) {
        assertEquals(expected, requireNotNull(actual).toDouble(), 1e-10)
    }

    @Test
    fun amountOnlyShouldExposeAmountAndDeriveWeightWhenCatalogProvided() {
        val material = material(
            no = "M-AMOUNT",
            unitWeightKg = FltX(2.0)
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
            unitWeightKg = FltX(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    weight = FltX(5.0) * Kilogram
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
            unitWeightKg = FltX(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    amount = UInt64(3),
                    weight = FltX(9.0) * Kilogram
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
            unitWeightKg = FltX.one
        )
        val materialWeight = material(
            no = "M-QUANTITY-WEIGHT",
            unitWeightKg = FltX.one
        )
        val program = PackingProgram.innerPackageWithMaterialQuantities(
            shape = shape(),
            materials = mapOf(
                materialAmount.key to Quantity(FltX(3), DiscreteCountUnit),
                materialWeight.key to Quantity(FltX(5), Kilogram)
            )
        )

        assertEquals(UInt64(3), program.materialAmounts()[materialAmount.key])
        assertScalarEquals(5.0, program.materialWeights()[materialWeight.key]?.value)

        val quantities = program.materialQuantities()
        assertScalarEquals(3.0, quantities[materialAmount.key]?.value)
        assertScalarEquals(5.0, quantities[materialWeight.key]?.value)
    }

    @Test
    fun materialQuantityMapShouldAcceptFltXWeightsAndExposeFlt64View() {
        val material = material(
            no = "M-QUANTITY-FLTX",
            unitWeightKg = FltX.one
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
