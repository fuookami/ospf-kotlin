package fuookami.ospf.kotlin.benchmark

import kotlinx.coroutines.runBlocking
import fuookami.ospf.kotlin.utils.error.*
import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.math.ordinary.PrimeCache
import fuookami.ospf.kotlin.math.symbol.operation.*
import fuookami.ospf.kotlin.math.symbol.monomial.LinearMonomial
import fuookami.ospf.kotlin.math.symbol.polynomial.LinearPolynomial
import fuookami.ospf.kotlin.quantities.unit.*
import fuookami.ospf.kotlin.quantities.quantity.*
import fuookami.ospf.kotlin.core.variable.*
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.token.*
import fuookami.ospf.kotlin.framework.solver.*
import fuookami.ospf.kotlin.framework.network_scheduling.infrastructure.*
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.cutting_plan_generation.service.*

/** 跨版本共有输出语义差分（非计时）。 / Cross-version differential of shared output semantics (untimed).
 *
 * 只调用两个被比较版本共同具备的生产入口，把方案集合、必要顺序、数值渲染、可达集合、索引结果和缓存计数渲染为稳定文本行，
 * 供两端分别运行后逐行比对；本文件必须在基线上也能编译，因此不得引用仅候选才有的 API。
 * 输出不含进程级身份计数器，冷热状态在各自小节内显式区分。用法：`java -cp <runtime classpath>
 * fuookami.ospf.kotlin.benchmark.CrossVersionSemanticDump > dump.txt`，两端输出做逐行 diff。
 * Calls only production entries shared by the two compared revisions and renders plan sets, required order, numeric
 * views, reachability, index results and cache counters as stable lines for line-by-line comparison; this file must
 * compile on the baseline as well, so it may not reference candidate-only APIs. Process-global identity counters are
 * excluded and cold/warm states are separated per section. Run
 * `java -cp <runtime classpath> fuookami.ospf.kotlin.benchmark.CrossVersionSemanticDump > dump.txt` on both sides and
 * diff the outputs.
 */
object CrossVersionSemanticDump {
    private val out = StringBuilder()

    private fun line(text: String) {
        out.append(text).append('\n')
    }

    /** @param value 浮点值 / Floating value
     * @return 可往返渲染的文本 / Round-trip renderable text
     */
    private fun num(value: Any?): String = value.toString()

    @JvmStatic
    fun main(args: Array<String>) {
        dumpNetwork()
        dumpProjection()
        dumpGeneration()
        dumpGenerationTruncated()
        dumpToken()
        dumpPrime()
        print(out.toString())
    }

    // ==================== Network ====================

    private data class Arc(val id: NetworkArcId, val from: NetworkNodeId, val to: NetworkNodeId)

