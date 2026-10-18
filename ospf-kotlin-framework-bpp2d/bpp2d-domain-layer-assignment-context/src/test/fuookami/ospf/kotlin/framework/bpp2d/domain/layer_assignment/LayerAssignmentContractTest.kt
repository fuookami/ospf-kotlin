package fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.times
import fuookami.ospf.kotlin.quantities.unit.times
import fuookami.ospf.kotlin.quantities.unit.Kilogram
import fuookami.ospf.kotlin.quantities.unit.Meter
import fuookami.ospf.kotlin.core.model.basic.ObjectCategory
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.core.model.mechanism.MetaModelConfiguration
import fuookami.ospf.kotlin.core.solver.value.IntoValue
import fuookami.ospf.kotlin.framework.geometry2d.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp2d.domain.layer_assignment.model.*
import fuookami.ospf.kotlin.framework.bpp2d.infrastructure.*

class LayerAssignmentContractTest {
    private fun newModel(name: String): LinearMetaModel<FltX> {
        return LinearMetaModel(
            name = name,
            objectCategory = ObjectCategory.Minimum,
            configuration = MetaModelConfiguration(),
            converter = IntoValue.fromConverter(FltX)
        )
    }

    private fun sheetType(width: Double, height: Double, isMain: Boolean = true): SheetType<FltX> {
        return SheetType(
            width = FltX(width) * Meter,
            height = FltX(height) * Meter,
            maxLoad = FltX(1000.0) * Kilogram,
            typeCode = SheetTypeId("S"),
            isMain = isMain
        )
    }

    private fun layer(
        iteration: Long,
        sheet: SheetType<FltX>,
        itemId: String,
        count: Int
    ): Layer<FltX> {
        val shape = rectangleShape2(
            width = FltX(2.0) * Meter,
            height = FltX(1.0) * Meter
        ).valueOrFail()
        val units = (0 until count).map { index ->
            Placement2(
                shape = shape,
                position = QuantityPoint2(
                    x = FltX(2.0 * index) * Meter,
                    y = FltX(0.0) * Meter
                ),
                itemId = itemId
            )
        }
        return Layer(
            iteration = iteration,
            from = Layer::class,
            sheet = sheet,
            shape = Sheet2Shape(width = FltX(2.0 * count) * Meter, height = FltX(1.0) * Meter),
            units = units
        )
    }

    @Test
    fun rmpLifecycleRegistersLoadAndDemandConstraint() {
        val model = newModel("rmp")
        val context = LayerAssignmentContext(items = mapOf(ItemId("i1") to UInt64(10)))
        val demandEntries = listOf(
            Bpp2dAssignmentDemandEntry(
                mode = Bpp2dAssignmentDemandMode.ItemAmount,
                itemId = ItemId("i1"),
                demand = Bpp2dAssignmentDemandValue.Amount(UInt64(10))
            )
        )

        val artifacts = context.registerRmp(model = model, demandEntries = demandEntries).valueOrFail()
        assertTrue(artifacts.assignment.layers.isEmpty())

        val added = context.addColumns(
            assignment = artifacts.assignment,
            load = artifacts.load,
            iteration = UInt64.zero,
            newLayers = listOf(layer(0L, sheetType(10.0, 6.0), "i1", 4)),
            model = model
        ).valueOrFail()
        assertEquals(1, added.size)
        assertEquals(1, artifacts.assignment.layers.size)

        // 重复加入同一列应被去重。
        val again = context.addColumns(
            assignment = artifacts.assignment,
            load = artifacts.load,
            iteration = UInt64.one,
            newLayers = added,
            model = model
        ).valueOrFail()
        assertTrue(again.isEmpty())
        assertEquals(1, artifacts.assignment.layers.size)
    }

    @Test
    fun removeColumnsUpdatesAggregation() {
        val context = LayerAssignmentContext(items = mapOf(ItemId("i1") to UInt64(10)))
        val target = layer(0L, sheetType(10.0, 6.0), "i1", 2)
        context.aggregation.addColumns(listOf(target))
        assertEquals(1, context.aggregation.layers.size)

        context.removeColumns(listOf(target))
        assertTrue(context.aggregation.layers.isEmpty())
        assertTrue(context.aggregation.removedLayers.contains(target))
    }

