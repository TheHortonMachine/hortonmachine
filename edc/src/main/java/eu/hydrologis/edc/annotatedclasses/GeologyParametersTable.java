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

import static eu.hydrologis.edc.utils.Constants.DEFAULTVALUE;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.GEOLOGYPARAMETERS;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.PROCESSES_ID;
import static eu.hydrologis.edc.utils.Constants.UNITS_ID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The {@link GeologyCategoriesTable geology categories} parameters Table.
 * 
 * <p>
 * The table defines parameters that are related to the geology.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = GEOLOGYPARAMETERS, schema = EDC_SCHEMA)
public class GeologyParametersTable {
    /**
     * The unique id of the geology parameter.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The short name of the geology parameter.
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * A description of the geology parameter.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The unit of the geology parameter.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = UNITS_ID, referencedColumnName = ID, nullable = false)
    private UnitsTable unit;

    /**
     * A default value for the geology parameter.
     */
    @Column(name = DEFAULTVALUE, nullable = true)
    private Double defaultValue;
    
    /**
     * The physical processes this geology parameter is part of.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = PROCESSES_ID, referencedColumnName = ID, nullable = true)
    private ProcessesTable processes;

    /*
     * =====================================
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

    public UnitsTable getUnit() {
        return unit;
    }

    public void setUnit( UnitsTable unit ) {
        this.unit = unit;
    }

    public void setDefaultValue( Double defaultValue ) {
        this.defaultValue = defaultValue;
    }

    public Double getDefaultValue() {
        return defaultValue;
    }

    public void setProcesses( ProcessesTable processes ) {
        this.processes = processes;
    }

    public ProcessesTable getProcesses() {
        return processes;
    }



}
