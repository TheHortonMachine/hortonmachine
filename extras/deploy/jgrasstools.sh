 #
 # This file is part of JGrasstools (http://www.jgrasstools.org)
 # (C) HydroloGIS - www.hydrologis.com 
 # 
 # JGrasstools is free software: you can redistribute it and/or modify
 # it under the terms of the GNU General Public License as published by
 # the Free Software Foundation, either version 3 of the License, or
 # (at your option) any later version.
 #
 # This program is distributed in the hope that it will be useful,
 # but WITHOUT ANY WARRANTY; without even the implied warranty of
 # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 # GNU General Public License for more details.
 #
 # You should have received a copy of the GNU General Public License
 # along with this program.  If not, see <http://www.gnu.org/licenses/>.
 #

MEM="-Xmx2g"

CURRENT=`pwd`

java $MEM \
-Doms.sim.resources="$CURRENT/modules/jgrassgears-0.1-SNAPSHOT.jar:$CURRENT/modules/hortonmachine-0.1-SNAPSHOT.jar" \
-cp "./modules/*:./libs/*" \
org.jgrasstools.hortonmachine.utils.oms.CLI $1 $2 $3 $4
