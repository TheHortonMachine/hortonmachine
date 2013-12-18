/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gears.utils.colors;

import java.util.HashMap;

public class DefaultTables {
    private HashMap<String, String> map;

    public DefaultTables() {

        map = new HashMap<String, String>();
        String rainbow = "255 255 0\n" + //
                "0 255 0\n" + //
                "0 255 255\n" + //
                "0 0 255\n" + //
                "255 0 255\n" + //
                "255 0 0\n";

        map.put(ColorTables.rainbow.name(), rainbow);

        String extRainbow = "255 255 0\n" + //
                "128 255 0\n" + //
                "0 255 0\n" + //
                "0 255 128\n" + //
                "0 255 255\n" + //
                "0 128 255\n" + //
                "0 0 255\n" + //
                "128 0 255\n" + //
                "255 0 255\n" + //
                "255 0 128\n" + //
                "255 0 0\n";

        map.put(ColorTables.extrainbow.name(), extRainbow);

        String aspect = "255 255 255\n" + //
                "0 0 0\n" + //
                "255 255 255";
        map.put(ColorTables.aspect.name(), aspect);

        String bath = "-30000    0   0   0    -20000    0   0   0  \n" + //
                "-20000    0   0   0    -10000    0   0   59  \n" + //
                "-10000    0   0   59   -9000     0   0   130\n" + //
                "-9000     0   0   130  -8000     0   0   202\n" + //
                "-8000     0   0   202  -7000     0   18  255\n" + //
                "-7000     0   18  255  -6000     0   90  255\n" + //
                "-6000     0   90  255  -5000     0   157 255\n" + //
                "-5000     0   157 255  -4000     0   227 255\n" + //
                "-4000     0   227 255  -3000     43  255 255\n" + //
                "-3000     43  255 255  -2000     115 255 255\n" + //
                "-2000     115 255 255  -1000     184 255 255\n" + //
                "-1000     184 255 255  0         250 255 255\n" + //
                "0         0   128 0    500       133 5   0  \n" + //
                "500       133 5   0    1000      255 128 0  \n" + //
                "1000      255 128 0    2000      255 255 0  \n" + //
                "2000      255 255 0    3000      255 255 127\n" + //
                "3000      255 255 127  4000      255 255 244\n" + //
                "4000      255 255 255  10000     255 255 255";
        map.put(ColorTables.bathymetric.name(), bath);

        String elev = "0 191 191 \n" + //
                "0 255 0 \n" + //
                "255 255 0 \n" + //
                "255 127 0\n" + //
                "191 127 63 \n" + //
                "20 21 20\n";
        map.put(ColorTables.elev.name(), elev);

        String flow = "1 255 255 0\n" + //
                "2 0 255 0\n" + //
                "3 0 255 255\n" + //
                "4 255 0 255\n" + //
                "5 0 0 255\n" + //
                "6 160 32 240\n" + //
                "7 255 165 0\n" + //
                "8 30 144 255\n" + //
                "10 255 0 0\n";
        map.put(ColorTables.flow.name(), flow);

        String loga = "-1.0 255 255 255 0 255 255 255\n" + //
                "0.0 255 255 0 1 0 255 0\n" + //
                "1 0 255 0 10 0 255 255\n" + //
                "10 0 255 255 100 0 0 255\n" + //
                "100 0 0 255 1000 255 0 255\n" + //
                "1000 255 0 255 10000 255 0 0\n" + //
                "10000 255 0 0 100000 110 0 0\n" + //
                "100000 110 0 0 1000000 0 0 0\n";
        map.put(ColorTables.logarithmic.name(), loga);

        String sea = "-30000.0 255 255 255 -8000.0 255 255 255\n" + //
                "-8000.0 0 0 255 -2500.0 30 144 255\n" + //
                "-2500.0 30 144 255 -2000 162 208 252\n" + //
                "-2000 162 208 252 -1500 250 117 117\n" + //
                "-1500 250 117 117 0.0 255 0 0\n";
        map.put(ColorTables.sea.name(), sea);

        String radiation = "198 198 224\n" + //
                "0 0 115\n" + //
                "0 100 210\n" + //
                "90 183 219\n" + //
                "0 255 255\n" + //
                "40 254 100\n" + //
                "80 131 35\n" + //
                "160 190 0\n" + //
                "255 255 100\n" + //
                "255 180 0\n" + //
                "255 0 0\n";

        map.put(ColorTables.radiation.name(), radiation);

        String net = "2 255 0 0 2 255 0 0";
        map.put(ColorTables.net.name(), net);

        String greyscale = "0 0 0 \n" + //
                "255 255 255\n";
        map.put(ColorTables.greyscale.name(), greyscale);

        String greyscaleInverse = "255 255 255\n" + //
                "0 0 0\n";
        map.put(ColorTables.greyscaleinverse.name(), greyscaleInverse);

        String shalstab = "1.0 255 0 0 1.0 255 0 0\n" + //
                "2.0 0 255 0 2.0 0 255 0\n" + //
                "3.0 255 255 0 3.0 255 255 0\n" + //
                "4.0 0 0 255 4.0 0 0 255\n" + //
                "8888.0 77 77 77 8888.0 77 77 77\n";
        map.put(ColorTables.shalstab.name(), shalstab);

        String slope = " -5.0 255 0 0   -2.0 255 0 128  \n" + //
                " -2.0 255 0 128 -1.0 255 0 255  \n" + //
                " -1.0 255 0 255 -0.7 128 0 255  \n" + //
                " -0.7 128 0 255 -0.5 0 0 255    \n" + //
                " -0.5 0 0 255   -0.3 0 128 255  \n" + //
                " -0.3 0 128 255 -0.1 0 255 255  \n" + //
                " -0.1 0 255 255 -0.07 0 255 128 \n" + //
                "-0.07 0 255 128 -0.03 0 255 0   \n" + //
                "-0.03 0 255 0 -0.01 128 255 0  \n" + //
                "-0.01 128 255 0 0 255 255 0    \n" + //
                // invert
                "0 255 255 0 0.01 128 255 0\n" + //
                "0.01 128 255 0 0.03 0 255 0\n" + //
                "0.03 0 255 0 0.07 0 255 128\n" + //
                "0.07 0 255 128 0.1 0 255 255\n" + //
                "0.1 0 255 255 0.3 0 128 255\n" + //
                "0.3 0 128 255 0.5 0 0 255\n" + //
                "0.5 0 0 255 0.7 128 0 255\n" + //
                "0.7 128 0 255 1.0 255 0 255\n" + //
                "1.0 255 0 255 2.0 255 0 128\n" + //
                "2.0 255 0 128 5.0 255 0 0";

        map.put(ColorTables.slope.name(), slope);

    }

    public String getTableString( String name ) {
        return map.get(name);
    }
}
