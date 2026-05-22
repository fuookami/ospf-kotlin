param(
    [Parameter(Mandatory = $true)]
    [string] $LogPath,

    [string] $Output = "",

    [ValidateSet("markdown", "text")]
    [string] $Format = "markdown"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

function Assert-LogExists {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Path
    )

    if (!(Test-Path -LiteralPath $Path)) {
        throw "Build log file not found: $Path"
    }
}

function Escape-MarkdownCell {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Text
    )

    return ($Text -replace "\|", "\|" -replace "`r", " " -replace "`n", " ")
}

function Get-WarningFileHint {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $patterns = @(
        "file:///+(?<path>[A-Za-z]:/[^:\r\n]+\.(kt|kts|java|xml|properties|ps1|md|yml|yaml))",
        "file:///+(?<path>/[^:\r\n]+\.(kt|kts|java|xml|properties|ps1|md|yml|yaml))",
        "(?<path>[A-Za-z]:\\[^:\r\n]+\.(kt|kts|java|xml|properties|ps1|md|yml|yaml))",
        "(?<path>/[^:\r\n]+\.(kt|kts|java|xml|properties|ps1|md|yml|yaml))",
        "(?<path>[^:\s]+\.(kt|kts|java|xml|properties|ps1|md|yml|yaml)):[0-9]+(:[0-9]+)?"
    )

    foreach ($pattern in $patterns) {
        $match = [Regex]::Match($Message, $pattern)
        if ($match.Success) {
            return $match.Groups["path"].Value
        }
    }
    return ""
}

function Get-NormalizedWarningKey {
    param(
        [Parameter(Mandatory = $true)]
        [string] $Message
    )

    $trimmed = $Message.Trim()
    if ($trimmed.Length -eq 0) {
        return "n/a"
    }
    return ($trimmed -replace ":[0-9]+(:[0-9]+)?", "" -replace "\s+", " ")
}

Assert-LogExists -Path $LogPath

$lines = Get-Content -LiteralPath $LogPath
$currentModule = "unknown"
$warnings = New-Object System.Collections.Generic.List[object]

foreach ($line in $lines) {
    if ($line -match "^\[INFO\]\s+Building\s+(.+?)\s+[0-9].*$") {
        $currentModule = $Matches[1].Trim()
        continue
    }

    if ($line -match "^\[INFO\]\s+--- .+ @ ([^ ]+) ---$") {
        $currentModule = $Matches[1].Trim()
        continue
    }

    if ($line -notmatch "^\[WARNING\]\s*(.+)$") {
        continue
    }

    $message = $Matches[1].Trim()
    if ($message.Length -eq 0) {
        continue
    }

    $fileHint = Get-WarningFileHint -Message $message
    $key = if ($fileHint.Length -gt 0) {
        $fileHint
    } else {
        Get-NormalizedWarningKey -Message $message
    }

    $warnings.Add([pscustomobject]@{
        Module = $currentModule
        Key = $key
        Message = $message
    })
}

$grouped = $warnings |
    Group-Object Module, Key |
    ForEach-Object {
        $first = $_.Group[0]
        [pscustomobject]@{
            Module = $first.Module
            Key = $first.Key
            Count = $_.Count
            Sample = $first.Message
        }
    } |
    Sort-Object -Property @{Expression = "Count"; Descending = $true}, @{Expression = "Module"; Descending = $false}, @{Expression = "Key"; Descending = $false}

$reportLines = New-Object System.Collections.Generic.List[string]
$reportLines.Add("# Maven Warning Summary")
$reportLines.Add("")
$reportLines.Add("- Source log: ``$LogPath``")
$reportLines.Add("- Total warnings: $($warnings.Count)")
$reportLines.Add("- Unique groups: $(@($grouped).Count)")
$reportLines.Add("- Aggregation key: module + file-hint (fallback: normalized warning message)")
$reportLines.Add("")

if ($Format -eq "markdown") {
    $reportLines.Add("| Module | File/Key | Count | Sample |")
    $reportLines.Add("| --- | --- | ---: | --- |")
    foreach ($item in $grouped) {
        $module = Escape-MarkdownCell -Text $item.Module
        $key = Escape-MarkdownCell -Text $item.Key
        $sample = Escape-MarkdownCell -Text $item.Sample
        $reportLines.Add("| $module | $key | $($item.Count) | $sample |")
    }
} else {
    foreach ($item in $grouped) {
        $reportLines.Add(("{0} | {1} | count={2} | sample={3}" -f $item.Module, $item.Key, $item.Count, $item.Sample))
    }
}

$report = $reportLines -join [Environment]::NewLine
if ($Output.Trim().Length -gt 0) {
    $outputDir = Split-Path -Parent $Output
    if ($outputDir.Trim().Length -gt 0) {
        New-Item -ItemType Directory -Force -Path $outputDir | Out-Null
    }
    Set-Content -LiteralPath $Output -Value $report -Encoding UTF8
    Write-Output ("Warning summary written to: {0}" -f $Output)
}

Write-Output $report
