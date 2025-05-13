@echo off
setlocal enabledelayedexpansion

cd /d "%~dp0"

REM Extract VERSION from turicum_versions.turi
set "VERSION="
for /f "tokens=2 delims== " %%a in ('findstr /r "^let VERSION *= *\".*\";" "..\turicum_versions.turi"') do (
    if not defined VERSION (
        set "VERSION=%%~a"
    )
)

REM Remove surrounding quotes from VERSION
set "VERSION=%VERSION:"=%"

REM Replace -SNAPSHOT with 1.0.0 in TVERSION
set "TVERSION=%VERSION:-SNAPSHOT=1.0.0%"

echo Version=%VERSION%
echo Translated Version=%TVERSION%

REM Remove existing target\JARS directory if it exists
if exist target\JARS (
    rd /s /q target\JARS
)
mkdir target\JARS

REM Unzip the file using 7zip
7z x "./cli/target/turicum-cli-%VERSION%-distribution.zip" -otarget\JARS

REM Loop over the installer types to create both exe and msi installers
for %%I in (exe msi) do (
    echo Creating installer type %%I
    jpackage --input target\JARS ^
        --vendor "Peter Verhas" ^
        --name turicum ^
        --app-version %TVERSION% ^
        --main-jar turicum-cli-%VERSION%.jar ^
        --main-class ch.turic.cli.Main ^
        --type %%I ^
        --dest output ^
        --java-options -Xmx2048m ^
        --win-console ^
        --resource-dir packaging-resources
)

endlocal

