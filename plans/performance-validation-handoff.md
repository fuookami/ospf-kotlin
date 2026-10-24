# 缓存与准备阶段性能补充验证交接计划

日期：2026-10-25。性质：正式跨环境验证任务规范，不是运行报告或临时进度日志。

## 1. 接手目标与边界

本计划可由没有原会话上下文的会话独立执行。当前环境没有独占的性能测试资源，因此本次交付不继续计时，也不将尚有波动的均值宣称为稳定提速。

接手目标：在隔离、可复现的环境中，逐批确认下列缓存及准备入口的时间、分配、并发扩展性和回退成本；对每项给出“受益 / 等效范围内 / 回退 / 证据不足”的结论，以及保留、调整或回退的建议。不是要求所有项目都测出正收益。

- 先验证现有同步按需/惰性构建实现，不预设更多线程必然有益。
- 只有证明缓存被高频使用、miss 成本显著且存在安全重叠窗口后，才提出后台构建方案；实现此类方案需另行确定范围。
- 不默认启用 BPP 批量定价，不引入 CSP1D 全局共享 quantity cache、按 key 并发构建或完整 DFS single-flight。
- 不改模型修改检测、浮点累加顺序、自定义回调调用顺序、首匹配、变量身份、预算、取消或 timeout 语义。
- 读取并遵守根 `AGENTS.md`、`.rules/` 和涉及子目录的规则。日志、原始结果、剖析文件、临时清单均放系统临时目录或外部 artifact 存储，禁止提交。

本计划不依赖旧会话、被忽略的 `logs/`、原机器的临时文件或旧字节码。下文的路径均相对仓库根目录。

## 2. 固定对照版本，避免测错实现

| 名称 | 版本 | 用途 |
| --- | --- | --- |
| B0 | `b8c99b829a83b1aaf878d3a86fc85c04a6e9fb09` | 本次两轮缓存构建优化之前的已提交源码 |
| C0 | 首次新增本计划文件的提交 | 本次缓存实现及配套 benchmark 的候选版本 |
| A0 | `8593fd1c570376463269717e2818302421110ad4` | 更早的全仓准备阶段优化之前，仅用于第六批 |

在包含本次提交的仓库中解析并冻结 C0，不能直接使用将来的 HEAD：

```powershell
$C0 = (git log --diff-filter=A -1 --format=%H -- plans/performance-validation-handoff.md).Trim()
$B0 = 'b8c99b829a83b1aaf878d3a86fc85c04a6e9fb09'
$A0 = '8593fd1c570376463269717e2818302421110ad4'
if (-not $C0) { throw '未找到正式交接提交 / Handoff commit not found' }
git show --no-patch --format=fuller $C0
```

重要归因限制：两轮缓存优化之间没有独立提交，不能在其它环境重建当时的临时快照。默认 B0 → C0 衡量两轮整体效果。若要拆分某一轮/某一改动，必须审查独立源码消融 patch、固定其 hash 并重建，不得把 B0 冒充仅第二轮前的状态，不得拼接旧结果或相乘加速比。

两端使用完全相同的 benchmark 源码、POM、输入、参数和测量协议。B0 缺少本次四个新增夹具，必须移植测试支撑，但不能移植候选生产实现。推荐在临时目录创建两个独立 clone；不在有用户修改的工作目录切换、覆盖文件。

```powershell
$Source = (Get-Location).Path
$RunRoot = Join-Path ([IO.Path]::GetTempPath()) ('ospf-perf-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $RunRoot | Out-Null
$Baseline = Join-Path $RunRoot 'baseline'
$Candidate = Join-Path $RunRoot 'candidate'
git clone --no-hardlinks -- $Source $Baseline
if ($LASTEXITCODE -ne 0) { throw '基线克隆失败 / Baseline clone failed' }
git clone --no-hardlinks -- $Source $Candidate
if ($LASTEXITCODE -ne 0) { throw '候选克隆失败 / Candidate clone failed' }
git -C $Baseline switch --detach $B0
if ($LASTEXITCODE -ne 0) { throw '基线切换失败 / Baseline checkout failed' }
git -C $Candidate switch --detach $C0
if ($LASTEXITCODE -ne 0) { throw '候选切换失败 / Candidate checkout failed' }
$HarnessPatch = Join-Path $RunRoot 'common-harness.patch'
git diff --binary "--output=$HarnessPatch" $B0 $C0 -- ospf-kotlin-benchmark
if ($LASTEXITCODE -ne 0) { throw '夹具补丁导出失败 / Harness export failed' }
git -C $Baseline apply --check $HarnessPatch
if ($LASTEXITCODE -ne 0) { throw '夹具补丁不适用 / Harness patch not applicable' }
git -C $Baseline apply $HarnessPatch
if ($LASTEXITCODE -ne 0) { throw '夹具补丁应用失败 / Harness patch failed' }
git -C $Baseline diff --name-only
```

