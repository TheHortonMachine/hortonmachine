package eu.hydrologis.edc.annotatedclasses.timeseries.pressure;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_pressure_2015", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_pressure_2015", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_pressure_2015",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesPressure2015 extends SeriesMonitoringPointsTable {
}
