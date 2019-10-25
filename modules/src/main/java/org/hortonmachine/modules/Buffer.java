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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.modules.v.vectoroperations.OmsBuffer.*;

import java.io.IOException;

import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;

import static org.hortonmachine.gears.libs.modules.Variables.CAP_FLAT;
import static org.hortonmachine.gears.libs.modules.Variables.CAP_ROUND;
import static org.hortonmachine.gears.libs.modules.Variables.CAP_SQUARE;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_BEVEL;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_MITRE;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_ROUND;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectoroperations.OmsBuffer;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSBUFFER_DESCRIPTION)
@Author(name = OMSBUFFER_AUTHORNAMES, contact = OMSBUFFER_AUTHORCONTACTS)
@Keywords(OMSBUFFER_KEYWORDS)
@Label(OMSBUFFER_LABEL)
@Name("_" + OMSBUFFER_NAME)
@Status(OMSBUFFER_STATUS)
@License(OMSBUFFER_LICENSE)
public class Buffer extends HMModel {

    @Description(OMSBUFFER_IN_MAP_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inMap = null;

    @Description(OMSBUFFER_P_BUFFER_DESCRIPTION)
    @In
    public double pBuffer = 10.0;

    @Description(OMSBUFFER_P_BUFFERFIELD_DESCRIPTION)
    @In
    public String pBufferField;

    @Description(OMSBUFFER_DO_SINGLE_SIDED_DESCRIPTION)
    @In
    public boolean doSinglesided = false;

    @Description(OMSBUFFER_P_JOIN_STYLE_DESCRIPTION)
    @UI("combo:" + JOIN_ROUND + "," + JOIN_MITRE + "," + JOIN_BEVEL)
    @In
    public String pJoinstyle = JOIN_ROUND;

    @Description(OMSBUFFER_P_CAP_STYLE_DESCRIPTION)
    @UI("combo:" + CAP_ROUND + "," + CAP_FLAT + "," + CAP_SQUARE)
    @In
    public String pCapstyle = CAP_ROUND;

    @Description(OMSBUFFER_OUT_MAP_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsBuffer buffer = new OmsBuffer();
        buffer.inMap = getVector(inMap);
        buffer.pBuffer = pBuffer;
        buffer.pBufferField = pBufferField;
        buffer.doSinglesided = doSinglesided;
        buffer.pJoinstyle = pJoinstyle;
        buffer.pCapstyle = pCapstyle;
        buffer.pm = pm;
        buffer.process();
        dumpVector(buffer.outMap, outMap);
    }

    public static void main( String[] args ) throws Exception {
        OmsBuffer buffer = new OmsBuffer();
        buffer.inMap = OmsVectorReader.readVector("/home/hydrologis/TMP/R3GIS/data/P1.shp");
        buffer.pBuffer = 10;
        buffer.pBufferField = "diam_chiom";
        buffer.doSinglesided = false;
        buffer.process();
        OmsVectorWriter.writeVector("/home/hydrologis/TMP/R3GIS/data/P1_buf.shp", buffer.outMap);
    }

}
