package eu.hydrologis.edc.annotatedclasses.timeseries.temperature;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_temperature_2004", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_temperature_2004", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_temperature_2004",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesTemperature2004 extends SeriesMonitoringPointsTable {
}
