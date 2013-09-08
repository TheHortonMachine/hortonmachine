:: This file is part of JGrasstools (http://www.jgrasstools.org)
:: (C) HydroloGIS - www.hydrologis.com 
:: 
:: JGrasstools is free software: you can redistribute it and/or modify
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

set MEM="-Xmx1g"
set cmdArgs=%1 %2 %3 %4
if [%2]==[] (
    set cmdArgs=-r %1
)

@echo off
for /f "delims=" %%a in ('dir /b modules') do (
   call set MODULESJARS=%%MODULESJARS%%;%CD%/modules/%%a
)
set MODULESJARS=%MODULESJARS:~1%
echo %MODULESJARS%

java %MEM% -Doms.sim.resources="%MODULESJARS%" -cp ".\modules\*;.\libs\*" org.jgrasstools.hortonmachine.utils.oms.CLI %cmdArgs%

endlocal
