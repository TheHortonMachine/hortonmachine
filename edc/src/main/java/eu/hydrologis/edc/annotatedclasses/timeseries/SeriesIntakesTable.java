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
import static eu.hydrologis.edc.utils.Constants.INTAKES_ID;
import static eu.hydrologis.edc.utils.Constants.RELIABILITY_ID;
import static eu.hydrologis.edc.utils.Constants.SERIES_INTAKES;
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
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import eu.hydrologis.edc.annotatedclasses.IntakesTable;
import eu.hydrologis.edc.annotatedclasses.ReliabilityTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;

/**
 * The intakes series Table.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = SERIES_INTAKES, schema = EDCSERIES_SCHEMA)
@org.hibernate.annotations.Table(appliesTo = SERIES_INTAKES, indexes = @org.hibernate.annotations.Index(name = "IDX_TIMESTAMP_INTAKE_series_intakes", columnNames = {
        TIMESTAMPUTC, INTAKES_ID}))
public class SeriesIntakesTable {
    /**
     * The unique id of the intakes value entry.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The intake generating the value.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = INTAKES_ID, referencedColumnName = ID, nullable = false)
    private IntakesTable intake;

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
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable unit;

    /*
     * =================================================================
     */

    @SuppressWarnings("nls")
    @Transient
    public String toString() {
        StringBuilder retValue = new StringBuilder();

        retValue.append("Series ( \n").append("id = ").append(this.id).append(" / intakes = ")
                .append(this.getIntake().getId()).append(" / timestampUtc = ").append(
                        this.timestampUtc).append(" / value = ").append(this.value).append(
                        " / timeStep = ").append(this.timeStep).append(" / reliability = ").append(
                        this.reliability).append(" / unit = ").append(this.getUnit().getId())
                .append(" )");

        return retValue.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setIntake( IntakesTable intake ) {
        this.intake = intake;
    }

    public IntakesTable getIntake() {
        return intake;
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
