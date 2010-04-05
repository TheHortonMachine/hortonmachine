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

import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.LANDCOVERPARAMETERSSET;
import static eu.hydrologis.edc.utils.Constants.VALUE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * The {@link LandcoverCategoriesTable landcover} parameters set Table.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = LANDCOVERPARAMETERSSET, schema = EDC_SCHEMA)
@IdClass(LandcoverParametersSetTablePK.class)
public class LandcoverParametersSetTable {
    /**
     * The id of the run.
     */
    @Id
    private RunsTable run;

    /**
     * The id of the landcover category.
     */
    @Id
    private LandcoverCategoriesTable landcoverCategory;

    /**
     * The id of the landcover parameter.
     */
    @Id
    private LandcoverParametersTable landcoverParameter;

    /**
     * The value for the landcover parameter.
     */
    @Column(name = VALUE, nullable = true)
    private Double value;

    /*
     * =====================================
     */

    public void setRun( RunsTable run ) {
        this.run = run;
    }

    public RunsTable getRun() {
        return run;
    }

    public void setLandcoverCategory( LandcoverCategoriesTable landcoverCategory ) {
        this.landcoverCategory = landcoverCategory;
    }

    public LandcoverCategoriesTable getLandcoverCategory() {
        return landcoverCategory;
    }

    public void setLandcoverParameter( LandcoverParametersTable landcoverParameter ) {
        this.landcoverParameter = landcoverParameter;
    }

    public LandcoverParametersTable getLandcoverParameter() {
        return landcoverParameter;
    }

    public void setValue( Double value ) {
        this.value = value;
    }

    public Double getValue() {
        return value;
    }


}
