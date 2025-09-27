@echo off
setlocal

:: Try to locate Python interpreter
for %%P in (python3 python py python.exe python3.exe py.exe pythong) do (
    where %%P >nul 2>nul
    if not errorlevel 1 (
        set "PYTHON=%%P"
        goto :found
    )
)

echo No Python interpreter found in PATH.
exit /b 1

:found
%PYTHON% package.py