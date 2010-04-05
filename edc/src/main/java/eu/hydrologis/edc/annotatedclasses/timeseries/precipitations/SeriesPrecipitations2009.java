package eu.hydrologis.edc.annotatedclasses.timeseries.precipitations;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_precipitations_2009", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_precipitations_2009", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_precipitations_2009",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesPrecipitations2009 extends SeriesMonitoringPointsTable {
}
