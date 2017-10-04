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
package org.hortonmachine.gears.io.csv;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_FORMATS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_IDS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_LABELS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_SUBTITLE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_TITLE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_OUT_TYPES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSCSVMATRIXREADER_UI;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSCSVMATRIXREADER_DESCRIPTION)
@Author(name = OMSCSVMATRIXREADER_AUTHORNAMES, contact = OMSCSVMATRIXREADER_AUTHORCONTACTS)
@Keywords(OMSCSVMATRIXREADER_KEYWORDS)
@Label(OMSCSVMATRIXREADER_LABEL)
@Name(OMSCSVMATRIXREADER_NAME)
@Status(OMSCSVMATRIXREADER_STATUS)
@License(OMSCSVMATRIXREADER_LICENSE)
@UI(OMSCSVMATRIXREADER_UI)
public class OmsCsvMatrixReader extends HMModel {

    @Description(OMSCSVMATRIXREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(OMSCSVMATRIXREADER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSCSVMATRIXREADER_NOVALUE_DESCRIPTION)
    @In
    public double novalue = HMConstants.doubleNovalue;

    @Description(OMSCSVMATRIXREADER_OUT_DATA_DESCRIPTION)
    @Out
    public double[][] outData;

    @Description(OMSCSVMATRIXREADER_OUT_TITLE_DESCRIPTION)
    @Out
    public String outTitle;

    @Description(OMSCSVMATRIXREADER_OUT_SUBTITLE_DESCRIPTION)
    @Out
    public String outSubTitle;

    @Description(OMSCSVMATRIXREADER_OUT_IDS_DESCRIPTION)
    @Out
    public String[] outIds;

    @Description(OMSCSVMATRIXREADER_OUT_LABELS_DESCRIPTION)
    @Out
    public String[] outLabels;

    @Description(OMSCSVMATRIXREADER_OUT_FORMATS_DESCRIPTION)
    @Out
    public String[] outFormats;

    @Description(OMSCSVMATRIXREADER_OUT_TYPES_DESCRIPTION)
    @Out
    public String[] outTypes;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private int columnCount;

    private List<String> outIdsList = new ArrayList<>();
    private List<String> outLabelsList = new ArrayList<>();
    private List<String> outFormatsList = new ArrayList<>();
    private List<String> outTypesList = new ArrayList<>();

    private List<double[]> outDataList = new ArrayList<>();

    private DateTimeFormatter dateFormatter;

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);

            outTitle = table.getName();
            Map<String, String> info = table.getInfo();
            Set<Entry<String, String>> entrySet = info.entrySet();
            for( Entry<String, String> entry : entrySet ) {
                String key = entry.getKey();
                String value = entry.getValue();
                if ("subtitle".equalsIgnoreCase(key)) {
                    outSubTitle = value;
                }
            }

            columnCount = table.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = table.getColumnName(i);
                if (i > 1)
                    outIdsList.add(columnName);

                Map<String, String> columnInfo = table.getColumnInfo(i);
                Set<Entry<String, String>> entrySet1 = columnInfo.entrySet();
                for( Entry<String, String> entry : entrySet1 ) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if ("label".equalsIgnoreCase(key)) {
                        outLabelsList.add(value);
                    }
                    if ("format".equalsIgnoreCase(key)) {
                        outFormatsList.add(value);
                    }
                    if ("type".equalsIgnoreCase(key)) {
                        value = value.toLowerCase().trim();
                        if (value.length() == 0) {
                            value = "double";
                        }
                        outTypesList.add(value);
                    }
                }
            }

            if (!outIdsList.isEmpty()) {
                outIds = outIdsList.toArray(new String[0]);
            }
            if (!outLabelsList.isEmpty()) {
                outLabels = outLabelsList.toArray(new String[0]);
            }
            if (!outFormatsList.isEmpty()) {
                outFormats = outFormatsList.toArray(new String[0]);
            }

            if (outTypesList.isEmpty()) {
                for( int i = 1; i <= columnCount; i++ ) {
                    outTypesList.add("double");
                }
            }
            outTypes = outTypesList.toArray(new String[0]);

            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
        }
    }

    @Execute
    public void read() throws IOException {
        ensureOpen();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();
            double[] record = new double[columnCount];
            for( int i = 1; i <= columnCount; i++ ) {
                if (i == 0 && outTypes[i].equals("date") && outFormats.length > i) {
                    if (dateFormatter == null)
                        dateFormatter = DateTimeFormat.forPattern(outFormats[i]);

                    DateTime dateTime = dateFormatter.parseDateTime(row[i]);
                    record[i - 1] = dateTime.getMillis();
                } else {
                    double value = Double.parseDouble(row[i]);
                    record[i - 1] = value;
                }
            }
            outDataList.add(record);
        }

        outData = new double[outDataList.size()][];
        int index = 0;
        for( double[] record : outDataList ) {
            outData[index++] = record;
        }
    }
    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }

}
