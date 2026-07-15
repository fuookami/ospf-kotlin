package fuookami.ospf.kotlin.framework.csp1d.application.model

import fuookami.ospf.kotlin.math.algebra.concept.RealNumber
import fuookami.ospf.kotlin.math.algebra.number.*
import fuookami.ospf.kotlin.core.model.mechanism.LinearMetaModel
import fuookami.ospf.kotlin.framework.csp1d.application.service.WasteMinimizationConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.length_assignment.model.LengthAssignmentModelingConfig
import fuookami.ospf.kotlin.framework.csp1d.domain.material.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.produce.model.*
import fuookami.ospf.kotlin.framework.csp1d.domain.yield.model.YieldModelingConfig
import fuookami.ospf.kotlin.framework.model.Pipeline

/** CSP1D DSL 标记 / CSP1D DSL marker */
@DslMarker
annotation class Csp1dDsl

/**
 * CSP1D 问题定义 builder / Builder for CSP1D problem definition
 *
 * @param V 数值类型 / Numeric value type
*/
@Csp1dDsl
class Csp1dProblemBuilder<V : RealNumber<V>> {
    private val productBuffer = ArrayList<Product<V>>()
    private val materialBuffer = ArrayList<Material<V>>()
    private val machineBuffer = ArrayList<Machine<V>>()
    private val costarBuffer = ArrayList<Costar<V>>()
    private val demandBuffer = ArrayList<ProductDemand<V>>()
    private var configurationValue: Csp1dConfiguration<V> = Csp1dConfiguration()
    private var solveConfigValue: Csp1dSolveConfig<V>? = null

    /**
     * 增加产品 / Add a product
     *
     * @param product 产品 / Product
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun product(product: Product<V>): Csp1dProblemBuilder<V> {
        productBuffer.add(product)
        return this
    }

    /**
     * 增加产品列表 / Add products
     *
     * @param products 产品列表 / Products
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun products(products: Iterable<Product<V>>): Csp1dProblemBuilder<V> {
        productBuffer.addAll(products)
        return this
    }

    /**
     * 增加物料 / Add a material
     *
     * @param material 物料 / Material
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun material(material: Material<V>): Csp1dProblemBuilder<V> {
        materialBuffer.add(material)
        return this
    }

    /**
     * 增加物料列表 / Add materials
     *
     * @param materials 物料列表 / Materials
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun materials(materials: Iterable<Material<V>>): Csp1dProblemBuilder<V> {
        materialBuffer.addAll(materials)
        return this
    }

    /**
     * 增加设备 / Add a machine
     *
     * @param machine 设备 / Machine
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun machine(machine: Machine<V>): Csp1dProblemBuilder<V> {
        machineBuffer.add(machine)
        return this
    }

    /**
     * 增加设备列表 / Add machines
     *
     * @param machines 设备列表 / Machines
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun machines(machines: Iterable<Machine<V>>): Csp1dProblemBuilder<V> {
        machineBuffer.addAll(machines)
        return this
    }

    /**
     * 增加配规 / Add a costar
     *
     * @param costar 配规 / Costar
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun costar(costar: Costar<V>): Csp1dProblemBuilder<V> {
        costarBuffer.add(costar)
        return this
    }

    /**
     * 增加配规列表 / Add costars
     *
     * @param costars 配规列表 / Costars
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun costars(costars: Iterable<Costar<V>>): Csp1dProblemBuilder<V> {
        costarBuffer.addAll(costars)
        return this
    }

    /**
     * 增加需求 / Add a demand
     *
     * @param demand 需求 / Demand
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun demand(demand: ProductDemand<V>): Csp1dProblemBuilder<V> {
        demandBuffer.add(demand)
        return this
    }

    /**
     * 增加需求列表 / Add demands
     *
     * @param demands 需求列表 / Demands
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun demands(demands: Iterable<ProductDemand<V>>): Csp1dProblemBuilder<V> {
        demandBuffer.addAll(demands)
        return this
    }

    /**
     * 设置列生成配置 / Set column generation configuration
     *
     * @param configuration 配置 / Configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun configuration(configuration: Csp1dConfiguration<V>): Csp1dProblemBuilder<V> {
        configurationValue = configuration
        return this
    }

    /**
     * 设置一站式求解配置 / Set one-stop solve configuration
     *
     * @param solveConfig 求解配置 / Solve configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun solveConfig(solveConfig: Csp1dSolveConfig<V>): Csp1dProblemBuilder<V> {
        solveConfigValue = solveConfig
        return this
    }

    /**
     * 设置一站式求解配置 / Set one-stop solve configuration
     *
     * @param block 求解配置 builder / Solve configuration builder
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun solveConfig(block: Csp1dSolveConfigBuilder<V>.() -> Unit): Csp1dProblemBuilder<V> {
        solveConfigValue = csp1dSolveConfig(block)
        return this
    }

    /**
     * 构建问题定义 / Build problem definition
     *
     * @return CSP1D 问题定义 / CSP1D problem definition
    */
    fun build(): Csp1dProblem<V> {
        return Csp1dProblem(
            products = productBuffer.toList(),
            materials = materialBuffer.toList(),
            machines = machineBuffer.toList(),
            costars = costarBuffer.toList(),
            demands = demandBuffer.toList(),
            configuration = configurationValue,
            solveConfig = solveConfigValue
        )
    }
}

