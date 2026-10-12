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

import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_IN_TABLE_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_LABEL;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_NAME;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner.OMSVECTORTABLEJOINER_STATUS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.dbf.OmsDbfTableReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner;
import org.hortonmachine.gears.utils.features.FeatureUtilities;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSVECTORTABLEJOINER_DESCRIPTION)
@Documentation(OMSVECTORTABLEJOINER_DOCUMENTATION)
@Author(name = OMSVECTORTABLEJOINER_AUTHORNAMES, contact = OMSVECTORTABLEJOINER_AUTHORCONTACTS)
@Keywords(OMSVECTORTABLEJOINER_KEYWORDS)
@Label(OMSVECTORTABLEJOINER_LABEL)
@Name("_" + OMSVECTORTABLEJOINER_NAME)
@Status(OMSVECTORTABLEJOINER_STATUS)
@License(OMSVECTORTABLEJOINER_LICENSE)
public class VectorTableJoiner extends HMModel {

    @Description(OMSVECTORTABLEJOINER_IN_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inVector;

    @Description(OMSVECTORTABLEJOINER_IN_TABLE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_GENERIC)
    @In
    public String inTable = null;

    @Description(OMSVECTORTABLEJOINER_F_COMMON_DESCRIPTION)
    @In
    public String fCommon = null;

    @Description(OMSVECTORTABLEJOINER_P_FIELDS_DESCRIPTION)
    @In
    public String pFields = null;

    @Description(OMSVECTORTABLEJOINER_OUT_VECTOR_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVector;

    @Execute
    public void process() throws Exception {
        checkNull(inVector, inTable);
        OmsVectorTableJoiner vectortablejoiner = new OmsVectorTableJoiner();
        vectortablejoiner.inVector = getVector(inVector);
        vectortablejoiner.tabledata = readTable(inTable);
        vectortablejoiner.fCommon = fCommon;
        vectortablejoiner.pFields = pFields;
        vectortablejoiner.pm = pm;
        vectortablejoiner.doProcess = doProcess;
        vectortablejoiner.doReset = doReset;
        vectortablejoiner.process();
        dumpVector(vectortablejoiner.outVector, outVector);
    }

    /**
     * Read a dbf file, or the attribute table of a vector, as the values of each field by name.
     */
    private Map<String, List<Object>> readTable( String path ) throws Exception {
        if (path.toLowerCase().endsWith(".dbf")) {
            return OmsDbfTableReader.readDbf(path);
        }
        SimpleFeatureCollection table = getVector(path);
        Map<String, List<Object>> tableData = new HashMap<>();
        List<String> names = new ArrayList<>();
        for( AttributeDescriptor descriptor : table.getSchema().getAttributeDescriptors() ) {
            if (!(descriptor instanceof GeometryDescriptor)) {
                String name = descriptor.getLocalName();
                names.add(name);
                tableData.put(name, new ArrayList<>());
            }
        }
        for( SimpleFeature feature : FeatureUtilities.featureCollectionToList(table) ) {
            for( String name : names ) {
                tableData.get(name).add(feature.getAttribute(name));
            }
        }
        return tableData;
    }
}
