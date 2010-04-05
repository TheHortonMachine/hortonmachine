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
import static eu.hydrologis.edc.utils.Constants.LEVELS_ID;
import static eu.hydrologis.edc.utils.Constants.RUNS_ID;
import static eu.hydrologis.edc.utils.Constants.SOILTYPECATEGORIES_ID;
import static eu.hydrologis.edc.utils.Constants.SOILTYPEPARAMETERS_ID;

import java.io.Serializable;

import javax.persistence.Embeddable;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * The compound primary key for the {@link SoilTypeParametersSetTable}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Embeddable
public class SoilTypeParametersSetTablePK implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The id of the run.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = RUNS_ID, referencedColumnName = ID, nullable = false)
    private RunsTable run;

    /**
     * The id of the soil category.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SOILTYPECATEGORIES_ID, referencedColumnName = ID, nullable = false)
    private SoilTypeCategoriesTable soilTypeCategory;

    /**
     * The id of the soil parameter.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = SOILTYPEPARAMETERS_ID, referencedColumnName = ID, nullable = false)
    private SoilTypeParametersTable soilTypeParameter;

    /**
     * The id of the soil level.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = LEVELS_ID, referencedColumnName = ID, nullable = false)
    private LevelsTable soilLevel;

    public SoilTypeParametersSetTablePK() {
    }

    public SoilTypeParametersSetTablePK( RunsTable run, SoilTypeCategoriesTable soilTypeCategory,
            SoilTypeParametersTable soilTypeParameter, LevelsTable soilLevel ) {
        this.run = run;
        this.soilTypeCategory = soilTypeCategory;
        this.soilTypeParameter = soilTypeParameter;
        this.soilLevel = soilLevel;
    }

    public boolean equals( Object obj ) {
        if (obj instanceof SoilTypeParametersSetTablePK) {
            SoilTypeParametersSetTablePK other = (SoilTypeParametersSetTablePK) obj;
            RunsTable otherRun = other.getRun();
            SoilTypeCategoriesTable otherSoilTypeCategory = other.getSoilTypeCategory();
            SoilTypeParametersTable otherSoilTypeParameter = other.getSoilTypeParameter();
            LevelsTable otherSoilLevel = other.getSoilLevel();
            if (otherRun == null || otherSoilTypeCategory == null || otherSoilTypeParameter == null
                    || otherSoilLevel == null) {
                return false;
            }
            Long otherSoilTypeCategoryId = otherSoilTypeCategory.getId();
            if (otherSoilTypeCategoryId == null) {
                return false;
            }
            Long otherSoilTypeParameterId = otherSoilTypeParameter.getId();
            if (otherSoilTypeParameterId == null) {
                return false;
            }
            Long otherRunId = otherRun.getId();
            if (otherRunId == null) {
                return false;
            }
            Long otherLevelId = otherSoilLevel.getId();
            if (otherLevelId == null) {
                return false;
            }
            if (run.getId() != otherRunId) {
                return false;
            }
            if (soilTypeCategory.getId() != otherSoilTypeCategoryId) {
                return false;
            }
            if (soilTypeParameter.getId() != otherSoilTypeParameterId) {
                return false;
            }
            if (soilLevel.getId() != otherLevelId) {
                return false;
            }
            return true;
        }
        return false;
    }

    public int hashCode() {
        int code = 0;
        code = code + run.getId().intValue();
        if (soilTypeCategory != null) {
            code = code + soilTypeCategory.getId().intValue();
        }
        if (soilTypeParameter != null) {
            code = code + soilTypeParameter.getId().intValue();
        }
        if (soilLevel != null) {
            code = code + soilLevel.getId().intValue();
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

    public void setSoilTypeCategory( SoilTypeCategoriesTable soilTypeCategory ) {
        this.soilTypeCategory = soilTypeCategory;
    }

    public SoilTypeCategoriesTable getSoilTypeCategory() {
        return soilTypeCategory;
    }

    public void setSoilTypeParameter( SoilTypeParametersTable soilTypeParameter ) {
        this.soilTypeParameter = soilTypeParameter;
    }

    public SoilTypeParametersTable getSoilTypeParameter() {
        return soilTypeParameter;
    }

    public void setSoilLevel( LevelsTable soilLevel ) {
        this.soilLevel = soilLevel;
    }

    public LevelsTable getSoilLevel() {
        return soilLevel;
    }

}
