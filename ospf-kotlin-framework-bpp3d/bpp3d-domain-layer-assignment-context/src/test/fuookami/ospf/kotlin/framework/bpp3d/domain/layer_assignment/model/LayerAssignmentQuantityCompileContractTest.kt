/**
 * 层分配物理量编译契约测试。
 * Layer assignment quantity compile contract test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*

class LayerAssignmentQuantityCompileContractTest {
    private val cargo = object : AbstractCargoAttribute {}

    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun q(value: FltX, unit: PhysicalUnit): Quantity<FltX> {
        return value * unit
    }

    private fun toFlt64Quantity(value: Quantity<FltX>): Quantity<FltX> {
        return Quantity(FltX(value.value.toDouble()), value.unit)
    }

    @Test
    fun fltXQuantityCanFlowThroughDomainAndSolverAdapterDirectly() {
        val material = QuantityMaterial(
            no = MaterialNo("M-1"),
            type = MaterialType.RawMaterial,
            cargo = cargo,
            name = "M-1",
            weight = q(FltX(0.5), Kilogram)
        )
        val shape = QuantityPackageShape(
            width = q(FltX(1.2), Meter),
            height = q(FltX(0.8), Meter),
            depth = q(FltX(0.6), Meter),
            weight = q(FltX(0.3), Kilogram),
            packageType = PackageType.CartonContainer
        )
        val pack = QuantityPackage.innerPackage(
            shape = shape,
            materials = mapOf(material to UInt64(2))
        )
        val item = QuantityItem(
            id = itemIdOf("item-1"),
            name = "item-1",
            pack = pack,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-1"),
            packageAttribute = defaultPackageAttribute()
        )

        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = LayerAssignmentQuantityCompileContractTest::class,
            width = q(FltX(5.0), Meter),
            height = q(FltX(5.0), Meter),
            depth = q(FltX(5.0), Meter),
            units = listOf(
                QuantityItemPlacement(
                    item = item,
                    x = q(FltX.zero, Meter),
                    y = q(FltX.zero, Meter),
                    z = q(FltX.zero, Meter)
                )
            )
        )

        val adapter = ScaledBpp3dSolverValueAdapter(
            scale = Bpp3dSolverFltXScale(
                amount = FltX(2.0),
                weight = FltX(10.0)
            )
        )

        val itemAmountStats = item.statistics(
            mode = Bpp3dDemandMode.ItemAmount,
            amount = UInt64(3)
        )
        val amountSolverValue = when (val amountValue = itemAmountStats.values.single()) {
            is QuantityBpp3dDemandValue.Amount -> adapter.amountToSolver(amountValue.value)
            else -> error("Unexpected amount statistics value: $amountValue")
        }
        assertEquals(FltX(6.0), amountSolverValue)

        val layerWeightStats = layer.statistics(Bpp3dDemandMode.ItemMaterialWeight)
        val materialNo = when (val weightKey = layerWeightStats.keys.single()) {
            is QuantityBpp3dDemandKey.Material -> weightKey.material.no
            else -> error("Unexpected weight statistics key: $weightKey")
        }
        assertEquals(MaterialNo("M-1"), materialNo)

        val weightSolverValue = when (val weightValue = layerWeightStats.values.single()) {
            is QuantityBpp3dDemandValue.Weight -> adapter.weightToSolver(toFlt64Quantity(weightValue.value))
            else -> error("Unexpected weight statistics value: $weightValue")
        }
        assertEquals(FltX(10.0), weightSolverValue)
    }
}
