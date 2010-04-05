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
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.DISTRICT;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.*;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.MUNICIPALITY;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.POI;
import static eu.hydrologis.edc.utils.Constants.PROVINCE;
import static eu.hydrologis.edc.utils.Constants.RESORT;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The Point of Interest Table.
 * 
 * <p>
 * This table holds the data for points of interest, like for example stations 
 * or dams, which have a unique position and administrative description
 * but may provide different datatypes. The different datatypes are 
 * handled their own tables, like for example {@link MonitoringPointsTable}, 
 * {@link ReservoirsTable} and {@link HydrometersTable}.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = POI, schema = EDC_SCHEMA)
public class PoiTable {
    /**
     * The unique id of the poi.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The name of the poi.
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * A description of the poi.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The name of the municipality the poi is located in.
     */
    @Column(name = MUNICIPALITY, nullable = true)
    private String municipality;

    /**
     * The name of the province the poi is located in.
     */
    @Column(name = PROVINCE, nullable = true)
    private String province;

    /**
     * The name of the district the poi is located in.
     */
    @Column(name = DISTRICT, nullable = true)
    private String district;

    /**
     * The name of the resort the poi is located in.
     */
    @Column(name = RESORT, nullable = true)
    private String resort;

    /**
     * The name of the agency that handles the poi.
     */
    @Column(name = AGENCY, nullable = true)
    private String agency;

    /**
     * The elevation of the poi.
     */
    @Column(name = ELEVATION, nullable = false)
    private Double elevation;

    /**
     * The skyview factor of the poi.
     */
    @Column(name = SKY, nullable = true)
    private Double sky;

    /*
     * ===================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public String getMunicipality() {
        return municipality;
    }

    public void setMunicipality( String municipality ) {
        this.municipality = municipality;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince( String province ) {
        this.province = province;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict( String district ) {
        this.district = district;
    }

    public String getResort() {
        return resort;
    }

    public void setResort( String resort ) {
        this.resort = resort;
    }

    public String getAgency() {
        return agency;
    }

    public void setAgency( String agency ) {
        this.agency = agency;
    }

    public Double getElevation() {
        return elevation;
    }

    public void setElevation( Double elevation ) {
        this.elevation = elevation;
    }

    public void setSky( Double sky ) {
        this.sky = sky;
    }

    public Double getSky() {
        return sky;
    }

}
