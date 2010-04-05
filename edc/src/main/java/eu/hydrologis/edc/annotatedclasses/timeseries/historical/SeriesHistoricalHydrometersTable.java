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
package eu.hydrologis.edc.annotatedclasses.timeseries.historical;

import static eu.hydrologis.edc.utils.Constants.DAY;
import static eu.hydrologis.edc.utils.Constants.EDCSERIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.HYDROMETER_ID;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.MONTH;
import static eu.hydrologis.edc.utils.Constants.SERIES_HISTORICALHYDROMETERS;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;
import static eu.hydrologis.edc.utils.Constants.VALUE;
import static eu.hydrologis.edc.utils.Constants.VALUEDESCRIPTION_ID;
import static eu.hydrologis.edc.utils.Constants.YEAR;

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

import eu.hydrologis.edc.annotatedclasses.HydrometersTable;
import eu.hydrologis.edc.annotatedclasses.UnitsTable;
import eu.hydrologis.edc.annotatedclasses.ValueDescriptionTable;

/**
 * The historical hydrometers series Table.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = SERIES_HISTORICALHYDROMETERS, schema = EDCSERIES_SCHEMA)
@org.hibernate.annotations.Table(appliesTo = SERIES_HISTORICALHYDROMETERS, 
        indexes = @org.hibernate.annotations.Index(
                name = "IDX_TIMESTAMP_HISTHYDROM_" + SERIES_HISTORICALHYDROMETERS,
                columnNames = {HYDROMETER_ID, YEAR, MONTH, DAY}
))
public class SeriesHistoricalHydrometersTable {
    /**
     * The unique id of the historical hydrometer value entry.
     */
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The historical hydrometer generating the value.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = HYDROMETER_ID, referencedColumnName = ID, nullable = false)
    private HydrometersTable hydrometer;

    /**
     * The year.
     */
    @Column(name = YEAR, nullable = false)
    private Integer year;
    
    /**
     * The month.
     */
    @Column(name = MONTH, nullable = false)
    private Integer month;

    /**
     * The day.
     */
    @Column(name = DAY, nullable = false)
    private Integer day;

    /**
     * The data value.
     */
    @Column(name = VALUE, nullable = false)
    private Double value;
    
    /**
     * The type of the discharge data.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = VALUEDESCRIPTION_ID, referencedColumnName = ID, nullable = false)
    private ValueDescriptionTable valueDescription;
    
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

        retValue.append("Series ( \n")
        .append("id = ").append(this.id)
        .append(" / historical hydrometer = ").append(this.getHydrometer().getId())
        .append(" / year = ").append(this.year)
        .append(" / month = ").append(this.month)
        .append(" / day = ").append(this.day)
        .append(" / value = ").append(this.value)
        .append(" / type = ").append(this.valueDescription.getId())
        .append(" / unit = ").append(this.getUnit().getId())
        .append(" )");

        return retValue.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public HydrometersTable getHydrometer() {
        return hydrometer;
    }

    public void setHydrometer( HydrometersTable hydrometer ) {
        this.hydrometer = hydrometer;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear( Integer year ) {
        this.year = year;
    }

    public Integer getMonth() {
        return month;
    }

    public void setMonth( Integer month ) {
        this.month = month;
    }

    public Integer getDay() {
        return day;
    }

    public void setDay( Integer day ) {
        this.day = day;
    }

    public Double getValue() {
        return value;
    }

    public void setValue( Double value ) {
        this.value = value;
    }

    public ValueDescriptionTable getValueDescription() {
        return valueDescription;
    }

    public void setValueDescription( ValueDescriptionTable valueDescription ) {
        this.valueDescription = valueDescription;
    }

    public UnitsTable getUnit() {
        return unit;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }



}
