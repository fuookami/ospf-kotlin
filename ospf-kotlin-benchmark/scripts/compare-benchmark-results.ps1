param(
    [Parameter(Mandatory = $true)]
    [string] $Baseline,

    [Parameter(Mandatory = $true)]
    [string] $Current,

    [string] $Output = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Read-BenchmarkResults {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    if (!(Test-Path -LiteralPath $Path)) {
        throw "Benchmark result file not found: $Path"
    }

    $json = Get-Content -LiteralPath $Path -Raw | ConvertFrom-Json
    $items = @{}
    foreach ($entry in $json) {
        $params = @()
        if ($null -ne $entry.params) {
            $entry.params.PSObject.Properties |
                Sort-Object Name |
                ForEach-Object { $params += "$($_.Name)=$($_.Value)" }
        }
        $key = if ($params.Count -eq 0) {
            $entry.benchmark
        } else {
            "$($entry.benchmark) [$($params -join ', ')]"
        }
        $items[$key] = [pscustomobject]@{
            Benchmark = $key
            Score = [double] $entry.primaryMetric.score
            Error = [double] $entry.primaryMetric.scoreError
            Unit = [string] $entry.primaryMetric.scoreUnit
        }
    }
    return $items
}

function Format-PercentDelta {
    param(
        [double] $BaselineScore,
        [double] $CurrentScore
    )

    if ([Math]::Abs($BaselineScore) -lt 1e-12) {
        return "n/a"
    }
    $delta = (($CurrentScore - $BaselineScore) / $BaselineScore) * 100.0
    return ("{0:+0.00;-0.00;0.00}%" -f $delta)
}

$baselineItems = Read-BenchmarkResults -Path $Baseline
$currentItems = Read-BenchmarkResults -Path $Current
$keys = @($baselineItems.Keys + $currentItems.Keys | Sort-Object -Unique)

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Benchmark Trend Comparison")
$lines.Add("")
$lines.Add("Baseline: ``$Baseline``")
$lines.Add("Current: ``$Current``")
$lines.Add("")
$lines.Add("| Benchmark | Baseline | Current | Delta | Unit |")
$lines.Add("| --- | ---: | ---: | ---: | --- |")

foreach ($key in $keys) {
    $base = $baselineItems[$key]
    $curr = $currentItems[$key]
    if ($null -eq $base) {
        $lines.Add("| $key | n/a | $([Math]::Round($curr.Score, 6)) | new | $($curr.Unit) |")
    } elseif ($null -eq $curr) {
        $lines.Add("| $key | $([Math]::Round($base.Score, 6)) | n/a | missing | $($base.Unit) |")
    } else {
        $delta = Format-PercentDelta -BaselineScore $base.Score -CurrentScore $curr.Score
        $unit = if ($base.Unit -eq $curr.Unit) { $base.Unit } else { "$($base.Unit) -> $($curr.Unit)" }
        $lines.Add("| $key | $([Math]::Round($base.Score, 6)) | $([Math]::Round($curr.Score, 6)) | $delta | $unit |")
    }
}

$report = $lines -join [Environment]::NewLine
if ($Output.Trim().Length -gt 0) {
    $outputDir = Split-Path -Parent $Output
    if ($outputDir.Trim().Length -gt 0) {
        New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    }
    Set-Content -LiteralPath $Output -Value $report -Encoding UTF8
}

Write-Output $report
