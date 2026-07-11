/** COPT 求解器常量定义 / COPT solver constants definition */
@file:OptIn(kotlin.time.ExperimentalTime::class)
package copt

/** COPT 求解器常量和枚举定义 / COPT solver constants and enum definitions */
data object COPT {
    const val MINIMIZE = 1
    const val MAXIMIZE = -1

    const val INFINITY = 1e30
    const val UNDEFINED = 1e40

    const val BUFF_SIZE = 1000

    const val LESS_EQUAL = 'L'
    const val GREATER_EQUAL = 'G'
    const val EQUAL = 'E'
    const val FREE = 'N'
    const val RANGE = 'R'

    const val CONTINUOUS = 'C'
    const val BINARY = 'B'
    const val INTEGER = 'I'

    const val SOS_TYPE1 = 1
    const val SOS_TYPE2 = 2

    const val INDICATOR_IF = 1
    const val INDICATOR_ONLY_IF = 2
    const val INDICATOR_IF_AND_ONLY_IF = 3

    const val CONE_QUAD = 1
    const val CONE_RQUAD = 2

    const val EXPCONE_PRIMAL = 3
    const val EXPCONE_DUAL = 4

    const val CALL_BACK_CONTEXT_MIP_RELAX = 1
    const val CALL_BACK_CONTEXT_MIP_SOL = 2
    const val CALL_BACK_CONTEXT_MIP_NODE = 3
    const val CALL_BACK_CONTEXT_INCUMBENT = 4

    /** 返回码枚举 / Return code enum */
    enum class RetCode(val value: Int) {
        /** 成功 / Success */
        Ok(0),
        /** 内存错误 / Memory error */
        Memory(1),
        /** 文件错误 / File error */
        File(2),
        /** 无效参数 / Invalid parameter */
        Invalid(3),
        /** 许可证错误 / License error */
        License(4),
        /** 内部错误 / Internal error */
        Internal(5),
        /** 线程错误 / Thread error */
        Thread(6),
        /** 服务器错误 / Server error */
        Sever(7),
        /** 非凸问题 / Non-convex problem */
        NonConvex(8)
    }

    /** 基状态枚举 / Basis status enum */
    enum class BasisStatus(val value: Int) {
        /** 下界 / Lower bound */
        Lower(0),
        /** 基变量 / Basic variable */
        Basic(1),
        /** 上界 / Upper bound */
        Upper(2),
        /** 超基变量 / Super basic variable */
        SuperBasic(3),
        /** 固定变量 / Fixed variable */
        Fixed(4)
    }

    /** 求解状态枚举 / Solving status enum */
    enum class Status(val value: Int) {
        /** 未开始 / Not started */
        Unstarted(0),
        /** 最优解 / Optimal solution */
        Optimal(1),
        /** 不可行 / Infeasible */
        Infeasible(2),
        /** 无界 / Unbounded */
        Unbounded(3),
        /** 不可行或无界 / Infeasible or unbounded */
        InfeasibleOrUnbounded(4),
        /** 数值问题 / Numerical issue */
        Numerical(5),
        /** 节点限制 / Node limit reached */
        NodeLimit(6),
        /** 不精确解 / Imprecise solution */
        Imprecise(7),
        /** 超时 / Time out */
        TimeOut(8),
        /** 未完成 / Unfinished */
        Unfinished(9),
        /** 被中断 / Interrupted */
        Interrupted(10),
    }

