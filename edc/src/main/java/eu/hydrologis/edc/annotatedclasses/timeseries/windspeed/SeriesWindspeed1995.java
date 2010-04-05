package eu.hydrologis.edc.annotatedclasses.timeseries.windspeed;

import javax.persistence.Entity;
import javax.persistence.Table;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_windspeed_1995", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_windspeed_1995", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_windspeed_1995",
                columnNames = {"timestamputc","monitoringpoints_id"}
))
public class SeriesWindspeed1995 extends SeriesMonitoringPointsTable {
}
