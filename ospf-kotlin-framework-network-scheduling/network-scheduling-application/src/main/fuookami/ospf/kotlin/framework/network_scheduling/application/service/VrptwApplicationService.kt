package fuookami.ospf.kotlin.framework.network_scheduling.application.service

import fuookami.ospf.kotlin.utils.functional.*
import fuookami.ospf.kotlin.math.algebra.concept.FloatingNumber
import fuookami.ospf.kotlin.core.model.mechanism.*
import fuookami.ospf.kotlin.core.solver.output.*
import fuookami.ospf.kotlin.framework.solver.ColumnGenerationSolver
import fuookami.ospf.kotlin.framework.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.model.*
import fuookami.ospf.kotlin.framework.network_scheduling.domain.vrp.service.VrptwValidator
import fuookami.ospf.kotlin.framework.network_scheduling.application.model.*

/**
 * VRPTW 应用服务。 / VRPTW application service.
 *
 * 校验输入、构造策略与 contexts、调用 Branch-and-Price 算法并组装业务 solution。
 * 正常算法终态返回 Ok(VrptwSolveResult)；输入、单位转换、solver 调用
 * 或对偶证书失败返回 Failed。 / Validates input, constructs strategies and contexts, invokes the
 * Branch-and-Price algorithm, and assembles the business solution.
 * Normal algorithm terminal states return Ok(VrptwSolveResult);
 * input, unit conversion, solver call, or dual certificate failures
 * return Failed.
 */
class VrptwApplicationService<V : FloatingNumber<V>>(
    private val instance: VrptwInstance<V>,
    private val solver: ColumnGenerationSolver,
    private val configuration: BranchAndPriceAlgorithm.Configuration = BranchAndPriceAlgorithm.Configuration(),
    private val policy: BranchAndPriceAlgorithm.Policy<V>,
    private val solutionEnrichers: List<SolutionEnricher<V>> = emptyList()
) {
    /**
     * 求解 VRPTW 实例。 / Solve the VRPTW instance.
     *
     * @return 求解结果 / Solve result
     */
    suspend fun solve(): Ret<VrptwSolveResult<V>> {
        // 1. 校验输入 / Validate input
        when (val validation = VrptwValidator.validate(instance)) {
            is Ok -> {}
            is Failed -> return Failed(validation.error)
            is Fatal -> return Fatal(validation.errors)
        }

        // 2. 构造算法 / Construct algorithm
        val algorithm = BranchAndPriceAlgorithm(
            instance = instance,
            solver = solver,
            configuration = configuration,
            policy = policy
        )

        // 3. 调用求解 / Invoke solve
        val bpResult = when (val result = algorithm.solve()) {
            is Ok -> result.value
            is Failed -> return Failed(result.error)
            is Fatal -> return Fatal(result.errors)
        }

        // 4. 映射结果 / Map result
        var solveResult = VrptwSolveResult(
            status = bpResult.status,
            solution = bpResult.solution,
            lowerBound = bpResult.lowerBound,
            upperBound = bpResult.upperBound,
            relativeGap = bpResult.trace.relativeGap,
            trace = bpResult.trace
        )

        // 5. 应用 enricher / Apply enrichers
        for (enricher in solutionEnrichers) {
            solveResult = enricher.enrich(solveResult)
        }

        return Ok(solveResult)
    }
}
