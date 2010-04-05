package eu.hydrologis.edc.annotatedclasses.timeseries.shortwaveradiationglobal;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_shortwaveradiationglobal_2005", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_shortwaveradiationglobal_2005", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_shortwaveradiationglobal_2005",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesShortwaveradiationglobal2005 extends SeriesMonitoringPointsTable {
}
