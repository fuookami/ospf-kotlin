package fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.math.algebra.number.Flt64
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.Csp1dInitialCuttingPlanGenerator
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationBenchmarkSnapshot
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationInput
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStatistics
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.CuttingPlanGenerationStopReason
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.GenerationConstraints
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Flt64QuantityArithmetic
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Material
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.Product
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.ProductDemand
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.QuantityRange
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.RollCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.SheetCountUnit
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.WidthRange

class GeneratorMediumScaleBaselineTest {
    private val arithmetic = Flt64QuantityArithmetic

    @Test
    fun generatorsShouldReportMediumScaleBaselineStatistics() {
        val products = listOf(
            product(id = "p-025", width = 0.25),
            product(id = "p-030", width = 0.30),
            product(id = "p-035", width = 0.35),
            product(id = "p-040", width = 0.40),
            product(id = "p-045", width = 0.45),
            product(id = "p-050", width = 0.50)
        )
        val materials = listOf(
            material(id = "m-120", upperBound = 1.20),
            material(id = "m-140", upperBound = 1.40),
            material(id = "m-160", upperBound = 1.60)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = materials,
            machines = emptyList(),
            demands = products.mapIndexed { index, product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(10.0 + index), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(5UL),
            parallelism = 2,
            enableDominancePruning = true
        )
        val generatorCases = generatorCases(
            constraints = constraints,
            maxPlans = 512,
            nSumDepth = UInt64(5UL)
        )

        val snapshots = LinkedHashMap<String, CuttingPlanGenerationBenchmarkSnapshot>()
        for (case in generatorCases) {
            val report = case.generator.generateWithReport(input)
            val baseline = report.statistics
            snapshots[case.name] = CuttingPlanGenerationBenchmarkSnapshot.from(
                generatorName = case.name,
                statistics = baseline
            )

            assertMediumScaleBaseline(
                name = case.name,
                statistics = baseline
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should generate plans")
            assertTrue(report.plans.size <= 512, "${case.name} should respect maxPlans")
        }
        assertTrue(snapshots.keys == setOf("DFS", "NSum", "NSame", "FullSum"))
        assertTrue(snapshots.values.all { it.acceptedPlans > 0 })
        assertEquals(
            listOf(
                "generator=DFS;visitedNodes=685;generatedCandidates=405;acceptedPlans=405;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSum;visitedNodes=683;generatedCandidates=405;acceptedPlans=405;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSame;visitedNodes=60;generatedCandidates=60;acceptedPlans=60;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=FullSum;visitedNodes=683;generatedCandidates=405;acceptedPlans=405;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted"
            ),
            snapshots.values.map { it.toStableLine() }
        )
    }

    @Test
    fun generatorsShouldReportMixedUnitBenchmarkStatistics() {
        val rollNarrow = product(id = "p-roll-060", width = 0.60)
        val rollWide = product(id = "p-roll-075", width = 0.75)
        val sheetNarrow = product(id = "p-sheet-025", width = 0.25)
        val sheetWide = product(id = "p-sheet-050", width = 0.50)
        val products = listOf(
            rollNarrow,
            rollWide,
            sheetNarrow,
            sheetWide
        )
        val materials = listOf(
            material(id = "m-mixed-100", upperBound = 1.00),
            material(id = "m-mixed-125", upperBound = 1.25),
            material(id = "m-mixed-150", upperBound = 1.50)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = materials,
            machines = emptyList(),
            demands = listOf(
                ProductDemand.roll(
                    product = rollNarrow,
                    quantity = Quantity(Flt64(8.0), RollCountUnit)
                ),
                ProductDemand.roll(
                    product = rollWide,
                    quantity = Quantity(Flt64(6.0), RollCountUnit)
                ),
                ProductDemand.sheet(
                    product = sheetNarrow,
                    quantity = Quantity(Flt64(20.0), SheetCountUnit)
                ),
                ProductDemand.sheet(
                    product = sheetWide,
                    quantity = Quantity(Flt64(12.0), SheetCountUnit)
                )
            )
        )
        val constraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(4UL),
            parallelism = 2,
            enableDominancePruning = true
        )
        val generatorCases = generatorCases(
            constraints = constraints,
            maxPlans = 384,
            nSumDepth = UInt64(4UL)
        )

        val snapshots = LinkedHashMap<String, CuttingPlanGenerationBenchmarkSnapshot>()
        for (case in generatorCases) {
            val report = case.generator.generateWithReport(input)
            val baseline = report.statistics
            snapshots[case.name] = CuttingPlanGenerationBenchmarkSnapshot.from(
                generatorName = case.name,
                statistics = baseline
            )

            assertMediumScaleBaseline(
                name = case.name,
                statistics = baseline
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should generate plans")
            assertTrue(report.plans.size <= 384, "${case.name} should respect maxPlans")
        }
        assertTrue(snapshots.keys == setOf("DFS", "NSum", "NSame", "FullSum"))
        assertTrue(snapshots.values.all { it.acceptedPlans > 0 })
        assertEquals(
            listOf(
                "generator=DFS;visitedNodes=100;generatedCandidates=59;acceptedPlans=59;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSum;visitedNodes=99;generatedCandidates=59;acceptedPlans=59;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSame;visitedNodes=28;generatedCandidates=28;acceptedPlans=28;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=FullSum;visitedNodes=99;generatedCandidates=59;acceptedPlans=59;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=3;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted"
            ),
            snapshots.values.map { it.toStableLine() }
        )
    }

    @Test
    fun generatorsShouldReportExpandedScaleBenchmarkStatistics() {
        val products = listOf(
            product(id = "p-expanded-018", width = 0.18),
            product(id = "p-expanded-023", width = 0.23),
            product(id = "p-expanded-031", width = 0.31),
            product(id = "p-expanded-037", width = 0.37),
            product(id = "p-expanded-044", width = 0.44),
            product(id = "p-expanded-052", width = 0.52),
            product(id = "p-expanded-067", width = 0.67)
        )
        val materials = listOf(
            material(id = "m-expanded-095", upperBound = 0.95),
            material(id = "m-expanded-115", upperBound = 1.15),
            material(id = "m-expanded-135", upperBound = 1.35),
            material(id = "m-expanded-160", upperBound = 1.60),
            material(id = "m-expanded-185", upperBound = 1.85)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = materials,
            machines = emptyList(),
            demands = products.mapIndexed { index, product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(12.0 + index), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(5UL),
            minKnifeCount = UInt64(2UL),
            parallelism = 3,
            enableDominancePruning = true
        )
        val generatorCases = generatorCases(
            constraints = constraints,
            maxPlans = 8192,
            nSumDepth = UInt64(5UL)
        )

        val snapshots = LinkedHashMap<String, CuttingPlanGenerationBenchmarkSnapshot>()
        for (case in generatorCases) {
            val report = case.generator.generateWithReport(input)
            val baseline = report.statistics
            snapshots[case.name] = CuttingPlanGenerationBenchmarkSnapshot.from(
                generatorName = case.name,
                statistics = baseline
            )

            assertMediumScaleBaseline(
                name = case.name,
                statistics = baseline
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should generate expanded-scale plans")
            assertTrue(report.plans.size <= 8192, "${case.name} should respect expanded maxPlans")
        }
        assertTrue(snapshots.keys == setOf("DFS", "NSum", "NSame", "FullSum"))
        assertEquals(
            listOf(
                "generator=DFS;visitedNodes=2181;generatedCandidates=1162;acceptedPlans=1162;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=37;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSum;visitedNodes=2031;generatedCandidates=1162;acceptedPlans=1162;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=37;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=NSame;visitedNodes=117;generatedCandidates=82;acceptedPlans=82;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=0;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted",
                "generator=FullSum;visitedNodes=2031;generatedCandidates=1162;acceptedPlans=1162;infeasibleCandidates=0;duplicateCandidates=0;dominatedCandidates=0;widthBoundPrunedNodes=0;knifeBoundPrunedNodes=37;lengthBoundPrunedEntries=0;materialWidthIndexCacheHits=0;stopReason=Exhausted"
            ),
            snapshots.values.map { it.toStableLine() }
        )
    }

    @Test
    fun generatorsShouldReportLengthBoundPruning() {
        val shortProduct = product(
            id = "p-short",
            width = 0.40,
            length = 1.0
        )
        val longProduct = product(
            id = "p-long",
            width = 0.45,
            length = 5.0
        )
        val products = listOf(shortProduct, longProduct)
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = listOf(
                material(
                    id = "m-length-bound",
                    upperBound = 1.00
                )
            ),
            machines = emptyList(),
            demands = products.map { product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(10.0), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints(
            maxKnifeCount = UInt64(3UL),
            maxOverProduceLength = Quantity(Flt64(2.0), Meter),
            enableDominancePruning = true
        )

        for (case in generatorCases(
            constraints = constraints,
            maxPlans = 64,
            nSumDepth = UInt64(3UL)
        )) {
            val report = case.generator.generateWithReport(input)

            assertEquals(
                expected = 1L,
                actual = report.statistics.lengthBoundPrunedEntries,
                message = "${case.name} should prune the long entry"
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should keep feasible short-product plans")
            assertTrue(
                report.plans.none { plan -> plan.slices.any { slice -> slice.production.id == "p-long" } },
                "${case.name} should not emit the over-length product"
            )
        }
    }

    @Test
    fun generatorsShouldReportMaterialEquivalentWidthIndexReuse() {
        val products = listOf(
            product(id = "p-eq-030", width = 0.30),
            product(id = "p-eq-045", width = 0.45)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = listOf(
                material(
                    id = "m-eq-a",
                    upperBound = 1.20
                ),
                material(
                    id = "m-eq-b",
                    upperBound = 1.20
                )
            ),
            machines = emptyList(),
            demands = products.map { product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(8.0), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints<Flt64>(
            maxKnifeCount = UInt64(4UL),
            enableDominancePruning = true
        )

        for (case in generatorCases(
            constraints = constraints,
            maxPlans = 128,
            nSumDepth = UInt64(4UL)
        )) {
            val report = case.generator.generateWithReport(input)
            val expectedCacheHits = if (case.name == "NSame") {
                0L
            } else {
                1L
            }

            assertEquals(
                expected = expectedCacheHits,
                actual = report.statistics.materialWidthIndexCacheHits,
                message = "${case.name} material-equivalent cache hit count should match"
            )
            assertTrue(report.plans.isNotEmpty(), "${case.name} should generate plans")
        }
    }

    @Test
    fun generatorsShouldReportMinKnifeReachabilityPruning() {
        val products = listOf(
            product(id = "p-min-060", width = 0.60),
            product(id = "p-min-070", width = 0.70)
        )
        val input = CuttingPlanGenerationInput(
            products = products,
            materials = listOf(
                material(
                    id = "m-min-100",
                    upperBound = 1.00
                )
            ),
            machines = emptyList(),
            demands = products.map { product ->
                ProductDemand.roll(
                    product = product,
                    quantity = Quantity(Flt64(5.0), RollCountUnit)
                )
            }
        )
        val constraints = GenerationConstraints<Flt64>(
            minKnifeCount = UInt64(2UL),
            enableDominancePruning = true
        )

        for (case in generatorCases(
            constraints = constraints,
            maxPlans = 64,
            nSumDepth = UInt64(4UL)
        )) {
            val report = case.generator.generateWithReport(input)
            val expectedPrunedNodes = if (case.name == "NSame") {
                0L
            } else {
                1L
            }

            assertEquals(
                expected = expectedPrunedNodes,
                actual = report.statistics.knifeBoundPrunedNodes,
                message = "${case.name} min-knife reachability pruning count should match"
            )
            assertTrue(report.plans.isEmpty(), "${case.name} should not emit under-min-knife plans")
        }
    }

    private data class GeneratorCase(
        val name: String,
        val generator: Csp1dInitialCuttingPlanGenerator<Flt64>
    )

    private fun generatorCases(
        constraints: GenerationConstraints<Flt64>,
        maxPlans: Int,
        nSumDepth: UInt64
    ): List<GeneratorCase> {
        return listOf(
            GeneratorCase(
                name = "DFS",
                generator = DFSGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxPlans = maxPlans
                )
            ),
            GeneratorCase(
                name = "NSum",
                generator = NSumGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxDepth = nSumDepth,
                    maxPlans = maxPlans
                )
            ),
            GeneratorCase(
                name = "NSame",
                generator = NSameGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    allAmount = true,
                    maxPlans = maxPlans
                )
            ),
            GeneratorCase(
                name = "FullSum",
                generator = FullSumGenerator(
                    constraints = constraints,
                    arithmetic = arithmetic,
                    maxPlans = maxPlans
                )
            )
        )
    }

    private fun assertMediumScaleBaseline(
        name: String,
        statistics: CuttingPlanGenerationStatistics
    ) {
        assertTrue(statistics.visitedNodes > 0L, "$name should visit nodes")
        assertTrue(statistics.generatedCandidates > 0L, "$name should generate candidates")
        assertTrue(statistics.acceptedPlans > 0, "$name should accept plans")
        assertTrue(
            statistics.generatedCandidates >= statistics.acceptedPlans.toLong(),
            "$name generated candidates should cover accepted plans"
        )
        assertTrue(statistics.duplicateCandidates >= 0L, "$name duplicate count should be non-negative")
        assertTrue(statistics.dominatedCandidates >= 0L, "$name dominance count should be non-negative")
        assertTrue(statistics.widthBoundPrunedNodes >= 0L, "$name width-bound pruning count should be non-negative")
        assertTrue(statistics.knifeBoundPrunedNodes >= 0L, "$name knife-bound pruning count should be non-negative")
        assertTrue(statistics.lengthBoundPrunedEntries >= 0L, "$name length-bound pruning count should be non-negative")
        assertTrue(statistics.materialWidthIndexCacheHits >= 0L, "$name width-index cache hit count should be non-negative")
        assertTrue(statistics.elapsedMilliseconds >= 0L, "$name elapsed time should be non-negative")
        assertTrue(
            statistics.stopReason == CuttingPlanGenerationStopReason.Exhausted ||
                    statistics.stopReason == CuttingPlanGenerationStopReason.MaxPlans,
            "$name should stop by exhaustion or max plan limit"
        )
    }

    private fun product(
        id: String,
        width: Double,
        length: Double? = null
    ): Product<Flt64> {
        return Product(
            id = id,
            name = "product-$id",
            width = listOf(
                Quantity(Flt64(width), Meter)
            ),
            length = length?.let { Quantity(Flt64(it), Meter) }
        )
    }

    private fun material(
        id: String,
        upperBound: Double
    ): Material<Flt64> {
        return Material(
            id = id,
            name = "material-$id",
            widthRange = WidthRange(
                width = QuantityRange(
                    lowerBound = Quantity(Flt64.zero, Meter),
                    upperBound = Quantity(Flt64(upperBound), Meter)
                ),
                step = Quantity(Flt64(0.05), Meter)
            )
        )
    }
}
