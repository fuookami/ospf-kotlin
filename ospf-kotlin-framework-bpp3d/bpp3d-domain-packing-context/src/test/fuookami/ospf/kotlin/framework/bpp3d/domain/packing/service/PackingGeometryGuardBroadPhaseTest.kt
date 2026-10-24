/**
 * 装箱几何 broad-phase 差分测试。 / Packing geometry broad-phase differential tests.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.random.*
import kotlin.test.*
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.geometry.Axis3
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.*

class PackingGeometryGuardBroadPhaseTest {
    private data class GeometrySpec(
        val x: Double,
        val y: Double,
        val z: Double,
        val width: Double,
        val height: Double,
        val depth: Double,
        val cylinderAxis: Axis3? = null,
        val radius: Double = 0.0
    )

    private data class TestCuboid(
        override val width: Quantity<FltX>,
        override val height: Quantity<FltX>,
        override val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>
    ) : AbstractCuboid<FltX>

    private class DynamicCuboidGetterCounts {
        var widthReads = 0
        var heightReads = 0
        var depthReads = 0

        fun snapshot(): List<Int> {
            return listOf(widthReads, heightReads, depthReads)
        }
    }

    private class DynamicCylinderGetterCounts {
        var radiusReads = 0
        var heightReads = 0
        var axisReads = 0

        fun snapshot(): List<Int> {
            return listOf(radiusReads, heightReads, axisReads)
        }
    }

    private class DynamicCuboid(
        private val widthValue: Quantity<FltX>,
        private val heightValue: Quantity<FltX>,
        private val depthValue: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        private val reads: DynamicCuboidGetterCounts
    ) : AbstractCuboid<FltX> {
        override val width: Quantity<FltX>
            get() {
                reads.widthReads++
                return widthValue
            }

        override val height: Quantity<FltX>
            get() {
                reads.heightReads++
                return heightValue
            }

        override val depth: Quantity<FltX>
            get() {
                reads.depthReads++
                return depthValue
            }
    }

    private class DynamicCylinder(
        private val radiusValue: Quantity<FltX>,
        private val heightValue: Quantity<FltX>,
        private val axisValue: Axis3,
        override val weight: Quantity<FltX>,
        private val reads: DynamicCylinderGetterCounts
    ) : AbstractCylinder<FltX> {
        override val radius: Quantity<FltX>
            get() {
                reads.radiusReads++
                return radiusValue
            }

        override val height: Quantity<FltX>
            get() {
                reads.heightReads++
                return heightValue
            }

        override val axis: Axis3
            get() {
                reads.axisReads++
                return axisValue
            }
    }

    private class StatefulShapeGetterCounts {
        var widthReads = 0
        var heightReads = 0
        var depthReads = 0
        var algorithmTypeReads = 0
        var axisReads = 0

        fun snapshot(): List<Int> {
            return listOf(widthReads, heightReads, depthReads, algorithmTypeReads, axisReads)
        }
    }

    private class StatefulCustomShape(
        private val width: Quantity<FltX>,
        private val height: Quantity<FltX>,
        private val depth: Quantity<FltX>,
        override val weight: Quantity<FltX>,
        private val reads: StatefulShapeGetterCounts
    ) : PackingShape3<FltX> {
        override val shapeType = PackingShapeType.Cuboid
        override val algorithmShapeType: PackingAlgorithmShapeType
            get() {
                reads.algorithmTypeReads++
                return PackingAlgorithmShapeType.Cuboid
            }
        override val axis: Axis3?
            get() {
                reads.axisReads++
                return null
            }
        override val boundingWidth: Quantity<FltX>
            get() {
                reads.widthReads++
                return width
            }
        override val boundingHeight: Quantity<FltX>
            get() {
                reads.heightReads++
                return height
            }
        override val boundingDepth: Quantity<FltX>
            get() {
                reads.depthReads++
                return depth
            }
        override val actualVolume: Quantity<FltX> get() = width * height * depth

        override fun footprint(): ShapeFootprint2<FltX> {
            return ShapeFootprint2.Rectangle(width = width, depth = depth)
        }
    }

    private fun packageAttribute(): PackageAttribute {
        return PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun packingShape(spec: GeometrySpec): PackingShape3<FltX> {
        val weight = FltX.one * Kilogram
        return if (spec.cylinderAxis == null) {
            CuboidPackingShape3(
                cuboid = TestCuboid(
                    width = FltX(spec.width) * Meter,
                    height = FltX(spec.height) * Meter,
                    depth = FltX(spec.depth) * Meter,
                    weight = weight
                )
            )
        } else {
            val shapeAxis = requireNotNull(spec.cylinderAxis)
            CylinderPackingShape3(
                cylinder = object : AbstractCylinder<FltX> {
                    override val radius = FltX(spec.radius) * Meter
                    override val height = FltX(
                        when (shapeAxis) {
                            Axis3.X -> spec.width
                            Axis3.Y -> spec.height
                            Axis3.Z -> spec.depth
                        }
                    ) * Meter
                    override val axis = shapeAxis
                    override val weight = weight
                }
            )
        }
    }

    private fun item(spec: GeometrySpec, index: Int): Item {
        val shape = packingShape(spec)
        return ActualItem(
            id = itemIdOf("geometry-$index"),
            name = "geometry-$index",
            width = shape.boundingWidth,
            height = shape.boundingHeight,
            depth = shape.boundingDepth,
            weight = FltX.one * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            packageAttribute = packageAttribute(),
            packingShapeOverride = shape,
            materialAmountsOverride = emptyMap()
        )
    }

    private fun packedBin(
        specs: List<GeometrySpec>,
        binSize: Double = 100.0,
        name: String = "geometry-bin",
        coordinateScale: FltX = FltX.one
    ): PackedBin {
        val type = BinType(
            width = FltX(binSize) * coordinateScale * Meter,
            height = FltX(binSize) * coordinateScale * Meter,
            depth = FltX(binSize) * coordinateScale * Meter,
            capacity = FltX(1_000_000.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("GEOMETRY-BIN")
        )
        return PackedBin(
            name = name,
            type = type,
            items = specs.mapIndexed { index, spec ->
                PackedItem(
                    placement = itemPlacement3Of(
                        item = item(spec = spec, index = index),
                        position = point3(
                            x = FltX(spec.x) * coordinateScale * Meter,
                            y = FltX(spec.y) * coordinateScale * Meter,
                            z = FltX(spec.z) * coordinateScale * Meter
                        )
                    ),
                    loadingOrder = UInt64(index)
                )
            }
        )
    }

    private fun packedBinWithShapes(
        shapes: List<PackingShape3<FltX>>,
        xPositions: List<Double>,
        name: String
    ): PackedBin {
        val type = BinType(
            width = FltX(100.0) * Meter,
            height = FltX(100.0) * Meter,
            depth = FltX(100.0) * Meter,
            capacity = FltX(1_000_000.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("DYNAMIC-GEOMETRY-BIN")
        )
        return PackedBin(
            name = name,
            type = type,
            items = shapes.mapIndexed { index, shape ->
                val item = ActualItem(
                    id = itemIdOf("dynamic-$index"),
                    name = "dynamic-$index",
                    width = shape.boundingWidth,
                    height = shape.boundingHeight,
                    depth = shape.boundingDepth,
                    weight = FltX.one * Kilogram,
                    enabledOrientations = listOf(Orientation.Upright),
                    packageAttribute = packageAttribute(),
                    packingShapeOverride = shape,
                    materialAmountsOverride = emptyMap()
                )
                PackedItem(
                    placement = itemPlacement3Of(
                        item = item,
                        position = point3(
                            x = FltX(xPositions[index]) * Meter,
                            y = FltX.zero * Meter,
                            z = FltX.zero * Meter
                        )
                    ),
                    loadingOrder = UInt64(index)
                )
            }
        )
    }

    private fun dynamicCuboidBin(): Pair<PackedBin, List<DynamicCuboidGetterCounts>> {
        val reads = (0 until 16).map { DynamicCuboidGetterCounts() }
        val shapes = reads.map { getterReads ->
            CuboidPackingShape3(
                cuboid = DynamicCuboid(
                    widthValue = FltX.one * Meter,
                    heightValue = FltX.one * Meter,
                    depthValue = FltX.one * Meter,
                    weight = FltX.one * Kilogram,
                    reads = getterReads
                )
            )
        }
        return Pair(
            packedBinWithShapes(
                shapes = shapes,
                xPositions = (0 until 16).map { index -> index * 3.0 },
                name = "dynamic-cuboid-bin"
            ),
            reads
        )
    }

    private fun dynamicCylinderBin(): Pair<PackedBin, List<DynamicCylinderGetterCounts>> {
        val reads = (0 until 16).map { DynamicCylinderGetterCounts() }
        val shapes = reads.map { getterReads ->
            CylinderPackingShape3(
                cylinder = DynamicCylinder(
                    radiusValue = FltX(0.5) * Meter,
                    heightValue = FltX.one * Meter,
                    axisValue = Axis3.Y,
                    weight = FltX.one * Kilogram,
                    reads = getterReads
                )
            )
        }
        return Pair(
            packedBinWithShapes(
                shapes = shapes,
                xPositions = (0 until 16).map { index -> index * 3.0 },
                name = "dynamic-cylinder-bin"
            ),
            reads
        )
    }

    private fun getterReadDeltas(
        before: List<List<Int>>,
        after: List<List<Int>>
    ): List<List<Int>> {
        return before.zip(after).map { (initial, final) ->
            final.zip(initial).map { (finalCount, initialCount) -> finalCount - initialCount }
        }
    }

    private fun signature(result: Try): String {
        return when (result) {
            is Ok -> "ok"
            is Failed -> "failed:${result.error.message}"
            is Fatal -> "fatal:${result.errors.joinToString { it.message }}"
        }
    }

    private fun assertMatchesReference(bin: PackedBin) {
        val expected = requirePackedBinShapeGeometryReference(bin = bin, source = "broad-phase-test")
        val actual = requirePackedBinShapeGeometry(bin = bin, source = "broad-phase-test")
        assertEquals(signature(expected), signature(actual))
    }

    private fun statefulCustomBin(): Pair<PackedBin, List<StatefulShapeGetterCounts>> {
        val binSize = 100.0
        val type = BinType(
            width = FltX(binSize) * Meter,
            height = FltX(binSize) * Meter,
            depth = FltX(binSize) * Meter,
            capacity = FltX(1_000_000.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("STATEFUL-GEOMETRY-BIN")
        )
        val reads = (0 until 16).map { StatefulShapeGetterCounts() }
        val items = reads.mapIndexed { index, getterReads ->
            val x = when (index) {
                2 -> 10.0
                3 -> 10.5
                else -> 30.0 + index * 3.0
            }
            val shape = StatefulCustomShape(
                width = FltX.one * Meter,
                height = FltX.one * Meter,
                depth = FltX.one * Meter,
                weight = FltX.one * Kilogram,
                reads = getterReads
            )
            val item = ActualItem(
                id = itemIdOf("stateful-$index"),
                name = "stateful-$index",
                width = FltX.one * Meter,
                height = FltX.one * Meter,
                depth = FltX.one * Meter,
                weight = FltX.one * Kilogram,
                enabledOrientations = listOf(Orientation.Upright),
                packageAttribute = packageAttribute(),
                packingShapeOverride = shape
            )
            PackedItem(
                placement = itemPlacement3Of(
                    item = item,
                    position = point3(
                        x = FltX(x) * Meter,
                        y = FltX.zero * Meter,
                        z = FltX.zero * Meter
                    )
                ),
                loadingOrder = UInt64(index)
            )
        }
        return Pair(
            PackedBin(
                name = "stateful-bin",
                type = type,
                items = items
            ),
            reads
        )
    }

    private fun box(
        x: Double,
        y: Double = 0.0,
        z: Double = 0.0,
        width: Double = 1.0,
        height: Double = 1.0,
        depth: Double = 1.0
    ): GeometrySpec {
        return GeometrySpec(
            x = x,
            y = y,
            z = z,
            width = width,
            height = height,
            depth = depth
        )
    }

    private fun cylinder(
        axis: Axis3,
        x: Double,
        y: Double,
        z: Double,
        radius: Double,
        length: Double
    ): GeometrySpec {
        val diameter = radius * 2.0
        val (width, height, depth) = when (axis) {
            Axis3.X -> Triple(
                first = length,
                second = diameter,
                third = diameter
            )

            Axis3.Y -> Triple(
                first = diameter,
                second = length,
                third = diameter
            )

            Axis3.Z -> Triple(
                first = diameter,
                second = diameter,
                third = length
            )
        }
        return GeometrySpec(
            x = x,
            y = y,
            z = z,
            width = width,
            height = height,
            depth = depth,
            cylinderAxis = axis,
            radius = radius
        )
    }

    @Test
    fun randomizedCuboidsAndMixedCylinderAxesShouldMatchHeadExhaustiveGuard() {
        for (seed in 0 until 20) {
            val random = Random(seed)
            val specs = (0 until 48).map { _ ->
                when (random.nextInt(4)) {
                    0 -> box(
                        x = random.nextDouble(0.0, 90.0),
                        y = random.nextDouble(0.0, 90.0),
                        z = random.nextDouble(0.0, 90.0),
                        width = random.nextDouble(0.2, 2.5),
                        height = random.nextDouble(0.2, 2.5),
                        depth = random.nextDouble(0.2, 2.5)
                    )

                    else -> {
                        val axis = when (random.nextInt(3)) {
                            0 -> Axis3.X
                            1 -> Axis3.Y
                            else -> Axis3.Z
                        }
                        cylinder(
                            axis = axis,
                            x = random.nextDouble(0.0, 90.0),
                            y = if (axis == Axis3.Y) random.nextDouble(0.0, 90.0) else 0.0,
                            z = random.nextDouble(0.0, 90.0),
                            radius = random.nextDouble(0.2, 1.0),
                            length = random.nextDouble(0.2, 2.5)
                        )
                    }
                }
            }
            assertMatchesReference(
                packedBin(
                    specs = specs,
                    name = "random-$seed"
                )
            )
        }
    }

    @Test
    fun separatedSparsePackingShouldMatchHeadExhaustiveGuard() {
        val specs = (0 until 128).map { index ->
            box(
                x = index * 1.5,
                width = 1.0,
                height = 1.0,
                depth = 1.0
            )
        }
        assertMatchesReference(
            packedBin(
                specs = specs,
                binSize = 200.0
            )
        )
    }

    @Test
    fun overlapToleranceBoundaryShouldMatchHeadExhaustiveGuard() {
        val below = (0 until 16).map { index ->
            when (index) {
                0 -> box(x = 1.0)
                1 -> box(x = 2.0 - 0.999e-7)
                else -> box(x = 10.0 + index * 2.0)
            }
        }
        val above = (0 until 16).map { index ->
            when (index) {
                0 -> box(x = 1.0)
                1 -> box(x = 2.0 - 1.001e-7)
                else -> box(x = 10.0 + index * 2.0)
            }
        }
        assertMatchesReference(packedBin(specs = below))
        assertMatchesReference(packedBin(specs = above))
    }

    @Test
    fun firstOverlapMustRemainInOriginalPairOrder() {
        val specs = mutableListOf(
            box(x = 10.0, width = 2.0),
            box(x = 20.0, width = 2.0),
            box(x = 11.0, width = 2.0),
            box(x = 10.5, width = 2.0)
        )
        specs += (0 until 14).map { index -> box(x = 30.0 + index * 3.0) }
        val bin = packedBin(specs = specs)
        assertMatchesReference(bin)
        val result = requirePackedBinShapeGeometry(bin = bin, source = "broad-phase-test")
        assertTrue(signature(result).contains("item[0]") && signature(result).contains("overlaps item[2]"))
    }

    @Test
    fun unknownStatefulShapeGettersAndDiagnosticsMustMatchHeadExhaustiveGuard() {
        val (referenceBin, referenceReads) = statefulCustomBin()
        val reference = requirePackedBinShapeGeometryReference(
            bin = referenceBin,
            source = "broad-phase-test"
        )
        val referenceSignature = signature(reference)
        val referenceCounts = referenceReads.map { reads -> reads.snapshot() }

        val (optimizedBin, optimizedReads) = statefulCustomBin()
        val optimized = requirePackedBinShapeGeometry(
            bin = optimizedBin,
            source = "broad-phase-test"
        )
        assertEquals(referenceSignature, signature(optimized))
        assertEquals(referenceCounts, optimizedReads.map { reads -> reads.snapshot() })
        assertTrue(referenceSignature.contains("item[2]") && referenceSignature.contains("overlaps item[3]"))
    }

    @Test
    fun dynamicCuboidGetterCountsMustMatchHeadExhaustiveGuard() {
        val (referenceBin, referenceReads) = dynamicCuboidBin()
        val referenceBefore = referenceReads.map { reads -> reads.snapshot() }
        val reference = requirePackedBinShapeGeometryReference(
            bin = referenceBin,
            source = "broad-phase-test"
        )
        val referenceCounts = getterReadDeltas(
            before = referenceBefore,
            after = referenceReads.map { reads -> reads.snapshot() }
        )

        val (optimizedBin, optimizedReads) = dynamicCuboidBin()
        val optimizedBefore = optimizedReads.map { reads -> reads.snapshot() }
        val optimized = requirePackedBinShapeGeometry(
            bin = optimizedBin,
            source = "broad-phase-test"
        )
        val optimizedCounts = getterReadDeltas(
            before = optimizedBefore,
            after = optimizedReads.map { reads -> reads.snapshot() }
        )

        assertEquals(signature(reference), signature(optimized))
        assertEquals(referenceCounts, optimizedCounts)
    }

    @Test
    fun dynamicCylinderGetterCountsMustMatchHeadExhaustiveGuard() {
        val (referenceBin, referenceReads) = dynamicCylinderBin()
        val referenceBefore = referenceReads.map { reads -> reads.snapshot() }
        val reference = requirePackedBinShapeGeometryReference(
            bin = referenceBin,
            source = "broad-phase-test"
        )
        val referenceCounts = getterReadDeltas(
            before = referenceBefore,
            after = referenceReads.map { reads -> reads.snapshot() }
        )

        val (optimizedBin, optimizedReads) = dynamicCylinderBin()
        val optimizedBefore = optimizedReads.map { reads -> reads.snapshot() }
        val optimized = requirePackedBinShapeGeometry(
            bin = optimizedBin,
            source = "broad-phase-test"
        )
        val optimizedCounts = getterReadDeltas(
            before = optimizedBefore,
            after = optimizedReads.map { reads -> reads.snapshot() }
        )

        assertEquals(signature(reference), signature(optimized))
        assertEquals(referenceCounts, optimizedCounts)
    }

    @Test
    fun horizontalCylinderSupportFailureMustPrecedeOverlapDiagnostics() {
        val specs = mutableListOf(
            cylinder(
                axis = Axis3.X,
                x = 10.0,
                y = 1.0,
                z = 10.0,
                radius = 1.0,
                length = 2.0
            ),
            box(x = 10.5, y = 1.0, z = 10.0, width = 1.0, height = 1.0, depth = 1.0)
        )
        specs += (0 until 14).map { index -> box(x = 30.0 + index * 3.0) }
        val result = signature(
            requirePackedBinShapeGeometry(
                bin = packedBin(specs = specs),
                source = "broad-phase-test"
            )
        )
        assertTrue(result.contains("type=horizontal_support"))
        assertFalse(result.contains("type=overlap"))
        assertMatchesReference(packedBin(specs = specs))
    }

    @Test
    fun unsafeNumericRangesShouldMatchHeadExhaustiveGuard() {
        val nearUlp = (0 until 16).map { index ->
            when (index) {
                0 -> box(x = 100_000_000.0, width = 1.0)
                1 -> box(x = 100_000_000.99999994, width = 1.0)
                else -> box(x = 100_000_010.0 + index * 2.0)
            }
        }
        assertMatchesReference(
            packedBin(
                specs = nearUlp,
                binSize = 200_000_000.0
            )
        )

        val huge = mutableListOf(
            cylinder(
                axis = Axis3.X,
                x = 1e160,
                y = 0.0,
                z = 1e160,
                radius = 1e160,
                length = 1e160
            ),
            cylinder(
                axis = Axis3.Z,
                x = 1e160,
                y = 0.0,
                z = 1e160,
                radius = 1e160,
                length = 1e160
            )
        )
        huge += (0 until 14).map { index ->
            box(
                x = 3e160 + index * 3e160,
                width = 1e160,
                height = 1e160,
                depth = 1e160
            )
        }
        assertMatchesReference(
            packedBin(
                specs = huge,
                binSize = 1e200
            )
        )

        // FltX rejects NaN; finite decimal coordinates can still overflow the guard's Double conversion.
        // FltX 不接受 NaN，但有限十进制坐标仍可能在守卫转换为 Double 时溢出。
        val nonFinite = List(16) { index -> box(x = index * 2.0) }
        assertMatchesReference(packedBin(
            specs = nonFinite,
            coordinateScale = FltX("1e400")
        ))
    }
}