    private fun dumpNetwork() {
        val access = NetworkGraphAccess<NetworkNodeId, Arc>(
            nodeId = { it },
            arcId = { it.id },
            from = { it.from },
            to = { it.to }
        )
        for (size in listOf(256, 1024)) {
            val nodes = List(size) { NetworkNodeId("n$it") }
            val arcs = nodes.indices.flatMap { from ->
                (1..4).map { step -> Arc(id = NetworkArcId("a${from}_$step"), from = nodes[from], to = nodes[(from + step) % size]) }
            }
            val graph = NetworkGraph(nodes = nodes, arcs = arcs, access = access).value!!
            val topology = NetworkTopology(graph)
            val reachable = topology.reachable(setOf(nodes.first())).value!!
            line("NET|$size|reachableFirst|count=${reachable.size}|sortedDigest=${reachable.map { it.value }.sorted().joinToString(";").hashCode()}|first=${reachable.first()}|last=${reachable.last()}")
            val components = topology.stronglyConnectedComponents()
            line("NET|$size|scc|count=${components.size}|sizes=${components.map { it.size }.sorted()}")
            val reverse = graph.reverse()
            val reverseRead = reverse.outgoing(nodes.first())
            line("NET|$size|reverseOutgoing|ids=${reverseRead.map { it.id.value }}")
            val incoming = graph.incoming(nodes.first())
            line("NET|$size|reverseEqualsIncoming|same=${reverseRead.map { it.id.value } == incoming.map { it.id.value }}")
            // 反向视图必须与原图入弧在全节点上逐项一致，且反向图的入弧等于原图出弧：这是 N7 不建第二套索引的设计前提。
            // The reverse view must match original incoming arcs node by node, and reverse incoming must match original
            // outgoing: that is the design premise behind measuring reverse reads without a second index set.
            var reverseMismatch = 0
            var reverseIncomingMismatch = 0
            for (node in nodes) {
                if (reverse.outgoing(node).map { it.id.value } != graph.incoming(node).map { it.id.value }) reverseMismatch++
                if (reverse.incoming(node).map { it.id.value } != graph.outgoing(node).map { it.id.value }) reverseIncomingMismatch++
            }
            line("NET|$size|reverseExhaustiveMismatch|outgoing=$reverseMismatch|incoming=$reverseIncomingMismatch")
            val reverseTopology = NetworkTopology(reverse).reachable(setOf(nodes.first())).value!!
            line("NET|$size|reverseReachable|count=${reverseTopology.size}|sortedDigest=${reverseTopology.map { it.value }.sorted().joinToString(";").hashCode()}")
            val warm = NetworkTopology(graph).reachable(setOf(nodes[7])).value!!
            line("NET|$size|warmReachable|count=${warm.size}|sortedDigest=${warm.map { it.value }.sorted().joinToString(";").hashCode()}")
            val filtered = NetworkTopology(graph).filtered(
                allowedNode = { it != nodes.last() },
                allowedArc = { it.to != nodes.last() }
            ).reachable(setOf(nodes.first())).value!!
            line("NET|$size|filteredReachable|count=${filtered.size}|sortedDigest=${filtered.map { it.value }.sorted().joinToString(";").hashCode()}")
            val cold = NetworkTopology(
                NetworkGraph(nodes = nodes, arcs = arcs, access = access).value!!
            ).reachable(setOf(nodes.first())).value!!
            line("NET|$size|coldEqualsWarm|same=${cold == reachable}")
        }
    }

    // ==================== Projection ====================

    private fun dumpProjection() {
        for (size in listOf(256, 1024)) {
            runBlocking {
                val source = LinearMetaModel(name = "dump_projection")
                val variables = List(size) { BinVar("v$it") }
                check(source.add(variables) is Ok)
                repeat(size / 2) { row ->
                    check(source.addConstraint(
                        relation = LinearPolynomial(
                            monomials = (0..2).map { offset -> LinearMonomial(Flt64.one, variables[(row + offset * 4) % size]) },
                            constant = Flt64.zero
                        ) leq Flt64.two,
                        name = "r$row"
                    ) is Ok)
                }
                check(source.minimize(LinearPolynomial(variables.map { LinearMonomial(Flt64.one, it) }, Flt64.zero)) is Ok)
                val blocks = (0 until 4).map { "b$it" }.toSet()
                val decisions = variables.mapIndexed { index, variable ->
                    ColumnDecision(id = "d$index", blockId = "b${index % 4}", variable = variable)
                }
                val columns = blocks.map { block ->
                    ProjectedColumn(
                        id = block,
                        blockId = block,
                        values = decisions.filter { it.blockId == block }.associate { it.id to Flt64.zero }
                    )
                }
                val projection = ColumnProjection(source = source, decisions = decisions, blockIds = blocks)
                val snapshot = projection.snapshot().value!!
                line("PRJ|$size|snapshot|rows=${snapshot.rows.size}|vars=${snapshot.variables.size}")
                line("PRJ|$size|snapshotRow0|id=${snapshot.rows.first().id}|rhs=${num(snapshot.rows.first().rhs)}|coeffs=${snapshot.rows.first().coefficients.entries.sortedBy { it.key.name }.joinToString(",") { it.key.name + "=" + num(it.value) }}")
                line("PRJ|$size|snapshotObjective|constant=${num(snapshot.objectiveConstant)}|terms=${snapshot.objective.entries.sortedBy { it.key.name }.take(8).joinToString(",") { it.key.name + "=" + num(it.value) }}")
                val pricing = projection.pricingRequest(blockId = "b0", phaseOne = false, seed = true).value!!
                line("PRJ|$size|pricing|block=${pricing.blockId}|coeffCount=${pricing.coefficients.size}|convexity=${num(pricing.convexityPrice)}|head=${pricing.coefficients.entries.take(4).joinToString(",") { it.key + "=" + num(it.value) }}")
                line("PRJ|$size|pricingRowOrder|keys=${pricing.rowPrices.keys.take(6).joinToString(",")}|dualHead=${pricing.rowPrices.values.take(4).joinToString(",") { num(it) }}")
                val reduced = pricing.reducedCost(pricing.coefficients.keys.associateWith { Flt64.one }).value!!
                line("PRJ|$size|pricingReducedCost|value=${num(reduced)}")
                val registration = projection.master(columns = columns, phaseOne = false, integer = false).value!!
                try {
                    line("PRJ|$size|master|rows=${registration.rows.size}|convexities=${registration.convexities.size}|columnVars=${registration.columnVariables.size}|refined=${registration.refinedResources.size}|order=${registration.rows.keys.joinToString(",")}")
                } finally {
                    registration.model.close()
                }
                val warmPricing = projection.pricingRequest(blockId = "b0", phaseOne = false, seed = true).value!!
                line("PRJ|$size|warmSameAsCold|coeffs=${warmPricing.coefficients == pricing.coefficients}|convexity=${num(warmPricing.convexityPrice) == num(pricing.convexityPrice)}")
                source.close()
            }
        }
    }

