@echo off
setlocal EnableExtensions EnableDelayedExpansion

cd /d "%~dp0"

set "APP_ID=com.vocabheroes.kioskbrowser"
set "ACTIVITY=.MainActivity"
set "AVD_NAME=%~1"
if "%AVD_NAME%"=="" set "AVD_NAME=kioskApi35"

set "SDK_ROOT=%ANDROID_SDK_ROOT%"
if "%SDK_ROOT%"=="" set "SDK_ROOT=%ANDROID_HOME%"
if "%SDK_ROOT%"=="" set "SDK_ROOT=%LOCALAPPDATA%\Android\Sdk"

set "ADB=%SDK_ROOT%\platform-tools\adb.exe"
set "EMULATOR=%SDK_ROOT%\emulator\emulator.exe"
set "APK=%~dp0app\build\outputs\apk\debug\app-debug.apk"

if not exist "%~dp0gradlew.bat" (
  echo [FEHLER] gradlew.bat nicht gefunden im Projektordner.
  goto :error
)

if not exist "%ADB%" (
  echo [FEHLER] adb nicht gefunden: %ADB%
  echo Setze ANDROID_SDK_ROOT oder installiere Android SDK Platform-Tools.
  goto :error
)

if not exist "%EMULATOR%" (
  echo [FEHLER] emulator.exe nicht gefunden: %EMULATOR%
  echo Installiere den Android Emulator via sdkmanager.
  goto :error
)

where java >nul 2>&1
if errorlevel 1 (
  if not defined JAVA_HOME (
    for /d %%D in (C:\Progra~1\Microsoft\jdk-*) do (
      set "JAVA_HOME=%%~fD"
    )
  )
  if defined JAVA_HOME set "PATH=!JAVA_HOME!\bin;%PATH%"
  where java >nul 2>&1
  if errorlevel 1 (
    echo [FEHLER] Java nicht gefunden. Bitte JDK 17 installieren oder JAVA_HOME setzen.
    goto :error
  )
)

echo [1/5] Baue Debug-APK ...
call "%~dp0gradlew.bat" assembleDebug
if errorlevel 1 goto :error

if not exist "%APK%" (
  echo [FEHLER] APK nicht gefunden: %APK%
  goto :error
)

echo [2/5] Pruefe laufenden Emulator ...
set "SERIAL="
for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
  if /i "%%B"=="device" (
    set "DEV=%%A"
    if /i "!DEV:~0,9!"=="emulator-" (
      if not defined SERIAL set "SERIAL=%%A"
    )
  )
)

if not defined SERIAL (
  echo [3/5] Starte Emulator AVD "%AVD_NAME%" ...
  start "Android Emulator" "%EMULATOR%" -avd "%AVD_NAME%" -netdelay none -netspeed full -no-snapshot-save
) else (
  echo [3/5] Emulator bereits online: %SERIAL%
)

echo [4/5] Warte auf Emulator-Start ...
if not defined SERIAL (
  for /l %%I in (1,1,180) do (
    for /f "tokens=1,2" %%A in ('"%ADB%" devices') do (
      if /i "%%B"=="device" (
        set "DEV=%%A"
        if /i "!DEV:~0,9!"=="emulator-" (
          if not defined SERIAL set "SERIAL=%%A"
        )
      )
    )
    if defined SERIAL goto :serial_ready
    timeout /t 2 >nul
  )
)

:serial_ready
if not defined SERIAL (
  echo [FEHLER] Kein online Emulator gefunden.
  goto :error
)

"%ADB%" -s %SERIAL% wait-for-device >nul 2>&1
set "BOOT="
for /l %%I in (1,1,180) do (
  for /f %%B in ('"%ADB%" -s %SERIAL% shell getprop sys.boot_completed 2^>nul') do set "BOOT=%%B"
  if "!BOOT!"=="1" goto :booted
  timeout /t 2 >nul
)

echo [FEHLER] Emulator ist nicht rechtzeitig komplett hochgefahren.
goto :error

:booted
"%ADB%" -s %SERIAL% shell input keyevent 82 >nul 2>&1

echo [5/5] Installiere und starte App auf %SERIAL% ...
"%ADB%" -s %SERIAL% install -r "%APK%"
if errorlevel 1 goto :error

"%ADB%" -s %SERIAL% shell am start -n "%APP_ID%/%ACTIVITY%"
if errorlevel 1 goto :error

echo.
echo [OK] App wurde im Emulator gestartet.
echo      Device: %SERIAL%
echo      Package: %APP_ID%
exit /b 0

:error
echo.
echo [ABBRUCH] Testlauf fehlgeschlagen.
exit /b 1
