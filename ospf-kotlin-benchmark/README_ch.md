# ospf-kotlin-benchmark

`PatternColumnBenchmark.addColumnBatches` 直接调用生产 BPP1D pattern-assignment 的注册、
addColumns 与关闭入口，不执行求解。small/medium/large 分别为256/512/1024个不同列，
`batchSize` 独立取1或32；前后对照应使用相同规模和批大小。测量包含必要的可变输入核验和
缓存失效，不只是项追加；smoke 不构成加速比或总成本线性化的证据。

:us: [English](README.md) | :cn: 简体中文

## 简介

`ospf-kotlin-benchmark` 包含基于 JMH 的基准入口，用于覆盖 OSPF Kotlin 中部分热点路径。它是只生成报告的 benchmark 模块，不作为库 artifact 发布。

## 作用范围

当前 benchmark 覆盖：

| 范围 | Benchmark |
| --- | --- |
| `multiarray` | block 访问和 contains 热点路径 |
| `math` | 符号组合热点路径 |
| `core` | core 模型热点路径 |
| `coreplugin` | core plugin dumping 热点路径 |
| `constraintprogramming` | CP snapshot 编码、Fake 穷举、固定/可选/可变时长排程、exact MIP 降阶和 portable checkpoint 捕获 |
| `preparation` | 生产素数缓存、常量数组、冷 token 位置索引、稀疏整数域查询、带 incumbent 的 checkpoint 捕获/恢复 |

`PreparationHotPathBenchmark` 直接调用生产 API，不复制被测算法。`small`/`medium`/`large`
采用 N/2N/4N：冷缓存连续素性查询 1万/2万/4万次，数组 1024/2048/4096 个元素，checkpoint
包含 128/256/512 个变量。checkpoint 夹具携带完整 incumbent，实际覆盖变量值域校验。
token 夹具包含 1024/2048/4096 个 token，计入列表创建、求解器排序及全部不同 key 的首次位置查询；
稀疏整数域包含 4096/8192/16384 个值，固定查询 128 次，只扩展域基数。
同规模还覆盖逐项/批量 token 注册，以及逆序共享 owner 下先清空再批量回填解的真实入口。
既有 `LikeP19`/`LikeSolver` 夹具只作算法示意，不能替代真实入口性能回归。smoke 只证明可运行，
不能据此宣称加速。

## 运行 Benchmark

编译 benchmark 模块：

```powershell
mvn -B -ntp -pl ospf-kotlin-benchmark -am -Pbench -DskipTests compile
```

运行 smoke benchmark：

```powershell
$result = Join-Path $env:TEMP "ospf-multiarray-smoke.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*MultiArrayHotPathBenchmark.blockGetAndContains.* small 1 1 1 json $result"
```

运行不依赖原生求解器的 CP benchmark 夹具：

```powershell
$result = Join-Path $env:TEMP "ospf-cp-small.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*ConstraintProgrammingBenchmark.* small 1 1 1 json $result"
```

CP 夹具的可移植 smoke 刻意使用 Fake solver，并提供固定、可选和可变时长 interval 的 exact MIP 降阶构建基准；
SCIP 原生求解耗时、节点数和峰值内存仍需由插件集成套件单独记录。

## 结果报告

`NetworkIndexBuildBenchmark` 测量真实图构造、出向可达性、双向强连通分量、反向视图、过滤可达性和稳态遍历。
`size=256,1024,4096` 扩展节点数量，每节点四条弧；仅构造/仅反向场景特意不读索引，冷遍历计入首次建表成本。
`ProjectionIndexBuildBenchmark` 用相同规模扩展决策变量，行数为变量数的一半，每行三个局部项。
`consumer=snapshot,pricing,master` 区分消费面；`coldRevision` 新建投影，`warmRevision` 复用同一投影，
二者均包含生产新鲜度检查。使用标准 `org.openjdk.jmh.Main`，保持 forks/JVM/预热一致，启用 `-prof gc`，
将 JSON 和完整日志写入临时目录。这些夹具不能代替线上频率、求解时间或后台重叠窗口的测量。

`ModelPreparationBenchmark` 覆盖长依赖链的真实 token 注册、稀疏 `ColumnProjection.master`
装配与标量局部定价请求/模型准备。small/medium/large 按 N/2N/4N 扩展链长度或决策数，
行数、块数、列数固定。调用内模型创建与必要 freshness 检查计时，夹具输入创建不计时。
该夹具不求解，标量定价也不代表显式开启的 BPP 批量定价器；前后保持相同输入，保留
GC 分配数据和置信区间。

