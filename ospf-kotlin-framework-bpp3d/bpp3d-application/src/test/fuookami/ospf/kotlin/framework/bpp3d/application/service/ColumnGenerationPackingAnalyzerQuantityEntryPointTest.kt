/**
 * 列生成装箱分析器物理量入口测试。
 * Column generation packing analyzer quantity entry point test.
 */
package fuookami.ospf.kotlin.framework.bpp3d.application.service

import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.layer_generation.*

/** 获取 Ret 错误消息或失败 / Get Ret error message or fail */
private fun Ret<*>.errorMessageOrFail(message: String): String {
    return when (this) {
        is Ok -> fail(message)
        is Failed -> error.message ?: ""
        is Fatal -> errors.joinToString("; ") { it.message }
    }
}

class ColumnGenerationPackingAnalyzerQuantityEntryPointTest {
    private object Cargo : AbstractCargoAttribute

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX(0.0)),
            hangingPolicy = AbsoluteHangingPolicy(FltX(0.0)),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    @Test
    fun analyzerShouldSupportQuantityLayerEntryPoint() = runBlocking {
        val material = QuantityMaterial(
            no = MaterialNo("M-ANALYZER-GENERIC"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ANALYZER-GENERIC",
            weight = FltX(0.2) * Kilogram
        )
        val shape = QuantityPackageShape(
            width = FltX(1.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(1.0) * Meter,
            weight = FltX(0.2) * Kilogram,
            packageType = PackageType.CartonContainer
        )
        val item = QuantityItem(
            id = itemIdOf("item-analyzer-quantity"),
            name = "item-analyzer-quantity",
            pack = QuantityPackage.innerPackage(
                shape = shape,
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ANALYZER-GENERIC"),
            packageAttribute = packageAttribute()
        )
        val layer = QuantityBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationPackingAnalyzerQuantityEntryPointTest::class,
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            units = listOf(
                QuantityItemPlacement(
                    item = item,
                    x = FltX(0.0) * Meter,
                    y = FltX(0.0) * Meter,
                    z = FltX(0.0) * Meter
                )
            )
        )
        val bin = BinType(
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ANALYZER-GENERIC")
        )
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromQuantity(
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

            val error = layer.toModel().toLayerPlacement()
                .errorMessageOrFail("layer placement should fail")

            assertTrue(error.contains("only verified axis-aware generated candidates are allowed"))
        }
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderCandidate() = runBlocking {
        val item = horizontalCylinderLayer(Axis3.X).units.single().item.toModel()
        val bin = BinType(
            width = FltX(3.0) * Meter,
            height = FltX(1.0) * Meter,
            depth = FltX(2.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ADAPTER-GENERATED-CYLINDER-X")
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
        val placement = layer.toLayerPlacement().value ?: fail("layer placement should be built")

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
        val material = QuantityMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-STACK"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-STACK",
            weight = FltX(0.2) * Kilogram
        )
        val support = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-stack-support"),
            name = "item-adapter-cylinder-stack-support",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
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
        val cylinder = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-stack-cylinder"),
            name = "item-adapter-cylinder-stack-cylinder",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
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
            width = FltX(2.0) * Meter,
            height = FltX(1.5) * Meter,
            depth = FltX(1.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ADAPTER-GENERATED-CYLINDER-STACK")
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
        val placement = layer.toLayerPlacement().value ?: fail("layer placement should be built")

        assertEquals(2, placement.unit.units.size)
        assertTrue(
            placement.unit.units.any { unitPlacement ->
                val shape = unitPlacement.resolvedPackingShape()
                shape is CylinderPackingShape3 && shape.axis == Axis3.X
            }
        )
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderMultiSupportStack() = runBlocking {
        val material = QuantityMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-MULTI-STACK"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-MULTI-STACK",
            weight = FltX(0.2) * Kilogram
        )
        val support = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-multi-stack-support"),
            name = "item-adapter-cylinder-multi-stack-support",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(0.4) * Meter,
                    height = FltX(0.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-MULTI-STACK-SUPPORT"),
            packageAttribute = packageAttribute()
        ).toModel()
        val cylinder = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-multi-stack-cylinder"),
            name = "item-adapter-cylinder-multi-stack-cylinder",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                        radius = FltX(0.5) * Meter,
                        axis = Axis3.X
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-MULTI-STACK-CYLINDER"),
            packageAttribute = packageAttribute()
        ).toModel()
        val bin = BinType(
            width = FltX(1.2) * Meter,
            height = FltX(1.5) * Meter,
            depth = FltX(1.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ADAPTER-GENERATED-CYLINDER-MULTI-STACK")
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
            result.source == "circle-packing-horizontal-supported-stack-multi-axis=x"
        }.layer
        val placement = layer.toLayerPlacement().value ?: fail("layer placement should be built")

        assertEquals(4, placement.unit.units.size)
        assertTrue(
            placement.unit.units.any { unitPlacement ->
                val shape = unitPlacement.resolvedPackingShape()
                shape is CylinderPackingShape3 && shape.axis == Axis3.X
            }
        )
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderMultiHangingSupport() = runBlocking {
        for (axis in listOf(Axis3.X, Axis3.Z)) {
            assertGeneratedHorizontalCylinderMultiHangingSupport(axis)
        }
    }

    private suspend fun assertGeneratedHorizontalCylinderMultiHangingSupport(axis: Axis3) {
        val material = QuantityMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-MULTI-HANGING-$axis"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-MULTI-HANGING-$axis",
            weight = FltX(0.2) * Kilogram
        )
        val support = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-multi-hanging-support-$axis"),
            name = "item-adapter-cylinder-multi-hanging-support-$axis",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(0.4) * Meter,
                    height = FltX(0.2) * Meter,
                    depth = FltX(0.2) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-MULTI-HANGING-SUPPORT-$axis"),
            packageAttribute = packageAttribute()
        ).toModel()
        val cylinder = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-multi-hanging-cylinder-$axis"),
            name = "item-adapter-cylinder-multi-hanging-cylinder-$axis",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                        radius = FltX(0.5) * Meter,
                        axis = axis
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-MULTI-HANGING-CYLINDER-$axis"),
            packageAttribute = packageAttribute()
        ).toModel()
        val bin = BinType(
            width = FltX(1.2) * Meter,
            height = FltX(1.5) * Meter,
            depth = FltX(1.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ADAPTER-GENERATED-CYLINDER-MULTI-HANGING-$axis")
        )

        val generated = CirclePackingLayerGenerator<FltX>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(support, cylinder),
                maxCandidates = 16
            )
        )
        val sourceAxis = axis.name.lowercase()
        val layer = generated.first { result ->
            result.source == "circle-packing-horizontal-hanging-support-multi-axis=$sourceAxis"
        }.layer
        val placement = layer.toLayerPlacement().value ?: fail("layer placement should be built")

        val expectedSupportCount = if (axis == Axis3.X) {
            3
        } else {
            5
        }
        assertEquals(expectedSupportCount + 1, placement.unit.units.size)
        assertTrue(
            placement.unit.units.any { unitPlacement ->
                val shape = unitPlacement.resolvedPackingShape()
                shape is CylinderPackingShape3 && shape.axis == axis
            }
        )
    }

    @Test
    fun layerPlacementAdapterShouldAcceptGeneratedHorizontalCylinderHeterogeneousSupportStack() = runBlocking {
        val material = QuantityMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-HETEROGENEOUS-STACK"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-HETEROGENEOUS-STACK",
            weight = FltX(0.2) * Kilogram
        )
        val supportA = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-heterogeneous-stack-support-a"),
            name = "item-adapter-cylinder-heterogeneous-stack-support-a",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(0.4) * Meter,
                    height = FltX(0.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-HETEROGENEOUS-STACK-SUPPORT-A"),
            packageAttribute = packageAttribute()
        ).toModel()
        val supportB = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-heterogeneous-stack-support-b"),
            name = "item-adapter-cylinder-heterogeneous-stack-support-b",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(0.6) * Meter,
                    height = FltX(0.2) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-HETEROGENEOUS-STACK-SUPPORT-B"),
            packageAttribute = packageAttribute()
        ).toModel()
        val cylinder = QuantityItem(
            id = itemIdOf("item-adapter-cylinder-heterogeneous-stack-cylinder"),
            name = "item-adapter-cylinder-heterogeneous-stack-cylinder",
            pack = QuantityPackage.innerPackage(
                shape = QuantityPackageShape(
                    width = FltX(1.0) * Meter,
                    height = FltX(1.0) * Meter,
                    depth = FltX(1.0) * Meter,
                    weight = FltX(0.2) * Kilogram,
                    packageType = PackageType.CartonContainer,
                    shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                        radius = FltX(0.5) * Meter,
                        axis = Axis3.X
                    )
                ),
                materials = mapOf(material to UInt64.one)
            ),
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-ADAPTER-CYLINDER-HETEROGENEOUS-STACK-CYLINDER"),
            packageAttribute = packageAttribute()
        ).toModel()
        val bin = BinType(
            width = FltX(1.2) * Meter,
            height = FltX(1.5) * Meter,
            depth = FltX(1.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ADAPTER-GENERATED-CYLINDER-HETEROGENEOUS-STACK")
        )

        val generated = CirclePackingLayerGenerator<FltX>().generate(
            Bpp3dLayerGenerationRequest(
                iteration = 0,
                bin = bin,
                items = listOf(supportA, supportB, cylinder),
                maxCandidates = 16
            )
        )
        val layer = generated.first { result ->
            result.source == "circle-packing-horizontal-supported-stack-heterogeneous-axis=x"
        }.layer
        val placement = layer.toLayerPlacement().value ?: fail("layer placement should be built")

        assertEquals(3, placement.unit.units.size)
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

        analyzer.analyzeFromQuantity(
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
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ANALYZER-CYLINDER-Z")
        )
        val explicitBin = layerBinOf(
            shape = bin,
            units = listOf(modelLayer.toKnownCoordinateLayerPlacement())
        )
        val analyzer = ColumnGenerationPackingAnalyzer()

        analyzer.analyzeFromQuantity(
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

        analyzer.analyzeFromQuantity(
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

        val result = analyzer.analyzeFromQuantity(
            iteration = 9,
            columns = listOf(layer)
        )

        assertTrue(result is Failed, "Expected Failed result but got ${result::class.simpleName}")
        val errorMessage = result.errorMessageOrFail("Expected Failed result")
        assertTrue(errorMessage.contains("mixes cylinder axes"), "Error message should contain 'mixes cylinder axes': $errorMessage")
    }

    @Test
    fun analyzerShouldApplyDepthBoundaryPolicyToKnownCoordinateQuantityBins() = runBlocking {
        val analyzer = ColumnGenerationPackingAnalyzer()

        val successResult = analyzer.analyzeFromQuantity(
            iteration = 10,
            columns = listOf(horizontalCylinderLayer(Axis3.X)),
            depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.X),
                lastLayerAllowedCylinderAxes = setOf(Axis3.X)
            )
        )

        assertTrue(successResult is Ok, "Expected Ok result for valid policy")
        val latest = analyzer.latest
        assertNotNull(latest)
        assertEquals("10", latest.schema.kpi["cg_iteration"])

        val result = analyzer.analyzeFromQuantity(
            iteration = 11,
            columns = listOf(horizontalCylinderLayer(Axis3.X)),
            depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                firstLayerAllowedCylinderAxes = setOf(Axis3.Z)
            )
        )

        assertTrue(result is Failed, "Expected Failed result but got ${result::class.simpleName}")
        val errorMessage = result.errorMessageOrFail("Expected Failed result")
        assertTrue(errorMessage.contains("boundary=first"), "Error message should contain 'boundary=first': $errorMessage")
        assertTrue(errorMessage.contains("cylinder_axis=X"), "Error message should contain 'cylinder_axis=X': $errorMessage")
    }

    @Test
    fun analyzerShouldApplyDepthBoundaryPolicyToExplicitQuantityBins() = runBlocking {
        val layer = horizontalCylinderLayer(Axis3.Z)
        val modelLayer = layer.toModel()
        val bin = BinType(
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            capacity = FltX(20.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("BIN-ANALYZER-CYLINDER-Z-POLICY")
        )
        val explicitBin = layerBinOf(
            shape = bin,
            units = listOf(modelLayer.toKnownCoordinateLayerPlacement())
        )

        val result = ColumnGenerationPackingAnalyzer().analyzeFromQuantity(
            iteration = 12,
            columns = listOf(layer),
            bins = listOf(explicitBin),
            depthBoundaryLayerOrientationPolicy = DepthBoundaryLayerOrientationPolicy(
                lastLayerAllowedCylinderAxes = setOf(Axis3.X)
            )
        )

        assertTrue(result is Failed, "Expected Failed result but got ${result::class.simpleName}")
        val errorMessage = result.errorMessageOrFail("Expected Failed result")
        assertTrue(errorMessage.contains("boundary=last"), "Error message should contain 'boundary=last': $errorMessage")
        assertTrue(errorMessage.contains("cylinder_axis=Z"), "Error message should contain 'cylinder_axis=Z': $errorMessage")
    }

    private fun horizontalCylinderLayer(axis: Axis3): QuantityBinLayer<FltX> {
        return horizontalCylinderLayer(
            axes = listOf(axis),
            positions = listOf(Pair(0.0, 0.0))
        )
    }

    private fun horizontalCylinderLayer(
        axes: List<Axis3>,
        positions: List<Pair<Double, Double>>
    ): QuantityBinLayer<FltX> {
        require(axes.size == positions.size)
        val material = QuantityMaterial(
            no = MaterialNo("M-ADAPTER-CYLINDER-${axes.joinToString("-")}"),
            type = MaterialType.RawMaterial,
            cargo = Cargo,
            name = "M-ADAPTER-CYLINDER-${axes.joinToString("-")}",
            weight = FltX(0.2) * Kilogram
        )
        val items = axes.map { axis ->
            val shape = QuantityPackageShape(
                width = FltX(1.0) * Meter,
                height = FltX(1.0) * Meter,
                depth = FltX(1.0) * Meter,
                weight = FltX(0.2) * Kilogram,
                packageType = PackageType.CartonContainer,
                shapeSpec = QuantityPackageShapeSpec.VerticalCylinder(
                    radius = FltX(0.4) * Meter,
                    axis = axis
                )
            )
            QuantityItem(
                id = itemIdOf("item-adapter-cylinder-$axis"),
                name = "item-adapter-cylinder-$axis",
                pack = QuantityPackage.innerPackage(
                    shape = shape,
                    materials = mapOf(material to UInt64.one)
                ),
                enabledOrientations = listOf(Orientation.Upright),
                batchNo = BatchNo("B-ADAPTER-CYLINDER-$axis"),
                packageAttribute = packageAttribute()
            )
        }
        return QuantityBinLayer(
            iteration = Int64.zero,
            from = ColumnGenerationPackingAnalyzerQuantityEntryPointTest::class,
            width = FltX(2.0) * Meter,
            height = FltX(2.0) * Meter,
            depth = FltX(2.0) * Meter,
            units = items.zip(positions).map { (item, position) ->
                val (x, z) = position
                QuantityItemPlacement(
                    item = item,
                    x = FltX(x) * Meter,
                    y = FltX(0.0) * Meter,
                    z = FltX(z) * Meter
                )
            }
        )
    }
}