/**
 * CSP1D 一站式求解配置 builder / Builder for CSP1D one-stop solve configuration
 *
 * @param V 数值类型 / Numeric value type
*/
@Csp1dDsl
class Csp1dSolveConfigBuilder<V : RealNumber<V>> {
    private var columnGenerationValue: Csp1dConfiguration<V> = Csp1dConfiguration()
    private var yieldConfigValue: YieldModelingConfig<V>? = null
    private var wasteConfigValue: WasteMinimizationConfig<V>? = null
    private var lengthConfigValue: LengthAssignmentModelingConfig<V>? = null
    private var topKPlanLimitValue: Int64? = null
    private var allowPartialSolutionValue: Boolean = true
    private val extensionsBuffer = ArrayList<Csp1dModelingExtension<V>>()
    private val domainPolicyBuffer = ArrayList<Csp1dDomainPolicy<V>>()
    private val objectivePolicyBuffer = ArrayList<Csp1dObjectivePolicy<V>>()
    private val generationStrategyBuffer = ArrayList<Csp1dGenerationStrategy<V>>()
    private val pricingPolicyBuffer = ArrayList<Csp1dPricingPolicy<V>>()
    private val flowPolicyBuffer = ArrayList<Csp1dFlowPolicy<V>>()
    private val extractionPolicyBuffer = ArrayList<Csp1dExtractionPolicy<V>>()

