package fuookami.ospf.kotlin.benchmark

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.service.ItemMerger

/**
 * 对内置货物的真实堆叠入口计时，不复制算法。 / Time the production pile entry point without replicating its algorithm.
 *
 * @property count 货物数 N/2N/4N / Item count N/2N/4N
 * @property scenario 无可合并或多堆场景 / Unmergeable or multiple-pile scenario
 */
@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class PilePreparationBenchmark {
    @Param("64", "128", "256")
    var count: Int = 0

    @Param("unmergeable", "multiplePiles")
    lateinit var scenario: String

    private lateinit var items: List<Item>
    private lateinit var space: Container3Shape

    /** 只读输入在计时外构造。 / Construct read-only inputs outside timing. */
    @Setup
    fun setup() {
        val attribute = PackageAttribute(
            packageType = PackageType.CartonContainer,
            packageMaxLayer = UInt64(if (scenario == "unmergeable") 1 else 4),
            maxHeight = FltX(5.0) * Meter,
            maxDepth = FltX(5.0) * Meter,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(FltX.zero),
            hangingPolicy = AbsoluteHangingPolicy(FltX.zero),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
        items = List(count) { index ->
            ActualItem(
                id = itemIdOf("pile_$index"),
                name = "pile_$index",
                width = FltX.one * Meter,
                height = FltX.one * Meter,
                depth = FltX.one * Meter,
                weight = FltX((index % 5 + 1).toDouble()) * Kilogram,
                enabledOrientations = listOf(Orientation.Upright),
                packageAttribute = attribute
            )
        }
        space = Container3Shape(
            width = FltX(3.0) * Meter,
            height = FltX(4.0) * Meter,
            depth = FltX(3.0) * Meter
        )
    }

    /**
     * 合并会返回新列表，不修改夹具。 / Merging returns fresh lists without mutating the fixture.
     * @return 完整生产结果 / Full production result
     */
    @Benchmark
    fun mergePiles(): Ret<Pair<List<Pile>, List<Item>>> {
        return ItemMerger.mergePiles(
            items = items,
            space = space,
            restWeight = FltX(1000.0)
        )
    }
}
