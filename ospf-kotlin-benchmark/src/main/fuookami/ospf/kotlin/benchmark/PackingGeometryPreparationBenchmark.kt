package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.sqrt
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.dto.SchemaDTO
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service.PackingRendererAdapter

/**
 * 通过生产渲染入口测量几何守卫与 DTO 准备，不运行求解器。 / Measure geometry validation and DTO preparation through the production renderer entry point, without running a solver.
 *
 * @property count 单箱物品规模，交叉采样 64/256/1024，并非线性趋势样本 / Per-bin item count at 64/256/1024 crossover scales, not a linear-trend fixture
 * @property scenario 稀疏成功路径或首 pair 冲突失败路径 / Sparse success path or first-pair conflict failure path
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class PackingGeometryPreparationBenchmark {
    @Param("64", "256", "1024")
    var count: Int = 0

    @Param("sparse", "denseFirstConflict")
    lateinit var scenario: String

    private val adapter = PackingRendererAdapter()
    private lateinit var result: PackingResult

    /** 在试验准备阶段构造真实不可变输入，避免将夹具构造计入被测时间。 / Build the real immutable input at trial setup so fixture construction is outside the timed operation. */
    @Setup(Level.Trial)
    fun setup() {
        val binType = BinType(
            width = FltX(100.0) * Meter,
            height = FltX(100.0) * Meter,
            depth = FltX(100.0) * Meter,
            capacity = FltX(100_000.0) * Kilogram,
            longitudinalBalance = null,
            lateralBalance = null,
            typeCode = binTypeIdOf("PACKING-GEOMETRY-BENCH")
        )
        val packageAttribute = PackageAttribute(
            packageType = PackageType.CartonContainer,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
        val itemSide = ceil(sqrt(count.toDouble())).toInt()
        val denseFirstConflict = scenario == "denseFirstConflict"
        val items = List(count) { index ->
            val item = ActualItem(
                id = itemIdOf("packing-geometry-$scenario-$count-$index"),
                name = "packing-geometry-$index",
                width = FltX(0.5) * Meter,
                height = FltX(0.5) * Meter,
                depth = FltX(0.5) * Meter,
                weight = FltX.one * Kilogram,
                enabledOrientations = listOf(Orientation.Upright),
                packageAttribute = packageAttribute,
                materialAmountsOverride = emptyMap()
            )
            val (x, y) = placementCoordinates(
                index = index,
                itemSide = itemSide,
                denseFirstConflict = denseFirstConflict
            )
            PackedItem(
                placement = itemPlacement3Of(
                    item = item,
                    position = point3(
                        x = FltX(x) * Meter,
                        y = FltX(y) * Meter,
                        z = FltX.zero * Meter
                    )
                ),
                loadingOrder = UInt64(index)
            )
        }
        val bin = PackedBin(
            name = "packing-geometry-$scenario-$count",
            type = binType,
            items = items
        )
        result = PackingResult(
            aggregation = PackingAggregation(bins = listOf(bin))
        )
    }

    /**
     * 调用真实公开 renderer API；成功路径包含几何守卫和完整 DTO 构造，冲突路径在守卫处短路。 / Call the real public renderer API; the success path includes geometry validation and full DTO construction, while the conflict path short-circuits in validation.
     *
     * @return 生产 renderer 的结果对象 / The production renderer result object
     */
    @Benchmark
    fun toSchema(): Ret<SchemaDTO> {
        return adapter.toSchema(result)
    }

    private fun placementCoordinates(
        index: Int,
        itemSide: Int,
        denseFirstConflict: Boolean
    ): Pair<Double, Double> {
        if (denseFirstConflict && index < 2) {
            return Pair(0.0, 0.0)
        }
        val gridIndex = if (denseFirstConflict) index - 2 else index
        val offset = if (denseFirstConflict) 2.0 else 0.0
        val x = offset + (gridIndex % itemSide) * 1.5
        val y = offset + (gridIndex / itemSide) * 1.5
        return Pair(x, y)
    }
}
