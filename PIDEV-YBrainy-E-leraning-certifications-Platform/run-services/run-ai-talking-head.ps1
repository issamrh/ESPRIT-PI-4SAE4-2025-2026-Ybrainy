param(
    [int]$Port = 8765,
    [string]$SpeakerWav = "",
    [switch]$Cpu,
    [switch]$SkipWait,
    [switch]$DryRun
)

$repoRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$fairFaceRoot = (Resolve-Path -LiteralPath (Join-Path $repoRoot "..\..\fair face")).Path

if (-not $SpeakerWav) {
    $SpeakerWav = (Resolve-Path -LiteralPath (Join-Path $repoRoot "courses\Course\tp-foyer\src\main\resources\ai\harvard.wav")).Path
}

$escapedSpeaker = $SpeakerWav -replace "'", "''"
$cpuArg = ""
if ($Cpu) {
    $cpuArg = " -Cpu"
}

$command = "& '.\run_talking_head_tts.ps1' -Port $Port -SpeakerWav '$escapedSpeaker' -AgreeToCoquiCpml$cpuArg"

& "$PSScriptRoot\_run-service.ps1" `
    -Name "AI Talking Head TTS" `
    -ProjectPath $fairFaceRoot `
    -Kind Command `
    -Port $Port `
    -WaitUrl "http://localhost:$Port/" `
    -Command $command `
    -SkipWait:$SkipWait `
    -DryRun:$DryRun
