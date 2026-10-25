param(
    # 源仓库路径。 / Path of the source repository.
    [Parameter(Mandatory = $true)]
    [string] $Source,

    # 基线提交（不含本次候选实现）。 / Baseline commit without the candidate implementation.
    [Parameter(Mandatory = $true)]
    [string] $BaselineRef,

    # 候选提交（本次实现及配套夹具）。 / Candidate commit holding the implementation and its fixtures.
    [Parameter(Mandatory = $true)]
    [string] $CandidateRef,

    # 运行根目录，必须不存在；一切日志与产物只写此处，禁止写入仓库。 / Run root that must not exist; all logs and artifacts go here, never into the repository.
    [Parameter(Mandatory = $true)]
    [string] $RunRoot,

    # 两端一致的 JDK 目录（须匹配仓库 jvmTarget）。 / JDK directory shared by both sides (must match the repository jvmTarget).
    [Parameter(Mandatory = $true)]
    [string] $Jdk,

    # 用于解析交接提交与导出夹具补丁的相对路径。 / Relative path whose first commit defines the candidate, used for harness export.
    [string] $HarnessScope = "ospf-kotlin-benchmark",

    # 可选：复用已有下载缓存的种子本地仓库。 / Optional: user Maven repository whose external downloads may be reused.
    [string] $SeedRepository = "",

    # 项目 groupId 目录名，用于清除同版本残留。 / Group directory used to purge same-version project artifacts from a seeded repository.
    [string] $ProjectGroupPath = "io\github\fuookami",

    # 基准模块路径。 / Benchmark module path.
    [string] $BenchmarkModule = "ospf-kotlin-benchmark",

    # 两端标签，与 analyze-paired-benchmark.ps1 的 -BaselineLabel/-CandidateLabel 对应。
    # Labels for both sides, matching -BaselineLabel/-CandidateLabel of analyze-paired-benchmark.ps1.
    [string] $BaselineLabel = "B0",

    [string] $CandidateLabel = "C0"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# 冻结两个对照版本、建立两个独立 clone、把同一份仅含测量支撑的夹具补丁移植到基线，
# 并各自用独立 Maven 本地仓库从源码构建，导出运行时类路径。
# 全程只写 RunRoot：仓库不得产生任何构建产物或结果文件。
# Freezes the two compared revisions, creates two isolated clones, ports one measurement-only fixture patch onto the
# baseline, then builds each side against its own Maven repository and exports runtime classpaths.
# Everything is written under RunRoot only; the repository must stay free of build outputs and results.

function Assert-Step {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Step,

        [Parameter(Mandatory = $true)]
        [int] $ExitCode
    )

    if ($ExitCode -ne 0) {
        throw "$Step failed with exit code $ExitCode"
    }
}

$Source = (Resolve-Path -LiteralPath $Source).Path
if (Test-Path -LiteralPath $RunRoot) {
    throw "RunRoot already exists; refusing to mix runs: $RunRoot"
}
New-Item -ItemType Directory -Path $RunRoot | Out-Null

Push-Location $Source
try {
    $resolvedCandidate = (git rev-parse --verify "$CandidateRef^{commit}").Trim()
    Assert-Step "Resolve candidate" -ExitCode $LASTEXITCODE
    $resolvedBaseline = (git rev-parse --verify "$BaselineRef^{commit}").Trim()
    Assert-Step "Resolve baseline" -ExitCode $LASTEXITCODE

    $baseline = Join-Path $RunRoot "baseline"
    $candidate = Join-Path $RunRoot "candidate"

    git clone --no-hardlinks -- $Source $baseline | Out-Null
    Assert-Step "Clone baseline" -ExitCode $LASTEXITCODE
    git clone --no-hardlinks -- $Source $candidate | Out-Null
    Assert-Step "Clone candidate" -ExitCode $LASTEXITCODE

    git -C $baseline switch --detach $resolvedBaseline | Out-Null
    Assert-Step "Detach baseline" -ExitCode $LASTEXITCODE
    git -C $candidate switch --detach $resolvedCandidate | Out-Null
    Assert-Step "Detach candidate" -ExitCode $LASTEXITCODE

    $harnessPatch = Join-Path $RunRoot "common-harness.patch"
    git diff --binary "--output=$harnessPatch" $resolvedBaseline $resolvedCandidate -- $HarnessScope | Out-Null
    Assert-Step "Export harness patch" -ExitCode $LASTEXITCODE
    git -C $baseline apply --check $harnessPatch | Out-Null
    Assert-Step "Harness patch is not applicable to the baseline (it may contain production code)" -ExitCode $LASTEXITCODE
    git -C $baseline apply $harnessPatch | Out-Null
    Assert-Step "Apply harness patch" -ExitCode $LASTEXITCODE

    $stray = @(git -C $baseline diff --name-only | Where-Object { $_ -notlike "$HarnessScope/*" })
    if ($stray.Count -gt 0) {
        throw "Harness port touched non-fixture paths: $($stray -join ', ')"
    }
}
finally {
    Pop-Location
}

