@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "APP_ID=com.vocabheroes.kioskbrowser"
set "ACTIVITY=.MainActivity"
set "SDK_ROOT=%ANDROID_SDK_ROOT%"
if "%SDK_ROOT%"=="" set "SDK_ROOT=%ANDROID_HOME%"
if "%SDK_ROOT%"=="" set "SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"

set "ADB=%SDK_ROOT%\platform-tools\adb.exe"
set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"

if not exist "%ADB%" (
  echo [FEHLER] adb nicht gefunden: %ADB%
  goto :error
)

where java >nul 2>&1
if errorlevel 1 (
  if not defined JAVA_HOME (
    for /d %%D in (C:\Progra~1\Microsoft\jdk-*) do set "JAVA_HOME=%%~fD"
  )
  if defined JAVA_HOME set "PATH=!JAVA_HOME!\bin;%PATH%"
)

where java >nul 2>&1
if errorlevel 1 (
  echo [FEHLER] Java nicht gefunden.
  goto :error
)

echo [1/4] Baue Debug-APK ...
call "%~dp0gradlew.bat" assembleDebug
if errorlevel 1 goto :error

for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
  if /i "%%B"=="device" (
    set "DEV=%%A"
    if /i "!DEV:~0,9!"=="emulator-" set "SERIAL=%%A"
  )
)

if not defined SERIAL (
  echo [FEHLER] Kein laufender Emulator gefunden. Starte zuerst den Emulator.
  goto :error
)

echo [2/4] Nutze Emulator: %SERIAL%
echo [3/4] Installiere APK ...
"%ADB%" -s %SERIAL% install -r "%APK%"
if errorlevel 1 goto :error

echo [4/4] Starte App ...
"%ADB%" -s %SERIAL% shell am start -n "%APP_ID%/%ACTIVITY%"
if errorlevel 1 goto :error

echo.
echo [OK] App gestartet auf %SERIAL%
exit /b 0

:error
echo.
echo [ABBRUCH] Testlauf fehlgeschlagen.
exit /b 1
