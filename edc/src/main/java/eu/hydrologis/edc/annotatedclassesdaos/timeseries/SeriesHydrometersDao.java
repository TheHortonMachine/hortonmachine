package eu.hydrologis.edc.annotatedclassesdaos.timeseries;

import org.joda.time.DateTime;

import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesHydrometersTable;
import eu.hydrologis.edc.annotatedclassesdaos.AbstractEdcDao;
import eu.hydrologis.edc.databases.EdcSessionFactory;

public class SeriesHydrometersDao extends AbstractEdcDao {

    public SeriesHydrometersDao( EdcSessionFactory edcSessionFactory ) {
        super(edcSessionFactory);
    }

    protected void processLine( String[] lineSplit ) {
        SeriesHydrometersTable hdT = new SeriesHydrometersTable();

        Long id = Long.parseLong(lineSplit[0].trim());
        hdT.setId(id);

        String hydrometer_id = lineSplit[1].trim();
        if (hydrometer_id.length() > 0) {
            HydrometersTable hT = new HydrometersTable();
            hT.setId(Long.parseLong(hydrometer_id));
            hdT.setHydrometer(hT);
        }

        String timestampUtcStr = lineSplit[2].trim();
        if (timestampUtcStr.length() > 0) {
            DateTime timeStampUtc = formatter.parseDateTime(timestampUtcStr);
            hdT.setTimestampUtc(timeStampUtc);
        }

        String value = lineSplit[3].trim();
        if (value.length() > 0) {
            hdT.setValue(Double.parseDouble(value));
        }

        String timeStepStr = lineSplit[4].trim();
        if (timeStepStr.length() > 0) {
            hdT.setTimeStep(Double.parseDouble(timeStepStr));
        }

        String reliabilityStr = lineSplit[5].trim();
        if (reliabilityStr.length() > 0) {
            ReliabilityTable rT = new ReliabilityTable();
            rT.setId(new Long(reliabilityStr));
            hdT.setReliability(rT);
        }

        String unit = lineSplit[6].trim();
        if (unit.length() > 0) {
            UnitsTable unitsTable = new UnitsTable();
            unitsTable.setId(Long.parseLong(unit));
            hdT.setUnit(unitsTable);
        }

        session.save(hdT);
    }

    public String getRecordDefinition() throws Exception {
        StringBuilder sB = new StringBuilder();
        sB.append(tableAnnotationToString(SeriesHydrometersTable.class)).append(": ");
        sB.append(columnAnnotationToString(SeriesHydrometersTable.class, "id")).append(", ");
        sB.append(joinColumnAnnotationToString(SeriesHydrometersTable.class, "hydrometer")).append(
                ", ");
        sB.append(columnAnnotationToString(SeriesHydrometersTable.class, "timestampUtc")).append(
                ", ");
        sB.append(columnAnnotationToString(SeriesHydrometersTable.class, "value")).append(", ");
        sB.append(columnAnnotationToString(SeriesHydrometersTable.class, "timeStep")).append(", ");
        sB.append(joinColumnAnnotationToString(SeriesHydrometersTable.class, "reliability")).append(
                ", ");
        sB.append(joinColumnAnnotationToString(SeriesHydrometersTable.class, "unit"));
        return sB.toString();
    }
}
