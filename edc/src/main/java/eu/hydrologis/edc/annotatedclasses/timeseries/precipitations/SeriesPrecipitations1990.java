package eu.hydrologis.edc.annotatedclasses.timeseries.precipitations;

import javax.persistence.Entity;
import javax.persistence.Table;
import static eu.hydrologis.edc.utils.Constants.*;

import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;

@Entity
@Table(name = SERIES_PRECIPITATIONS+ "1990", schema = EDCSERIES_SCHEMA)
@org.hibernate.annotations.Table(appliesTo = SERIES_PRECIPITATIONS + "1990", 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_MONPOINT_"+SERIES_PRECIPITATIONS+"1990",
                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}
))
public class SeriesPrecipitations1990 extends SeriesMonitoringPointsTable {
}