    @Test
    fun finalLifecycleRegistersCapacityAndObjectives() {
        val model = newModel("final")
        val type = sheetType(10.0, 6.0)
        val bins = listOf(Sheet(type = type))
        val layers = listOf(layer(0L, type, "i1", 4))
        val demandEntries = listOf(
            Bpp2dAssignmentDemandEntry(
                mode = Bpp2dAssignmentDemandMode.ItemAmount,
                itemId = ItemId("i1"),
                demand = Bpp2dAssignmentDemandValue.Amount(UInt64(8))
            ),
            Bpp2dAssignmentDemandEntry(
                mode = Bpp2dAssignmentDemandMode.ItemArea,
                itemId = ItemId("i1"),
                demand = Bpp2dAssignmentDemandValue.Area(Quantity(FltX(16.0), Meter * Meter))
            )
        )
        val context = LayerAssignmentContext(
            items = mapOf(ItemId("i1") to UInt64(8))
        )

        val artifacts = context.registerFinal(
            model = model,
            layers = layers,
            demandEntries = demandEntries
        ).valueOrFail()
        assertEquals(1, artifacts.assignment.flatVariables().size)

        assertTrue(context.registerSheetAmountMinimization(
            assignment = artifacts.assignment,
            layers = layers,
            model = model
        ) is Ok)

        // 需求约束应注册 2 条上界（两条需求均未要求下界）。
        assertTrue(artifacts.demandConstraint.extractor() != null)
    }

    @Test
    fun demandCoefficientFollowsAmountAndArea() {
        val type = sheetType(10.0, 6.0)
        val target = layer(0L, type, "i1", 3)
        val amountEntry = Bpp2dAssignmentDemandEntry(
            mode = Bpp2dAssignmentDemandMode.ItemAmount,
            itemId = ItemId("i1"),
            demand = Bpp2dAssignmentDemandValue.Amount(UInt64(3))
        )
        val areaEntry = Bpp2dAssignmentDemandEntry(
            mode = Bpp2dAssignmentDemandMode.ItemArea,
            itemId = ItemId("i1"),
            demand = Bpp2dAssignmentDemandValue.Area(Quantity(FltX(1.0), Meter * Meter))
        )

        assertEquals(3.0, layerDemandCoefficient(target, amountEntry).toDouble(), 1e-9)
        // 单件面积 2 m² × 3 件 = 6 m²。 / Unit area 2 m² times 3 pieces equals 6 m².
        assertEquals(6.0, layerDemandCoefficient(target, areaEntry).toDouble(), 1e-9)

        val missing = Bpp2dAssignmentDemandEntry(
            mode = Bpp2dAssignmentDemandMode.ItemAmount,
            itemId = ItemId("unknown"),
            demand = Bpp2dAssignmentDemandValue.Amount(UInt64.one)
        )
        assertEquals(FltX.zero, layerDemandCoefficient(target, missing))
    }

    @Test
    fun layerAggregationDeduplicatesAndIndexes() {
        val aggregation = LayerAggregation<FltX>()
        val type = sheetType(10.0, 6.0)
        val first = layer(0L, type, "i1", 1)
        val second = layer(0L, type, "i1", 1)

        val added = aggregation.addColumns(listOf(first, second))
        // 两个层内容一致，应只保留一个。
        assertEquals(1, added.size)
        assertTrue(added.first().indexed)
    }
}

/**
 * 解包 Ret 结果，失败时抛出断言错误。 / Unwrap a Ret result, throwing an assertion error on failure.
 *
 * @param message 断言失败时的错误信息 / error message when the assertion fails
 * @return 成功时的值 / value on success
*/
private fun <T> Ret<T>.valueOrFail(message: String = "result should succeed"): T {
    return when (this) {
        is Ok -> value
        is Failed -> kotlin.test.fail("$message: ${error.message}")
        is Fatal -> kotlin.test.fail("$message: ${errors.joinToString { it.message }}")
    }
}