/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.utils.colors;

import java.awt.Color;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class DefaultTables {
    private static HashMap<String, String> map = new HashMap<String, String>();

    public DefaultTables() {
        String rainbow = hexToColortable( //
                "#FFFF00", //
                "#00FF00", //
                "#00FFFF", //
                "#0000FF", //
                "#FF00FF", //
                "#FF0000"//
        );

        map.put(EColorTables.rainbow.name(), rainbow);

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

        map.put(EColorTables.extrainbow.name(), extRainbow);

        String aspect = hexToColortable(//
                "#FFFFFF", //
                "#000000", //
                "#FFFFFF")//
        ;
        map.put(EColorTables.aspect.name(), aspect);

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
        map.put(EColorTables.bathymetric.name(), bath);

        String elev = "0 191 191 \n" + //
                "0 255 0 \n" + //
                "255 255 0 \n" + //
                "255 127 0\n" + //
                "191 127 63 \n" + //
                "20 21 20\n";
        map.put(EColorTables.elev.name(), elev);

        String flow = "1 255 255 0\n" + //
                "2 0 255 0\n" + //
                "3 0 255 255\n" + //
                "4 255 0 255\n" + //
                "5 0 0 255\n" + //
                "6 160 32 240\n" + //
                "7 255 165 0\n" + //
                "8 30 144 255\n" + //
                "10 255 0 0\n";
        map.put(EColorTables.flow.name(), flow);

        String loga = "-1.0 255 255 255 0 255 255 255\n" + //
                "0.0 255 255 0 1 0 255 0\n" + //
                "1 0 255 0 10 0 255 255\n" + //
                "10 0 255 255 100 0 0 255\n" + //
                "100 0 0 255 1000 255 0 255\n" + //
                "1000 255 0 255 10000 255 0 0\n" + //
                "10000 255 0 0 100000 110 0 0\n" + //
                "100000 110 0 0 1000000 0 0 0\n";
        map.put(EColorTables.logarithmic.name(), loga);

        String tca = "1 255 255 255\n" + //
                "10 0 255 0\n" + //
                "100 0 255 255\n" + //
                "1000 0 0 255\n" + //
                "10000 255 0 255\n" + //
                "100000 255 0 0\n" + //
                "1000000 110 0 0\n" + //
                "10000000 0 0 0\n";
        map.put(EColorTables.tca.name(), tca);

        String sea = "-30000.0 255 255 255 -8000.0 255 255 255\n" + //
                "-8000.0 0 0 255 -2500.0 30 144 255\n" + //
                "-2500.0 30 144 255 -2000 162 208 252\n" + //
                "-2000 162 208 252 -1500 250 117 117\n" + //
                "-1500 250 117 117 0.0 255 0 0\n";
        map.put(EColorTables.sea.name(), sea);

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

        map.put(EColorTables.radiation.name(), radiation);

        String net = "2 255 0 0 2 255 0 0";
        map.put(EColorTables.net.name(), net);

        String greyscale = "0 0 0 \n" + //
                "255 255 255\n";
        map.put(EColorTables.greyscale.name(), greyscale);

        String greyscaleInverse = "255 255 255\n" + //
                "0 0 0\n";
        map.put(EColorTables.greyscaleinverse.name(), greyscaleInverse);

        String reds = "255 245 240\n" + //
                "254 224 210\n" + //
                "252 187 161\n" + //
                "252 146 114\n" + //
                "251 106 74\n" + //
                "239 59 44\n" + //
                "203 24 29\n" + //
                "165 15 21\n" + //
                "103 0 13\n";
        map.put(EColorTables.reds.name(), reds);

        String greens = "247 252 245\n" + //
                "229 245 224\n" + //
                "199 233 192\n" + //
                "161 217 155\n" + //
                "116 196 118\n" + //
                "65 171 93\n" + //
                "35 139 69\n" + //
                "0 109 44\n" + //
                "0 68 27\n";
        map.put(EColorTables.greens.name(), greens);

        String blues = "247 251 255\n" + //
                "222 235 247\n" + //
                "198 219 239\n" + //
                "158 202 225\n" + //
                "107 174 214\n" + //
                "66 146 198\n" + //
                "33 113 181\n" + //
                "8 81 156\n" + //
                "8 48 107\n";
        map.put(EColorTables.blues.name(), blues);

        String violets = "252 251 253\n" + //
                "239 237 245\n" + //
                "218 218 235\n" + //
                "188 189 220\n" + //
                "158 154 200\n" + //
                "128 125 186\n" + //
                "106 81 163\n" + //
                "84 39 143\n" + //
                "63 0 125\n";
        map.put(EColorTables.violets.name(), violets);

        String shalstab = "1.0 255 0 0 1.0 255 0 0\n" + //
                "2.0 0 255 0 2.0 0 255 0\n" + //
                "3.0 255 255 0 3.0 255 255 0\n" + //
                "4.0 0 0 255 4.0 0 0 255\n" + //
                "8888.0 77 77 77 8888.0 77 77 77\n";
        map.put(EColorTables.shalstab.name(), shalstab);

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

        map.put(EColorTables.slope.name(), slope);

        String geomorphon = "1000.0 127 127 127\n" + //
                "1001.0 108 0 0\n" + //
                "1002.0 255 0 0\n" + //
                "1003.0 255 165 0\n" + //
                "1004.0 255 219 61\n" + //
                "1005.0 255 255 0\n" + //
                "1006.0 143 203 44\n" + //
                "1007.0 50 189 160\n" + //
                "1008.0 0 0 255\n";
        map.put(EColorTables.geomorphon.name(), geomorphon);

        String tc3 = "15 255 255 0\n" + //
                "25 0 0 255\n" + //
                "35 255 0 0\n";
        map.put(EColorTables.tc3.name(), tc3);

        String tc9 = "10:planar-planar 255 255 0\n" + //
                "20 0 255 0\n" + //
                "30 0 255 128\n" + //
                "40 0 255 255\n" + //
                "50 0 128 255\n" + //
                "60 0 0 255\n" + //
                "70 128 0 255\n" + //
                "80 255 0 255\n" + //
                "90 255 0 0\n";
        map.put(EColorTables.tc9.name(), tc9);

        String contrasting = hexToColortable(//
                "#2E2532", //
                "#210203", //
                "#432818", //
                "#343A1A", //
                "#99582A", //
                "#BB9457", //
                "#BFAE48", //
                "#C9ADA1", //
                "#AB87FF", //
                "#DECDF5", //
                "#084887", //
                "#4D6A6D", //
                "#33658A", //
                "#4D9078", //
                "#28AFB0", //
                "#08BDBD", //
                "#92AFD7", //
                "#B4E1FF", //
                "#B7FDFE", //
                "#5FAD56", //
                "#29BF12", //
                "#ABFF4F", //
                "#B4436C", //
                "#BF3100", //
                "#D33F49", //
                "#F21B3F", //
                "#FF5964", //
                "#F58A07", //
                "#F6AE2D", //
                "#F4D35E", //
                "#FFE74C", //
                "#FFFD98", //
                "#F5FFC6", //
                "#798478", //
                "#898989", //
                "#A0A083", //
                "#E3E3E3", //
                "#F8F1FF", //
                "#F2FDFF" //
        );

        map.put(EColorTables.contrasting.name(), contrasting);

        String contrasting130Num = hexToColortable(//
                "0 #2E2532", //
                "1 #210203", //
                "2 #432818", //
                "3 #343A1A", //
                "4 #99582A", //
                "5 #BB9457", //
                "6 #BFAE48", //
                "7 #C9ADA1", //
                "8 #AB87FF", //
                "9 #DECDF5", //
                "10 #084887", //
                "11 #4D6A6D", //
                "12 #33658A", //
                "13 #4D9078", //
                "14 #28AFB0", //
                "15 #08BDBD", //
                "16 #92AFD7", //
                "17 #B4E1FF", //
                "18 #B7FDFE", //
                "19 #5FAD56", //
                "20 #29BF12", //
                "21 #ABFF4F", //
                "22 #B4436C", //
                "23 #BF3100", //
                "24 #D33F49", //
                "25 #F21B3F", //
                "26 #FF5964", //
                "27 #F58A07", //
                "28 #F6AE2D", //
                "29 #F4D35E", //
                "30 #FFE74C", //
                "31 #FFFD98", //
                "32 #F5FFC6", //
                "33 #798478", //
                "34 #898989", //
                "35 #A0A083", //
                "36 #E3E3E3", //
                "37 #F8F1FF", //
                "38 #F2FDFF", //
                "39 #93a3b1", //
                "40 #7c898b", //
                "41 #636564", //
                "42 #4c443c", //
                "43 #322214", //
                "44 #bcf4f5", //
                "45 #b4ebca", //
                "46 #d9f2b4", //
                "47 #d3fac7", //
                "48 #ffb7c3", //
                "49 #a7a5c6", //
                "50 #8797b2", //
                "51 #6d8a96", //
                "52 #5d707f", //
                "53 #66ced6", //
                "54 #eef4d4", //
                "55 #daefb3", //
                "56 #ea9e8d", //
                "57 #d64550", //
                "58 #1c2826", //
                "59 #5d2a42", //
                "60 #fb6376", //
                "61 #fcb1a6", //
                "62 #ffdccc", //
                "63 #fff9ec", //
                "64 #c2efb3", //
                "65 #97abb1", //
                "66 #746f72", //
                "67 #735f3d", //
                "68 #594a26", //
                "69 #544b3d", //
                "70 #4e6e58", //
                "71 #4c8577", //
                "72 #a6ece0", //
                "73 #7adfbb", //
                "74 #8d3b72", //
                "75 #8a7090", //
                "76 #89a7a7", //
                "77 #72e1d1", //
                "78 #b5d8cc", //
                "79 #2d82b7", //
                "80 #42e2b8", //
                "81 #f3dfbf", //
                "82 #eb8a90", //
                "83 #d7fdf0", //
                "84 #b2ffd6", //
                "85 #b4d6d3", //
                "86 #b8bac8", //
                "87 #aa78a6", //
                "88 #944bbb", //
                "89 #aa7bc3", //
                "90 #cc92c2", //
                "91 #dba8ac", //
                "92 #424b54", //
                "93 #b38d97", //
                "94 #d5aca9", //
                "95 #ebcfb2", //
                "96 #c5baaf", //
                "97 #8ed081", //
                "98 #b4d2ba", //
                "99 #dce2aa", //
                "100 #b57f50", //
                "101 #4b543b", //
                "102 #bcd4de", //
                "103 #a5ccd1", //
                "104 #a0b9bf", //
                "105 #9dacb2", //
                "106 #949ba0", //
                "107 #507dbc", //
                "108 #a1c6ea", //
                "109 #bbd1ea", //
                "110 #dae3e5", //
                "111 #292f36", //
                "112 #4ecdc4", //
                "113 #f7fff7", //
                "114 #ff6b6b", //
                "115 #ffe66d", //
                "116 #8b9474", //
                "117 #6cae75", //
                "118 #8bbd8b", //
                "119 #c1cc99", //
                "120 #f5a65b", //
                "121 #d3bdb0", //
                "122 #c1ae9f", //
                "123 #89937c", //
                "124 #715b64", //
                "125 #69385c", //
                "126 #e0f2e9", //
                "127 #ceb5a7", //
                "128 #a17c6b", //
                "129 #5b7b7a", //
                "130 #3c887e" //

        );

        map.put(EColorTables.contrasting130.name(), contrasting130Num);
    }

    private String hexToColortable( String... hexes ) {
        return Arrays.asList(hexes).stream().map(hex -> {
            String[] split = hex.split("\\s+");
            if (split.length == 1) {
                Color c = ColorUtilities.fromHex(hex);
                return c.getRed() + " " + c.getGreen() + " " + c.getBlue();
            } else if (split.length == 2) {
                Color c = ColorUtilities.fromHex(split[1]);
                return split[0] + " " + c.getRed() + " " + c.getGreen() + " " + c.getBlue();
            } else {
                return hex;
            }
        }).collect(Collectors.joining("\n"));
    }

    /**
     * Method to add a table at runtime to the tables available.
     *
     * @param name    the name of the table.
     * @param palette the colors and values.
     */
    public static void addRuntimeTable( String name, String palette ) {
        map.put(name, palette);
    }

    public String getTableString( String name ) {
        checkInit();
        return map.get(name);
    }

    public List<Color> getTableColors( String name ) {
        String tableString = getTableString(name);
        String[] rgbColors = tableString.split("\n");
        Color[] availColors = new Color[rgbColors.length];
        for( int i = 0; i < availColors.length; i++ ) {
            String rgb = rgbColors[i];
            availColors[i] = ColorUtilities.colorFromRbgString(rgb);
        }
        return Arrays.asList(availColors);
    }

    public static String[] getTableNames() {
        checkInit();
        return map.keySet().stream().sorted().collect(Collectors.toList()).toArray(new String[0]);
    }

    private static void checkInit() {
        if (map.size() == 0) {
            new DefaultTables();
        }
    }

}