检查最后的差异只能位于 `ospf-kotlin-benchmark/`。后续新增 Token 并发或 Prime 混合读写夹具时，两端应用同一个仅含测量支撑的补丁并记录 hash。若跨 A0 比较时新夹具依赖基线没有的 API，先设计调用双方共有生产入口的适配；不能把候选算法复制到基线来“修复”编译。

## 3. 环境、构建与类路径隔离

### 3.1 环境准入

- 使用独占主机，或具有独占物理核/内存配额、无明显 CPU steal 的执行环境；只有独立目录或独立 JVM 不等于资源隔离。
- JDK 25、相同供应商及 patch 版本、Maven 版本、JMH 1.37；记录 OS、CPU、物理/逻辑核、NUMA/SMT、内存、GC、堆、CPU 配额和电源策略。
- 确认 `java -version` 与 `mvn -version` 指向同一 JDK。JVM 参数及 `JAVA_TOOL_OPTIONS`、`JDK_JAVA_OPTIONS`、`MAVEN_OPTS` 等环境设置需记录并保持一致。
- 构建全部完成后再计时。同一主机只允许一组计时任务，不能并行运行 baseline/current、其它基准、Maven 或重型剖析。不要擅自终止其它用户的进程。
- 固定输入规模、分布和随机种子，记录运行顺序；监控热降频、GC、后台负载。环境不合格时保留阻塞原因，不用“增加线程数”替代隔离。

### 3.2 构建与类路径

两个版本具有相同 Maven 坐标和版本号，必须使用各自的临时 Maven 本地仓库，防止同版本依赖覆盖。不要沿用原机器已安装的同名 jar。可以复用外部依赖下载缓存，但本项目 artifact 必须各自从源码构建。

以下函数在 PowerShell 7 中执行。首次独立构建使用 `install` 安装上游模块；benchmark 本身禁止 install，运行时显式使用其 `target/classes`。编译产物留在被忽略的 `target/`，报告、完整命令输出与 classpath 文件均在临时目录。

