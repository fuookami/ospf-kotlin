# 阶段0执行基线（2026-05-10）

## 范围
- `ospf-kotlin-math/src/main`
- `ospf-kotlin-core/src/main`
- `ospf-kotlin-framework/src/main`

## 阶段0任务对齐

### 1) 两类数值域边界
- 建模值类型：`V`（用于表达式、约束、目标、结果、框架算法内部）。
- 求解器原生类型：`Raw`（当前主要为 `Flt64`，仅应在 adapter/compat 边界暴露）。

### 2) 命中扫描脚本与结果
- 新增脚本：`scripts/scan-stage0-baseline.ps1`
- 输出结果：`scripts/scan-stage0-baseline-result.json`
- 复用脚本：`scripts/scan-full-genericization.ps1`
- 复用结果：`scripts/scan-full-genericization-result.json`

#### 关键模式统计（阶段0基线）
- `Flt64`: 5300
- `adapter.flt64`: 23
- `toFlt64`: 295
- `toDouble`: 200
- `LinearMetaModel<Flt64>`: 41
- `FeasibleSolverOutput<Flt64>`: 90

#### 分模块统计
- `ospf-kotlin-math`: `Flt64=1645`, `adapter.flt64=15`, `toFlt64=222`, `toDouble=83`, `LinearMetaModel<Flt64>=0`, `FeasibleSolverOutput<Flt64>=0`
- `ospf-kotlin-core`: `Flt64=3541`, `adapter.flt64=7`, `toFlt64=73`, `toDouble=116`, `LinearMetaModel<Flt64>=17`, `FeasibleSolverOutput<Flt64>=64`
- `ospf-kotlin-framework`: `Flt64=114`, `adapter.flt64=1`, `toFlt64=0`, `toDouble=1`, `LinearMetaModel<Flt64>=24`, `FeasibleSolverOutput<Flt64>=26`

#### core 风险命中（用于阶段2/3/4优先清理）
- `createTokenTable(`: 1
- `flt64Tokens`: 0
- `Flt64.zero as V`: 1
- `this as Flt64`: 1
- `model as LinearMechanismModel<Flt64>`: 2

### 3) `Flt64` 允许/禁止清单

#### 允许清单（Allowed）
- `math.symbol.adapter.flt64`
- solver plugin / solver adapter 边界
- deprecated compatibility overload
- `Flt64` 数值实现及其测试

#### 禁止清单（Forbidden）
- 泛型建模主 API 签名（`LinearMetaModel<V>`/`QuadraticMetaModel<V>` 等）
- 框架算法主 API 签名
- 约束/目标/解输出主类型签名
- `core` 主流程业务实现中的非边界 `Flt64` 泄漏

### 4) 当前 guard 基线

#### Full Genericization 扫描
命令：
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/scan-full-genericization.ps1
```
结果：`GATE: PASS`

#### C8/P7 Guard 扫描
命令：
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7
```
结果：失败（当前基线未收敛），主要失败项：
- `P4-4-1`: 已通过（`intermediate_symbol` 下 `AbstractTokenTable<*>` 非豁免命中为 0）
- `P4-4-2`: 已通过（`intermediate_symbol` 下 `as AbstractTokenTable<Flt64>` 非豁免命中为 0，`Product.kt` 6 处仍属豁免）
- `P5-3-1`: 已通过（`core/model` 下 `AbstractTokenTable<Flt64>|AbstractMutableTokenTable<Flt64>` 命中为 0）
- `P7-0-1~0-6`: 仍未通过（当前：`P7-0-1=1348`、`P7-0-2=318`、`P7-0-3=86`、`P7-0-4=321`、`P7-0-5=219`、`P7-0-6=1`）

### 5) 编译/测试基线
命令：
```powershell
mvn -pl ospf-kotlin-math,ospf-kotlin-core,ospf-kotlin-framework -am test -DskipITs
```
结果：
- `ospf-kotlin-math`: 通过
- `ospf-kotlin-core`: 通过
- `ospf-kotlin-framework`: 通过

### 6) 回归命令（后续阶段统一使用）
```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/scan-stage0-baseline.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/scan-full-genericization.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File ospf-kotlin-core/scripts/check-c8-guards.ps1 -GuardMode P7
mvn -pl ospf-kotlin-math,ospf-kotlin-core,ospf-kotlin-framework -am test -DskipITs
```

## 环境记录
- 当前环境无 `pwsh.exe`，使用 `powershell` 执行。
- 本机执行策略默认禁脚本，需 `-ExecutionPolicy Bypass`。
- 已修复 `check-c8-guards.ps1` 对 `ConvertFrom-Json -AsHashtable` 的版本兼容问题，使其可在当前 PowerShell 运行。
