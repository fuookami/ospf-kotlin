@file:Suppress("DEPRECATION")

/**
 * 物料装箱器。
 * Material packer.
 */
package fuookami.ospf.kotlin.framework.bpp3d.domain.packing.service

import kotlin.math.ceil
import fuookami.ospf.kotlin.quantities.quantity.Quantity
import fuookami.ospf.kotlin.quantities.quantity.to
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.AbsoluteHangingPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.ActualItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.FilterStackingOnPolicy
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.LinearDeformationAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Material
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.MaterialKey
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.Package
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.PackageAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.item.model.WeightAttribute
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingAssignment
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingDemand
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingObjectiveConfig
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingPlan
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingProgramCandidate
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingSolveInfo
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingStatus
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.PackageSelection
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.PackagedItem
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingNumber
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.MaterialPackingQuantity
import fuookami.ospf.kotlin.framework.bpp3d.domain.packing.model.materialPackingZero
import fuookami.ospf.kotlin.framework.bpp3d.infrastructure.InfraNumber
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.math.algebra.number.FltX
import fuookami.ospf.kotlin.math.algebra.number.UInt64

class MaterialPacker(
    private val solverExecutor: MaterialPackingSolverExecutor = ExhaustiveMaterialPackingSolverExecutor()
) {
    private data class PackageSlot(
        val candidateIndex: Int,
        val pack: Package<*>,
        val assigned: MutableMap<MaterialKey, UInt64>,
        val pending: Boolean
    )

    private data class SlotSignature(
        val candidateIndex: Int,
        val pending: Boolean,
        val materials: List<Pair<MaterialKey, UInt64>>
    )

    @Suppress("UNCHECKED_CAST")
    private fun packQuantityToInfra(value: Quantity<*>): Quantity<InfraNumber> {
        return when (value.value) {
            is InfraNumber -> value as Quantity<InfraNumber>
            else -> Quantity(InfraNumber(value.value.toString().toDouble()), value.unit)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun weightDemandToPackingQuantity(value: Quantity<*>): MaterialPackingQuantity {
        return when (value.value) {
            is MaterialPackingNumber -> value as MaterialPackingQuantity
            is FltX -> {
                val quantity = value as Quantity<FltX>
                Quantity(MaterialPackingNumber(quantity.value.toDouble()), quantity.unit)
            }
            else -> throw IllegalArgumentException("Unsupported material packing weight scalar: ${value.value}")
        }
    }

    suspend fun <V : FloatingNumber<V>> plan(
        demands: List<MaterialPackingDemand<V>>,
        candidates: List<MaterialPackingProgramCandidate<V>>,
        objective: MaterialPackingObjectiveConfig = MaterialPackingObjectiveConfig()
    ): MaterialPackingPlan {
        val normalizedDemands = LinkedHashMap<MaterialKey, UInt64>()
        val materialByKey = LinkedHashMap<MaterialKey, Material<MaterialPackingNumber>>()
        val impossibleDemands = LinkedHashMap<MaterialKey, UInt64>()

        for (demand in demands) {
            val material = demand.material
            val key = material.key
            materialByKey.putIfAbsent(key, material)
            normalizedDemands[key] = (normalizedDemands[key] ?: UInt64.zero) + demand.amount
            val weightDemand = demand.weight?.let { weightDemandToPackingQuantity(it) }
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
                id = itemId,
                name = "${candidate.itemName}-$sequence",
                pack = pack,
                width = packQuantityToInfra(pack.width),
                height = packQuantityToInfra(pack.height),
                depth = packQuantityToInfra(pack.depth),
                weight = packQuantityToInfra(pack.weight),
                enabledOrientations = candidate.enabledOrientations,
                batchNo = candidate.batchNo,
                warehouse = candidate.warehouse,
                packageAttribute = packageAttribute
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

    private fun toUInt64(value: Long): UInt64 {
        return if (value <= 0L) {
            UInt64.zero
        } else {
            UInt64(value.toULong())
        }
    }

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

