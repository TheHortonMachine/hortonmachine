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
import static eu.hydrologis.edc.utils.Constants.LANDCOVERCATEGORIES_ID;
import static eu.hydrologis.edc.utils.Constants.LANDCOVERPARAMETERS_ID;
import static eu.hydrologis.edc.utils.Constants.RUNS_ID;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * The compound primary key for the {@link LandcoverParametersSetTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Embeddable
public class LandcoverParametersSetTablePK implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The id of the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RUNS_ID, referencedColumnName = ID, nullable = false)
    private RunsTable run;

    /**
     * The id of the landcover category.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LANDCOVERCATEGORIES_ID, referencedColumnName = ID, nullable = false)
    private LandcoverCategoriesTable landcoverCategory;

    /**
     * The id of the landcover parameter.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LANDCOVERPARAMETERS_ID, referencedColumnName = ID, nullable = false)
    private LandcoverParametersTable landcoverParameter;

    public LandcoverParametersSetTablePK() {
    }

    public LandcoverParametersSetTablePK( RunsTable run,
            LandcoverCategoriesTable landcoverCategory, LandcoverParametersTable landcoverParameter ) {
        this.run = run;
        this.landcoverCategory = landcoverCategory;
        this.landcoverParameter = landcoverParameter;
    }

    public boolean equals( Object obj ) {
        if (obj instanceof LandcoverParametersSetTablePK) {
            LandcoverParametersSetTablePK other = (LandcoverParametersSetTablePK) obj;
            RunsTable otherRun = other.getRun();
            LandcoverCategoriesTable otherLandcoverCategory = other.getLandcoverCategory();
            LandcoverParametersTable otherLandcoverParameter = other.getLandcoverParameter();
            if (otherRun == null || otherLandcoverCategory == null
                    || otherLandcoverParameter == null) {
                return false;
            }
            Long otherLandcoverCategoryId = otherLandcoverCategory.getId();
            if (otherLandcoverCategoryId == null) {
                return false;
            }
            Long otherLandcoverParameterId = otherLandcoverParameter.getId();
            if (otherLandcoverParameterId == null) {
                return false;
            }
            Long otherRunId = otherRun.getId();
            if (otherRunId == null) {
                return false;
            }
            if (run.getId() != otherRunId) {
                return false;
            }
            if (landcoverCategory.getId() != otherLandcoverCategoryId) {
                return false;
            }
            if (landcoverParameter.getId() != otherLandcoverParameterId) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int code = 0;
        code = code + run.getId().intValue();
        if (landcoverCategory != null) {
            code = code + landcoverCategory.getId().intValue();
        }
        if (landcoverParameter != null) {
            code = code + landcoverParameter.getId().intValue();
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

}
