package fuookami.ospf.kotlin.framework.bpp3d.domain.item.model

import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.Scale
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.PhysicalUnit
import fuookami.ospf.kotlin.quantities.unit.QuantityUnit
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
        override val scale = Scale()
    }

    private fun material(
        no: String,
        unitWeightKg: Flt64
    ): Material {
        return Material(
            no = MaterialNo(no),
            type = MaterialType.RawMaterial,
            cargo = CargoAttr,
            name = no,
            weight = unitWeightKg * Kilogram
        )
    }

    private fun shape(): PackageShape {
        return PackageShape(
            width = 1.0 * Meter,
            height = 1.0 * Meter,
            depth = 1.0 * Meter,
            weight = 1.0 * Kilogram,
            packageType = PackageType.CartonContainer
        )
    }

    @Test
    fun amountOnlyShouldExposeAmountAndDeriveWeightWhenCatalogProvided() {
        val material = material(
            no = "M-AMOUNT",
            unitWeightKg = Flt64(2.0)
        )
        val program = PackingProgram.innerPackage(
            shape = shape(),
            materials = mapOf(material.key to UInt64(3))
        )

        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        assertEquals(Flt64(6.0), program.materialWeights(mapOf(material.key to material))[material.key]?.value)
    }

    @Test
    fun weightOnlyShouldNotCreateAmountContribution() {
        val material = material(
            no = "M-WEIGHT",
            unitWeightKg = Flt64(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    weight = 5.0 * Kilogram
                )
            )
        )

        assertTrue(program.materialAmounts().isEmpty())
        assertEquals(Flt64(5.0), program.materialWeights()[material.key]?.value)
    }

    @Test
    fun amountAndWeightShouldPreferExplicitWeight() {
        val material = material(
            no = "M-BOTH",
            unitWeightKg = Flt64(2.0)
        )
        val program = PackingProgram.innerPackageWithMaterialValues(
            shape = shape(),
            materials = mapOf(
                material.key to PackingProgramMaterialValue(
                    amount = UInt64(3),
                    weight = 9.0 * Kilogram
                )
            )
        )

        assertEquals(UInt64(3), program.materialAmounts()[material.key])
        assertEquals(Flt64(9.0), program.materialWeights(mapOf(material.key to material))[material.key]?.value)
    }

    @Test
    fun quantityMapShouldSupportDiscreteAndContinuousUnits() {
        val materialAmount = material(
            no = "M-QUANTITY-AMOUNT",
            unitWeightKg = Flt64.one
        )
        val materialWeight = material(
            no = "M-QUANTITY-WEIGHT",
            unitWeightKg = Flt64.one
        )
        val program = PackingProgram.innerPackageWithMaterialQuantities(
            shape = shape(),
            materials = mapOf(
                materialAmount.key to Quantity(Flt64(3), DiscreteCountUnit),
                materialWeight.key to Quantity(Flt64(5), Kilogram)
            )
        )

        assertEquals(UInt64(3), program.materialAmounts()[materialAmount.key])
        assertEquals(Flt64(5.0), program.materialWeights()[materialWeight.key]?.value)

        val quantities = program.materialQuantities()
        assertEquals(Flt64(3.0), quantities[materialAmount.key]?.value)
        assertEquals(Flt64(5.0), quantities[materialWeight.key]?.value)
    }

    @Test
    fun materialQuantityMapShouldAcceptFltXWeightsAndKeepFlt64CompatibilityView() {
        val material = material(
            no = "M-QUANTITY-FLTX",
            unitWeightKg = Flt64.one
        )
        val program = PackingProgram.innerPackageWithMaterialQuantities(
            shape = shape(),
            materials = mapOf(
                material.key to Quantity(FltX(2.5), Kilogram)
            )
        )

        assertTrue(program.materials[material.key]?.weight?.value is FltX)
        assertEquals(Flt64(2.5), program.materialWeights()[material.key]?.value)
        assertEquals(Flt64(2.5), program.materialQuantities()[material.key]?.value)
    }
}
