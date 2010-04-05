package eu.hydrologis.edc.annotatedclasses.timeseries.cloudiness;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_cloudiness_2008", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_cloudiness_2008", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_cloudiness_2008",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesCloudiness2008 extends SeriesMonitoringPointsTable {
}
