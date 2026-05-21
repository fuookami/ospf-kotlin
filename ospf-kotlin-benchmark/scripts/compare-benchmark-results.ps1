param(
    [string] $Baseline = "",

    [string] $Current = "",

    [string] $ResultsDir = "",

    [string] $Dataset = "",

    [string] $Output = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Get-DatasetTagFromPath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path,

        [Parameter(Mandatory = $true)]
        [string] $Prefix
    )

    $fileName = [System.IO.Path]::GetFileName($Path)
    $pattern = "^{0}-(.+)\.json$" -f [Regex]::Escape($Prefix)
    $match = [Regex]::Match($fileName, $pattern, [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
    if ($match.Success -and $match.Groups.Count -ge 2) {
        return $match.Groups[1].Value
    }
    return ""
}

function Resolve-DefaultOutputPath {
    param(
        [Parameter(Mandatory = $true)]
        [string] $BaselinePath,

        [Parameter(Mandatory = $true)]
        [string] $CurrentPath
    )

    $baselineDataset = Get-DatasetTagFromPath -Path $BaselinePath -Prefix "baseline"
    $currentDataset = Get-DatasetTagFromPath -Path $CurrentPath -Prefix "current"
    if ($baselineDataset.Length -gt 0 -and $baselineDataset -eq $currentDataset) {
        $currentDir = Split-Path -Parent $CurrentPath
        return (Join-Path $currentDir ("trend-{0}.md" -f $currentDataset))
    }
    return ""
}

function Resolve-BenchmarkInputPaths {
    param(
        [string] $BaselinePath,
        [string] $CurrentPath,
        [string] $ResultsDirectory,
        [string] $DatasetTag
    )

    $baselineTrimmed = $BaselinePath.Trim()
    $currentTrimmed = $CurrentPath.Trim()
    $resultsDirTrimmed = $ResultsDirectory.Trim()
    $datasetTrimmed = $DatasetTag.Trim()

    if (($baselineTrimmed.Length -gt 0) -or ($currentTrimmed.Length -gt 0)) {
        if (($baselineTrimmed.Length -eq 0) -or ($currentTrimmed.Length -eq 0)) {
            throw "Both -Baseline and -Current must be provided together."
        }
        if (!(Test-Path -LiteralPath $baselineTrimmed)) {
            throw "Benchmark result file not found: $baselineTrimmed"
        }
        if (!(Test-Path -LiteralPath $currentTrimmed)) {
            throw "Benchmark result file not found: $currentTrimmed"
        }
        return [pscustomobject]@{
            BaselinePath = $baselineTrimmed
            CurrentPath = $currentTrimmed
            Dataset = ""
            Mode = "explicit-files"
        }
    }

    if ($resultsDirTrimmed.Length -eq 0) {
        throw "Either provide -Baseline/-Current, or provide -ResultsDir (and optional -Dataset)."
    }
    if (!(Test-Path -LiteralPath $resultsDirTrimmed)) {
        throw "Benchmark result directory not found: $resultsDirTrimmed"
    }

    $baselineFiles = Get-ChildItem -LiteralPath $resultsDirTrimmed -File -Filter "baseline-*.json"
    $currentFiles = Get-ChildItem -LiteralPath $resultsDirTrimmed -File -Filter "current-*.json"

    $baselineMap = @{}
    foreach ($file in $baselineFiles) {
        $tag = Get-DatasetTagFromPath -Path $file.Name -Prefix "baseline"
        if ($tag.Length -gt 0 -and -not $baselineMap.ContainsKey($tag)) {
            $baselineMap[$tag] = $file.FullName
        }
    }

    $currentMap = @{}
    foreach ($file in $currentFiles) {
        $tag = Get-DatasetTagFromPath -Path $file.Name -Prefix "current"
        if ($tag.Length -gt 0 -and -not $currentMap.ContainsKey($tag)) {
            $currentMap[$tag] = $file.FullName
        }
    }

    $matchedDatasets = @($baselineMap.Keys | Where-Object { $currentMap.ContainsKey($_) } | Sort-Object)
    if ($matchedDatasets.Count -eq 0) {
        throw "No matched baseline/current dataset pair found in: $resultsDirTrimmed"
    }

    if ($datasetTrimmed.Length -eq 0) {
        if ($matchedDatasets.Count -gt 1) {
            throw ("Multiple matched datasets found: {0}. Please specify -Dataset." -f ($matchedDatasets -join ", "))
        }
        $datasetTrimmed = $matchedDatasets[0]
    } elseif (-not ($baselineMap.ContainsKey($datasetTrimmed) -and $currentMap.ContainsKey($datasetTrimmed))) {
        throw ("Dataset '{0}' not found as matched baseline/current pair in {1}. Available: {2}" -f $datasetTrimmed, $resultsDirTrimmed, ($matchedDatasets -join ", "))
    }

    return [pscustomobject]@{
        BaselinePath = $baselineMap[$datasetTrimmed]
        CurrentPath = $currentMap[$datasetTrimmed]
        Dataset = $datasetTrimmed
        Mode = "results-dir"
    }
}

function Format-Score {
    param(
        [double] $Score
    )

    if ([double]::IsNaN($Score) -or [double]::IsInfinity($Score)) {
        return "n/a"
    }
    return ("{0}" -f [Math]::Round($Score, 6))
}

function Format-Error {
    param(
        [double] $Error
    )

    if ([double]::IsNaN($Error) -or [double]::IsInfinity($Error)) {
        return "n/a"
    }
    return ("{0}" -f [Math]::Round($Error, 6))
}

function Format-ScoreWithError {
    param(
        [double] $Score,
        [double] $Error
    )

    return ("{0} ± {1}" -f (Format-Score -Score $Score), (Format-Error -Error $Error))
}

function Get-SampleCount {
    param(
        [Parameter(Mandatory = $true)]
        $Metric,

        [int] $Fallback = 0
    )

    if ($null -eq $Metric -or $null -eq $Metric.rawData) {
        return $Fallback
    }

    $count = 0
    foreach ($forkData in $Metric.rawData) {
        if ($null -ne $forkData) {
            $count += @($forkData).Count
        }
    }
    return $count
}

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
            Samples = [int] (Get-SampleCount -Metric $entry.primaryMetric -Fallback ([int] $entry.measurementIterations))
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

$resolvedInput = Resolve-BenchmarkInputPaths -BaselinePath $Baseline -CurrentPath $Current -ResultsDirectory $ResultsDir -DatasetTag $Dataset
$Baseline = $resolvedInput.BaselinePath
$Current = $resolvedInput.CurrentPath

$baselineItems = Read-BenchmarkResults -Path $Baseline
$currentItems = Read-BenchmarkResults -Path $Current
$keys = @($baselineItems.Keys + $currentItems.Keys | Sort-Object -Unique)
$baselineDataset = Get-DatasetTagFromPath -Path $Baseline -Prefix "baseline"
$currentDataset = Get-DatasetTagFromPath -Path $Current -Prefix "current"
$detectedDataset = if ($resolvedInput.Dataset.Trim().Length -gt 0) {
    $resolvedInput.Dataset.Trim()
} elseif ($baselineDataset.Length -gt 0 -and $baselineDataset -eq $currentDataset) {
    $baselineDataset
} else {
    "n/a"
}

$lines = New-Object System.Collections.Generic.List[string]
$lines.Add("# Benchmark Trend Comparison")
$lines.Add("")
$lines.Add("- Baseline: ``$Baseline``")
$lines.Add("- Current: ``$Current``")
$lines.Add("- Input mode: ``$($resolvedInput.Mode)``")
$lines.Add("- Naming convention: ``baseline-<dataset>.json`` / ``current-<dataset>.json`` / ``trend-<dataset>.md``")
$lines.Add("- Detected dataset: ``$detectedDataset``")
$lines.Add("- Gate policy: report only (no hard performance gate)")
$lines.Add("")
$lines.Add("| Benchmark | Baseline (score ± err) | Current (score ± err) | Delta | Samples (B/C) | Unit | Notes |")
$lines.Add("| --- | ---: | ---: | ---: | ---: | --- | --- |")

foreach ($key in $keys) {
    $base = $baselineItems[$key]
    $curr = $currentItems[$key]
    if ($null -eq $base) {
        $lines.Add("| $key | n/a | $(Format-ScoreWithError -Score $curr.Score -Error $curr.Error) | new | n/a/$($curr.Samples) | $($curr.Unit) | new benchmark in current |")
    } elseif ($null -eq $curr) {
        $lines.Add("| $key | $(Format-ScoreWithError -Score $base.Score -Error $base.Error) | n/a | missing | $($base.Samples)/n/a | $($base.Unit) | missing in current |")
    } else {
        $delta = Format-PercentDelta -BaselineScore $base.Score -CurrentScore $curr.Score
        $unit = if ($base.Unit -eq $curr.Unit) { $base.Unit } else { "$($base.Unit) -> $($curr.Unit)" }
        $notes = @()
        if ($base.Unit -ne $curr.Unit) {
            $notes += "unit changed"
        }
        if ([double]::IsNaN($base.Error) -or [double]::IsNaN($curr.Error)) {
            $notes += "error unavailable"
        }
        if ($notes.Count -eq 0) {
            $notes += "ok"
        }
        $lines.Add("| $key | $(Format-ScoreWithError -Score $base.Score -Error $base.Error) | $(Format-ScoreWithError -Score $curr.Score -Error $curr.Error) | $delta | $($base.Samples)/$($curr.Samples) | $unit | $($notes -join '; ') |")
    }
}

$report = $lines -join [Environment]::NewLine
if ($Output.Trim().Length -eq 0) {
    $Output = Resolve-DefaultOutputPath -BaselinePath $Baseline -CurrentPath $Current
}

if ($Output.Trim().Length -gt 0) {
    $outputDir = Split-Path -Parent $Output
    if ($outputDir.Trim().Length -gt 0) {
        New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    }
    Set-Content -LiteralPath $Output -Value $report -Encoding UTF8
    Write-Output ("Trend report written to: {0}" -f $Output)
}

Write-Output $report
