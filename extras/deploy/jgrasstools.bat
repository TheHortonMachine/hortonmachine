rem JGrass - Free Open Source Java GIS http://www.jgrass.org 
rem (C) HydroloGIS - www.hydrologis.com 
rem 
rem This library is free software; you can redistribute it and/or modify it under
rem the terms of the GNU Library General Public License as published by the Free
rem Software Foundation; either version 2 of the License, or (at your option) any
rem later version.
rem 
rem This library is distributed in the hope that it will be useful, but WITHOUT
rem ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
rem FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
rem details.
rem 
rem You should have received a copy of the GNU Library General Public License
rem along with this library; if not, write to the Free Foundation, Inc., 59
rem Temple Place, Suite 330, Boston, MA 02111-1307 USA

set MEM="-Xmx2g"

java %MEM% -cp ".\modules\*;.\libs\*" org.jgrasstools.hortonmachine.oms.ScriptLauncher %1
