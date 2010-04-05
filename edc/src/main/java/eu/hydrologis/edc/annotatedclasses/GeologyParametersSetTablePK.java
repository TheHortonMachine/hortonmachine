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

import static eu.hydrologis.edc.utils.Constants.GEOLOGYCATEGORIES_ID;
import static eu.hydrologis.edc.utils.Constants.GEOLOGYPARAMETERS_ID;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.RUNS_ID;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * The compound primary key for the {@link GeologyParametersSetTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Embeddable
public class GeologyParametersSetTablePK implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The id of the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RUNS_ID, referencedColumnName = ID, nullable = false)
    private RunsTable run;

    /**
     * The id of the geology category.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = GEOLOGYCATEGORIES_ID, referencedColumnName = ID, nullable = false)
    private GeologyCategoriesTable geologyCategory;

    /**
     * The id of the geology parameter.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = GEOLOGYPARAMETERS_ID, referencedColumnName = ID, nullable = false)
    private GeologyParametersTable geologyParameter;

    public GeologyParametersSetTablePK() {
    }

    public GeologyParametersSetTablePK( RunsTable run,
            GeologyCategoriesTable geologyCategory, GeologyParametersTable geologyParameter ) {
        this.run = run;
        this.geologyCategory = geologyCategory;
        this.geologyParameter = geologyParameter;
    }

    public boolean equals( Object obj ) {
        if (obj instanceof GeologyParametersSetTablePK) {
            GeologyParametersSetTablePK other = (GeologyParametersSetTablePK) obj;
            RunsTable otherRun = other.getRun();
            GeologyCategoriesTable otherGeologyCategory = other.getGeologyCategory();
            GeologyParametersTable otherGeologyParameter = other.getGeologyParameter();
            if (otherRun == null || otherGeologyCategory == null
                    || otherGeologyParameter == null) {
                return false;
            }
            Long otherGeologyCategoryId = otherGeologyCategory.getId();
            if (otherGeologyCategoryId == null) {
                return false;
            }
            Long otherGeologyParameterId = otherGeologyParameter.getId();
            if (otherGeologyParameterId == null) {
                return false;
            }
            Long otherRunId = otherRun.getId();
            if (otherRunId == null) {
                return false;
            }
            if (run.getId() != otherRunId) {
                return false;
            }
            if (geologyCategory.getId() != otherGeologyCategoryId) {
                return false;
            }
            if (geologyParameter.getId() != otherGeologyParameterId) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int code = 0;
        code = code + run.getId().intValue();
        if (geologyCategory != null) {
            code = code + geologyCategory.getId().intValue();
        }
        if (geologyParameter != null) {
            code = code + geologyParameter.getId().intValue();
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

    public void setGeologyCategory( GeologyCategoriesTable geologyCategory ) {
        this.geologyCategory = geologyCategory;
    }

    public GeologyCategoriesTable getGeologyCategory() {
        return geologyCategory;
    }

    public void setGeologyParameter( GeologyParametersTable geologyParameter ) {
        this.geologyParameter = geologyParameter;
    }

    public GeologyParametersTable getGeologyParameter() {
        return geologyParameter;
    }



}
