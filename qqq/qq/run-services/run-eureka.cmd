@echo off
powershell.exe -NoExit -ExecutionPolicy Bypass -File "%~dp0run-eureka.ps1" %*
