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
package eu.hydrologis.edc.oms.csv;

import java.io.File;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Role;
import oms3.annotations.Status;
import oms3.io.DataIO;
import oms3.io.MemoryTable;

import org.hibernate.Criteria;
import org.hibernate.classic.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.edc.EDC;
import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesHydrometersTable;
import eu.hydrologis.edc.annotatedclassesdaos.AbstractEdcDao;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.utils.Constants;

@Description("Utility class for reading edc data to oms analizable csv.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Database, Csv, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class SeriesHydrometer2CsvWriter {
    @Description("The EDC instance to use.")
    @In
    public EdcSessionFactory edcSessionFactory = null;

    @Role(Role.PARAMETER)
    @Description("The id of the hydrometer to use.")
    @In
    public String ids;

    @Role(Role.PARAMETER)
    @Description("The start date for data fetching (yyyy-mm-dd hh:mm).")
    @In
    public String startDate;

    @Role(Role.PARAMETER)
    @Description("The end date for data fetching (yyyy-mm-dd hh:mm).")
    @In
    public String endDate;

    @Role(Role.PARAMETER)
    @Description("The timestep (in minutes) filter for the fetched data.")
    @In
    public double timeStep;

    @Role(Role.PARAMETER)
    @Description("The unit filter for the fetched data.")
    @In
    public String unit;

    @Role(Role.PARAMETER)
    @Description("The dataType of the fetched data.")
    @In
    public String dataType;

    @Role(Role.PARAMETER)
    @Description("The discharge/level curve type of the fetched data.")
    @In
    public String scaleType;

    @Role(Role.PARAMETER)
    @Description("The csv separator.")
    @In
    public String separator = ",";

    @Description("The csv file path to which to write the fetched data.")
    @In
    public String csvFilePath;

    @SuppressWarnings("unchecked")
    @Execute
    public void toCsv() throws Exception {
        Session session = null;
        try {
            session = edcSessionFactory.openSession();

            DateTimeFormatter formatter = Constants.utcDateFormatterYYYYMMDDHHMM;
            String formatterPattern = Constants.utcDateFormatterYYYYMMDDHHMM_string;
            DateTime startDateTime = formatter.parseDateTime(startDate);
            DateTime endDateTime = formatter.parseDateTime(endDate);

            long interval = endDateTime.getMillis() - startDateTime.getMillis();
            long dt = (long) (timeStep * 60.0 * 1000.0);
            int stepsNum = (int) (interval / dt);

            String[] idSplit = ids.split(",");
            int idNum = idSplit.length;

            Object[][] values = new Object[stepsNum][idNum + 1];

            HashMap<Long, Integer> id2Index = new HashMap<Long, Integer>();
            for( int i = 0; i < idSplit.length; i++ ) {
                id2Index.put(new Long(idSplit[0]), i + 1);
            }
            HashMap<DateTime, Integer> dt2Index = new HashMap<DateTime, Integer>();
            DateTime runningTime = startDateTime;
            for( int i = 0; i < stepsNum; i++ ) {
                dt2Index.put(runningTime, i);
                runningTime = runningTime.plus(dt);
            }

            Long[] idsLongArray = new Long[idSplit.length];
            int index = 0;
            for( String idStr : idSplit ) {
                Long id = new Long(idStr);
                idsLongArray[index++] = id;

                Criteria criteria = session.createCriteria(UnitsTable.class);
                criteria.add(Restrictions.eq("name", unit));
                UnitsTable unitTable = (UnitsTable) criteria.uniqueResult();

                criteria = session.createCriteria(SeriesHydrometersTable.class);
                // set id
                HydrometersTable hydrometer = new HydrometersTable();
                hydrometer.setId(id);
                criteria.add(Restrictions.eq("hydrometer", hydrometer));
                // set time frame
                criteria.add(Restrictions.between("timestampUtc", startDateTime, endDateTime));
                criteria.addOrder(Order.asc("timestampUtc"));
                // set timestep
                criteria.add(Restrictions.eq("timeStep", timeStep));
                // set unit
                // UnitsTable unitTable = new UnitsTable();
                // unitTable.setName(unit);
                criteria.add(Restrictions.eq("unit", unitTable));

                List<SeriesHydrometersTable> seriesData = criteria.list();

                for( SeriesHydrometersTable seriesHydrometersTable : seriesData ) {
                    DateTime timestampUtc = seriesHydrometersTable.getTimestampUtc();
                    DateTime dateTime = timestampUtc.toDateTime(DateTimeZone.UTC);
                    Double value = seriesHydrometersTable.getValue();

                    Integer dtIndex = dt2Index.get(dateTime);
                    Integer idIndex = id2Index.get(id);
                    if (dtIndex == null || idIndex == null) {
                        System.out.println();
                    }
                    if (values[dtIndex][0] == null) {
                        values[dtIndex][0] = dateTime.toString(formatter);
                    }
                    values[dtIndex][idIndex] = value.toString();
                }
            }

            if (csvFilePath != null) {
                File csvFile = new File(csvFilePath);

                MemoryTable mt = new MemoryTable();
                mt.setName(AbstractEdcDao.tableAnnotationToString(SeriesHydrometersTable.class));
                mt.getInfo().put("Created", new DateTime().toString(formatter));
                mt.getInfo().put("Author", "Edc library");
                mt.getInfo().put("dataStart", startDate);
                mt.getInfo().put("dataEnd", endDate);

                // column names
                String[] columnNames = new String[idSplit.length + 1];
                columnNames[0] = "timestamp";
                for( int i = 0; i < idSplit.length; i++ ) {
                    columnNames[i + 1] = idSplit[i];
                }
                mt.setColumns(columnNames);

                // data types
                mt.getColumnInfo(1).put("Type", "Date");
                for( int j = 0; j < idSplit.length; j++ ) {
                    mt.getColumnInfo(j + 2).put("Type", "Double");
                }

                // data formats
                mt.getColumnInfo(1).put("Format", formatterPattern);
                for( int j = 0; j < idSplit.length; j++ ) {
                    mt.getColumnInfo(j + 2).put("Format", "");
                }

                // data units
                mt.getColumnInfo(1).put("Unit", "");
                for( int j = 0; j < idSplit.length; j++ ) {
                    mt.getColumnInfo(j + 2).put("Unit", unit);
                }

                // data timestep
                mt.getColumnInfo(1).put("Timestep", "");
                for( int j = 0; j < idSplit.length; j++ ) {
                    mt.getColumnInfo(j + 2).put("Timestep", String.valueOf(timeStep));
                }

                // int tmp = 0;
                for( Object[] valuesRow : values ) {
                    // for( Object object : valuesRow ) {
                    // if (object==null) {
                    // valuesRow = new String[]{"date...", "value..."};
                    // System.out.println();
                    // break;
                    // }
                    // }
                    // tmp++;
                    mt.addRow(valuesRow);
                }
                DataIO.print(mt, new PrintWriter(csvFile));
            }
        } finally {
            if (session != null)
                session.close();
        }
    }
    public static void main( String[] args ) throws Exception {
        Properties properties = new Properties();
        properties.put("TYPE", "H2");
        properties.put("HOST", "localhost");
        properties.put("PORT", "9092");
        properties.put("DATABASE", "C:\\TMP\\suapdb\\database");
        properties.put("USER", "sa");
        properties.put("PASS", "");

        EDC edc = new EDC(properties, System.out);

        SeriesHydrometer2CsvWriter writer = new SeriesHydrometer2CsvWriter();
        writer.edcSessionFactory = edc.getEdcSessionFactory();
        writer.ids = "1";
        writer.startDate = "1992-01-01 00:00";
        writer.endDate = "1993-01-01 00:00";
        writer.timeStep = 15.0;
        writer.unit = "m";
        writer.csvFilePath = "C:\\TMP\\suapdb\\hydrometer.csv";

        writer.toCsv();

        writer.edcSessionFactory.closeSessionFactory();

    }

}
