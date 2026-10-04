#!/bin/bash
 #
 # This file is part of HortonMachine (http://www.hortonmachine.org)
 # (C) Andrea Antonello - https://g-ant.eu
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
 # The HortonMachine command line. Run without arguments for the usage.

# the maximum memory, 2g by default: set the HM_MEM environment variable to change it, ex. HM_MEM=8g
MEM="-Xmx${HM_MEM:-2g} -Xss64m"

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

if [ -f "$DIR/jre/bin/java" ]; then
  JAVAEXE=$DIR/jre/bin/java
else
  JAVAEXE=java
fi

exec "$JAVAEXE" $MEM -Djava.awt.headless=true -Djava.util.logging.config.file="$DIR/quiet-logging.properties" -cp "$DIR/libs/*" org.hortonmachine.cli.HmCli "$@"