    /**
     * 回调信息枚举 / Callback info enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class CallBackInfo(val key: String) {
        /** 最优目标值 / Best objective value */
        BestObj("BestObj"),
        /** 最优界 / Best bound */
        BestBound("BestBnd"),
        /** 是否有可行解 / Has incumbents */
        HasIncumbents("HasIncumbents"),
        /** 当前最优解 / Incumbent solution */
        Incumbent("Incumbent"),
        /** MIP 候选解 / MIP candidate solution */
        MipCandidate("MipCandidate"),
        /** MIP 候选解目标值 / MIP candidate objective */
        MipCandObj("MipCandObj"),
        /** 松弛解 / Relaxation solution */
        RelaxSolution("RelaxSolution"),
        /** 松弛解目标值 / Relaxation solution objective */
        RelaxSolObj("RelaxSolObj"),
        /** 节点状态 / Node status */
        NodeStatus("NodeStatus"),
    }

    /**
     * 双精度参数枚举 / Double parameter enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class DoubleParam(val key: String) {
        /** 时间限制 / Time limit */
        TimeLimit("TimeLimit"),
        /** 求解时间限制 / Solution time limit */
        SolTimeLimit("SolTimeLimit"),
        /** 矩阵容差 / Matrix tolerance */
        MatrixTol("MatrixTol"),
        /** 可行性容差 / Feasibility tolerance */
        FeasTol("FeasTol"),
        /** 对偶容差 / Dual tolerance */
        DualTol("DualTol"),
        /** 整数容差 / Integer tolerance */
        IntTol("IntTol"),
        /** PDLP 容差 / PDLP tolerance */
        PDLPTol("PDLPTol"),
        /** 相对间隙 / Relative gap */
        RelGap("RelGap"),
        /** 绝对间隙 / Absolute gap */
        AbsGap("AbsGap"),
        /** 调优时间限制 / Tuning time limit */
        TuneTimeLimit("TuneTimeLimit"),
        /** 调优目标时间 / Tuning target time */
        TuneTargetTime("TuneTargetTime"),
        /** 调优目标相对间隙 / Tuning target relative gap */
        TuneTargetRelGap("TuneTargetRelGap"),
    }

    /**
     * 整数参数枚举 / Integer parameter enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class IntParam(val key: String) {
        /** 日志级别 / Logging level */
        Logging("Logging"),
        /** 输出到控制台 / Log to console */
        LogToConsole("LogToConsole"),
        /** 预求解 / Presolve */
        Presolve("Presolve"),
        /** 缩放 / Scaling */
        Scaling("Scaling"),
        /** 对偶化 / Dualize */
        Dualize("Dualize"),
        /** LP 求解方法 / LP method */
        LpMethod("LpMethod"),
        /** GPU 模式 / GPU mode */
        GPUMode("GPUMode"),
        /** GPU 设备 / GPU device */
        GPUDevice("GPUDevice"),
        /** 请求 Farkas 射线 / Request Farkas ray */
        ReqFarkasRay("ReqFarkasRay"),
        /** 对偶价格 / Dual price */
        DualPrice("DualPrice"),
        /** 对偶扰动 / Dual perturbation */
        DualPerturb("DualPerturb"),
        /** 割平面级别 / Cut level */
        CutLevel("CutLevel"),
        /** 根节点割平面级别 / Root cut level */
        RootCutLevel("RootCutLevel"),
        /** 树割平面级别 / Tree cut level */
        TreeCutLevel("TreeCutLevel"),
        /** 根节点割平面轮数 / Root cut rounds */
        RootCutRounds("RootCutRounds"),
        /** 节点割平面轮数 / Node cut rounds */
        NodeCutRounds("NodeCutRounds"),
        /** 启发式级别 / Heuristic level */
        HeurLevel("HeurLevel"),
        /** 舍入启发式级别 / Rounding heuristic level */
        RoundingHeurLevel("RoundingHeurLevel"),
        /** 潜水启发式级别 / Diving heuristic level */
        DivingHeurLevel("DivingHeurLevel"),
        /** FAP 启发式级别 / FAP heuristic level */
        FAPHeurLevel("FAPHeurLevel"),
        /** 子 MIP 启发式级别 / Sub-MIP heuristic level */
        SubMipHeurLevel("SubMipHeurLevel"),
        /** 强分支 / Strong branching */
        StrongBranching("StrongBranching"),
        /** 冲突分析 / Conflict analysis */
        ConflictAnalysis("ConflictAnalysis"),
        /** 节点限制 / Node limit */
        NodeLimit("NodeLimit"),
        /** MIP 任务数 / MIP tasks */
        MipTasks("MipTasks"),
        /** 内点法齐次模式 / Barrier homogeneous mode */
        BarHomogeneous("BarHomogeneous"),
        /** 内点法排序 / Barrier ordering */
        BarOrder("BarOrder"),
        /** 内点法起始点 / Barrier start */
        BarStart("BarStart"),
        /** 内点法迭代限制 / Barrier iteration limit */
        BarIterLimit("BarIterLimit"),
        /** 线程数 / Threads */
        Threads("Threads"),
        /** 内点法线程数 / Barrier threads */
        BarThreads("BarThreads"),
        /** 单纯形线程数 / Simplex threads */
        SimplexThreads("SimplexThreads"),
        /** 交叉线程数 / Crossover threads */
        CrossoverThreads("CrossoverThreads"),
        /** 交叉 / Crossover */
        Crossover("Crossover"),
        /** 半定方法 / Semi-definite method */
        SDMethod("SDMethod"),
        /** IIS 方法 / IIS method */
        IISMethod("IISMethod"),
        /** 可行性松弛模式 / Feasibility relaxation mode */
        FeasRelaxMode("FeasRelaxMode"),
        /** MIP 启动模式 / MIP start mode */
        MipStartMode("MipStartMode"),
        /** MIP 启动节点限制 / MIP start node limit */
        MipStartNodeLimit("MipStartNodeLimit"),
        /** 调优方法 / Tuning method */
        TuneMethod("TuneMethod"),
        /** 调优模式 / Tuning mode */
        TuneMode("TuneMode"),
        /** 调优度量 / Tuning measure */
        TuneMeasure("TuneMeasure"),
        /** 调优排列 / Tuning permutations */
        TunePermutes("TunePermutes"),
        /** 调优输出级别 / Tuning output level */
        TuneOutputLevel("TuneOutputLevel"),
        /** 惰性约束 / Lazy constraints */
        LazyConstraints("LazyConstraints"),
    }

    /**
     * 双精度属性枚举 / Double attribute enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class DoubleAttr(val key: String) {
        /** 求解时间 / Solving time */
        SolvingTime("SolvingTime"),
        /** 目标常数项 / Objective constant */
        ObjConst("ObjConst"),
        /** LP 目标值 / LP objective value */
        LpObjVal("LpObjval"),
        /** 最优目标值 / Best objective */
        BestObj("BestObj"),
        /** 最优界 / Best bound */
        BestBound("BestBnd"),
        /** 最优间隙 / Best gap */
        BestGap("BestGap"),
        /** 可行性松弛目标值 / Feasibility relaxation objective */
        FeasRelaxObj("FeasRelaxObj")
    }

    /**
     * 整数属性枚举 / Integer attribute enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class IntAttr(val key: String) {
        /** 列数 / Number of columns */
        Cols("Cols"),
        /** 半定列数 / Number of PSD columns */
        PSDCols("PSDCols"),
        /** 行数 / Number of rows */
        Rows("Rows"),
        /** 非零元素数 / Number of elements */
        Elems("Elems"),
        /** 二次元素数 / Number of quadratic elements */
        QElems("QElems"),
        /** 半定元素数 / Number of PSD elements */
        PSDElems("PSDElems"),
        /** 对称矩阵数 / Number of symmetric matrices */
        SymMats("SymMats"),
        /** 二进制变量数 / Number of binary variables */
        Bins("Bins"),
        /** 整数变量数 / Number of integer variables */
        Ints("Ints"),
        /** SOS 约束数 / Number of SOS constraints */
        Soss("Soss"),
        /** 锥约束数 / Number of cone constraints */
        Cones("Cones"),
        /** 指数锥约束数 / Number of exponential cone constraints */
        ExpCones("ExpCones"),
        /** 二次约束数 / Number of quadratic constraints */
        QConstrs("QConstrs"),
        /** 半定约束数 / Number of PSD constraints */
        PSDConstrs("PSDConstrs"),
        /** LMI 约束数 / Number of LMI constraints */
        LMIConstrs("LMIConstrs"),
        /** 指示器约束数 / Number of indicator constraints */
        Indicators("Indicators"),
        /** IIS 列数 / Number of IIS columns */
        IISCols("IISCols"),
        /** IIS 行数 / Number of IIS rows */
        IISRows("IISRows"),
        /** IIS SOS 数 / Number of IIS SOS */
        IISSOSs("IISSOSs"),
        /** IIS 指示器数 / Number of IIS indicators */
        IISIndicators("IISIndicators"),
        /** 目标方向 / Objective sense */
        ObjSense("ObjSense"),
        /** LP 状态 / LP status */
        LpStatus("LpStatus"),
        /** MIP 状态 / MIP status */
        MipStatus("MipStatus"),
        /** 单纯形迭代次数 / Simplex iterations */
        SimplexIter("SimplexIter"),
        /** 内点法迭代次数 / Barrier iterations */
        BarrierIter("BarrierIter"),
        /** 节点数 / Node count */
        NodeCnt("NodeCnt"),
        /** 解池大小 / Solution pool size */
        PoolSols("PoolSols"),
        /** 调优结果数 / Tuning results */
        TuneResults("TuneResults"),
        /** 是否有 LP 解 / Has LP solution */
        HasLpSol("HasLpSol"),
        /** 是否有对偶 Farkas / Has dual Farkas */
        HasDualFarkas("HasDualFarkas"),
        /** 是否有原始射线 / Has primal ray */
        HasPrimalRay("HasPrimalRay"),
        /** 是否有基 / Has basis */
        HasBasis("HasBasis"),
        /** 是否有 MIP 解 / Has MIP solution */
        HasMipSol("HasMipSol"),
        /** 是否有二次目标 / Has quadratic objective */
        HasQObj("HasQObj"),
        /** 是否有半定目标 / Has PSD objective */
        HasPSDObj("HasPSDObj"),
        /** 是否有 IIS / Has IIS */
        HasIIS("HasIIS"),
        /** 是否有可行性松弛解 / Has feasibility relaxation solution */
        HasFeasRelaxSol("HasFeasRelaxSol"),
        /** 是否为 MIP 问题 / Is MIP problem */
        IsMIP("IsMIP"),
        /** 是否为最小 IIS / Is minimum IIS */
        IsMinIIS("IsMinIIS"),
    }

    /**
     * 双精度信息枚举 / Double info enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class DoubleInfo(val key: String) {
        /** 目标系数 / Objective coefficient */
        Obj("Obj"),
        /** 下界 / Lower bound */
        LB("LB"),
        /** 上界 / Upper bound */
        UB("UB"),
        /** 变量值 / Variable value */
        Value("Value"),
        /** 松弛量 / Slack */
        Slack("Slack"),
        /** 对偶值 / Dual value */
        Dual("Dual"),
        /** 约束缩减成本 / Reduced cost */
        RedCost("RedCost"),
        /** 对偶 Farkas / Dual Farkas */
        DualFarkas("DualFarkas"),
        /** 原始射线 / Primal ray */
        PrimalRay("PrimalRay"),
        /** 松弛下界 / Relaxation lower bound */
        RelaxLB("RelaxLB"),
        /** 松弛上界 / Relaxation upper bound */
        RelaxUB("RelaxUB"),
        /** 松弛值 / Relaxation value */
        RelaxValue("RelaxValue"),
    }

    /**
     * 客户端配置枚举 / Client configuration enum
     *
     * @property key the parameter key string / 参数键字符串
    */
    enum class Client(val key: String) {
        /** CA 文件 / CA file */
        CaFile("CaFile"),
        /** 证书文件 / Certificate file */
        CertFile("CertFile"),
        /** 证书密钥文件 / Certificate key file */
        CertKeyFile("CertKeyFile"),
        /** 集群 / Cluster */
        Cluster("Cluster"),
        /** 浮动许可 / Floating license */
        Floating("Floating"),
        /** 密码 / Password */
        Password("Password"),
        /** 端口 / Port */
        Port("Port"),
        /** 优先级 / Priority */
        Priority("Priority"),
        /** 等待时间 / Wait time */
        WaitTime("WaitTime"),
        /** Web 服务器 / Web server */
        WebServer("WebServer"),
        /** Web 许可证 ID / Web license ID */
        WebLicenseId("WebLicenseId"),
        /** Web 访问密钥 / Web access key */
        WebAccessKey("WebAccessKey"),
        /** Web 令牌有效期 / Web token duration */
        WebTokenDuration("WebTokenDuration"),
    }
}

