package eu.hydrologis.edc.annotatedclasses.timeseries.relativehumidity;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = "series_relativehumidity_2002", schema = "edcseries")
@org.hibernate.annotations.Table(appliesTo = "series_relativehumidity_2002", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_series_relativehumidity_2002",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesRelativehumidity2002 extends SeriesMonitoringPointsTable {
}
