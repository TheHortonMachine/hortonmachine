/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
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
package eu.hydrologis.edc.annotatedclassesdaos.timeseries;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.hibernate.Criteria;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.edc.annotatedclasses.MonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;
import eu.hydrologis.edc.annotatedclassesdaos.AbstractEdcDao;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.utils.Constants;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("unchecked")
public class SeriesMonitoringPointsDao extends AbstractEdcDao {

    public SeriesMonitoringPointsDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    private DateTimeFormatter formatter = Constants.utcDateFormatterYYYYMMDDHHMMSS;
    protected int previousYear = -1;
    protected Class tableClass;
    private Map<String, Class> allTable2ClassesMap = Constants.getEdcSeriesTables2ClassesMap();

    protected void processLine( String[] lineSplit ) throws Exception {
        Long mpId = Long.parseLong(lineSplit[1].trim());
        DateTime dt = formatter.parseDateTime(lineSplit[2].trim());
        Double value = Double.parseDouble(lineSplit[3].trim());
        Double interv = Double.parseDouble(lineSplit[4].trim());

        int year = dt.getYear();
        if (previousYear != year) {
            System.out.println("Insert data of " + year);
            tableClass = allTable2ClassesMap.get(insertTable + year);
            if (tableClass == null) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "The table {0} doesn''t exist.", insertTable));
            }
            previousYear = year;
        }

        SeriesMonitoringPointsTable seriesTable = (SeriesMonitoringPointsTable) tableClass
                .newInstance();

        MonitoringPointsTable mpT = new MonitoringPointsTable();
        mpT.setId(mpId);

        seriesTable.setMonitoringPoint(mpT);
        seriesTable.setTimestampUtc(dt);
        seriesTable.setValue(value);
        seriesTable.setTimeStep(interv);

        String reliabilityStr = lineSplit[5].trim();
        if (reliabilityStr.length() > 0) {
            ReliabilityTable rT = new ReliabilityTable();
            rT.setId(new Long(reliabilityStr));
            seriesTable.setReliability(rT);
        }

        String unit = lineSplit[6].trim();
        if (unit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(unit));
            seriesTable.setUnit(unitsTable);
        }

        session.save(seriesTable);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append("SeriesMonitoringPointsTable:");
        sB.append(columnAnnotationToString(SeriesMonitoringPointsTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(SeriesMonitoringPointsTable.class, "monitoringPoint")).append(
                ", ");
        sB.append(columnAnnotationToString(SeriesMonitoringPointsTable.class, "timestampUtc")).append(", ");
        sB.append(columnAnnotationToString(SeriesMonitoringPointsTable.class, "value")).append(", ");
        sB.append(columnAnnotationToString(SeriesMonitoringPointsTable.class, "timeStep")).append(", ");
        sB.append(joinColumnAnnotationToString(SeriesMonitoringPointsTable.class, "reliability")).append(", ");
        sB.append(joinColumnAnnotationToString(SeriesMonitoringPointsTable.class, "unit"));
        return sB.toString();
    }
    

    public List<SeriesMonitoringPointsTable> extractData( String seriesTablePattern,
            DateTime startDate, DateTime endDate, List<Long> ids ) throws Exception {
        Map<String, Class> table2ClassesMap = Constants.getEdcSeriesTables2ClassesMap();

        List<SeriesMonitoringPointsTable> seriesData = null;

        int startYear = startDate.getYear();
        int endYear = endDate.getYear();

        int size = 0;

        for( int i = startYear; i <= endYear; i++ ) {

            Class tableClass = table2ClassesMap.get(seriesTablePattern + i);
            if (tableClass == null) {
                throw new IllegalArgumentException(MessageFormat.format(
                        "The table {0} doesn''t exist.", seriesTablePattern));
            }

            Criteria criteria = session.createCriteria(tableClass);
            Transaction transaction = session.beginTransaction();

            MonitoringPointsTable[] mPointsArray = new MonitoringPointsTable[ids.size()];
            for( int j = 0; j < mPointsArray.length; j++ ) {
                MonitoringPointsTable mpT = new MonitoringPointsTable();
                mpT.setId(ids.get(j));
                mPointsArray[j] = mpT;
            }

            criteria.add(Restrictions.in("monitoringPoint", mPointsArray));
            criteria.add(Restrictions.between("timestampUtc", startDate, endDate));
            criteria.addOrder(Order.asc("timestampUtc"));
            if (seriesData == null) {
                seriesData = criteria.list();
            } else {
                seriesData.addAll(criteria.list());
            }

            int currentSize = seriesData.size();
            size = size + currentSize;

            transaction.commit();
        }

        return seriesData;
    }

}