/**
 * 设置 COPT 客户端配置 / Set COPT client configuration
 *
 * @param key 客户端配置键 / client configuration key
 * @param value 配置值 / configuration value
*/
fun EnvrConfig.set(key: COPT.Client, value: String) {
    this.set(key.key, value)
}

/**
 * 设置 COPT 双精度属性 / Set COPT double attribute
 *
 * @param key 双精度属性键 / double attribute key
 * @param value 属性值 / attribute value
*/
fun EnvrConfig.set(key: COPT.DoubleAttr, value: Double) {
    this.set(key.key, value.toString())
}

/**
 * 设置 COPT 整数属性 / Set COPT integer attribute
 *
 * @param key 整数属性键 / integer attribute key
 * @param value 属性值 / attribute value
*/
fun EnvrConfig.set(key: COPT.IntAttr, value: Int) {
    this.set(key.key, value.toString())
}

/**
 * 获取回调信息 / Get callback information
 *
 * @param key 回调信息键 / callback info key
 * @return 回调信息值 / callback info value
*/
fun CallbackBase.get(key: COPT.CallBackInfo): Double {
    return this.getDblInfo(key.key)
}

/**
 * 获取模型双精度参数 / Get model double parameter
 *
 * @param key 双精度参数键 / double parameter key
 * @return 参数值 / parameter value
*/
fun Model.get(key: COPT.DoubleParam): Double {
    return this.getDblParam(key.key)
}

