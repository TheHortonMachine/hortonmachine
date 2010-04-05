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
import static eu.hydrologis.edc.utils.Constants.PARAMETERSSET;
import static eu.hydrologis.edc.utils.Constants.VALUE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;

/**
 * The runs parameters sets Table.
 * 
 * <p>
 * The table defines sets of parameters that were used in a run.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = PARAMETERSSET, schema = EDC_SCHEMA)
@IdClass(ParametersSetTablePK.class)
public class ParametersSetTable {
    
    @Id
    private RunsTable run;

    /**
     * The id of the parameter defined in the record.
     */
    @Id
    private ParametersTable parameter;

    /**
     * The value for the parameter.
     * 
     * <p>
     * In the case this value is null, the default value from 
     * the {@link ParametersTable} will be used.
     * </p>
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

    public void setParameter( ParametersTable parameter ) {
        this.parameter = parameter;
    }

    public ParametersTable getParameter() {
        return parameter;
    }

    public void setValue( Double value ) {
        this.value = value;
    }

    public Double getValue() {
        return value;
    }

}
