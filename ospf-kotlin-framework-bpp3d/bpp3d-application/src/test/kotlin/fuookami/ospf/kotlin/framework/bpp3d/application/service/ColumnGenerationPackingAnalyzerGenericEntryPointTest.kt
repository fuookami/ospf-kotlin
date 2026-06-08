package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.Int64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.MaterialNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.infraScalar
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbstractCargoAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.BinType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericBinLayer
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericItemPlacement
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericMaterial
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackage
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.GenericPackageShapeSpec
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialType
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.layerBinOf
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.resolvedPackingShape
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.Bpp3dLayerGenerationRequest
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.CirclePackingLayerGenerator
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.CylinderPackingShape3

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
                layerBinOf(
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

    @Test
    fun layerPlacementAdapterShouldRejectManualHorizontalCylinderGeneratedPlacement() {
        for (axis in listOf(Axis3.X, Axis3.Z)) {
            val layer = horizontalCylinderLayer(axis)

            val exception = kotlin.test.assertFailsWith<IllegalArgumentException> {
                layer.toModel().toLayerPlacement()
            }

            assertTrue(exception.message?.contains("only verified axis-aware generated candidates are allowed") == true)
        }
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderCandidate() = runBlocking {
        val item = horizontalCylinderLayer(Axis3.X).units.single().item.toModel()
        val bin = BinType(
            width = infraScalar(3.0) * Meter,
            height = infraScalar(1.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-ADAPTER-GENERATED-CYLINDER-X"
        )

        val generated = CirclePackingLayerGenerator<FltX>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(item),
                maxCandidates = 4
            )
        )
        val layer = generated.first { result ->
            result.source == "circle-packing-horizontal-grid-single-axis=x"
        }.layer
        val placement = layer.toLayerPlacement()

        assertEquals(layer.units.size, placement.unit.units.size)
        assertTrue(
            placement.unit.units.all { unitPlacement ->
                val shape = unitPlacement.resolvedPackingShape()
                shape is CylinderPackingShape3 && shape.axis == Axis3.X
            }
        )
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderSupportedStack() = runBlocking {
        val material = GenericMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-STACK"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-STACK",
            weight = FltX(0.2) * Kilogram
        )
        val support = GenericItem(
            id = "item-adapter-cylinder-stack-support",
            name = "item-adapter-cylinder-stack-support",
            pack = GenericPackage.innerPackage(
                shape = GenericPackageShape(
                    width = FltX(1.2) * Meter,
                    height = FltX(0.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-STACK-SUPPORT"),
            packageAttribute = packageAttribute()
        ).toModel()
        val cylinder = GenericItem(
            id = "item-adapter-cylinder-stack-cylinder",
            name = "item-adapter-cylinder-stack-cylinder",
            pack = GenericPackage.innerPackage(
                shape = GenericPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = GenericPackageShapeSpec.VerticalCylinder(
                        radius = FltX(0.5) * Meter,
                        axis = Axis3.X
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-STACK-CYLINDER"),
            packageAttribute = packageAttribute()
        ).toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(1.5) * Meter,
            depth = infraScalar(1.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-ADAPTER-GENERATED-CYLINDER-STACK"
        )

        val generated = CirclePackingLayerGenerator<FltX>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(support, cylinder),
                maxCandidates = 12
            )
        )
        val layer = generated.first { result ->
            result.source == "circle-packing-horizontal-supported-stack-axis=x"
        }.layer
        val placement = layer.toLayerPlacement()

        assertEquals(2, placement.unit.units.size)
        assertTrue(
            placement.unit.units.any { unitPlacement ->
                val shape = unitPlacement.resolvedPackingShape()
                shape is CylinderPackingShape3 && shape.axis == Axis3.X
            }
        )
    }

    @Test
    fun analyzerShouldAcceptKnownCoordinateHorizontalCylinderLayerPlacement() = runBlocking {
        val layer = horizontalCylinderLayer(Axis3.X)
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromGeneric(
            iteration = 6,
            columns = listOf(layer)
        )

        val latest = analyzer.latest
        assertNotNull(latest)
        val item = latest.schema.loadingPlans.single().items.single()
        assertEquals("HorizontalCylinderX", item.algorithmShapeType.name)
        assertEquals("X", item.axis?.name)
        assertEquals("6", latest.schema.kpi["cg_iteration"])
    }

    @Test
    fun analyzerShouldAcceptExplicitKnownCoordinateHorizontalCylinderBins() = runBlocking {
        val layer = horizontalCylinderLayer(Axis3.Z)
        val modelLayer = layer.toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-ANALYZER-CYLINDER-Z"
        )
        val explicitBin = layerBinOf(
            shape = bin,
            units = listOf(modelLayer.toKnownCoordinateLayerPlacement())
        )
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromGeneric(
            iteration = 7,
            columns = listOf(layer),
            bins = listOf(explicitBin)
        )

        val latest = analyzer.latest
        assertNotNull(latest)
        val item = latest.schema.loadingPlans.single().items.single()
        assertEquals("HorizontalCylinderZ", item.algorithmShapeType.name)
        assertEquals("Z", item.axis?.name)
        assertEquals("7", latest.schema.kpi["cg_iteration"])
    }

    @Test
    fun analyzerShouldBuildKnownCoordinateBinsForMultipleHorizontalCylinderAxes() = runBlocking {
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromGeneric(
            iteration = 8,
            columns = listOf(
                horizontalCylinderLayer(Axis3.X),
                horizontalCylinderLayer(Axis3.Z)
            )
        )

        val latest = analyzer.latest
        assertNotNull(latest)
        val items = latest.schema.loadingPlans.flatMap { it.items }
        assertEquals(2, latest.schema.loadingPlans.size)
        assertTrue(items.any { it.algorithmShapeType.name == "HorizontalCylinderX" && it.axis?.name == "X" })
        assertTrue(items.any { it.algorithmShapeType.name == "HorizontalCylinderZ" && it.axis?.name == "Z" })
        assertEquals("8", latest.schema.kpi["cg_iteration"])
    }

    @Test
    fun analyzerShouldRejectKnownCoordinateMixedCylinderAxesWithinSameLayer() = runBlocking {
        val layer = horizontalCylinderLayer(
            axes = listOf(Axis3.X, Axis3.Z),
            positions = listOf(Pair(0.0, 0.0), Pair(1.0, 1.0))
        )
        val analyzer = ColumnGenerationPackingAnalyzer()

        val exception = assertFailsWith<IllegalArgumentException> {
            analyzer.analyzeFromGeneric(
                iteration = 9,
                columns = listOf(layer)
            )
        }

        assertTrue(exception.message?.contains("mixes cylinder axes") == true)
    }

    @Test
    fun analyzerShouldApplyDepthBoundaryPolicyToKnownCoordinateGenericBins() = runBlocking {
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromGeneric(
            iteration = 10,
            columns = listOf(horizontalCylinderLayer(Axis3.X)),
            depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X),
                lastLayerAllowedCylinderAxes = setOf(Axis3.X)
            )
        )

        val latest = analyzer.latest
        assertNotNull(latest)
        assertEquals("10", latest.schema.kpi["cg_iteration"])

        val exception = assertFailsWith<IllegalArgumentException> {
            analyzer.analyzeFromGeneric(
                iteration = 11,
                columns = listOf(horizontalCylinderLayer(Axis3.X)),
                depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                    firstLayerAllowedCylinderAxes = setOf(Axis3.Z)
                )
            )
        }

        assertTrue(exception.message?.contains("boundary=first") == true)
        assertTrue(exception.message?.contains("cylinder_axis=X") == true)
    }

    @Test
    fun analyzerShouldApplyDepthBoundaryPolicyToExplicitGenericBins() = runBlocking {
        val layer = horizontalCylinderLayer(Axis3.Z)
        val modelLayer = layer.toModel()
        val bin = BinType(
            width = infraScalar(2.0) * Meter,
            height = infraScalar(2.0) * Meter,
            depth = infraScalar(2.0) * Meter,
            capacity = infraScalar(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = "BIN-ANALYZER-CYLINDER-Z-POLICY"
        )
        val explicitBin = layerBinOf(
            shape = bin,
            units = listOf(modelLayer.toKnownCoordinateLayerPlacement())
        )

        val exception = assertFailsWith<IllegalArgumentException> {
            ColumnGenerationPackingAnalyzer().analyzeFromGeneric(
                iteration = 12,
                columns = listOf(layer),
                bins = listOf(explicitBin),
                depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                    lastLayerAllowedCylinderAxes = setOf(Axis3.X)
                )
            )
        }

        assertTrue(exception.message?.contains("boundary=last") == true)
        assertTrue(exception.message?.contains("cylinder_axis=Z") == true)
    }

    private fun horizontalCylinderLayer(axis: Axis3): GenericBinLayer<FltX> {
        return horizontalCylinderLayer(
            axes = listOf(axis),
            positions = listOf(Pair(0.0, 0.0))
        )
    }

    private fun horizontalCylinderLayer(
        axes: List<Axis3>,
        positions: List<Pair<Double, Double>>
    ): GenericBinLayer<FltX> {
        require(axes.size == positions.size)
        val material = GenericMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-${axes.joinToString("-")}"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-${axes.joinToString("-")}",
            weight = FltX(0.2) * Kilogram
        )
        val items = axes.map { axis ->
            val shape = GenericPackageShape(
                width = FltX(1.0) * Meter,
                height = FltX(1.0) * Meter,
                depth = FltX(1.0) * Meter,
                weight = FltX(0.2) * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = GenericPackageShapeSpec.VerticalCylinder(
                    radius = FltX(0.4) * Meter,
                    axis = axis
                )
            )
            GenericItem(
                id = "item-adapter-cylinder-$axis",
                name = "item-adapter-cylinder-$axis",
                pack = GenericPackage.innerPackage(
                    shape = shape,
                    materials = mapOf(material to UInt64.one)
                ),
                enabledOrientations = listOf(Orientation.Upright),
                batchNo = BatchNo("B-ADAPTER-CYLINDER-$axis"),
                packageAttribute = packageAttribute()
            )
        }
        return GenericBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationPackingAnalyzerGenericEntryPointTest::class,
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            units = items.zip(positions).map { (item, position) ->
                val (x, z) = position
                GenericItemPlacement(
                    item = item,
                    x = FltX(x) * Meter,
                    y = FltX(0.0) * Meter,
                    z = FltX(z) * Meter
                )
            }
        )
    }
}
