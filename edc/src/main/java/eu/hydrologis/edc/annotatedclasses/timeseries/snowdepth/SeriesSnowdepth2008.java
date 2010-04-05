package eu.hydrologis.edc.annotatedclasses.timeseries.snowdepth;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_snowdepth_2008", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_snowdepth_2008", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_snowdepth_2008",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesSnowdepth2008 extends SeriesMonitoringPointsTable {
}