    // ==================== Generation ====================

    private fun generationInput(scenario: String): CuttingPlanGenerationInput<Flt64> {
        val products = List(8) { index ->
            Product(
                id = productIdOf("p$index"),
                name = "p$index",
                width = listOf(Quantity(Flt64(0.17 + index * 0.03), Meter))
            )
        }
        return CuttingPlanGenerationInput(
            products = products,
            materials = List(12) { index ->
                Material(
                    id = materialIdOf("m$index"),
                    name = "m$index",
                    widthRange = WidthRange(
                        width = QuantityRange(
                            lowerBound = Quantity(Flt64.zero, Meter),
                            upperBound = Quantity(Flt64(if (scenario == "distinct") 1.2 + index * 0.01 else 1.2), Meter)
                        ),
                        step = Quantity(Flt64(0.01), Meter)
                    )
                )
            },
            machines = emptyList(),
            demands = products.map { ProductDemand.roll(it, Quantity(Flt64(8), RollCountUnit)) }
        )
    }

    private fun generationConstraints(scenario: String): List<CuttingPlanConstraint<Flt64>> {
        val constraints = mutableListOf<CuttingPlanConstraint<Flt64>>(MaxKnifeCountConstraint(UInt64(5)))
        if (scenario == "custom") {
            constraints.add(object : CuttingPlanConstraint<Flt64> {
                override fun isSatisfied(context: CuttingPlanConstraintContext<Flt64>): Boolean = true
            })
        }
        return constraints
    }

