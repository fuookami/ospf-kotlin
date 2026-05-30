package fuookami.ospf.kotlin.framework.bpp3d.application.service

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Bin
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ColumnGenerationPackingAnalyzerGenericEntryPointTest {
    private object Cargo : AbstractCargoAttribute

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(infraScalar(0.0)),
            hangingPolicy = AbsoluteHangingPolicy(infraScalar(0.0)),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun analyzerShouldSupportGenericLayerEntryPoint() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-ANALYZER-GENERIC"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ANALYZER-GENERIC",
            weight = FltX(0.2) * Kilogram
        )
        val shape = GenericPackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(0.2) * Kilogram,
            packageType = PackageType.CartonContainer
        )
        val item = GenericItem(
            id = "item-analyzer-generic",
            name = "item-analyzer-generic",
            pack = GenericPackage.innerPackage(
                shape = shape,
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ANALYZER-GENERIC"),
            packageAttribute = packageAttribute()
        )
        val layer = GenericBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationPackingAnalyzerGenericEntryPointTest::class,
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            units = listOf(
                GenericItemPlacement(
                    item = item,
                    x = FltX(0.0) * Meter,
                    y = FltX(0.0) * Meter,
                    z = FltX(0.0) * Meter
                )
            )
        )
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-ANALYZER-GENERIC"
        )
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromGeneric(
            iteration = 5,
            columns = listOf(layer),
            bins = listOf(
                Bin(
                    shape = bin,
                    units = emptyList()
                )
            )
        )

        val latest = analyzer.latest
        assertNotNull(latest)
        assertEquals(1, latest.bins.size)
        assertEquals("5", latest.schema.kpi["cg_iteration"])
    }
}
