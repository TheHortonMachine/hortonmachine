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

import static eu.hydrologis.edc.utils.Constants.*;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.UNITS;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The units Table.
 * 
 * <p>
 * The table of available units, with the conversion factor to the 
 * <a href="http://en.wikipedia.org/wiki/SI"> SI system</a>, which 
 * will be used internally by the models for fast conversion.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = UNITS, schema = EDC_SCHEMA)
public class UnitsTable {

    /**
     * The unique id of the unit.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The short name of the unit (example: m for meter).
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * Description of the unit.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * Conversion value with which to multiply the value in the current
     * unit to get the same value in the principal units for the handled
     * dimension.
     */
    @Column(name = TOPRINCIPAL, nullable = false)
    private Double toPrincipal;

    /*
     * =============================================
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

    public void setToPrincipal( Double toPrincipal ) {
        this.toPrincipal = toPrincipal;
    }

    public Double getToPrincipal() {
        return toPrincipal;
    }


}
