param(
    [string]$OutputJson = "scripts/scan-stage0-baseline-result.json"
)

$ErrorActionPreference = "Stop"

function Get-KtFiles {
    param([string]$SrcRoot)
    if (-not (Test-Path $SrcRoot)) {
        return @()
    }
    return Get-ChildItem -Path $SrcRoot -Recurse -File -Include *.kt
}

function Count-PatternInFiles {
    param(
        [array]$Files,
        [string]$Pattern
    )
    if ($Files.Count -eq 0) {
        return 0
    }
    return ($Files | Select-String -SimpleMatch $Pattern).Count
}

function Collect-TopFiles {
    param(
        [array]$Files,
        [string]$Pattern,
        [int]$Top = 8
    )
    if ($Files.Count -eq 0) {
        return @()
    }
    return ($Files |
        Select-String -SimpleMatch $Pattern |
        Group-Object Path |
        Sort-Object Count -Descending |
        Select-Object -First $Top |
        ForEach-Object {
            [ordered]@{
                file = $_.Name
                hits = $_.Count
            }
        })
}

$moduleDefs = @(
    [ordered]@{
        name = "ospf-kotlin-math"
        src  = "ospf-kotlin-math/src/main"
    },
    [ordered]@{
        name = "ospf-kotlin-core"
        src  = "ospf-kotlin-core/src/main"
    },
    [ordered]@{
        name = "ospf-kotlin-framework"
        src  = "ospf-kotlin-framework/src/main"
    }
)

$patterns = @(
    "Flt64",
    "adapter.flt64",
    "toFlt64",
    "toDouble",
    "LinearMetaModel<Flt64>",
    "FeasibleSolverOutput<Flt64>"
)

$allowList = @(
    "math.symbol.adapter.flt64",
    "solver plugin or solver adapter boundary",
    "deprecated compatibility overload",
    "Flt64 numeric implementation and tests"
)

$forbidList = @(
    "generic modeling API signatures",
    "framework algorithm API signatures",
    "constraint/objective/main solution output signatures",
    "core main flow business implementation outside adapter/compat boundary"
)

# 阶段0基线扫描 / Stage-0 baseline scan
$result = [ordered]@{
    timestamp = (Get-Date -Format "yyyy-MM-ddTHH:mm:ssK")
    scope = @("ospf-kotlin-math/src/main", "ospf-kotlin-core/src/main", "ospf-kotlin-framework/src/main")
    pattern_counts = [ordered]@{}
    modules = @()
    allowed_flt64_zones = $allowList
    forbidden_flt64_zones = $forbidList
    core_risk_hits = [ordered]@{}
}

foreach ($pattern in $patterns) {
    $result.pattern_counts[$pattern] = 0
}

foreach ($module in $moduleDefs) {
    $files = Get-KtFiles -SrcRoot $module.src
    $moduleResult = [ordered]@{
        name = $module.name
        src = $module.src
        kotlin_file_count = $files.Count
        counts = [ordered]@{}
        adapter_flt64_dirs = @()
        top_files = [ordered]@{}
    }

    foreach ($pattern in $patterns) {
        $count = Count-PatternInFiles -Files $files -Pattern $pattern
        $moduleResult.counts[$pattern] = $count
        $result.pattern_counts[$pattern] += $count
        $moduleResult.top_files[$pattern] = Collect-TopFiles -Files $files -Pattern $pattern
    }

    $moduleResult.adapter_flt64_dirs = @(
        Get-ChildItem -Path $module.src -Recurse -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.FullName -match "adapter[\\/]+flt64" } |
            ForEach-Object { $_.FullName }
    )

    $result.modules += $moduleResult
}

$coreFiles = Get-KtFiles -SrcRoot "ospf-kotlin-core/src/main"
$riskPatterns = [ordered]@{
    "createTokenTable(" = "token table factory"
    "flt64Tokens" = "flt64Tokens reference"
    "Flt64.zero as V" = "unsafe cast: Flt64.zero as V"
    "this as Flt64" = "unsafe cast: this as Flt64"
    "model as LinearMechanismModel<Flt64>" = "unsafe cast: model as LinearMechanismModel<Flt64>"
}

foreach ($riskPattern in $riskPatterns.Keys) {
    $matches = $coreFiles | Select-String -SimpleMatch $riskPattern
    $result.core_risk_hits[$riskPattern] = [ordered]@{
        tag = $riskPatterns[$riskPattern]
        count = $matches.Count
        samples = @(
            $matches |
                Select-Object -First 10 |
                ForEach-Object {
                    [ordered]@{
                        file = $_.Path
                        line = $_.LineNumber
                        text = $_.Line.Trim()
                    }
                }
        )
    }
}

$json = $result | ConvertTo-Json -Depth 8
$json | Out-File -FilePath $OutputJson -Encoding utf8

Write-Host "Stage-0 baseline JSON written: $OutputJson"
foreach ($module in $result.modules) {
    Write-Host ""
    Write-Host "## $($module.name)"
    foreach ($pattern in $patterns) {
        Write-Host ("{0}`t{1}" -f $pattern, $module.counts[$pattern])
    }
}

