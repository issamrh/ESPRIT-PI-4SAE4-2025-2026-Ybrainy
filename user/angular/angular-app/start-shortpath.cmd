@echo off
setlocal EnableExtensions

set "APP_DIR=%~dp0"
for %%I in ("%APP_DIR%.") do set "APP_DIR=%%~fI"
for %%I in ("%APP_DIR%\..") do set "ANGULAR_PARENT=%%~fI"

set "SHORT_DRIVE=Y:"
set "SHORT_WORKSPACE=%SHORT_DRIVE%\angular-app"
set "PORT=%~1"

if "%PORT%"=="" set "PORT=4200"

subst %SHORT_DRIVE% "%ANGULAR_PARENT%" >nul 2>&1

if not exist "%SHORT_WORKSPACE%\angular.json" (
  echo Failed to map %SHORT_DRIVE% to "%ANGULAR_PARENT%".
  echo If %SHORT_DRIVE% is already used, free it first with: subst %SHORT_DRIVE% /d
  exit /b 1
)

if not exist "%SHORT_WORKSPACE%\node_modules\@angular\cli\bin\ng.js" (
  echo Angular dependencies are missing.
  echo Run this first from the app folder: npm.cmd install
  exit /b 1
)

echo Running Angular from %SHORT_WORKSPACE% on port %PORT%.
pushd "%SHORT_WORKSPACE%"
node .\node_modules\@angular\cli\bin\ng.js serve --host localhost --port %PORT%
set "EXIT_CODE=%ERRORLEVEL%"
popd

exit /b %EXIT_CODE%
