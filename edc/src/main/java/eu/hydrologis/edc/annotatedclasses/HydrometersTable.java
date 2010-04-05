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

import static eu.hydrologis.edc.utils.Constants.AGENCY;
import static eu.hydrologis.edc.utils.Constants.CODE;
import static eu.hydrologis.edc.utils.Constants.DATASOURCE_ID;
import static eu.hydrologis.edc.utils.Constants.DATATYPE_ID;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ENDDATE;
import static eu.hydrologis.edc.utils.Constants.HYDROMETERS;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.POINTTYPE_ID;
import static eu.hydrologis.edc.utils.Constants.POI_ID;
import static eu.hydrologis.edc.utils.Constants.STARTDATE;
import static eu.hydrologis.edc.utils.Constants.STATUS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * The hydrometers Table.
 * 
 * <p>
 * This table holds the data for the hydrometers.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = HYDROMETERS, schema = EDC_SCHEMA)
public class HydrometersTable {
    /**
     * The unique id of the hydrometer.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The poi this hydrometer is part of.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = POI_ID, referencedColumnName = ID, nullable = false)
    private PoiTable poi;

    /**
     * A short description for the hydrometer.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The creation date of the hydrometer.
     */
    @Column(name = STARTDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime startDate;

    /**
     * The deactivation date of the hydrometer.
     */
    @Column(name = ENDDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime endDate;

    /**
     * The name of the agency that provides the data taken by this hydrometer.
     */
    @Column(name = AGENCY, nullable = false)
    private String agency;

    /**
     * The administrative code given to this hydrometer.
     */
    @Column(name = CODE, nullable = true)
    private String code;

    /**
     * The type of this point.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = POINTTYPE_ID, referencedColumnName = ID, nullable = false)
    private PointTypeTable pointType;

    /**
     * The current status of this point.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = STATUS_ID, referencedColumnName = ID, nullable = false)
    private StatusTable status;

    /**
     * The datasource from which the current available data were taken.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DATASOURCE_ID, referencedColumnName = ID, nullable = false)
    private DataSourceTable dataSource;

    /**
     * The data type that are provided by this hydrometer.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DATATYPE_ID, referencedColumnName = ID, nullable = false)
    private DataTypeTable dataType;

    /*
     * ====================================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public PoiTable getPoi() {
        return poi;
    }

    public void setPoi( PoiTable poi ) {
        this.poi = poi;
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

    public String getAgency() {
        return agency;
    }

    public void setAgency( String agency ) {
        this.agency = agency;
    }

    public String getCode() {
        return code;
    }

    public void setCode( String code ) {
        this.code = code;
    }

    public PointTypeTable getPointType() {
        return pointType;
    }

    public void setPointType( PointTypeTable pointType ) {
        this.pointType = pointType;
    }

    public StatusTable getStatus() {
        return status;
    }

    public void setStatus( StatusTable status ) {
        this.status = status;
    }

    public DataSourceTable getDataSource() {
        return dataSource;
    }

    public void setDataSource( DataSourceTable dataSource ) {
        this.dataSource = dataSource;
    }

    public DataTypeTable getDataType() {
        return dataType;
    }

    public void setDataType( DataTypeTable dataType ) {
        this.dataType = dataType;
    }

}