    private fun dumpGeneration() {
        val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!
        for (scenario in listOf("equivalent", "distinct", "custom")) {
            val input = generationInput(scenario = scenario)
            val generators: List<Pair<String, Csp1dInitialCuttingPlanGenerator<Flt64>>> = listOf(
                "dfs" to DFSGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(100_000),
                    parallelism = Int64.one
                ),
                "nsum" to NSumGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(100_000),
                    parallelism = Int64.one
                ),
                "fullsum" to FullSumGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(100_000),
                    parallelism = Int64.one
                )
            )
            for ((name, generator) in generators) {
                val report = generator.generateWithReport(input)
                val keys = report.plans.map { it.canonicalKey().toString() }
                line("GEN|$scenario|$name|plans=${keys.size}|stop=${report.statistics.stopReason.name}")
                line("GEN|$scenario|$name|orderHead=${keys.take(3).joinToString("|") { it.hashCode().toString(16) }}")
                line("GEN|$scenario|$name|sortedDigest=${keys.sorted().joinToString(";").hashCode()}")
                val snapshot = CuttingPlanGenerationBenchmarkSnapshot.from(name, report.statistics)
                line("GEN|$scenario|$name|stats=${snapshot.toStableLine()}")
            }
        }
    }

    /** 预算截断负载的语义（单 worker，可确定复现）。 / Truncated-load semantics (single worker, deterministically reproducible).
     *
     * 计划要求完整输出与预算截断负载分别比较，并记录终止原因、方案数及顺序、visited nodes 与缓存计数。
     * 多 worker 下到达方案上限的先后可能与协程调度交错有关，因此本段不对并行截断负载声明逐项顺序等价；
     * 只有 workers=1 的截断路径做严格逐行差分，交错顺序本身需要另设重跑协议才能断言。
     * The plan requires comparing full-output and budget-truncated loads separately, recording termination reason,
     * plan count and order, visited nodes and cache counters. With several workers, arrival order at the plan limit
     * can depend on coroutine scheduling interleaves, so this section does not assert item-by-item order equality for
     * parallel truncated loads; only single-worker truncation is diffed strictly, while interleave order itself would
     * need a dedicated re-run protocol before it can be asserted.
    */
    private fun dumpGenerationTruncated() {
        val arithmetic = DefaultQuantityArithmetic.resolveFor(Flt64.zero).value!!
        for (scenario in listOf("equivalent", "distinct", "custom")) {
            val input = generationInput(scenario = scenario)
            val generators: List<Pair<String, Csp1dInitialCuttingPlanGenerator<Flt64>>> = listOf(
                "dfs" to DFSGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(200),
                    parallelism = Int64.one
                ),
                "nsum" to NSumGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(200),
                    parallelism = Int64.one
                ),
                "fullsum" to FullSumGenerator(
                    constraints = generationConstraints(scenario = scenario),
                    arithmetic = arithmetic,
                    maxPlans = Int64(200),
                    parallelism = Int64.one
                )
            )
            for ((name, generator) in generators) {
                val report = generator.generateWithReport(input)
                val keys = report.plans.map { it.canonicalKey().toString() }
                line("TRUNC|$scenario|$name|plans=${keys.size}|stop=${report.statistics.stopReason.name}")
                line("TRUNC|$scenario|$name|orderDigest=${keys.joinToString(";").hashCode()}|sortedDigest=${keys.sorted().joinToString(";").hashCode()}")
                line("TRUNC|$scenario|$name|stats=${CuttingPlanGenerationBenchmarkSnapshot.from(name, report.statistics).toStableLine()}")
            }
        }
    }

    // ==================== Token ====================

    private fun dumpToken() {
        for (size in listOf(1024, 4096)) {
            val variables = List(size) { RealVar("tok$it") }
            val writer = ManualTokenList<Flt64>(checkTokenExisted = true)
            writer.add(variables)
            val map = writer.tokens.associateBy { it.key }
            val list = TokenList(map)
            try {
                val ordered = list.tokensInSolver
                var checksum = 0L
                for (token in map.values) checksum += (list.indexOf(token) ?: -1)
                line("TOK|$size|coldChecksum|$checksum|orderedFirst=${ordered.first().solverIndex}|orderedLast=${ordered.last().solverIndex}")
                var warmChecksum = 0L
                for (token in map.values) warmChecksum += (list.indexOf(token) ?: -1)
                line("TOK|$size|warmEqualsCold|same=${warmChecksum == checksum}")
                val miss = list.indexOf(RealVar("tok$size"))
                line("TOK|$size|missIsNull|${miss == null}")
                val byItem = map.values.take(4).joinToString(",") { (list.indexOf(it.variable) ?: -1).toString() }
                line("TOK|$size|indexOfItem|$byItem")
            } finally {
                list.close()
                writer.close()
            }
        }
    }

    // ==================== Prime ====================

    private fun dumpPrime() {
        val cache = PrimeCache()
        var count = 0
        var digest = 0L
        for (value in 0..20_000) {
            val prime = cache.isPrime(UInt64(value))
            if (prime) {
                count++
                digest = digest * 31 + value
            }
        }
        line("PRM|isPrime0To20000|count=$count|digest=$digest")
        val primes = cache.getPrimes(UInt64(20_000))
        line("PRM|getPrimes|count=${primes.size}|head=${primes.take(10).joinToString(",")}|tail=${primes.takeLast(5).joinToString(",")}")
        val beyond = listOf(1_000_003, 1_999_999, 999_999_937, 1_000_000_007).map { UInt64(it) to cache.isPrime(UInt64(it)) }
        line("PRM|beyondSieve|${beyond.joinToString(",") { it.first.toString() + "=" + it.second }}")
        line("PRM|boundaries|0=${cache.isPrime(UInt64.zero)},1=${cache.isPrime(UInt64.one)},2=${cache.isPrime(UInt64(2))}")
        val repeat = (0..5000).map { cache.isPrime(UInt64(it)) }
        line("PRM|warmSameAsCold|count=${repeat.count { it }}")
    }
}
