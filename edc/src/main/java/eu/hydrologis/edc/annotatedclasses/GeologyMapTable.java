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

import static eu.hydrologis.edc.utils.Constants.CREATIONDATE;
import static eu.hydrologis.edc.utils.Constants.DATASOURCE_ID;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EAST;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.EPSG;
import static eu.hydrologis.edc.utils.Constants.FIELDNAME;
import static eu.hydrologis.edc.utils.Constants.GEOLOGYMAP;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.MAPTYPE_ID;
import static eu.hydrologis.edc.utils.Constants.NORTH;
import static eu.hydrologis.edc.utils.Constants.RESOLUTION;
import static eu.hydrologis.edc.utils.Constants.SOUTH;
import static eu.hydrologis.edc.utils.Constants.URL;
import static eu.hydrologis.edc.utils.Constants.WEST;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * The table containing all geology maps references.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = GEOLOGYMAP, schema = EDC_SCHEMA)
public class GeologyMapTable {
    /**
     * The unique id of the geology map.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The description of the geology map.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The creation date of the geology map.
     */
    @Column(name = CREATIONDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime creationDate;

    /**
     * The north coordinate of the covered region.
     */
    @Column(name = NORTH, nullable = false)
    private Double north;

    /**
     * The south coordinate of the covered region.
     */
    @Column(name = SOUTH, nullable = false)
    private Double south;

    /**
     * The east coordinate of the covered region.
     */
    @Column(name = EAST, nullable = false)
    private Double east;

    /**
     * The west coordinate of the covered region.
     */
    @Column(name = WEST, nullable = false)
    private Double west;

    /**
     * The epsg code for the projection in which {@link #north}, {@link #south}, 
     * {@link #east} and {@link #west} are expressed.
     */
    @Column(name = EPSG, nullable = false)
    private String epsg;

    /**
     * The type of the map this references to.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = MAPTYPE_ID, referencedColumnName = ID, nullable = false)
    private MapTypeTable mapType;

    /**
     * The url to the physical location of the map.
     * 
     * <p>
     * This, together with the {@link #mapType} makes
     * it possible to retrieve the map.
     * </p>
     */
    @Column(name = URL, nullable = false)
    private String url;

    /**
     * The name of the geology field.
     * 
     * <p>
     * While in the case of raster maps the value 
     * of the pixel represents the soil id as it
     * is contained in {@link GeologyCategoriesTable geology},
     * in the case of vector data it is necessary to 
     * know the name of the field from which to extract 
     * the category.
     * </p>
     * 
     */
    @Column(name = FIELDNAME, nullable = true)
    private String fieldName;

    /**
     * The resolution of the map, in case it is a raster.
     */
    @Column(name = RESOLUTION, nullable = true)
    private Double resolution;

    /**
     * The datasource from which the geology map was produced.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DATASOURCE_ID, referencedColumnName = ID, nullable = false)
    private DataSourceTable dataSource;

    /*
     * ==============================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public void setCreationDate( DateTime creationDate ) {
        this.creationDate = creationDate;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public Double getNorth() {
        return north;
    }

    public void setNorth( Double north ) {
        this.north = north;
    }

    public Double getSouth() {
        return south;
    }

    public void setSouth( Double south ) {
        this.south = south;
    }

    public Double getEast() {
        return east;
    }

    public void setEast( Double east ) {
        this.east = east;
    }

    public Double getWest() {
        return west;
    }

    public void setWest( Double west ) {
        this.west = west;
    }

    public String getEpsg() {
        return epsg;
    }

    public void setEpsg( String epsg ) {
        this.epsg = epsg;
    }

    public MapTypeTable getMapType() {
        return mapType;
    }

    public void setMapType( MapTypeTable mapType ) {
        this.mapType = mapType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

    public void setFieldName( String fieldName ) {
        this.fieldName = fieldName;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setResolution( Double resolution ) {
        this.resolution = resolution;
    }

    public Double getResolution() {
        return resolution;
    }

    public void setDataSource( DataSourceTable dataSource ) {
        this.dataSource = dataSource;
    }

    public DataSourceTable getDataSource() {
        return dataSource;
    }

}
