package eu.hydrologis.edc.annotatedclasses.timeseries.shortwaveradiationdiffuse;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_shortwaveradiationdiffuse_2000", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_shortwaveradiationdiffuse_2000", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_shortwaveradiationdiffuse_2000",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesShortwaveradiationdiffuse2000 extends SeriesMonitoringPointsTable {
}
