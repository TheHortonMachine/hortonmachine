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

import static eu.hydrologis.edc.utils.Constants.ACCESSORYMEASURE;
import static eu.hydrologis.edc.utils.Constants.ATTACHMENTURL;
import static eu.hydrologis.edc.utils.Constants.CREATIONDATE;
import static eu.hydrologis.edc.utils.Constants.DEPTH;
import static eu.hydrologis.edc.utils.Constants.DISTANCE;
import static eu.hydrologis.edc.utils.Constants.DYNAMICMONITORINGPOINTS_ID;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ELEVATION;
import static eu.hydrologis.edc.utils.Constants.EPSG;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LATITUDE;
import static eu.hydrologis.edc.utils.Constants.LONGITUDE;
import static eu.hydrologis.edc.utils.Constants.MAXERROR;
import static eu.hydrologis.edc.utils.Constants.MAXRANGE;
import static eu.hydrologis.edc.utils.Constants.MEASURES;
import static eu.hydrologis.edc.utils.Constants.MINERROR;
import static eu.hydrologis.edc.utils.Constants.MINRANGE;
import static eu.hydrologis.edc.utils.Constants.PRIMARYMEASURE;
import static eu.hydrologis.edc.utils.Constants.PROGRESSIVE;
import static eu.hydrologis.edc.utils.Constants.REPORTURL;
import static eu.hydrologis.edc.utils.Constants.SURVEYS_ID;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * Table of measures.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 *
 */
@Entity
@Table(name = MEASURES, schema = EDC_SCHEMA)
public class MeasuresTable {
    /**
     * A unique id for the measure. 
     */
    @Id
    @GeneratedValue(strategy=GenerationType.SEQUENCE)
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The survey this measure is part of.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SURVEYS_ID, referencedColumnName = ID, nullable = false)
    private SurveysTable survey;

    /**
     * The dynamic monitoring point that took this measure.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DYNAMICMONITORINGPOINTS_ID, referencedColumnName = ID, nullable = false)
    private DynamicMonitoringPointsTable dynamicMonitoringPoint;

    /**
     * The creation date of the measure.
     */
    @Column(name = CREATIONDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime creationDate;

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
     * The elevation of the measure.
     */
    @Column(name = ELEVATION, nullable = false)
    private Double elevation;

    /**
     * The progressive of the measure.
     */
    @Column(name = PROGRESSIVE, nullable = false)
    private Double progressive;

    /**
     * The distance of the measure.
     */
    @Column(name = DISTANCE, nullable = false)
    private Double distance;

    /**
     * The depth of the measure.
     */
    @Column(name = DEPTH, nullable = false)
    private Double depth;

    /**
     * The primary measure.
     */
    @Column(name = PRIMARYMEASURE, nullable = false)
    private Double primaryMeasure;

    /**
     * The accessory measure.
     */
    @Column(name = ACCESSORYMEASURE, nullable = false)
    private Double accessoryMeasure;

    /**
     * The maximum range for the measure.
     */
    @Column(name = MAXRANGE, nullable = true)
    private Double maxRange;

    /**
     * The minimum range for the measure.
     */
    @Column(name = MINRANGE, nullable = true)
    private Double minRange;

    /**
     * The maximum error for the measure.
     */
    @Column(name = MAXERROR, nullable = true)
    private Double maxError;

    /**
     * The minimum error for the measure.
     */
    @Column(name = MINERROR, nullable = true)
    private Double minError;

    /**
     * The unit of the measure.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable unit;

    /**
     * The url to the location of reports, if available.
     */
    @Column(name = REPORTURL, nullable = true)
    private String reportUrlString;

    /**
     * The url to the location of the attachments, if available.
     */
    @Column(name = ATTACHMENTURL, nullable = true)
    private String attachmentUrlString;

    /*
     * =======================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public SurveysTable getSurvey() {
        return survey;
    }

    public void setSurvey( SurveysTable survey ) {
        this.survey = survey;
    }

    public DynamicMonitoringPointsTable getDynamicMonitoringPoint() {
        return dynamicMonitoringPoint;
    }

    public void setDynamicMonitoringPoint( DynamicMonitoringPointsTable dynamicMonitoringPoint ) {
        this.dynamicMonitoringPoint = dynamicMonitoringPoint;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate( DateTime creationDate ) {
        this.creationDate = creationDate;
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

    public Double getProgressive() {
        return progressive;
    }

    public void setProgressive( Double progressive ) {
        this.progressive = progressive;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance( Double distance ) {
        this.distance = distance;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth( Double depth ) {
        this.depth = depth;
    }

    public Double getPrimaryMeasure() {
        return primaryMeasure;
    }

    public void setPrimaryMeasure( Double primaryMeasure ) {
        this.primaryMeasure = primaryMeasure;
    }

    public Double getAccessoryMeasure() {
        return accessoryMeasure;
    }

    public void setAccessoryMeasure( Double accessoryMeasure ) {
        this.accessoryMeasure = accessoryMeasure;
    }

    public Double getMaxRange() {
        return maxRange;
    }

    public void setMaxRange( Double maxRange ) {
        this.maxRange = maxRange;
    }

    public Double getMinRange() {
        return minRange;
    }

    public void setMinRange( Double minRange ) {
        this.minRange = minRange;
    }

    public Double getMaxError() {
        return maxError;
    }

    public void setMaxError( Double maxError ) {
        this.maxError = maxError;
    }

    public Double getMinError() {
        return minError;
    }

    public void setMinError( Double minError ) {
        this.minError = minError;
    }

    public UnitsTable getUnit() {
        return unit;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }

    public String getReportUrlString() {
        return reportUrlString;
    }

    public void setReportUrlString( String reportUrlString ) {
        this.reportUrlString = reportUrlString;
    }

    public String getAttachmentUrlString() {
        return attachmentUrlString;
    }

    public void setAttachmentUrlString( String attachmentUrlString ) {
        this.attachmentUrlString = attachmentUrlString;
    }
}
