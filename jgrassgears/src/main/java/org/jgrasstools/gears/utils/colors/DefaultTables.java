package org.jgrasstools.gears.utils.colors;

import java.util.HashMap;

public class DefaultTables {
    public static final String ASPECT = "aspect";
    public static final String FLOW = "flow";
    public static final String BATHYMETRIC = "bathymetric";
    public static final String ELEV = "elev";
    public static final String LOGARITHMIC = "logarithmic";
    public static final String SEA = "sea";

    private HashMap<String, String> map;

    public DefaultTables() {

        map = new HashMap<String, String>();
        String aspect = "255 255 255\n" + //
                "0 0 0\n" + //
                "255 255 255";
        map.put("aspect", aspect);

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
        map.put("bathymetric", bath);

        String elev = "0 191 191 \n" + //
                "0 255 0 \n" + //
                "255 255 0 \n" + //
                "255 127 0\n" + //
                "191 127 63 \n" + //
                "20 21 20\n";
        map.put("elev", elev);

        String flow = "1 255 255 0 1 255 255 0 \n" + //
                "2 0 255 0 2 0 255 0 \n" + //
                "3 0 255 255 3 0 255 255 \n" + //
                "4 255 0 255 4 255 0 255 \n" + //
                "5 0 0 255 5 0 0 255\n" + //
                "6 160 32 240 6 160 32 240 \n" + //
                "7 255 165 0 7 255 165 0 \n" + //
                "8 30 144 255 8 30 144 255 \n" + //
                "10 255 0 0 10 255 0 0\n";
        map.put("flow", flow);

        String loga = "-1.0 255 255 255 0 255 255 255\n" + //
                "0.0 255 255 0 1 0 255 0\n" + //
                "1 0 255 0 10 0 255 255\n" + //
                "10 0 255 255 100 0 0 255\n" + //
                "100 0 0 255 1000 255 0 255\n" + //
                "1000 255 0 255 10000 255 0 0\n" + //
                "10000 255 0 0 100000 110 0 0\n" + //
                "100000 110 0 0 1000000 0 0 0\n";
        map.put("logarithmic", loga);

        String sea = "-30000.0 255 255 255 -8000.0 255 255 255\n" + //
                "-8000.0 0 0 255 -2500.0 30 144 255\n" + //
                "-2500.0 30 144 255 -2000 162 208 252\n" + //
                "-2000 162 208 252 -1500 250 117 117\n" + //
                "-1500 250 117 117 0.0 255 0 0\n";
        map.put("sea", sea);

    }

    public String getTableString( String name ) {
        return map.get(name);
    }
}
