@echo off
:: This file is part of HortonMachine (http://www.hortonmachine.org)
:: (C) Andrea Antonello - https://g-ant.eu
::
:: HortonMachine is free software: you can redistribute it and/or modify
:: it under the terms of the GNU General Public License as published by
:: the Free Software Foundation, either version 3 of the License, or
:: (at your option) any later version.
::
:: This program is distributed in the hope that it will be useful,
:: but WITHOUT ANY WARRANTY; without even the implied warranty of
:: MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
:: GNU General Public License for more details.
::
:: You should have received a copy of the GNU General Public License
:: along with this program.  If not, see <http://www.gnu.org/licenses/>.
::
:: The HortonMachine web server. Run with --help for the usage.

setlocal

IF EXIST "%~dp0\jre\bin\java.exe" (
	set JAVAEXE="%~dp0\jre\bin\java.exe"
) ELSE (
	set JAVAEXE="java"
)

:: the maximum memory of the server itself, 1g by default: the modules run in their own processes.
:: Set the HM_MEM environment variable to change it, ex. set HM_MEM=2g
IF "%HM_MEM%"=="" (set MEM=-Xmx1g) ELSE (set MEM=-Xmx%HM_MEM%)

set PATH=%~dp0\natives\;%PATH%
%JAVAEXE% %MEM% -Djava.awt.headless=true -Djava.util.logging.config.file="%~dp0\quiet-logging.properties" -Djava.library.path="%~dp0\natives" -cp "%~dp0\libs\*" org.hortonmachine.server.HmServer %*

endlocal & exit /b %ERRORLEVEL%
