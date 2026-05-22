Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [Parameter(Mandatory = $true)]
        [bool] $Condition,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Assert-Contains {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Text,

        [Parameter(Mandatory = $true)]
        [string] $Expected,

        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    if (-not $Text.Contains($Expected)) {
        throw "$Message`nExpected: $Expected"
    }
}

function New-BenchmarkEntry {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Name,

        [Parameter(Mandatory = $true)]
        [double] $Score,

        [Parameter(Mandatory = $true)]
        [double] $Error,

        [Parameter(Mandatory = $true)]
        [string] $Unit
    )

    return @{
        benchmark = $Name
        mode = "thrpt"
        threads = 1
        forks = 1
        jvmArgs = @()
        measurementIterations = 2
        params = @{
            scale = "smoke"
        }
        primaryMetric = @{
            score = $Score
            scoreError = $Error
            scoreUnit = $Unit
            rawData = @(
                @($Score),
                @($Score + 1.0)
            )
        }
    }
}

function Write-BenchmarkFile {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [double] $Score,

        [string] $Name = "demo.Benchmark.hotPath"
    )

    $payload = @(
        New-BenchmarkEntry -Name $Name -Score $Score -Error 0.123456 -Unit "ops/s"
    )
    $json = $payload | ConvertTo-Json -Depth 10
    Set-Content -LiteralPath $Path -Value $json -Encoding UTF8
}

$compareScript = Join-Path $PSScriptRoot "compare-benchmark-results.ps1"
Assert-True -Condition (Test-Path -LiteralPath $compareScript) -Message "Missing script: $compareScript"

