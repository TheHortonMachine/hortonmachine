package eu.hydrologis.edc.annotatedclasses.timeseries.winddirection;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_winddirection_1995", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_winddirection_1995", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_winddirection_1995",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesWinddirection1995 extends SeriesMonitoringPointsTable {
}
