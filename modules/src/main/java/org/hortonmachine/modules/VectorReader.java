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

import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_AUTHORNAMES;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_DESCRIPTION;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_KEYWORDS;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_LABEL;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_LICENSE;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_NAME;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_STATUS;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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

@Description(OMSVECTORREADER_DESCRIPTION)
@Author(name = OMSVECTORREADER_AUTHORNAMES, contact = OMSVECTORREADER_AUTHORCONTACTS)
@Keywords(OMSVECTORREADER_KEYWORDS)
@Label(OMSVECTORREADER_LABEL)
@Name("_" + OMSVECTORREADER_NAME)
@Status(OMSVECTORREADER_STATUS)
@License(OMSVECTORREADER_LICENSE)
public class VectorReader extends HMModel {

    @Description(OMSVECTORREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String file = null;

    @Description(OMSVECTORREADER_OUT_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        OmsVectorReader vectorreader = new OmsVectorReader();
        vectorreader.file = file;
        vectorreader.pm = pm;
        vectorreader.doProcess = doProcess;
        vectorreader.doReset = doReset;
        vectorreader.process();
        outVector = vectorreader.outVector;
    }

    public static SimpleFeatureCollection readVector( String path ) throws Exception {
        return OmsVectorReader.readVector(path);
    }
}