/**
 * 设置模型双精度参数 / Set model double parameter
 *
 * @param key 双精度参数键 / double parameter key
 * @param value 参数值 / parameter value
*/
fun Model.set(key: COPT.DoubleParam, value: Double) {
    this.setDblParam(key.key, value)
}

/**
 * 获取模型整数参数 / Get model integer parameter
 *
 * @param key 整数参数键 / integer parameter key
 * @return 参数值 / parameter value
*/
fun Model.get(key: COPT.IntParam): Int {
    return this.getIntParam(key.key)
}

/**
 * 设置模型整数参数 / Set model integer parameter
 *
 * @param key 整数参数键 / integer parameter key
 * @param value 参数值 / parameter value
*/
fun Model.set(key: COPT.IntParam, value: Int) {
    this.setIntParam(key.key, value)
}

/**
 * 获取模型双精度属性 / Get model double attribute
 *
 * @param key 双精度属性键 / double attribute key
 * @return 属性值 / attribute value
*/
fun Model.get(key: COPT.DoubleAttr): Double {
    return this.getDblAttr(key.key)
}

/**
 * 获取模型整数属性 / Get model integer attribute
 *
 * @param key 整数属性键 / integer attribute key
 * @return 属性值 / attribute value
*/
fun Model.get(key: COPT.IntAttr): Int {
    return this.getIntAttr(key.key)
}

/**
 * 获取变量双精度信息 / Get variable double info
 *
 * @param key 双精度信息键 / double info key
 * @return 信息值 / info value
*/
fun Var.get(key: COPT.DoubleInfo): Double {
    return this.get(key.key)
}

/**
 * 获取约束双精度信息 / Get constraint double info
 *
 * @param key 双精度信息键 / double info key
 * @return 信息值 / info value
*/
fun Constraint.get(key: COPT.DoubleInfo): Double {
    return this.get(key.key)
}

/**
 * 获取二次约束双精度信息 / Get quadratic constraint double info
 *
 * @param key 双精度信息键 / double info key
 * @return 信息值 / info value
*/
fun QConstraint.get(key: COPT.DoubleInfo): Double {
    return this.get(key.key)
}