```powershell
function Build-Benchmark {
    param([string]$Checkout, [string]$Label)
    $Out = Join-Path $RunRoot $Label
    New-Item -ItemType Directory -Path $Out -Force | Out-Null
    $Repo = Join-Path $Out 'm2'
    $CpFile = Join-Path $Out 'runtime-classpath.txt'
    Push-Location $Checkout
    try {
        & mvn -B -ntp "-Dmaven.repo.local=$Repo" `
            "-Dospf.test.reportsDirectory=$Out/test-reports" `
            -pl ospf-kotlin-benchmark -am -Pbench -DskipTests install -T 0.75C `
            *> (Join-Path $Out 'build.log')
        if ($LASTEXITCODE -ne 0) { throw "构建失败，分析完整日志 / Build failed: $Out/build.log" }
        & mvn -B -ntp "-Dmaven.repo.local=$Repo" -pl ospf-kotlin-benchmark -Pbench `
            dependency:build-classpath -DincludeScope=runtime "-Dmdep.outputFile=$CpFile" `
            *> (Join-Path $Out 'classpath.log')
        if ($LASTEXITCODE -ne 0) { throw "类路径生成失败 / Classpath failed: $Out/classpath.log" }
        $Classes = Join-Path $Checkout 'ospf-kotlin-benchmark/target/classes'
        return $Classes + [IO.Path]::PathSeparator + (Get-Content -LiteralPath $CpFile -Raw).Trim()
    } finally {
        Pop-Location
    }
}
$BaselineCp = Build-Benchmark -Checkout $Baseline -Label 'B0'
$CandidateCp = Build-Benchmark -Checkout $Candidate -Label 'C0'
```

正式测量前检查：两份 classpath 的本项目依赖只能指向各自 repo；源码/夹具/生产 jar/关键 class 的 SHA-256、Git SHA、补丁差异和完整命令均保存到外部运行清单。使用 `java -cp $BaselineCp org.openjdk.jmh.Main -l` 和候选同样命令确认入口存在；必要时单独进行类加载诊断，不能在正式计时中开启详细类加载日志。

禁止使用不存在的 `target/benchmarks.jar`，也不能使用 Maven `exec:java` 的同进程便捷 runner 代替正式 JMH forks。

## 4. 分批任务与完成条件

下列批次依次推进。每批先检查正确性、再小规模 smoke、再按预注册矩阵正式计时。smoke 只证明可运行，不参与加速结论。前五批比较 B0 → C0；第六批单独比较 A0 → B0，必要时追加 C0 的回归观察，不能混淆收益来源。

### 第一批：Network 热遍历与冷建表权衡（优先）

生产入口：

- `ospf-kotlin-framework-network-scheduling/network-scheduling-infrastructure/src/main/fuookami/ospf/kotlin/framework/network_scheduling/infrastructure/NetworkGraph.kt`
- 同目录 `NetworkTopology.kt`。

当前设计：不可变拓扑共享；反向视图共享原拓扑，过滤图独立；弧身份索引 eager，其余查询表及双向邻接按需同步 lazy。算法循环外获取方向表，不为未消费的数据建立整套索引。

夹具：`NetworkIndexBuildBenchmark`，`size=256,1024,4096`，每节点四条弧。

- 首要：`warmReachable`，确认稳态成本，不能只展示冷路径收益。
- 配套：`constructOnly`、`coldReachable`、`coldComponents`、`reverseOnly`、`reverseAndRead`、`filteredReachable`，计入真实首次消费成本。
- 追加夹具：空图、稀疏/高出度图、只构造未消费、一次消费/反复消费；复用同一不可变图的并发首次消费与稳态读。现有夹具是 `Scope.Thread`，直接增大 JMH `-t` 只会得到各线程独立图，不代表共享图锁竞争。
- 收集：时间、B/op、索引实际构建次数、首次读延迟、反向共享/过滤隔离、对象保留量。构建次数在非计时正确性/诊断轮统计。

完成条件：完整列出冷热场景及规模，尤其热遍历是否跨越回退阈值；明确以何种消费频率抵消冷建表节省，不能宣称“无影响”。

### 第二批：Projection 冷/热 snapshot、pricing、master（优先）

生产入口：`ospf-kotlin-framework/src/main/fuookami/ospf/kotlin/framework/solver/ColumnProjection.kt`、同目录 `ColumnProjectionIndex.kt`。

当前设计：按原边界 eager 捕获外部映射和变量身份，私有项的排序/分组按需 lazy；完整 freshness 校验保留，master 循环外取表，仍按原数值次序累加。

夹具：`ProjectionIndexBuildBenchmark`，`size=256,1024,4096` × `consumer=snapshot,pricing,master` × `coldRevision,warmRevision`。

- 首要验证 `warmRevision` / `master` 的时间与分配，而不是只量私有排序函数。
- 冷场景包含索引首次消费；热场景不能移除完整 freshness 检查。
- 追加测量行数、每行局部项数、块数、候选列数、每次 revision 的消费次数；每轮只扩一个维度，覆盖无项及高稀疏度。
- 成本拆分使用独立 JFR/采样轮区分 freshness、排序分组、模型创建；master 关闭临时模型，不能通过资源泄漏制造收益。

完成条件：给出各消费面冷热比率及分配结论，保留热 master 的不利结果；不能将冷分配减少外推为端到端定价/求解提速。

### 第三批：CSP1D 缓存禁用路径与 worker 扩展性（优先）

生产入口：`ospf-kotlin-framework-csp1d/csp1d-domain-cutting-plan-generation-context/src/main/fuookami/ospf/kotlin/framework/csp1d/domain/cutting_plan_generation/service/` 下的 `DFSGenerator.kt`、`NSumGenerator.kt`、`FullSumGenerator.kt`。

当前改动仅在模板复用禁用时不再创建 recorder/复制最终丢弃的模板，保留原 `shouldStop` 检查时机。宽度缓存仍同步按需，quantity cache 保持 worker 局部；没有引入完整 DFS single-flight。

现有夹具：`GenerationCacheBuildBenchmark.generate`，`scenario=equivalent,distinct,custom`，默认 8 产品/12 物料/5 刀上限；`custom` 用始终为真的自定义约束关闭模板复用。

- 正式矩阵：`workers=1,2,4,8`，JMH 始终 `-t 1`，避免 JMH 调用线程数乘内部 worker 数造成超额并发。
- 首要：custom / 多 worker 的耗时与 B/op；同时保留 equivalent/distinct 及顺序未改路径作为对照。已有时间/分配方向有波动，不挑有利轮次。
- 现有夹具只执行 DFS；给 NSum/FullSum 添加共有生产 API 夹具，不能用 DFS 数据代替另外两种算法。
- 扩展产品/物料数量、等价 key 比例、宽度选择密度、方案上限/超时场景；完整输出负载和预算截断负载分别比较，记录终止原因、方案数及顺序、visited nodes、cache hits/misses 和策略调用次数。
- 在独立诊断轮量宽度缓存锁等待、同 key 重复 DFS、不同 key 冷构建成本。不能把“少复制模板”直接等同于总分配一定下降。

完成条件：区分减少无用工作、真实宏观收益及生产热度证据。若提议更强并发缓存，先定义策略纯度、取消/失败传播、等待计入 deadline、完整模板发布和计数汇总协议；这些设计不在本次验证中偷偷落地。

### 第四批：Token 首次并发读与稳态读

生产入口：`ospf-kotlin-core/src/main/fuookami/ospf/kotlin/core/token/TokenList.kt`。当前使用 volatile 热读和锁内双检首建，失效使用同一锁；仅支持结构固定时的并发读取，不承诺并发模型修改安全。

- 现有 `PreparationHotPathBenchmark.coldTokenIndices`：`dataset=small,medium,large`，1024/2048/4096 token，包含创建、排序及全部不同 key 首查，是单线程整体成本哨兵，不是并发夹具。
- 新增共享固定列表的 `readers=1,2,4,8` 同时首次访问夹具，以及同一已就绪列表的稳态读夹具。使用 barrier 对齐冷启动；两端包含相同调度/同步成本，不能每 reader 各建一份列表。
- 新增夹具必须明确每次操作是否包含 executor、barrier、建表和全部 reader 完成；避免 `Setup(Level.Invocation)` 的隐藏成本/计时外分配污染 GC 解释。优先将完整冷读批次作为操作，另报空调度对照。
- 收集每版本实际建表次数、整批耗时、首读/等待 p95/p99、B/op；固定建表工作量减少不等于已证明延迟收益。
- 失效后的下一代首次读单独验证；不要为测量加入未经支持的并发写。

完成条件：明确单次建表工作量、冷并发等待成本和热读成本；并发正确性测试不能替代性能测量。

### 第五批：Prime 混合读写、扩容和保留内存

生产入口：`ospf-kotlin-math/src/main/fuookami/ospf/kotlin/math/ordinary/Prime.kt`。当前单 writer 复制并发布完整代快照，覆盖范围内读无扩容锁，缺失范围同步扩容；没有后台预热。

- `SharedPrimeReadBenchmark.readyRead` 为 `Scope.Benchmark`，共享一份已就绪表，使用 `-t 1,2,4,8`；查询 997、初始范围 1000，初始建表不计时。
- `PreparationHotPathBenchmark.coldPrimeSequence` 用 `dataset=small,medium,large` 检查 1万/2万/4万冷连续查询整体成本。
- 新增混合 reader/writer 夹具：固定 reader 数和一条明确的 writer 扩容序列，分别覆盖偶发大扩容、连续缺失、频繁显式小步扩容。现有 readyRead 不覆盖这些情形。
- 防止预热已把表扩到最大后测量退化为纯 readyRead：每轮重置代/范围并固定完成协议，说明吞吐按哪个参与者计数；记录 reader 与 writer 各自进度、p95/p99，检查 writer 饥饿。
- 单独测量分配、峰值/保留堆、GC 和旧代存活时间。读取线程暂留旧代及扩容复制旧素数 List 的成本不能省略；heap/RSS 不与 B/op 混为一谈。

完成条件：把已就绪查询、冷查询、扩容和混合读写分开报告，不用纳秒热读结论推导生产频率、尾延迟或全仓求解收益。

### 第六批：较早优化中的未量化/不稳定入口

本批是独立对照 A0 → B0，共同夹具来自 C0 或经审查的共有入口适配；不要将这些旧改动的收益归入 B0 → C0。优先补现有夹具，再补缺失入口：

| 项目 | 现有入口或定位 | 补充工作 |
| --- | --- | --- |
| checked Token 批注册 | `PreparationHotPathBenchmark.batchTokenRegistration` / `singleTokenRegistration` | 三规模；重复、冲突、首错误与检查成本单独场景；不能去掉验证 |
| 货物合并 | `PilePreparationBenchmark.mergePiles` | `count=64,128,256` × `scenario=unmergeable,multiplePiles`；不能称必要逐对可行性检查为线性 |
| 标量定价准备 | `ModelPreparationBenchmark.prepareScalarPricingModels` | 三规模、保留 freshness；不代表 opt-in 批量定价 |
| 缩并/累计及 worker | `MathPreparationBenchmark.contraction,cumsumAxis,boundedWorkerMap` | 三规模、时间和 B/op；纯 worker 调度收益与业务计算吞吐分开 |
| 单因子梯度哨兵 | `CanonicalLinearPreparationBenchmark.strictLinear,exactLinear`，定义在 `MathPreparationBenchmark.kt` | 防止小输入回归，不能只测大共享 DAG |
| F01 显式批量定价 | `ColumnProjection.preparePricingBatch`；`ospf-kotlin-framework-bpp/src/main/fuookami/ospf/kotlin/framework/bpp/domain/partition_loading/PartitionLoadingContext.kt` | 新夹具比较候选中的 scalar/batch 共有语义，明确 opt-in、候选/分区数、失败和异步返回后 freshness；基线没有批量 API，不伪造该 API |
| F15 Path 定价批次 | `ospf-kotlin-framework-network-scheduling/network-scheduling-domain-path-compilation-context/src/main/fuookami/ospf/kotlin/framework/network_scheduling/domain/path_compilation/model/PathCompilationAggregation.kt` 中的 `reducedCost/reducedCosts` | 新夹具比较逐候选/只读批次，保留 adapter 顺序、批次前后审计、stale dual 拒绝及失败优先级；无性能承诺先于证据 |
| F08 Gantt 选列 | `ospf-kotlin-framework-gantt-scheduling/gantt-scheduling-domain-capacity-scheduling-context/src/main/fuookami/ospf/kotlin/framework/gantt_scheduling/domain/capacity_scheduling/model/IterativeCapacityCompilation.kt` | 新夹具覆盖多批、历史列规模、变量换代、bounds/旧 token 重绑及公开可变输入，不能按纯 append 测量 |

完成条件：每项有明确对照及覆盖，或明确记录无法适配/仍未量化的原因；不得把全部源码优化清单包装为每项都有独立提速数据。

## 5. 测量协议与可复制命令

### 5.1 预注册与执行顺序

正式计时前固定矩阵、最小实用效应、采样数、统计方式和资源预算。默认最小实用时间效应为 5%，分配和内存成本单独判断；按业务变更阈值必须先说明理由，不能看完结果再挑阈值。

1. Smoke：每入口/参数用最小轮次确认真实生产 API、完整返回值和正确性，不出性能结论。
2. 筛查：先中等规模、关键场景，定位 JIT/GC/运行时间风险；筛查不作为最终确认样本。
3. 确认：每个版本每参数至少 5 个独立 JVM fork，每 fork 5 × 2s 预热、10 × 2s 测量。若未达到稳态，在正式采样前统一加长两端预热。
4. 使用交替顺序 B/C、C/B、B/C、C/B、B/C；每次调用 `-f 1`，每对两次调用串行。不要一次完成全部 B 后再全部 C，也不要同时运行两端。
5. 时间主轮不加 profiler；相同矩阵另跑 `-prof gc` 分配轮并分别报告。JFR/async-profiler/锁等待/heap 诊断使用额外独立轮，不能拿其时间混入主轮。
6. 如需追加一次确认，最多再加预先约定的 5 对 fork；所有样本保留，并说明是否采用序贯校正或独立确认判定。资源耗尽仍无足够证据时结论为证据不足，不持续追加直到显著。

以下为单参数时间主轮模板；先完成 smoke，再用于对应矩阵。例中 4 GiB 固定堆仅在机器留有充足内存时使用；任何调整必须两端一致、在采样前冻结。

```powershell
$Common = @('-f', '1', '-wi', '5', '-i', '10', '-w', '2s', '-r', '2s',
    '-bm', 'avgt', '-t', '1', '-foe', 'true', '-rf', 'json',
    '-jvmArgs', '-Xms4g -Xmx4g -XX:+UseG1GC')
