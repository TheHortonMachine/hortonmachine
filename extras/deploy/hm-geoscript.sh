#!/bin/bash
 #
 # This file is part of HortonMachine (http://www.hortonmachine.org)
 # (C) HydroloGIS - www.hydrologis.com 
 # 
 # HortonMachine is free software: you can redistribute it and/or modify
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

MEM="-Xmx4g"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

SPLASH=""
if [ -z "$1" ]
then 
    SPLASH=-splash:$DIR/imgs/splash_geoscript.png
fi

if [ -f "$DIR/jre/bin/java" ]; then
  JAVAEXE=$DIR/jre/bin/java
else
  JAVAEXE=java
fi


"$JAVAEXE" $SPLASH $MEM -Djava.util.logging.config.file=$DIR/quiet-logging.properties -cp "$DIR/libs/*" org.hortonmachine.geoscript.GeoscriptConsole $1
