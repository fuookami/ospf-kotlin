# BPP3D 主流程重构交接（最终收口）

日期：2026-05-31  
当前基线提交：`69b162e7 chore(bpp3d): 修复 strict scanner 漏检并回调 refactor 验收状态`

## 1. 当前结论

本轮已完成 strict generic boundary 收口，BPP3D 主源码路径（`src/main`）已通过严格门禁校验，默认回归与 Gurobi 回归均通过。

核心结论：

1. strict scanner 结果为 `STRICT_GENERIC_BOUNDARY_PASS`。
2. `bpp3d-application -am compile` 通过。
3. `bpp3d-application -am test` 通过。
4. 启用 Gurobi 的 `GurobiColumnGenerationTest` 通过（非 disabled skip）。
5. CSV suite 两种入口均通过（`suite.paths` 与 `suite.dir`，使用仓内数据集）。

## 2. 本轮完成项

### 2.1 门禁命中清零（12 项）

已完成以下命名与别名清理：

1. `PlacementPlaneBridge.kt` -> `PlacementPlaneMapping.kt`
2. `OrientationAxisPermutationBridge.kt` -> `OrientationAxisPermutationMapping.kt`
3. `ProjectivePlaneGeometryBridge.kt` -> `ProjectivePlaneGeometryMapping.kt`
4. `LegacyAliases.kt` -> `LayerAssignmentAliases.kt`
5. `InfraLegacyAliases.kt` -> `InfraAliases.kt`
6. `PackingScalarAliases.kt` -> `PackingNumberAliases.kt`
7. 清理 `PackageScalar`
8. 清理 `PatternScalar`
9. 清理 `ShadowPriceScalar`
10. 清理 `HeightScalar`
11. 清理 `MergeScalar`
12. 清理 `MaterialPackingScalar`

### 2.2 相关主链同步

1. 所有受影响引用已同步到新命名。
2. 代码已恢复可编译和可测试状态。
3. 修复了中途批量替换造成的源码编码异常（`MalformedInputException`）。

## 3. 验证记录（2026-05-31）

### 3.1 Strict Scanner

命令：

```powershell
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/generic-boundary-check.ps1 -ProjectRoot .
```

结果：`STRICT_GENERIC_BOUNDARY_PASS`

### 3.2 Compile

命令：

```powershell
powershell.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am compile '-Dgpg.skip=true'"
```

结果：`BUILD SUCCESS`

### 3.3 默认回归

命令：

```powershell
powershell.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am test '-Dgpg.skip=true'"
```

结果：`BUILD SUCCESS`

### 3.4 Gurobi 回归（启用）

命令：

```powershell
powershell.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' '-Dbpp3d.gurobi.cg.test.enabled=true' test '-Dgpg.skip=true'"
```

结果：`Tests run: 10, Failures: 0, Errors: 0`，`BUILD SUCCESS`

### 3.5 CSV Suite（paths）

命令：

```powershell
powershell.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.cg.test.enabled=true' '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.paths=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi/production-like-dataset.csv' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`

### 3.6 CSV Suite（dir）

命令：

```powershell
powershell.exe -NoLogo -NoProfile -Command "mvn -f pom.xml -pl bpp3d-application -am -Pgurobi-cg-test '-Dbpp3d.gurobi.cg.test.enabled=true' '-Dbpp3d.gurobi.dataset.suite.enabled=true' '-Dbpp3d.gurobi.dataset.suite.dir=E:/workspace/ospf-kotlin/ospf-kotlin-framework-bpp3d/bpp3d-application/src/test/resources/gurobi' -Dtest=GurobiColumnGenerationTest '-Dsurefire.failIfNoSpecifiedTests=false' test '-Dgpg.skip=true'"
```

结果：`Tests run: 10, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`

## 4. 已知说明

1. 当前环境无 `pwsh.exe`，本轮等价使用 `powershell.exe` 执行命令。
2. surefire 输出存在 `Corrupted channel by directly writing to native stream` 警告，但不影响本轮用例通过与构建成功。
3. CSV suite 本轮使用仓内 `production-like-dataset.csv` 与其目录完成验证；未提交任何真实业务原始 CSV。

## 5. 下一步建议

如需继续推进，可选：

1. 在真实业务 CSV（外部数据）上按 `suite.paths` / `suite.dir` 再跑一轮基准验收。
2. 清理非阻断编译告警（如 `Duplicate branch condition in when`）。
3. 合并提交本次收口改动并打上“strict boundary pass + gurobi pass”里程碑标签。