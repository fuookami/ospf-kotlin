package fuookami.ospf.kotlin.framework.bpp3d.domain.layer_assignment.model

import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.BatchNo
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.Orientation
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.PackageType
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.math.algebra.value_range.Interval
import fuookami.ospf.kotlin.math.algebra.value_range.ValueRange
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.quantities.unit.SquareMeter
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SolverAdapterBoundaryTest {
    private fun defaultPackageAttribute(type: PackageType = PackageType.CartonContainer): PackageAttribute {
        return PackageAttribute(
            packageType = type,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }

    private fun item(id: String): ActualItem {
        return ActualItem(
            id = id,
            name = id,
            width = fltX(1.0) * Meter,
            height = fltX(1.0) * Meter,
            depth = fltX(1.0) * Meter,
            weight = fltX(1.0) * Kilogram,
            enabledOrientations = listOf(Orientation.Upright),
            batchNo = BatchNo("B-$id"),
            packageAttribute = defaultPackageAttribute()
        )
    }

    @Test
    fun defaultAdapterShouldExposeFlt64Path() {
        assertEquals(3.0, DefaultBpp3dSolverValueAdapter.amountToSolver(UInt64(3)).toDouble(), 1e-10)
        assertEquals(2.5, DefaultBpp3dSolverValueAdapter.lengthToSolver(fltX(2.5) * Meter).toDouble(), 1e-10)
        assertEquals(2.5, DefaultBpp3dSolverValueAdapter.weightToSolver(fltX(2.5) * Kilogram).toDouble(), 1e-10)
    }

    @Test
    fun demandEntriesFromItemRangesShouldUseAdapterAmountAndRange() {
        val adapter = ScaledBpp3dSolverValueAdapter(
            scale = Bpp3dSolverFltXScale(
                amount = FltX(2.0)
            )
        )
        val demandRange = ValueRange(
            UInt64(2),
            UInt64(5),
            Interval.Closed,
            Interval.Closed,
            UInt64
        ).value!!

        val entries = demandEntriesFromItemRanges(
            items = listOf(Triple(item("item-1"), UInt64(3), demandRange)),
            demandValueAdapter = adapter
        )

        val entry = entries.single()
        assertEquals(6.0, entry.demand.toDouble(), 1e-10)
        assertEquals(4.0, entry.demandRange.lowerBound.value.unwrap().toDouble(), 1e-10)
        assertEquals(10.0, entry.demandRange.upperBound.value.unwrap().toDouble(), 1e-10)
    }

    @Test
    fun fltXScaledSolverAdapterShouldApplyConfiguredScales() {
        val adapter = ScaledBpp3dSolverValueAdapter(
            scale = Bpp3dSolverFltXScale(
                amount = FltX(2.0),
                length = FltX(3.0),
                area = FltX(4.0),
                volume = FltX(5.0),
                depth = FltX(6.0),
                weight = FltX(7.0)
            )
        )

        assertEquals(6.0, adapter.amountToSolver(UInt64(3)).toDouble(), 1e-10)
        assertEquals(6.0, adapter.lengthToSolver(fltX(2.0) * Meter).toDouble(), 1e-10)
        assertEquals(8.0, adapter.areaToSolver(fltX(2.0) * SquareMeter).toDouble(), 1e-10)
        assertEquals(10.0, adapter.volumeToSolver(fltX(2.0) * fuookami.ospf.kotlin.quantities.unit.CubicMeter).toDouble(), 1e-10)
        assertEquals(12.0, adapter.depthToSolver(fltX(2.0) * Meter).toDouble(), 1e-10)
        assertEquals(14.0, adapter.weightToSolver(fltX(2.0) * Kilogram).toDouble(), 1e-10)
    }

    @Test
    fun toFlt64ShouldOnlyExistInAdapterBoundary() {
        val allowedFiles = setOf(
            "Bpp3dSolverValueAdapter.kt",
            "ScaledBpp3dSolverValueAdapter.kt"
        )
        val sourceDir = moduleRoot().resolve("src").resolve("main")
        val offenders = ArrayList<String>()
        val paths = Files.walk(sourceDir)
        try {
            paths.filter { path ->
                Files.isRegularFile(path) && path.toString().endsWith(".kt")
            }.forEach { path ->
                if (path.fileName.toString() !in allowedFiles) {
                    val content = Files.readString(path)
                    if (content.contains("toFlt64(")) {
                        offenders.add(path.toString())
                    }
                }
            }
        } finally {
            paths.close()
        }

        assertTrue(offenders.isEmpty(), "Found forbidden toFlt64 usage outside adapter boundary: $offenders")
    }

    private fun moduleRoot(): Path {
        val cwd = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize()
        val directModule = cwd.resolve("src").resolve("main")
        return if (Files.exists(directModule)) {
            cwd
        } else {
            cwd
                .resolve("ospf-kotlin-framework-bpp3d")
                .resolve("bpp3d-domain-layer-assignment-context")
                .normalize()
        }
    }
}