$Pattern = '^fuookami\.ospf\.kotlin\.benchmark\.NetworkIndexBuildBenchmark\.warmReachable$'
$Params = @('-p', 'size=1024')
for ($Pair = 1; $Pair -le 5; $Pair++) {
    $Order = if ($Pair % 2 -eq 1) { @('B0', 'C0') } else { @('C0', 'B0') }
    foreach ($Label in $Order) {
        $Cp = if ($Label -eq 'B0') { $BaselineCp } else { $CandidateCp }
        $Stem = Join-Path $RunRoot "$Label-network-warm-1024-pair$Pair-time"
        & java -cp $Cp org.openjdk.jmh.Main $Pattern @Params @Common `
            -rff "$Stem.json" *> "$Stem.log"
        if ($LASTEXITCODE -ne 0) { throw "基准失败，保留全部输出 / Benchmark failed: $Stem.log" }
    }
}
```

其它场景替换 pattern/参数并使用不同输出文件名；分配轮追加 `-prof gc` 并使用 `-gc` 后缀，不能覆盖时间主轮。线程/参数规则：

| 入口 | 参数 | JMH 调用线程 |
| --- | --- | --- |
| Network | `-p size=256,1024,4096`，具体方法见第一批 | `-t 1`；共享图另建夹具 |
| Projection | `-p size=256,1024,4096 -p consumer=snapshot,pricing,master` | `-t 1` |
| CSP1D | `-p scenario=equivalent,distinct,custom -p workers=1,2,4,8` | `-t 1`，workers 是内部并行度 |
| Prime readyRead | 无 dataset 参数 | 分别替换 Common 中的 `-t` 值为 `1,2,4,8`，不要传逗号串 |
| PreparationHotPath | `-p dataset=small,medium,large` | `-t 1` |

多参数展开会明显增加总耗时，按批次预算串行运行，不能为了省时间在同机并行计时。每个参数组合单独对应结果单元，不能合并不同规模分数。

### 5.2 统计与结论

- 使用 fork 级独立样本，不把同 JVM 内多次迭代当作独立实验。保存每 fork 原始迭代值和有配对关系的运行顺序。
- 对 avg time 定义 `R = candidate / baseline`；时间减少百分比为 `(1 - R) × 100%`，加速倍数为 `1 / R`。吞吐应另用对应比率，不能混用单位和方向。
- 预先选定例如配对 fork 对数比 bootstrap 的 95% 置信区间，报告点估计、CI、方法、随机种子和样本数；不能仅根据两端 JMH CI 是否重叠作等效/优劣结论。
- 默认实用等效区间 `[0.95, 1.05]`：比率 CI 全低于 0.95 判为有实用收益；全高于 1.05 判为回退；全部落在区间内判为该阈值下等效；其余为证据不足，附可检测效应限制。多项比较需声明探索性质或采用预注册校正，不能挑显著项。
- p95/p99 需要足够独立的尾延迟样本及明确操作边界。AverageTime 分数不是尾延迟；为混合/冷读另建 sample-time 或专用延迟夹具，注意采样扰动。
- 分别报告 B/op、GC、峰值内存、保留内存和超时/失败比例；均值更快但明显增加内存或恶化尾延迟时不能只给“提速”结论。
- 环境异常、失败、OOM、不利结果均保留。预先定义异常排除规则，记录理由并整对重测，不静默删样本。
- 有冷热权衡时报告每 revision 的读取次数、命中率、失效频率及盈亏平衡点；没有实际调用频率证据就不能断言适合后台异步构建。

## 6. 正确性及最终验收

以下是 C0 的重点测试入口。B0 不具备所有新测试，不能要求它通过候选特有的建表次数断言；共有输出语义使用独立差分检查，候选不变量使用候选测试。

```powershell
$Tests = 'TokenColdReadConcurrencyTest,TokenSolverIndexTest,TokenListAtomicityTest,' +
    'PrimeTest,NetworkGraphIndexTest,NetworkTopologyTest,NetworkInfrastructureTest,' +
    'ColumnProjectionIndexTest,ColumnProjectionTest,GenerationCacheObservationTest,' +
    'GenerationWidthCacheConcurrencyTest,GeneratorParallelismTest,GeneratorMediumScaleBaselineTest'
$Reports = Join-Path $RunRoot 'correctness'
Push-Location $Candidate
try {
    & mvn -B -ntp "-Dmaven.repo.local=$RunRoot/C0/m2" `
        -pl ospf-kotlin-benchmark -am -Pbench '-DskipTests=false' `
        "-Dtest=$Tests" '-Dsurefire.failIfNoSpecifiedTests=false' `
        "-Dospf.test.reportsDirectory=$Reports" test -T 0.75C `
        *> (Join-Path $RunRoot 'correctness.log')
    if ($LASTEXITCODE -ne 0) { throw '定向测试失败 / Targeted tests failed' }
} finally {
    Pop-Location
}
```

补充夹具的验证：确认真正调用生产实现，返回值被消费，计时外输入没有遗漏应该计入的工作，冷/热状态不会在预热中串台；对比方案集合/必要顺序、数值 raw bits、错误与取消边界、缓存计数及完整结果发布。采样计数器放诊断轮或 worker 局部汇总，不向热读加入新的共享原子热点。

若接手期间修改代码/夹具，进行中优先增量。完成全部修改后，在候选仓库根串行执行最终全仓验收，日志与报告仍在临时目录：

```powershell
Push-Location $Candidate
try {
    mvn -B -ntp "-Dmaven.repo.local=$RunRoot/C0/m2" `
        "-Dospf.test.reportsDirectory=$RunRoot/final-reports" `
        clean compile test-compile -T 0.75C *> "$RunRoot/final-build.log"
    if ($LASTEXITCODE -ne 0) { throw '全仓编译失败 / Full build failed' }
    mvn -B -ntp "-Dmaven.repo.local=$RunRoot/C0/m2" `
        "-Dospf.test.reportsDirectory=$RunRoot/final-reports" `
        test -T 0.75C *> "$RunRoot/final-test.log"
    if ($LASTEXITCODE -ne 0) { throw '全仓测试失败 / Full tests failed' }
} finally {
    Pop-Location
}
```

从同一次完整日志一次性分析全部错误，不能截取头尾后反复重启构建。记录原生 solver、数据库等环境依赖测试的跳过原因，不把跳过记作通过。构建验收与性能计时分开进行。

## 7. 交付与接手完成清单

接手会话按批次在外部交接记录中追踪状态，每个项目记录：对照 SHA/夹具 hash、环境、矩阵、运行命令、时间与分配结论、语义验证、限制、下一步和证据位置。临时目录会被清理，需要长期保存时上传用户认可的 artifact 存储，不承诺原机器临时路径长期可用。

- [ ] 固定 B0/C0（第六批 A0/B0）和共有夹具，确认两端实际加载的生产 jar/class 版本。
- [ ] 完成第一、二批冷热权衡验证，明确 Network 热遍历和 Projection 热 master 是否回退。
- [ ] 完成第三批 DFS/NSum/FullSum 的缓存关闭/启用与 worker 梯度，不遗漏不利分配数据。
- [ ] 完成第四批 Token 共享列表的冷并发、热读和失效后重建；没有扩大并发写契约。
- [ ] 完成第五批 Prime 就绪、冷扩容、混合读写、尾延迟及旧代内存测量。
- [ ] 完成第六批代表入口和缺失夹具；无法量化项目显式标注证据不足，不冒称全部提速。
- [ ] 为每项给出保留/调整/回退建议；后台预建建议具备热度、miss 成本、生命周期与预算协议证据，或明确继续不实施。
- [ ] 提交必要源码、测试、可复用 benchmark/runbook 修订；不提交运行结果、报告、临时脚本或进度生成物。
- [ ] 如有代码修改，完成定向与全仓验收；确认原仓库无误改，按仓库规则完成 worktree 合并/清理（若使用）。

最终面向用户的结论需回答“哪些入口优化、在何种输入下改善多少、代价是什么、哪些仍证据不足”，并附可复现命令和外部证据位置；不得将微基准推导为整体 solver 加速。
