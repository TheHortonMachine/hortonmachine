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
import static eu.hydrologis.edc.utils.Constants.LEVELS;
import static eu.hydrologis.edc.utils.Constants.NAME;
import static eu.hydrologis.edc.utils.Constants.ORDERING;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The levels Table.
 * 
 * <p>
 * The table of available levels of any type. For example there 
 * might be different soil type levels, in which case they can
 * be named and added here with their vertical order.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = LEVELS, schema = EDC_SCHEMA)
public class LevelsTable {

    /**
     * The unique id of the level.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The name of the level (example: soil_level_1).
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * The order number to be taken to sort the levels.
     * 
     * <p>
     * Note that for the order you can use positive or
     * negative values (i.e. if 0 the soil surface, -1 is
     * the first level in the soil)
     */
    @Column(name = ORDERING, nullable = false)
    private Integer ordering;

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

    public void setOrdering( Integer ordering ) {
        this.ordering = ordering;
    }

    public Integer getOrdering() {
        return ordering;
    }

}
