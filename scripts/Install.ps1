param([Parameter(Mandatory=$true)][string]$Serial,[Parameter(Mandatory=$true)][string]$Apk,[string]$Adb='adb')
$ErrorActionPreference='Stop'
if (!(Test-Path -LiteralPath $Apk)) { throw 'Download the signed APK from GitHub Releases first.' }
& $Adb -s $Serial install -r $Apk
if ($LASTEXITCODE -ne 0) { throw 'Install failed. Do not uninstall an existing app without a data backup.' }
& $Adb -s $Serial shell am start -n 'io.github.surzo18.couchytv/com.conreo.couchytv.MainActivity'
if ($LASTEXITCODE -ne 0) { throw 'Installed, but launch failed. Open Couchy TV Open from the TV app list.' }
