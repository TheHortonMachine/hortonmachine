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
 # Build the windows exe launchers of the HortonMachine apps from linux.
 #
 # Each <name>.exe runs the hm-<name>.bat placed next to it, without console window.
 # Needs mingw-w64: sudo apt install gcc-mingw-w64
 #
 # Usage: ./build-exe.sh [name...]     (default: stacbrowser)
 # Example: ./build-exe.sh stacbrowser lasviewer
 #
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
DEPLOY="$DIR/../deploy"
CC=x86_64-w64-mingw32-gcc
WINDRES=x86_64-w64-mingw32-windres

if ! command -v $CC > /dev/null || ! command -v $WINDRES > /dev/null; then
  echo "mingw-w64 not found, install it with: sudo apt install gcc-mingw-w64"
  exit 1
fi

NAMES="$@"
if [ -z "$NAMES" ]; then
  NAMES="stacbrowser"
fi

BUILD=$(mktemp -d)
trap 'rm -rf "$BUILD"' EXIT

for NAME in $NAMES; do
  if [ ! -f "$DEPLOY/hm-$NAME.bat" ]; then
    echo "WARNING: $DEPLOY/hm-$NAME.bat does not exist, the exe will not find its script."
  fi

  # the name in the version info of the exe
  echo "#define APP_NAME \"$NAME\"" > "$BUILD/app.h"
  $WINDRES -I "$BUILD" -I "$DIR" -O coff -o "$BUILD/launcher.res" "$DIR/launcher.rc"
  $CC -O2 -s -municode -mwindows -static -Wall -o "$DEPLOY/$NAME.exe" "$DIR/launcher.c" "$BUILD/launcher.res"

  echo "Built $DEPLOY/$NAME.exe -> runs hm-$NAME.bat"
done
