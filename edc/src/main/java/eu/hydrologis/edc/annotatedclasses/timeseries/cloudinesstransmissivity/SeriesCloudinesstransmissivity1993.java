package eu.hydrologis.edc.annotatedclasses.timeseries.cloudinesstransmissivity;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_cloudinesstransmissivity_1993", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_cloudinesstransmissivity_1993", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_cloudinesstransmissivity_1993",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesCloudinesstransmissivity1993 extends SeriesMonitoringPointsTable {
}
