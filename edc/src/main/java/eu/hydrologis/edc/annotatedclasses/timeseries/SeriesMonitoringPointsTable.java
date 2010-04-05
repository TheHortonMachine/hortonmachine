/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.hydrologis.edc.annotatedclasses.timeseries;

import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.MONITORINGPOINTS_ID;
import static eu.hydrologis.edc.utils.Constants.RELIABILITY_ID;
import static eu.hydrologis.edc.utils.Constants.TIMESTAMPUTC;
import static eu.hydrologis.edc.utils.Constants.TIMESTEP;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;
import static eu.hydrologis.edc.utils.Constants.VALUE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import eu.hydrologis.edc.annotatedclasses.MonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;

/**
 * The {@link MonitoringPointsTable monitoring points} series Table.
 * 
 * <p>
 * From this table all compatible monitoring points data tables inherit
 * the fields and methods. The tables are splitted into years to gain better
 * performance.
 * </p>
 * 
 * <p>
 * A note about how tables are split: the monitoring points tables 
 * are named following the pattern: <b>series_packagename_year</b>
 * so for example to get precipitation data from August 2008 to February
 * 2009 the query has to be done on the tables: <b>series_precipitations_2008</b>
 * and <b>series_precipitations_2009</b>.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@MappedSuperclass
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class SeriesMonitoringPointsTable {
    /**
     * The unique id of the precipitation entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The monitoring point generating the value.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = MONITORINGPOINTS_ID, referencedColumnName = ID, nullable = false)
    private MonitoringPointsTable monitoringPoint;

    /**
     * The timestamp of the data in UTC.
     */
    @Column(name = TIMESTAMPUTC, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime timestampUtc;

    /**
     * The data value.
     */
    @Column(name = VALUE, nullable = false)
    private Double value;

    /**
     * The time interval the data was taken.
     */
    @Column(name = TIMESTEP, nullable = false)
    private Double timeStep;

    /**
     * A measure of reliability.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RELIABILITY_ID, referencedColumnName = ID, nullable = false)
    private ReliabilityTable reliability;

    /**
     * The value's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = true)
    private UnitsTable unit;

    /*
     * =================================================================
     */

    @SuppressWarnings("nls")
    @Transient
    public String toString() {
        StringBuilder retValue = new StringBuilder();

        retValue.append("Series ( \n").append("id = ").append(this.id).append(
                " / monitoringPoint = ").append(this.monitoringPoint.getId()).append(
                " / timestampUtc = ").append(this.timestampUtc).append(" / value = ").append(
                this.value).append(" / timeStep = ").append(this.timeStep).append(
                " / reliability = ").append(this.reliability).append(" / unit = ").append(
                this.getUnit().getId()).append(" )");

        return retValue.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public MonitoringPointsTable getMonitoringPoint() {
        return monitoringPoint;
    }

    public void setMonitoringPoint( MonitoringPointsTable monitoringPoint ) {
        this.monitoringPoint = monitoringPoint;
    }

    public DateTime getTimestampUtc() {
        return timestampUtc;
    }

    public void setTimestampUtc( DateTime timestampUtc ) {
        this.timestampUtc = timestampUtc;
    }

    public Double getValue() {
        return value;
    }

    public void setValue( Double value ) {
        this.value = value;
    }

    public Double getTimeStep() {
        return timeStep;
    }

    public void setTimeStep( Double timeStep ) {
        this.timeStep = timeStep;
    }

    public void setReliability( ReliabilityTable reliability ) {
        this.reliability = reliability;
    }

    public ReliabilityTable getReliability() {
        return reliability;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }

    public UnitsTable getUnit() {
        return unit;
    }

}
