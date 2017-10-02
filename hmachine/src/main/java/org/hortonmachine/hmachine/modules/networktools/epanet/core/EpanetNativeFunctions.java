/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.networktools.epanet.core;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.sun.jna.Library;

public interface EpanetNativeFunctions extends Library {
    public static final int EN_AVERAGE = 1;
    public static final int EN_HEADLOSS = 10;
    public static final int EN_TANK = 2;
    public static final int EN_TRACE = 3;
    public static final int EN_MINORLOSS = 3;
    public static final int EN_CONCEN = 0;
    public static final int EN_PBV = 5;
    public static final int EN_SOURCEMASS = 13;
    public static final int EN_PSV = 4;
    public static final int EN_CURVECOUNT = 4;
    public static final int EN_AFD = 4;
    public static final int EN_TANKDIAM = 17;
    public static final int EN_EMITTER = 3;
    public static final int EN_INITQUAL = 4;
    public static final int EN_KWALL = 7;
    public static final int EN_DURATION = 0;
    public static final int EN_CHEM = 1;
    public static final int EN_TIMEOFDAY = 3;
    public static final int EN_MGD = 2;
    public static final int EN_CVPIPE = 0;
    public static final int EN_IMGD = 3;
    public static final int EN_PERIODS = 9;
    public static final int EN_REPORTSTEP = 5;
    public static final int EN_SETPOINT = 2;
    public static final int EN_MIX2 = 1;
    public static final int EN_FIFO = 2;
    public static final int EN_AGE = 2;
    public static final int EN_MIX1 = 0;
    public static final int EN_SETTING = 12;
    public static final int EN_PATTERNSTEP = 3;
    public static final int EN_MINIMUM = 2;
    public static final int EN_PATCOUNT = 3;
    public static final int EN_TIMER = 2;
    public static final int EN_MINLEVEL = 20;
    public static final int EN_NONE = 0;
    public static final int EN_LOWLEVEL = 0;
    public static final int EN_PATTERN = 2;
    public static final int EN_RULESTEP = 7;
    public static final int EN_TANKCOUNT = 1;
    public static final int EN_TOLERANCE = 2;
    public static final int EN_LENGTH = 1;
    public static final int EN_FCV = 6;
    public static final int EN_RESERVOIR = 1;
    public static final int EN_DEMAND = 9;
    public static final int EN_MIXMODEL = 15;
    public static final int EN_HEAD = 10;
    public static final int EN_GPV = 8;
    public static final int EN_DEMANDMULT = 4;
    public static final int EN_TRIALS = 0;
    public static final int EN_ACCURACY = 1;
    public static final int EN_GPM = 1;
    public static final int EN_NODECOUNT = 0;
    public static final int EN_FLOWPACED = 3;
    public static final int EN_MLD = 7;
    public static final int EN_ELEVATION = 0;
    public static final int EN_JUNCTION = 0;
    public static final int EN_PUMP = 2;
    public static final int EN_LIFO = 3;
    public static final int EN_TANKLEVEL = 8;
    public static final int EN_INITFLOW = 10;
    public static final int EN_STATUS = 11;
    public static final int EN_BASEDEMAND = 1;
    public static final int EN_SAVE = 1;
    public static final int EN_LPM = 6;
    public static final int EN_MAXLEVEL = 21;
    public static final int EN_PRESSURE = 11;
    public static final int EN_SOURCETYPE = 7;
    public static final int EN_SOURCEPAT = 6;
    public static final int EN_LPS = 5;
    public static final int EN_CONTROLCOUNT = 5;
    public static final int EN_SOURCEQUAL = 5;
    public static final int EN_INITVOLUME = 14;
    public static final int EN_QUALITY = 12;
    public static final int EN_MAXIMUM = 3;
    public static final int EN_TANK_KBULK = 23;
    public static final int EN_VELOCITY = 9;
    public static final int EN_CFS = 0;
    public static final int EN_LINKCOUNT = 2;
    public static final int EN_NOSAVE = 0;
    public static final int EN_EMITEXPON = 3;
    public static final int EN_REPORTSTART = 6;
    public static final int EN_TCV = 7;
    public static final int EN_MASS = 1;
    public static final int EN_MINVOLUME = 18;
    public static final int EN_HILEVEL = 1;
    public static final int EN_FLOW = 8;
    public static final int EN_INITSETTING = 5;
    public static final int EN_DIAMETER = 0;
    public static final int EN_STATISTIC = 8;
    public static final int EN_QUALSTEP = 2;
    public static final int EN_ROUGHNESS = 2;
    public static final int EN_VOLCURVE = 19;
    public static final int EN_ENERGY = 13;
    public static final int EN_INITSTATUS = 4;
    public static final int EN_PATTERNSTART = 4;
    public static final int EN_PIPE = 1;
    public static final int EN_MIXFRACTION = 22;
    public static final int EN_KBULK = 6;
    public static final int EN_CMD = 9;
    public static final int EN_CMH = 8;
    public static final int EN_PRV = 3;
    public static final int EN_HYDSTEP = 1;
    public static final int EN_RANGE = 4;
    public static final int EN_MIXZONEVOL = 16;

