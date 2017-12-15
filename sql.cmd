@echo off
setlocal EnableExtensions EnableDelayedExpansion

java -jar %~dp0sql.jar -c %~dp0sql.conf %*

pause
