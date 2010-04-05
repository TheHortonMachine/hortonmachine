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

import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.PARAMETERS_ID;
import static eu.hydrologis.edc.utils.Constants.RUNS_ID;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * The compound primary key for the {@link ParametersSetTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Embeddable
public class ParametersSetTablePK implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The id of the set.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RUNS_ID, referencedColumnName = ID, nullable = false)
    private RunsTable run;

    /**
     * The id of the parameter defined in the record.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = PARAMETERS_ID, referencedColumnName = ID, nullable = false)
    private ParametersTable parameter;

    public ParametersSetTablePK() {
    }

    public ParametersSetTablePK( RunsTable run, ParametersTable parameter ) {
        this.run = run;
        this.parameter = parameter;
    }

    public boolean equals( Object obj ) {
        if (obj instanceof ParametersSetTablePK) {
            ParametersSetTablePK other = (ParametersSetTablePK) obj;
            RunsTable otherRun = other.getRun();
            ParametersTable otherParameter = other.getParameter();
            if (otherRun == null || otherParameter == null) {
                return false;
            }
            Long otherParameterId = otherParameter.getId();
            if (otherParameterId == null) {
                return false;
            }
            Long otherRunId = otherRun.getId();
            if (otherRunId == null) {
                return false;
            }
            if (run.getId() != otherRunId) {
                return false;
            }
            if (parameter.getId() != otherParameterId) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int code = 0;
        code = code + run.getId().intValue();
        if (parameter != null) {
            code = code + parameter.getId().intValue();
        }
        return code;
    }

    /*
     * ====================
     */
    public void setRun( RunsTable run ) {
        this.run = run;
    }

    public RunsTable getRun() {
        return run;
    }

    public ParametersTable getParameter() {
        return parameter;
    }

    public void setParameter( ParametersTable parameter ) {
        this.parameter = parameter;
    }

}