$tempRoot = Join-Path ([System.IO.Path]::GetTempPath()) ("ospf-kotlin-benchmark-compare-smoke-{0}" -f [Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Force -Path $tempRoot | Out-Null

try {
    $explicitDir = Join-Path $tempRoot "explicit"
    $singleDir = Join-Path $tempRoot "single"
    $multiDir = Join-Path $tempRoot "multi"
    $errorDir = Join-Path $tempRoot "error"
    $mismatchDir = Join-Path $tempRoot "mismatch"
    New-Item -ItemType Directory -Force -Path $explicitDir, $singleDir, $multiDir, $errorDir, $mismatchDir | Out-Null

    $baselineSmoke = Join-Path $explicitDir "baseline-smoke.json"
    $currentSmoke = Join-Path $explicitDir "current-smoke.json"
    Write-BenchmarkFile -Path $baselineSmoke -Score 100.0
    Write-BenchmarkFile -Path $currentSmoke -Score 110.0

    $explicitOutput = Join-Path $explicitDir "trend-explicit.md"
    & $compareScript -Baseline $baselineSmoke -Current $currentSmoke -Output $explicitOutput | Out-Null
    Assert-True -Condition (Test-Path -LiteralPath $explicitOutput) -Message "Explicit-file mode did not generate output."

    $explicitReport = Get-Content -LiteralPath $explicitOutput -Raw
    Assert-Contains -Text $explicitReport -Expected "Input mode: ``explicit-files``" -Message "Explicit-file report missing input mode."
    Assert-Contains -Text $explicitReport -Expected "Detected dataset: ``smoke``" -Message "Explicit-file report missing detected dataset."
    Assert-Contains -Text $explicitReport -Expected "Gate policy: report only (no hard performance gate)" -Message "Explicit-file report missing gate policy."

    $singleBaseline = Join-Path $singleDir "baseline-smoke.json"
    $singleCurrent = Join-Path $singleDir "current-smoke.json"
    Copy-Item -LiteralPath $baselineSmoke -Destination $singleBaseline -Force
    Copy-Item -LiteralPath $currentSmoke -Destination $singleCurrent -Force

    & $compareScript -ResultsDir $singleDir | Out-Null
    $singleOutput = Join-Path $singleDir "trend-smoke.md"
    Assert-True -Condition (Test-Path -LiteralPath $singleOutput) -Message "Single-match directory mode did not generate default output."

    $singleReport = Get-Content -LiteralPath $singleOutput -Raw
    Assert-Contains -Text $singleReport -Expected "Input mode: ``results-dir``" -Message "Directory report missing input mode."
    Assert-Contains -Text $singleReport -Expected "Detected dataset: ``smoke``" -Message "Directory report missing detected dataset."
    Assert-Contains -Text $singleReport -Expected "Gate policy: report only (no hard performance gate)" -Message "Directory report missing gate policy."

    $multiBaselineSmoke = Join-Path $multiDir "baseline-smoke.json"
    $multiCurrentSmoke = Join-Path $multiDir "current-smoke.json"
    $multiBaselineAlpha = Join-Path $multiDir "baseline-alpha.json"
    $multiCurrentAlpha = Join-Path $multiDir "current-alpha.json"
    Copy-Item -LiteralPath $baselineSmoke -Destination $multiBaselineSmoke -Force
    Copy-Item -LiteralPath $currentSmoke -Destination $multiCurrentSmoke -Force
    Write-BenchmarkFile -Path $multiBaselineAlpha -Score 50.0
    Write-BenchmarkFile -Path $multiCurrentAlpha -Score 55.0

    $multiFailureMessage = ""
    try {
        & $compareScript -ResultsDir $multiDir | Out-Null
    } catch {
        $multiFailureMessage = $_.Exception.Message
    }

    Assert-True -Condition ($multiFailureMessage.Length -gt 0) -Message "Multi-match directory mode should fail when -Dataset is omitted."
    Assert-Contains -Text $multiFailureMessage -Expected "Multiple matched datasets found" -Message "Multi-match failure should explain dataset ambiguity."
    Assert-Contains -Text $multiFailureMessage -Expected "Please specify -Dataset" -Message "Multi-match failure should tell user to provide -Dataset."

    $multiOutput = Join-Path $multiDir "trend-smoke.md"
    & $compareScript -ResultsDir $multiDir -Dataset smoke -Output $multiOutput | Out-Null
    Assert-True -Condition (Test-Path -LiteralPath $multiOutput) -Message "Directory mode with explicit dataset did not generate output."
    $multiReport = Get-Content -LiteralPath $multiOutput -Raw
    Assert-Contains -Text $multiReport -Expected "Input mode: ``results-dir``" -Message "Multi-dataset explicit report missing input mode."
    Assert-Contains -Text $multiReport -Expected "Detected dataset: ``smoke``" -Message "Multi-dataset explicit report missing detected dataset."
    Assert-Contains -Text $multiReport -Expected "Gate policy: report only (no hard performance gate)" -Message "Multi-dataset explicit report missing gate policy."

    $missingBaseline = Join-Path $errorDir "baseline-missing.json"
    $missingFileMessage = ""
    try {
        & $compareScript -Baseline $missingBaseline -Current $currentSmoke | Out-Null
    } catch {
        $missingFileMessage = $_.Exception.Message
    }
    Assert-True -Condition ($missingFileMessage.Length -gt 0) -Message "Missing baseline file should fail."
    Assert-Contains -Text $missingFileMessage -Expected "Benchmark result file not found" -Message "Missing file failure message is not readable."

    $invalidBaseline = Join-Path $errorDir "baseline-invalid.json"
    Set-Content -LiteralPath $invalidBaseline -Value "{ invalid-json" -Encoding UTF8
    $invalidJsonMessage = ""
    try {
        & $compareScript -Baseline $invalidBaseline -Current $currentSmoke | Out-Null
    } catch {
        $invalidJsonMessage = $_.Exception.Message
    }
    Assert-True -Condition ($invalidJsonMessage.Length -gt 0) -Message "Invalid JSON input should fail."
    Assert-Contains -Text $invalidJsonMessage -Expected "JSON" -Message "Invalid JSON failure should mention JSON parsing."

    $missingMetricBaseline = Join-Path $errorDir "baseline-missing-metric.json"
    $missingMetricPayload = @(
        @{
            benchmark = "demo.Benchmark.hotPath"
            mode = "thrpt"
            threads = 1
            forks = 1
            jvmArgs = @()
            measurementIterations = 2
            params = @{
                scale = "smoke"
            }
        }
    )
    Set-Content -LiteralPath $missingMetricBaseline -Value ($missingMetricPayload | ConvertTo-Json -Depth 10) -Encoding UTF8
    $missingMetricMessage = ""
    try {
        & $compareScript -Baseline $missingMetricBaseline -Current $currentSmoke | Out-Null
    } catch {
        $missingMetricMessage = $_.Exception.Message
    }
    Assert-True -Condition ($missingMetricMessage.Length -gt 0) -Message "Missing primaryMetric should fail."
    Assert-Contains -Text $missingMetricMessage -Expected "primaryMetric" -Message "Missing metric failure should point to primaryMetric."

    $mismatchBaseline = Join-Path $mismatchDir "baseline-smoke.json"
    $mismatchCurrent = Join-Path $mismatchDir "current-smoke.json"
    Write-BenchmarkFile -Path $mismatchBaseline -Name "demo.Benchmark.onlyBaseline" -Score 90.0
    Write-BenchmarkFile -Path $mismatchCurrent -Name "demo.Benchmark.onlyCurrent" -Score 120.0
    $mismatchOutput = Join-Path $mismatchDir "trend-smoke.md"
    & $compareScript -Baseline $mismatchBaseline -Current $mismatchCurrent -Output $mismatchOutput | Out-Null
    $mismatchReport = Get-Content -LiteralPath $mismatchOutput -Raw
    Assert-Contains -Text $mismatchReport -Expected "missing in current" -Message "Mismatch report should include missing benchmark notes."
    Assert-Contains -Text $mismatchReport -Expected "new benchmark in current" -Message "Mismatch report should include new benchmark notes."

    Write-Output "compare-benchmark-results smoke passed."
} finally {
    if (Test-Path -LiteralPath $tempRoot) {
        Remove-Item -LiteralPath $tempRoot -Recurse -Force
    }
}