    /**
     * 设置列生成配置 / Set column generation configuration
     *
     * @param configuration 配置 / Configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun columnGeneration(configuration: Csp1dConfiguration<V>): Csp1dSolveConfigBuilder<V> {
        columnGenerationValue = configuration
        return this
    }

    /**
     * 设置列生成配置 / Set column generation configuration
     *
     * @param maxInitialPlans 初始方案上限 / Initial plan limit
     * @param maxPricingPlans 每轮定价方案上限 / Pricing plan limit per iteration
     * @param iterationLimit 迭代上限 / Iteration limit
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun columnGeneration(
        maxInitialPlans: Int64,
        maxPricingPlans: Int64,
        iterationLimit: Int64
    ): Csp1dSolveConfigBuilder<V> {
        columnGenerationValue = Csp1dConfiguration(
            maxInitialPlans = maxInitialPlans,
            maxPricingPlans = maxPricingPlans,
            iterationLimit = iterationLimit
        )
        return this
    }

    /**
     * 设置 yield 建模配置 / Set yield modeling configuration
     *
     * @param config yield 建模配置 / Yield modeling configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun yieldConfig(config: YieldModelingConfig<V>?): Csp1dSolveConfigBuilder<V> {
        yieldConfigValue = config
        return this
    }

    /**
     * 设置 waste 建模配置 / Set waste modeling configuration
     *
     * @param config waste 建模配置 / Waste modeling configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun wasteConfig(config: WasteMinimizationConfig<V>?): Csp1dSolveConfigBuilder<V> {
        wasteConfigValue = config
        return this
    }

    /**
     * 设置 length 建模配置 / Set length modeling configuration
     *
     * @param config length 建模配置 / Length modeling configuration
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun lengthConfig(config: LengthAssignmentModelingConfig<V>?): Csp1dSolveConfigBuilder<V> {
        lengthConfigValue = config
        return this
    }

    /**
     * 设置 Top-K 方案上限 / Set Top-K plan limit
     *
     * @param limit Top-K 方案上限 / Top-K plan limit
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun topKPlanLimit(limit: Int64?): Csp1dSolveConfigBuilder<V> {
        topKPlanLimitValue = limit
        return this
    }

    /**
     * 设置是否允许部分结果 / Set whether partial results are allowed
     *
     * @param enabled 是否允许 / Whether enabled
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun allowPartialSolution(enabled: Boolean): Csp1dSolveConfigBuilder<V> {
        allowPartialSolutionValue = enabled
        return this
    }

    /**
     * 追加建模扩展 / Add a modeling extension
     *
     * @param extension 建模扩展 / Modeling extension
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun extension(extension: Csp1dModelingExtension<V>): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.add(extension)
        return this
    }

    /**
     * 追加建模扩展列表 / Add modeling extensions
     *
     * @param extensions 建模扩展列表 / Modeling extensions
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun extensions(extensions: Iterable<Csp1dModelingExtension<V>>): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.addAll(extensions)
        return this
    }

    /**
     * 便捷方法：追加扩展管线（默认所有模式）/ Convenience: add an extension pipeline (all modes)
     *
     * @param pipeline 扩展管线 / Extension pipeline
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun extensionPipeline(pipeline: Pipeline<LinearMetaModel<Flt64>>): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.add(Csp1dModelingExtension(pipeline))
        return this
    }

    /**
     * 便捷方法：追加扩展管线（指定模式）/ Convenience: add an extension pipeline (specific mode)
     *
     * @param pipeline 扩展管线 / Extension pipeline
     * @param mode 扩展适用模式 / Extension applicable mode
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun extensionPipeline(
        pipeline: Pipeline<LinearMetaModel<Flt64>>,
        mode: Csp1dExtensionMode
    ): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.add(Csp1dModelingExtension(pipeline, mode))
        return this
    }

    /**
     * 便捷方法：追加上下文感知扩展管线（默认所有模式）/ Convenience: add a context-aware extension pipeline (all modes)
     *
     * 下游扩展管线通过 factory 接收 Csp1dModelingContext，无需闭包捕获即可访问领域数据。
     * Downstream extension pipelines receive Csp1dModelingContext through factory,
     * accessing domain data without closure capture.
     *
     * @param factory 上下文感知管线工厂 / Context-aware pipeline factory
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun contextAwareExtensionPipeline(
        factory: (Csp1dModelingContext<V>) -> Pipeline<LinearMetaModel<Flt64>>
    ): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.add(Csp1dModelingExtension(
            contextAwarePipeline = factory
        ))
        return this
    }

    /**
     * 便捷方法：追加上下文感知扩展管线（指定模式）/ Convenience: add a context-aware extension pipeline (specific mode)
     *
     * @param factory 上下文感知管线工厂 / Context-aware pipeline factory
     * @param mode 扩展适用模式 / Extension applicable mode
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun contextAwareExtensionPipeline(
        factory: (Csp1dModelingContext<V>) -> Pipeline<LinearMetaModel<Flt64>>,
        mode: Csp1dExtensionMode
    ): Csp1dSolveConfigBuilder<V> {
        extensionsBuffer.add(Csp1dModelingExtension(
            mode = mode,
            contextAwarePipeline = factory
        ))
        return this
    }

    /**
     * 追加领域策略 / Add a domain policy
     *
     * @param policy 领域策略 / Domain policy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun domainPolicy(policy: Csp1dDomainPolicy<V>): Csp1dSolveConfigBuilder<V> {
        domainPolicyBuffer.add(policy)
        return this
    }

    /**
     * 追加目标策略 / Add an objective policy
     *
     * @param policy 目标策略 / Objective policy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun objectivePolicy(policy: Csp1dObjectivePolicy<V>): Csp1dSolveConfigBuilder<V> {
        objectivePolicyBuffer.add(policy)
        return this
    }

    /**
     * 追加生成策略 / Add a generation strategy
     *
     * @param strategy 生成策略 / Generation strategy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun generationStrategy(strategy: Csp1dGenerationStrategy<V>): Csp1dSolveConfigBuilder<V> {
        generationStrategyBuffer.add(strategy)
        return this
    }

    /**
     * 追加定价策略 / Add a pricing policy
     *
     * @param policy 定价策略 / Pricing policy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun pricingPolicy(policy: Csp1dPricingPolicy<V>): Csp1dSolveConfigBuilder<V> {
        pricingPolicyBuffer.add(policy)
        return this
    }

    /**
     * 追加流程策略 / Add a flow policy
     *
     * @param policy 流程策略 / Flow policy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun flowPolicy(policy: Csp1dFlowPolicy<V>): Csp1dSolveConfigBuilder<V> {
        flowPolicyBuffer.add(policy)
        return this
    }

    /**
     * 追加提取策略 / Add an extraction policy
     *
     * @param policy 提取策略 / Extraction policy
     *
     * @return this builder for chaining / 此 builder 实例，用于链式调用
    */
    fun extractionPolicy(policy: Csp1dExtractionPolicy<V>): Csp1dSolveConfigBuilder<V> {
        extractionPolicyBuffer.add(policy)
        return this
    }

