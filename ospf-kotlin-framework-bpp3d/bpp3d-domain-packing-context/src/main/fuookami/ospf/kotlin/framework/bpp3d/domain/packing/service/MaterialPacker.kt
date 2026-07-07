/**
 * Material packer.
 * 物料装箱器。
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.ceil
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.*
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*

/**
 * Material packer for packing material demands into packaging programs.
 * 物料装箱器，用于将物料需求装箱到包装方案中。
 *
 * @param solverExecutor The material packing solver executor.
 * 物料装箱求解器执行器。
 */
class MaterialPacker(
    private val solverExecutor: MaterialPackingSolverExecutor = ExhaustiveMaterialPackingSolverExecutor()
) {
    /**
     * Package slot representing a packaging instance of a candidate program in the current slot.
     * 包装槽位，表示一个候选方案在当前槽位的包装实例。
     *
     * @property candidateIndex The candidate program index.
     * 候选方案索引。
     * @property pack The package instance.
     * 包装实例。
     * @property assigned The assigned materials and their amounts.
     * 已分配的物料及其数量。
     * @property pending Whether the slot is pending.
     * 是否待定。
     */
    private data class PackageSlot(
        val candidateIndex: Int,
        val pack: Package<*>,
        val assigned: MutableMap<MaterialKey, UInt64>,
        val pending: Boolean
    )

    /**
     * Slot signature for grouping slots with the same configuration.
     * 槽位签名，用于对相同配置的槽位进行分组。
     *
     * @property candidateIndex The candidate program index.
     * 候选方案索引。
     * @property pending Whether the slot is pending.
     * 是否待定。
     * @property materials The list of materials and their assigned amounts.
     * 物料及其分配数量列表。
     */
    private data class SlotSignature(
        val candidateIndex: Int,
        val pending: Boolean,
        val materials: List<Pair<MaterialKey, UInt64>>
    )

    /**
     * Convert a packing quantity to the FltX scalar type.
     * 将装箱数量转换为 FltX 标量类型。
     *
     * @param value The quantity to convert.
     * 待转换的数量。
     * @return The converted FltX quantity.
     * 转换后的 FltX 数量。
     */
    @Suppress("UNCHECKED_CAST")
    private fun packQuantityToFltX(value: Quantity<*>): Quantity<FltX> {
        return when (value.value) {
            is FltX -> value as Quantity<FltX>
            else -> Quantity(FltX(value.value.toString().toDouble()), value.unit)
        }
    }

    /**
     * Convert a weight demand quantity to the packing quantity type. Only FltX scalar type is supported.
     * 将重量需求量转换为装箱数量类型。仅支持 FltX 标量类型。
     *
     * @param value The weight demand quantity to convert.
     * 待转换的重量需求量。
     * @return The converted packing quantity, fails when scalar type is unsupported.
     * 转换后的装箱数量，标量类型不支持时失败。
     */
    @Suppress("UNCHECKED_CAST")
    private fun weightDemandToPackingQuantity(value: Quantity<*>): Ret<Quantity<FltX>> {
        return when (value.value) {
            is FltX -> ok(value as Quantity<FltX>)
            else -> Failed(ErrorCode.IllegalArgument, "Unsupported material packing weight scalar: ${value.value}")
        }
    }

    /**
     * Plan the material packing by solving the MIP and assigning materials to package slots.
     * 通过求解混合整数规划并分配物料到包装槽位来规划物料装箱。
     *
     * @param V The floating number type.
     * 浮点数类型。
     * @param demands The list of material packing demands.
     * 物料装箱需求列表。
     * @param candidates The list of candidate packing programs.
     * 候选装箱方案列表。
     * @param objective The objective weight configuration.
     * 目标权重配置。
     * @return The material packing plan.
     * 物料装箱计划。
     */
    suspend fun <V : FloatingNumber<V>> plan(
        demands: List<MaterialPackingDemand<V>>,
        candidates: List<MaterialPackingProgramCandidate<V>>,
        objective: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig()
    ): MaterialPackingPlan {
        val normalizedDemands = LinkedHashMap<MaterialKey, UInt64>()
        val materialByKey = LinkedHashMap<MaterialKey, Material<FltX>>()
        val impossibleDemands = LinkedHashMap<MaterialKey, UInt64>()

        for (demand in demands) {
            val material = demand.material
            val key = material.key
            materialByKey.putIfAbsent(key, material)
            normalizedDemands[key] = (normalizedDemands[key] ?: UInt64.zero) + demand.amount
            val weightDemand = demand.weight?.let { weightDemandToPackingQuantity(it).value!! }
            if (weightDemand != null && weightDemand.value.toDouble() > 0.0) {
                val unitWeight = material.weight
                if (unitWeight.value.toDouble() <= 0.0) {
                    impossibleDemands[key] = (impossibleDemands[key] ?: UInt64.zero) + toUInt64(ceil(weightDemand.value.toDouble()).toLong())
                } else {
                    val normalizedWeight = weightDemand.to(unitWeight.unit) ?: weightDemand
                    val amountFromWeight = ceil((normalizedWeight.value / unitWeight.value).toDouble()).toLong()
                    if (amountFromWeight > 0L) {
                        normalizedDemands[key] = (normalizedDemands[key] ?: UInt64.zero) + toUInt64(amountFromWeight)
                    }
                }
            }
        }

        if (normalizedDemands.values.all { it == UInt64.zero } && impossibleDemands.isEmpty()) {
            return MaterialPackingPlan(
                packages = emptyList(),
                packagedItems = emptyList(),
                selections = emptyList(),
                assignments = emptyList(),
                restMaterials = emptyMap(),
                normalizedDemands = emptyMap(),
                solveInfo = MaterialPackingSolveInfo(
                    status = MaterialPackingStatus.Optimal,
                    objective = materialPackingZero(),
                    gap = materialPackingZero(),
                    timeMillis = 0L,
                    selectedPackageCount = UInt64.zero,
                    rawStatus = "empty_demand"
                )
            )
        }
        if (impossibleDemands.isNotEmpty()) {
            return MaterialPackingPlan(
                packages = emptyList(),
                packagedItems = emptyList(),
                selections = emptyList(),
                assignments = emptyList(),
                restMaterials = impossibleDemands,
                normalizedDemands = normalizedDemands,
                solveInfo = MaterialPackingSolveInfo(
                    status = MaterialPackingStatus.Infeasible,
                    objective = null,
                    gap = null,
                    timeMillis = 0L,
                    selectedPackageCount = UInt64.zero,
                    rawStatus = "invalid_unit_weight"
                )
            )
        }

        val mipResult = solverExecutor.solve(
            request = MaterialPackingMipRequest(
                demands = normalizedDemands.filterValues { it != UInt64.zero },
                candidates = candidates,
                objective = objective
            )
        )
        if (mipResult.status != MaterialPackingMipStatus.Optimal) {
            return MaterialPackingPlan(
                packages = emptyList(),
                packagedItems = emptyList(),
                selections = emptyList(),
                assignments = emptyList(),
                restMaterials = normalizedDemands.filterValues { it != UInt64.zero },
                normalizedDemands = normalizedDemands,
                solveInfo = MaterialPackingSolveInfo(
                    status = MaterialPackingStatus.Infeasible,
                    objective = mipResult.objective,
                    gap = mipResult.gap,
                    timeMillis = mipResult.timeMillis,
                    selectedPackageCount = UInt64.zero,
                    rawStatus = mipResult.rawStatus
                )
            )
        }

        val selections = mipResult.selections
            .toList()
            .sortedBy { (index, _) -> index }
            .map { (index, amount) ->
                PackageSelection(
                    candidate = candidates[index],
                    amount = amount
                )
            }
        val selectedPackageCount = selections.fold(UInt64.zero) { acc, selection -> acc + selection.amount }

        val slotCandidates = ArrayList<Int>()
        for ((candidateIndex, selectedAmount) in mipResult.selections) {
            repeat(selectedAmount.toULong().toInt()) {
                slotCandidates.add(candidateIndex)
            }
        }
        val slotAssignments = MutableList(slotCandidates.size) {
            LinkedHashMap<MaterialKey, UInt64>()
        }
        val restMaterials = LinkedHashMap<MaterialKey, UInt64>()
        val demandOrder = normalizedDemands.entries
            .filter { (_, amount) -> amount != UInt64.zero }
            .map { (material, amount) -> Pair(material, amount) }

        for ((material, demandAmount) in demandOrder) {
            var remaining = demandAmount
            val sortedSlots = slotCandidates
                .mapIndexed { slotIndex, candidateIndex ->
                    val cap = candidates[candidateIndex].program.materialAmount(material)
                    Pair(slotIndex, cap)
                }
                .filter { (_, cap) -> cap != UInt64.zero }
                .sortedByDescending { (_, cap) -> cap.toULong() }
            for ((slotIndex, capacityAmount) in sortedSlots) {
                if (remaining == UInt64.zero) {
                    break
                }
                val assigned = slotAssignments[slotIndex][material] ?: UInt64.zero
                val available = if (capacityAmount > assigned) {
                    capacityAmount - assigned
                } else {
                    UInt64.zero
                }
                if (available == UInt64.zero) {
                    continue
                }
                val allocation = minOf(available, remaining)
                slotAssignments[slotIndex][material] = assigned + allocation
                remaining -= allocation
            }
            if (remaining != UInt64.zero) {
                restMaterials[material] = remaining
            }
        }

        val packageSlots = slotCandidates.mapIndexed { slotIndex, candidateIndex ->
            val candidate = candidates[candidateIndex]
            val assigned = slotAssignments[slotIndex]
            val materialMap = assigned.mapNotNull { (materialKey, amount) ->
                if (amount == UInt64.zero) {
                    null
                } else {
                    Pair(materialByKey[materialKey] ?: return@mapNotNull null, amount)
                }
            }.toMap()
            val pending = candidate.program.materialAmounts().any { (material, capacityAmount) ->
                (assigned[material] ?: UInt64.zero) < capacityAmount
            }
            PackageSlot(
                candidateIndex = candidateIndex,
                pack = Package.innerPackage(
                    program = candidate.program,
                    materials = materialMap,
                    amount = UInt64.one,
                    pending = pending
                ),
                assigned = assigned,
                pending = pending
            )
        }

        val assignmentCounter = LinkedHashMap<Pair<Int, MaterialKey>, UInt64>()
        for (slot in packageSlots) {
            for ((material, amount) in slot.assigned) {
                if (amount == UInt64.zero) {
                    continue
                }
                val key = Pair(slot.candidateIndex, material)
                assignmentCounter[key] = (assignmentCounter[key] ?: UInt64.zero) + amount
            }
        }
        val assignments = assignmentCounter.map { (key, amount) ->
            MaterialPackingAssignment(
                candidate = candidates[key.first],
                material = key.second,
                amount = amount
            )
        }.sortedBy { assignment ->
            "${assignment.candidate.id}|${assignment.material.no}"
        }

        val packagedItems = ArrayList<PackagedItem>()
        val groupedSlots = packageSlots.groupBy { slot ->
            val materials = slot.assigned.entries
                .filter { it.value != UInt64.zero }
                .sortedBy { (material, _) ->
                    "${material.no}|${material.type}|${material.manufacturer ?: ""}|${material.supplier ?: ""}"
                }
                .map { (material, amount) -> Pair(material, amount) }
            SlotSignature(
                candidateIndex = slot.candidateIndex,
                pending = slot.pending,
                materials = materials
            )
        }
        var sequence = 1
        for ((signature, sameSlots) in groupedSlots) {
            val candidate = candidates[signature.candidateIndex]
            val packageAttribute = candidate.packageAttribute ?: defaultPackageAttribute(candidate)
            val itemId = "${candidate.id}-$sequence"
            val pack = sameSlots.first().pack
            val item = ActualItem(
                id = fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.itemIdOf(itemId),
                name = "${candidate.itemName}-$sequence",
                pack = pack,
                width = packQuantityToFltX(pack.width),
                height = packQuantityToFltX(pack.height),
                depth = packQuantityToFltX(pack.depth),
                weight = packQuantityToFltX(pack.weight),
                enabledOrientations = candidate.enabledOrientations,
                batchNo = candidate.batchNo,
                warehouse = candidate.warehouse,
                packageAttribute = packageAttribute,
                shapeSpecOverride = pack.shape.shapeSpec
            )
            packagedItems.add(
                PackagedItem(
                    item = item,
                    amount = UInt64(sameSlots.size.toULong()),
                    pending = signature.pending
                )
            )
            sequence += 1
        }

        val status = if (restMaterials.values.any { it != UInt64.zero }) {
            MaterialPackingStatus.Infeasible
        } else {
            MaterialPackingStatus.Optimal
        }

        return MaterialPackingPlan(
            packages = packageSlots.map { it.pack },
            packagedItems = packagedItems,
            selections = selections,
            assignments = assignments,
            restMaterials = restMaterials,
            normalizedDemands = normalizedDemands,
            solveInfo = MaterialPackingSolveInfo(
                status = status,
                objective = mipResult.objective,
                gap = mipResult.gap,
                timeMillis = mipResult.timeMillis,
                selectedPackageCount = selectedPackageCount,
                rawStatus = mipResult.rawStatus
            )
        )
    }

    /**
     * Convert a Long value to UInt64, returning zero for negative values.
     * 将 Long 值转换为 UInt64，负值返回零。
     *
     * @param value The long value to convert.
     * 待转换的长整型值。
     * @return The converted UInt64 value.
     * 转换后的 UInt64 值。
     */
    private fun toUInt64(value: Long): UInt64 {
        return if (value <= 0L) {
            UInt64.zero
        } else {
            UInt64(value.toULong())
        }
    }

    /**
     * Create a default package attribute for a candidate program.
     * 为候选方案创建默认的包装属性。
     *
     * @param V The floating number type.
     * 浮点数类型。
     * @param candidate The candidate program.
     * 候选方案。
     * @return The default package attribute.
     * 默认的包装属性。
     */
    private fun <V : FloatingNumber<V>> defaultPackageAttribute(candidate: MaterialPackingProgramCandidate<V>): PackageAttribute {
        return PackageAttribute(
            packageType = candidate.program.packageType,
            weightAttribute = WeightAttribute(),
            deformationAttribute = LinearDeformationAttribute(materialPackingZero()),
            hangingPolicy = AbsoluteHangingPolicy(materialPackingZero()),
            stackingOnPolicy = FilterStackingOnPolicy()
        )
    }
}
