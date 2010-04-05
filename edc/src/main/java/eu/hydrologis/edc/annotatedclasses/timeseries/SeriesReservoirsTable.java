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

import static eu.hydrologis.edc.utils.Constants.EDCSERIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.OUTLETDISCHARGE;
import static eu.hydrologis.edc.utils.Constants.OUTLETDISCHARGEUNITS_ID;
import static eu.hydrologis.edc.utils.Constants.RELIABILITY_ID;
import static eu.hydrologis.edc.utils.Constants.RESERVOIRS_ID;
import static eu.hydrologis.edc.utils.Constants.SERIES_RESERVOIRS;
import static eu.hydrologis.edc.utils.Constants.TIMESTAMPUTC;
import static eu.hydrologis.edc.utils.Constants.TIMESTEP;
import static eu.hydrologis.edc.utils.Constants.WATERLEVEL;
import static eu.hydrologis.edc.utils.Constants.WATERLEVELUNITS_ID;
import static eu.hydrologis.edc.utils.Constants.WHIRLEDDISCHARGE;
import static eu.hydrologis.edc.utils.Constants.WHIRLEDDISCHARGEUNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.ReservoirsTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;

/**
 * The reservoirs series Table.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = SERIES_RESERVOIRS, schema = EDCSERIES_SCHEMA)
@org.hibernate.annotations.Table(appliesTo = SERIES_RESERVOIRS, indexes = @org.hibernate.annotations.Index(name = "IDX_TIMESTAMP_RESERVOIR"
        + SERIES_RESERVOIRS, columnNames = {TIMESTAMPUTC, RESERVOIRS_ID}))
public class SeriesReservoirsTable {
    /**
     * The unique id of the reservoir value entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The reservoir generating the value.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RESERVOIRS_ID, referencedColumnName = ID, nullable = false)
    private ReservoirsTable reservoir;

    /**
     * The timestamp of the data in UTC.
     */
    @Column(name = TIMESTAMPUTC, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime timestampUtc;

    /**
     * The waterlevel value.
     */
    @Column(name = WATERLEVEL, nullable = true)
    private Double waterLevel;

    /**
     * The waterlevel's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = WATERLEVELUNITS_ID, referencedColumnName = ID, nullable = true)
    private UnitsTable waterLevelUnit;

    /**
     * The whirleddischarge value.
     */
    @Column(name = WHIRLEDDISCHARGE, nullable = true)
    private Double whirledDischarge;

    /**
     * The whirleddischarge's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = WHIRLEDDISCHARGEUNITS_ID, referencedColumnName = ID, nullable = true)
    private UnitsTable whirledDischargeUnit;

    /**
     * The outletdischarge value.
     */
    @Column(name = OUTLETDISCHARGE, nullable = true)
    private Double outletDischarge;

    /**
     * The outletdischarge's unit.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = OUTLETDISCHARGEUNITS_ID, referencedColumnName = ID, nullable = true)
    private UnitsTable outletDischargeUnit;

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

    /*
     * =================================================================
     */

    @SuppressWarnings("nls")
    @Transient
    public String toString() {
        StringBuilder retValue = new StringBuilder();

        retValue.append("Series ( \n").append("id = ").append(this.id).append(" / reservoir = ")
                .append(this.getReservoir().getId()).append(" / timestampUtc = ").append(
                        this.timestampUtc).append(" / waterlevelvalue = ").append(this.waterLevel)
                .append(" / waterlevelunit = ").append(this.getWaterLevelUnit().getId()).append(
                        " / whirleddischargevalue = ").append(this.whirledDischarge).append(
                        " / whirleddischargeunit = ")
                .append(this.getWhirledDischargeUnit().getId())
                .append(" / outletdischargevalue = ").append(this.outletDischarge).append(
                        " / outletdischargeunit = ").append(this.getOutletDischargeUnit().getId())
                .append(" / timeStep = ").append(this.timeStep).append(" / reliability = ").append(
                        this.getReliability()).append(" )");

        return retValue.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setReservoir( ReservoirsTable reservoir ) {
        this.reservoir = reservoir;
    }

    public ReservoirsTable getReservoir() {
        return reservoir;
    }

    public DateTime getTimestampUtc() {
        return timestampUtc;
    }

    public void setTimestampUtc( DateTime timestampUtc ) {
        this.timestampUtc = timestampUtc;
    }

    public Double getWaterLevel() {
        return waterLevel;
    }

    public void setWaterLevel( Double waterLevel ) {
        this.waterLevel = waterLevel;
    }

    public UnitsTable getWaterLevelUnit() {
        return waterLevelUnit;
    }

    public void setWaterLevelUnit( UnitsTable waterLevelUnit ) {
        this.waterLevelUnit = waterLevelUnit;
    }

    public Double getWhirledDischarge() {
        return whirledDischarge;
    }

    public void setWhirledDischarge( Double whirledDischarge ) {
        this.whirledDischarge = whirledDischarge;
    }

    public UnitsTable getWhirledDischargeUnit() {
        return whirledDischargeUnit;
    }

    public void setWhirledDischargeUnit( UnitsTable whirledDischargeUnit ) {
        this.whirledDischargeUnit = whirledDischargeUnit;
    }

    public Double getOutletDischarge() {
        return outletDischarge;
    }

    public void setOutletDischarge( Double outletDischarge ) {
        this.outletDischarge = outletDischarge;
    }

    public UnitsTable getOutletDischargeUnit() {
        return outletDischargeUnit;
    }

    public void setOutletDischargeUnit( UnitsTable outletDischargeUnit ) {
        this.outletDischargeUnit = outletDischargeUnit;
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

}