`MathPreparationBenchmark` 覆盖真实规范梯度、轴归约、缩并、同序遍历、布尔规范化与有界
worker 映射入口，small/medium/large 按 N/2N/4N 改变一个维度。规范化同时测普通浅树与
共享标量 DAG；`CanonicalLinearPreparationBenchmark` 补充固定单因子梯度回归哨兵。
输入创建和梯度编译不计时，因此不能将其结果当作编译或端到端求解耗时。两类夹具均使用
标准 `org.openjdk.jmh.Main`、相同夹具/JVM 参数、GC 分配剖析与临时 JSON/日志路径，
同时报告置信区间、收益与回归。持久化查询规范化关闭去重和排序，不进入结构键缓存路径，
不能将规范化基准收益归于该查询配置。

`PilePreparationBenchmark.mergePiles` 调用真实 BPP3D 货物合并入口，
`count=64,128,256`、`scenario=unmergeable,multiplePiles`。输入构造不计时，
每次调用返回新的生产结果。使用标准 `org.openjdk.jmh.Main` CLI（不是只支持 dataset 的便捷 runner），
保留两种场景、GC 分配量、置信区间与显式临时结果路径。基线与当前版本必须使用相同夹具、
JVM、fork 与预热配置。该夹具衡量准备过程，不衡量求解或装箱质量，也不将必要的逐对可行性检查宣称为线性。

`PackingGeometryPreparationBenchmark.toSchema` 调用公开的生产装箱 renderer，
`count=64,256,1024`、`scenario=sparse,denseFirstConflict`。Trial 输入构造不计时；
稀疏成功路径包含几何守卫和 DTO 创建，密集失败路径在首个冲突 pair 处停止，不创建 DTO。
使用标准 `org.openjdk.jmh.Main`，不要用只支持 dataset 的便捷 runner。前后对照保留
两种分布、GC 分配和置信区间；圆柱密集或数值风险输入仍保留穷举检查。

在仓库本地运行时，显式指定系统临时目录中的结果路径：

```powershell
$result = Join-Path $env:TEMP "ospf-preparation-small.json"
mvn -pl ospf-kotlin-benchmark -Pbench -DskipTests exec:java "-Dexec.args=.*PreparationHotPathBenchmark.* small 2 3 5 json $result"
```

分别以 `medium`、`large` 和独立文件名重复，每个 benchmark 仅扩展一个规模维度，观察增长趋势。
比较时固定 JVM、堆、线程、fork 和预热配置；该夹具计入 API 校验/序列化开销，不包含原生求解器耗时。

runner 的后备路径是 `ospf-kotlin-benchmark/target/benchmark-results/`，仓库本地运行必须显式覆盖为临时路径。
比较脚本会生成 Markdown 趋势报告，但不设置性能硬门禁：

```powershell
pwsh.exe -File .\ospf-kotlin-benchmark\scripts\compare-benchmark-results.ps1 `
  -ResultsDir $env:TEMP `
  -Dataset small
```

benchmark 的正确性和回放元数据统一使用 `SolveReport<V>`；新增夹具应记录终止原因、解存在性、
诊断、provenance 以及模型/配置指纹。旧 solver output 仅作为兼容适配视图保留。其余插件迁移与能力范围
见 [`plans/solver_cp.md`](../plans/solver_cp.md)。

## 说明

`SharedPrimeReadBenchmark.readyRead` 共享一份生产 `PrimeCache`，初始建表不计时。通过标准
`org.openjdk.jmh.Main` 分别使用 `-t 1`、`-t 4`，并对照
`PreparationHotPathBenchmark.coldPrimeSequence` 检查冷扩容成本。
`GenerationCacheBuildBenchmark.generate` 在 `scenario=equivalent,distinct,custom`、
`workers=1,4` 下执行完整生产 DFS；custom 场景关闭模板复用。返回值是包含缓存命中、miss 和
访问节点数的真实报告。这些是受控负载，不代表生产命中率或尾延迟。比较时保持 JVM、fork、预热一致，
启用 `-prof gc`，保留置信区间和临时输出路径；不要与构建同时运行。

JMH 分数对机器环境敏感。CI smoke 应验证 benchmark 可运行并保留 artifact，不应把绝对分数比较作为硬门禁。
