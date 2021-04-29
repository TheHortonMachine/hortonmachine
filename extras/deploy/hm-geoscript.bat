:: This file is part of HortonMachine (http://www.hortonmachine.org)
:: (C) HydroloGIS - www.hydrologis.com 
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
 

setlocal

IF EXIST "%~dp0\jre\bin\java.exe" (
	set JAVAEXE="%~dp0\jre\bin\java.exe"
) ELSE (
	set JAVAEXE="java"
)

IF [%1]==[] (
    set SPLASH=-splash:imgs/splash_geoscript.png
)

:startit

set MEM="-Xmx2g"
set PATH=%~dp0\natives\;%PATH%
"%JAVAEXE%" %SPLASH% %MEM% -Djava.util.logging.config.file=.\quiet-logging.properties -Djava.library.path=%~dp0\natives\ -cp "%~dp0\libs\*" org.hortonmachine.geoscript.GeoscriptConsole %1

endlocal
