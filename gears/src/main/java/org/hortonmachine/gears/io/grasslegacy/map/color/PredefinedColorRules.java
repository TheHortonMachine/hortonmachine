package org.hortonmachine.gears.io.grasslegacy.map.color;

import java.util.HashMap;

public class PredefinedColorRules {
    public static HashMap<String, int[][]> colorRules = new HashMap<String, int[][]>();
    public static int[][] rainbow;
    public static int[][] flow;
    public static int[][] aspect;
    public static int[][] elevation;
    public static int[][] greyscale;
    public static int[][] btyc;

    static {
        rainbow = new int[][]{{255, 255, 0}, /* yellow */
        {0, 255, 0}, /* green */
        {0, 255, 255}, /* cyan */
        {0, 0, 255}, /* blue */
        {255, 0, 255}, /* magenta */
        {255, 0, 0} /* red */
        };
        colorRules.put("rainbow", rainbow);

        flow = new int[][]{{255, 255, 0}, {0, 255, 0}, {0, 255, 255}, {255, 0, 255}, {0, 0, 255},
                {160, 32, 240}, {255, 165, 0}, {30, 144, 255}, {255, 0, 0}};
        colorRules.put("flowdirections", flow);

        aspect = new int[][]{{0, 0, 0}, {255, 255, 255}, {0, 0, 0}};
        colorRules.put("aspect", aspect);

        elevation = new int[][]{{0, 191, 191}, {0, 255, 0}, {255, 255, 0}, {255, 127, 0},
                {191, 127, 63}, {20, 21, 20}};
        colorRules.put("elevation", elevation);

        greyscale = new int[][]{{0, 0, 0}, {255, 255, 255}};
        colorRules.put("greyscale", greyscale);

        btyc = new int[][]{{0, 0, 255}, {255, 255, 0}, {0, 255, 0}};
        colorRules.put("blue through yellow to green colors", btyc);


        // corine
	//	111     230:000:077
	//	112     255:000:000
	//	121     204:077:242
	//	122     204:000:000
	//	123     230:204:204
	//	124     230:204:230
	//	131     166:000:204
	//	132     166:077:000
	//	133     255:077:255
	//	141     255:166:255
	//	142     255:230:255
	//	211     255:255:168
	//	212     255:255:000
	//	213     230:230:000
	//	221     230:128:000
	//	222     242:166:077
	//	223     230:166:000
	//	231     230:230:077
	//	241     255:230:166
	//	242     255:230:077
	//	243     230:204:077
	//	244     242:204:166
	//	311     128:255:000
	//	312     000:166:000
	//	313     077:255:000
	//	321     204:242:077
	//	322     166:255:128
	//	323     166:230:077
	//	324     166:242:000
	//	331     230:230:230
	//	332     204:204:204
	//	333     204:255:204
	//	334     000:000:000
	//	335     166:230:204
	//	411     166:166:255
	//	412     077:077:255
	//	421     204:204:255
	//	422     230:230:255
	//	423     166:166:230
	//	511     000:204:242
	//	512     128:242:230
	//	521     000:255:166
	//	522     166:255:230
	//	523     230:242:255
	//	995     230:242:255
	



    }

}