    /**
     * 构建求解配置 / Build solve configuration
     *
     * @return 一站式求解配置 / One-stop solve configuration
    */
    fun build(): Csp1dSolveConfig<V> {
        return Csp1dSolveConfig(
            columnGeneration = columnGenerationValue,
            yieldConfig = yieldConfigValue,
            wasteConfig = wasteConfigValue,
            lengthConfig = lengthConfigValue,
            topKPlanLimit = topKPlanLimitValue,
            allowPartialSolution = allowPartialSolutionValue,
            extensions = extensionsBuffer.toList(),
            extensionSet = Csp1dExtensionSet(
                modelingExtensions = emptyList(),
                domainPolicies = domainPolicyBuffer.toList(),
                objectivePolicies = objectivePolicyBuffer.toList(),
                generationStrategies = generationStrategyBuffer.toList(),
                pricingPolicies = pricingPolicyBuffer.toList(),
                flowPolicies = flowPolicyBuffer.toList(),
                extractionPolicies = extractionPolicyBuffer.toList()
            )
        )
    }
}

/**
 * 构建 CSP1D 问题定义 / Build CSP1D problem definition
 *
 * @param V 数值类型 / Numeric value type
 * @param block 问题定义 builder / Problem definition builder
 * @return CSP1D 问题定义 / CSP1D problem definition
*/
fun <V : RealNumber<V>> csp1dProblem(block: Csp1dProblemBuilder<V>.() -> Unit): Csp1dProblem<V> {
    return Csp1dProblemBuilder<V>().apply(block).build()
}

/**
 * 构建 CSP1D 求解配置 / Build CSP1D solve configuration
 *
 * @param V 数值类型 / Numeric value type
 * @param block 求解配置 builder / Solve configuration builder
 * @return CSP1D 求解配置 / CSP1D solve configuration
*/
fun <V : RealNumber<V>> csp1dSolveConfig(block: Csp1dSolveConfigBuilder<V>.() -> Unit): Csp1dSolveConfig<V> {
    return Csp1dSolveConfigBuilder<V>().apply(block).build()
}
