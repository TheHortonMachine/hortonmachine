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
package org.hortonmachine.gears.io.dbf;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_OUT_TABLE_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSDBFTABLEREADER_STATUS;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

@Description(OMSDBFTABLEREADER_DESCRIPTION)
@Author(name = OMSDBFTABLEREADER_AUTHORNAMES, contact = OMSDBFTABLEREADER_AUTHORCONTACTS)
@Keywords(OMSDBFTABLEREADER_KEYWORDS)
@Label(OMSDBFTABLEREADER_LABEL)
@Name(OMSDBFTABLEREADER_NAME)
@Status(OMSDBFTABLEREADER_STATUS)
@License(OMSDBFTABLEREADER_LICENSE)
public class OmsDbfTableReader extends HMModel {

    @Description(OMSDBFTABLEREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_DBF)
    @In
    public String file = null;

    @Description(OMSDBFTABLEREADER_OUT_TABLE_DATA_DESCRIPTION)
    @Out
    public Map<String, List<Object>> outTabledata = null;

    @Execute
    public void readTable() throws IOException {
        if (!concatOr(outTabledata == null, doReset)) {
            return;
        }

        FileInputStream fis = null;
        DbaseFileReader dbfReader = null;
        try {

            fis = new FileInputStream(file);
            dbfReader = new DbaseFileReader(fis.getChannel(), false, Charset.defaultCharset());
            final DbaseFileHeader header = dbfReader.getHeader();
            int numFields = header.getNumFields();
            outTabledata = new HashMap<>();
            for( int i = 0; i < numFields; i++ ) {
                String fieldName = header.getFieldName(i);
                outTabledata.put(fieldName, new ArrayList<Object>());
            }

            while( dbfReader.hasNext() ) {
                dbfReader.read();
                for( int i = 0; i < numFields; i++ ) {
                    Object field = dbfReader.readField(i);
                    String fieldName = header.getFieldName(i);
                    List<Object> list = outTabledata.get(fieldName);
                    list.add(field);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (dbfReader != null)
                dbfReader.close();
            if (fis != null)
                fis.close();
        }
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the dbf path.
     * @return the read table.
     * @throws IOException
     */
    public static Map<String, List<Object>> readDbf( String path ) throws IOException {

        OmsDbfTableReader reader = new OmsDbfTableReader();
        reader.file = path;
        reader.readTable();

        return reader.outTabledata;
    }

}
