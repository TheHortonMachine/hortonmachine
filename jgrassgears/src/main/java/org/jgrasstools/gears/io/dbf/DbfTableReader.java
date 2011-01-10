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
package org.jgrasstools.gears.io.dbf;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.shapefile.dbf.DbaseFileHeader;
import org.geotools.data.shapefile.dbf.DbaseFileReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Utility class for reading dbf tables.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Shapefile, Feature, Vector, Reading")
@Label(JGTConstants.GENERICREADER)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DbfTableReader extends JGTModel {
    @Description("The shapefile.")
    @In
    public String file = null;

    @Description("The read dbf table.")
    @Out
    public HashMap<String, List<Object>> tabledata = null;

    @Execute
    public void readTable() throws IOException {
        if (!concatOr(tabledata == null, doReset)) {
            return;
        }

        FileInputStream fis = null;
        DbaseFileReader dbfReader = null;
        try {

            fis = new FileInputStream(file);
            dbfReader = new DbaseFileReader(fis.getChannel(), false, Charset.defaultCharset());
            final DbaseFileHeader header = dbfReader.getHeader();
            int numFields = header.getNumFields();
            tabledata = new HashMap<String, List<Object>>();
            for( int i = 0; i < numFields; i++ ) {
                String fieldName = header.getFieldName(i);
                tabledata.put(fieldName, new ArrayList<Object>());
            }

            while( dbfReader.hasNext() ) {
                dbfReader.read();
                for( int i = 0; i < numFields; i++ ) {
                    Object field = dbfReader.readField(i);
                    String fieldName = header.getFieldName(i);
                    List<Object> list = tabledata.get(fieldName);
                    list.add(field);
                }
            }
        } finally {
            dbfReader.close();
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
    public static HashMap<String, List<Object>> readDbf( String path ) throws IOException {

        DbfTableReader reader = new DbfTableReader();
        reader.file = path;
        reader.readTable();

        return reader.tabledata;
    }

}
