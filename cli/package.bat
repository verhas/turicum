@echo off

cd /d "%~dp0"

REM Read the version from the VERSION file
set /p VERSION=<VERSION

echo Version=%VERSION%

REM Remove existing target\JARS directory if it exists
if exist target\JARS (
    rd /s /q target\JARS
)
mkdir target\JARS

REM Unzip the file using 7zip
7z x target/turicum-cli-%VERSION%-distribution.zip -otarget\JARS

REM Loop over the installer types to create both exe and msi installers
for %%I in (exe msi) do (
    echo Creating installer type %%I
    jpackage --input target\JARS ^
        --vendor "Peter Verhas" ^
        --name turicum ^
        --app-version %VERSION% ^
        --main-jar turicum-cli-%VERSION%.jar ^
        --main-class ch.turic.cli.Main ^
        --type %%I ^
        --dest output ^
        --java-options -Xmx2048m ^
        --win-console ^
        --resource-dir packaging-resources
)
