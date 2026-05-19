$ErrorActionPreference = "Stop"

$studioJava = "C:\Program Files\Android\Android Studio\jbr"
if (-not (Test-Path (Join-Path $studioJava "bin\java.exe"))) {
    throw "Android Studio Java runtime was not found at $studioJava"
}

$env:JAVA_HOME = $studioJava
if ($args.Count -gt 0) {
    & "$PSScriptRoot\gradlew.bat" @args
} else {
    & "$PSScriptRoot\gradlew.bat" assembleDebug
}
