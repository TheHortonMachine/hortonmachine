package eu.hydrologis.edc.annotatedclasses.timeseries.windspeed;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_windspeed_2001", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_windspeed_2001", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_windspeed_2001",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesWindspeed2001 extends SeriesMonitoringPointsTable {
}