# 固定计时环境：同一 JDK，清空会影响 JVM 启动参数的环境变量。
# Pin the measurement environment: one JDK and no inherited JVM option variables.
$env:JAVA_HOME = $Jdk
$env:PATH = (Join-Path $Jdk "bin") + [IO.Path]::PathSeparator + $env:PATH
foreach ($name in "JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "MAVEN_OPTS") {
    if (Test-Path -LiteralPath "env:$name") { Remove-Item -LiteralPath "env:$name" }
}

function Seed-MavenRepository {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Repository,

        [Parameter(Mandatory = $true)]
        [string] $Label
    )

    New-Item -ItemType Directory -Path $Repository -Force | Out-Null
    if ($SeedRepository.Trim().Length -gt 0 -and (Test-Path -LiteralPath $SeedRepository)) {
        Copy-Item -Path (Join-Path $SeedRepository "*") -Destination $Repository -Recurse -Force
    }
    # 外部依赖下载缓存可以复用，但本项目同坐标同版本 artifact 必须各自从源码重建。
    # External download caches may be reused, but same-coordinate project artifacts must be rebuilt from source.
    $purgeRoot = Join-Path $Repository $ProjectGroupPath
    if (Test-Path -LiteralPath $purgeRoot) {
        $stale = @(Get-ChildItem -LiteralPath $purgeRoot -Recurse -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^[0-9]+\.[0-9]+' })
        foreach ($directory in $stale) {
            Remove-Item -LiteralPath $directory.FullName -Recurse -Force
        }
        Write-Host "Seeded $Label repository and purged $($stale.Count) prebuilt project version directories."
    }
}

function Build-BenchmarkSide {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Checkout,

        [Parameter(Mandatory = $true)]
        [string] $Label
    )

    $out = Join-Path $RunRoot $Label
    New-Item -ItemType Directory -Path $out -Force | Out-Null
    $repository = Join-Path $out "m2"
    $classpathFile = Join-Path $out "runtime-classpath.txt"
    if (-not (Test-Path -LiteralPath (Join-Path $repository "io"))) {
        Seed-MavenRepository -Repository $repository -Label $Label
    }

    Push-Location $Checkout
    try {
        & mvn -B -ntp "-Dmaven.repo.local=$repository" "-Dgpg.skip=true" `
            -pl $BenchmarkModule -am -Pbench -DskipTests install -T 0.75C `
            *> (Join-Path $out "build.log")
        Assert-Step "Build $Label (see $(Join-Path $out 'build.log'))" -ExitCode $LASTEXITCODE

        & mvn -B -ntp "-Dmaven.repo.local=$repository" "-Dgpg.skip=true" `
            -pl $BenchmarkModule -Pbench dependency:build-classpath -DincludeScope=runtime `
            "-Dmdep.outputFile=$classpathFile" *> (Join-Path $out "classpath.log")
        Assert-Step "Classpath export $Label" -ExitCode $LASTEXITCODE

        $classes = Join-Path $Checkout "$BenchmarkModule/target/classes"
        $full = $classes + [IO.Path]::PathSeparator + (Get-Content -LiteralPath $classpathFile -Raw).Trim()
        Set-Content -LiteralPath (Join-Path $out "runtime-classpath.full.txt") -Value $full -Encoding ascii -NoNewline
        Write-Host "Built ${Label} -> $((Get-ChildItem -LiteralPath $classes -Recurse -Filter *.class).Count) classes."
    }
    finally {
        Pop-Location
    }
}