    int ENopen( String charPtr1, String charPtr2, String charPtr3 );
    int ENsaveinpfile( String charPtr1 );
    int ENclose();
    int ENsolveH();
    int ENsaveH();
    int ENopenH();
    int ENinitH( int int1 );
    int ENrunH( long[] lPtr1 );
    int ENnextH( long[] lPtr1 );
    int ENcloseH();
    int ENsavehydfile( String charPtr1 );
    int ENusehydfile( String charPtr1 );
    int ENsolveQ();
    int ENopenQ();
    int ENinitQ( int int1 );
    int ENrunQ( Long lPtr1 );
    int ENnextQ( Long lPtr1 );
    int ENstepQ( Long lPtr1 );
    int ENcloseQ();
    int ENwriteline( ByteBuffer charPtr1 );
    int ENreport();
    int ENresetreport();
    int ENsetreport( String charPtr1 );
    int ENgetcontrol( int int1, IntBuffer intPtr1, IntBuffer intPtr2, FloatBuffer floatPtr1, IntBuffer intPtr3,
            FloatBuffer floatPtr2 );
    int ENgetcount( int countcode, int[] count );
    int ENgetoption( int int1, float[] floatPtr1 );
    int ENgettimeparam( int int1, long[] lPtr1 );
    int ENgetflowunits( IntBuffer intPtr1 );
    int ENgetpatternindex( String charPtr1, int[] intPtr1 );
    int ENgetpatternid( int int1, ByteBuffer charPtr1 );
    int ENgetpatternlen( int int1, int[] intPtr1 );
    int ENgetpatternvalue( int int1, int int2, float[] floatPtr1 );
    int ENgetqualtype( IntBuffer intPtr1, IntBuffer intPtr2 );
    int ENgeterror( int int1, ByteBuffer charPtr1, int int2 );
    int ENgetnodeindex( String charPtr1, int[] intPtr1 );
    int ENgetnodeid( int int1, ByteBuffer charPtr1 );
    int ENgetnodetype( int int1, int[] intPtr1 );
    int ENgetnodevalue( int int1, int int2, float[] floatPtr1 );
    int ENgetlinkindex( String charPtr1, int[] intPtr1 );
    int ENgetlinkid( int int1, ByteBuffer charPtr1 );
    int ENgetlinktype( int index, int[] typecode );
    int ENgetlinknodes( int int1, int[] intPtr1, int[] intPtr2 );
    // int ENgetlinknodes(int int1, IntBuffer intPtr1, IntBuffer intPtr2);
    int ENgetlinkvalue( int index, int paramcode, float[] value );
    int ENgetversion( int[] version );
    int ENsetcontrol( int int1, int int2, int int3, float float1, int int4, float float2 );
    int ENsetnodevalue( int int1, int int2, float float1 );
    int ENsetlinkvalue( int int1, int int2, float float1 );
    int ENaddpattern( String charPtr1 );
    int ENsetpattern( int int1, FloatBuffer floatPtr1, int int2 );
    int ENsetpatternvalue( int int1, int int2, float float1 );
    int ENsettimeparam( int int1, Long l1 );
    int ENsetoption( int int1, float float1 );
    int ENsetstatusreport( int int1 );
    int ENsetqualtype( int int1, ByteBuffer charPtr1, ByteBuffer charPtr2, ByteBuffer charPtr3 );
}
