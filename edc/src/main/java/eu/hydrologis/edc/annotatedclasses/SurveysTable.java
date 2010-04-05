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
package eu.hydrologis.edc.annotatedclasses;

import static eu.hydrologis.edc.utils.Constants.AREA;
import static eu.hydrologis.edc.utils.Constants.CODE;
import static eu.hydrologis.edc.utils.Constants.DENOMINATION;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ELEVATION;
import static eu.hydrologis.edc.utils.Constants.ENDDATE;
import static eu.hydrologis.edc.utils.Constants.EPSG;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LATITUDE;
import static eu.hydrologis.edc.utils.Constants.LONGITUDE;
import static eu.hydrologis.edc.utils.Constants.STARTDATE;
import static eu.hydrologis.edc.utils.Constants.SURVEYS;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * Table of dynamic sensors.
 * 
 * <p>
 * Dynamic here is related to the fact, that the 
 * sensors have no fixed position and can collect 
 * data both in time and on the vertical axis.
 * In one point more measure can therefore be taken. 
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 *
 */
@Entity
@Table(name = SURVEYS, schema = EDC_SCHEMA)
public class SurveysTable {
    /**
     * A unique id for the sensor. 
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The area covered by the survey. 
     */
    @Column(name = AREA, nullable = true)
    private Double area;

    /**
     * The longitude
     */
    @Column(name = LONGITUDE, nullable = false)
    private Double longitude;

    /**
     * The latitude
     */
    @Column(name = LATITUDE, nullable = false)
    private Double latitude;

    /**
     * The epsg code for the projection in which {@link #latitude} and {@link #longitude}
     * are expressed.
     */
    @Column(name = EPSG, nullable = false)
    private String epsg;

    /**
     * The elevation of the survey
     */
    @Column(name = ELEVATION, nullable = false)
    private Double elevation;

    /**
     * The denomination of the survey
     */
    @Column(name = DENOMINATION, nullable = true)
    private String denomination;

    /**
     * The description of the survey
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The start date of the survey.
     */
    @Column(name = STARTDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime startDate;

    /**
     * The end date of the survey.
     */
    @Column(name = ENDDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime endDate;

    /**
     * The code given to the survey.
     */
    @Column(name = CODE, nullable = true)
    private String code;

    /*
     * ==================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public Double getArea() {
        return area;
    }

    public void setArea( Double area ) {
        this.area = area;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude( Double longitude ) {
        this.longitude = longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude( Double latitude ) {
        this.latitude = latitude;
    }

    public String getEpsg() {
        return epsg;
    }

    public void setEpsg( String epsg ) {
        this.epsg = epsg;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation( Double elevation ) {
        this.elevation = elevation;
    }

    public String getDenomination() {
        return denomination;
    }

    public void setDenomination( String denomination ) {
        this.denomination = denomination;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public DateTime getStartDate() {
        return startDate;
    }

    public void setStartDate( DateTime startDate ) {
        this.startDate = startDate;
    }

    public DateTime getEndDate() {
        return endDate;
    }

    public void setEndDate( DateTime endDate ) {
        this.endDate = endDate;
    }

    public String getCode() {
        return code;
    }

    public void setCode( String code ) {
        this.code = code;
    }

}