Build-BenchmarkSide -Checkout $candidate -Label $CandidateLabel
Build-BenchmarkSide -Checkout $baseline -Label $BaselineLabel

# 类路径隔离自检：两端不得互相引用，也不得使用全局仓库中的本项目 jar。
# Classpath isolation check: neither side may reference the other, nor project jars from the global repository.
foreach ($pair in @(@($BaselineLabel, "baseline"), @($CandidateLabel, "candidate"))) {
    $label = $pair[0]
    $entries = (Get-Content -LiteralPath (Join-Path $RunRoot "$label\runtime-classpath.full.txt") -Raw).Trim() -split [IO.Path]::PathSeparator
    $other = if ($label -eq $BaselineLabel) { $CandidateLabel } else { $BaselineLabel }
    $leak = @($entries | Where-Object { $_ -like "$(Join-Path $RunRoot $other)*" })
    if ($leak.Count -gt 0) {
        throw "$label classpath leaks the $other tree: $($leak -join ', ')"
    }
    if ($SeedRepository.Trim().Length -gt 0) {
        $globalProject = @($entries | Where-Object { $_ -like "$SeedRepository*" -and $_ -match "fuookami|ospf" })
        if ($globalProject.Count -gt 0) {
            throw "$label classpath uses project jars from the seeded repository: $($globalProject -join ', ')"
        }
    }
    $own = Join-Path $RunRoot $label
    $checkout = Join-Path $RunRoot $pair[1]
    $foreign = @($entries | Where-Object { $_ -match "ospf|fuookami" } | Where-Object { ($_ -notlike "$own*") -and ($_ -notlike "$checkout*") })
    if ($foreign.Count -gt 0) {
        throw "$label project dependencies outside its own repository: $($foreign -join ', ')"
    }
    Write-Host "Isolation OK for $label ($($entries.Count) entries)."
}

# 确认两端 JMH 入口集合一致：夹具必须只调用双方共有的生产入口。
# Confirm identical JMH entry sets: fixtures may call only production entries shared by both revisions.
$lists = @{}
foreach ($pair in @(@($BaselineLabel, "baseline"), @($CandidateLabel, "candidate"))) {
    $label = $pair[0]
    $classpath = (Get-Content -LiteralPath (Join-Path $RunRoot "$label\runtime-classpath.full.txt") -Raw).Trim()
    $entries = & java -cp $classpath org.openjdk.jmh.Main -l 2>&1
    Assert-Step "List JMH entries for $label" -ExitCode $LASTEXITCODE
    $lists[$label] = @($entries | Where-Object { $_ -match "^fuookami\." } | Sort-Object)
    # 空清单意味着该侧产物缺少 JMH BenchmarkList（多为不带 -Pbench 的构建覆盖了 target/classes）；
    # 若放过去，后续计时会把每个入口静默跳过，产出“看似完成、实则零数据”的结果。
    # An empty list means that side lost META-INF/BenchmarkList (usually a non -Pbench build overwrote
    # target/classes). Passing it silently makes every later timing run skip every entry and report
    # "finished" with zero data, so fail here instead.
    if ($lists[$label].Count -eq 0) {
        throw "No JMH entries listed for $label; rebuild that side with -Pbench before timing.`n$($entries -join "`n")"
    }
    Set-Content -LiteralPath (Join-Path $RunRoot "$label-jmh-entries.txt") -Value ($lists[$label] -join "`n") -Encoding utf8
}
$difference = @(Compare-Object $lists[$BaselineLabel] $lists[$CandidateLabel])
if ($difference.Count -gt 0) {
    Write-Host "JMH entry sets differ; shared-semantics comparison across versions is not valid for these:"
    $difference | ForEach-Object { Write-Host "  $($_.SideIndicator) $($_.InputObject)" }
}
else {
    Write-Host "JMH entry sets are identical ($($lists[$BaselineLabel].Count) entries)."
}

Write-Host "Prepared run root: $RunRoot"
