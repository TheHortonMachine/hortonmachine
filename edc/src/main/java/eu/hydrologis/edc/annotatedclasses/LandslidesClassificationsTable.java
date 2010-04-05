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
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDESCLASSIFICATIONS;
import static eu.hydrologis.edc.utils.Constants.MATERIALTYPE;
import static eu.hydrologis.edc.utils.Constants.MOVEMENTTYPE;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The landslides classifications table.
 * 
 * <p>
 * <a href="http://en.wikipedia.org/wiki/Landslide_classification#Types_and_classification">
 * Implemented adopting the classification of Varnes 1978 and taking into account the modifications 
 * made by Cruden and Varnes, in 1996.</a> 
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = LANDSLIDESCLASSIFICATIONS, schema = EDC_SCHEMA)
public class LandslidesClassificationsTable {

    /**
     * The unique id of the classification.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The type of movement.
     */
    @Column(name = MOVEMENTTYPE, nullable = false)
    private String movementType;

    /**
     * The type of material.
     */
    @Column(name = MATERIALTYPE, nullable = false)
    private String materialType;

    /*
     * ====================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setMovementType( String movementType ) {
        this.movementType = movementType;
    }

    public String getMovementType() {
        return movementType;
    }

    public void setMaterialType( String materialType ) {
        this.materialType = materialType;
    }

    public String getMaterialType() {
        return materialType;
    }

}
