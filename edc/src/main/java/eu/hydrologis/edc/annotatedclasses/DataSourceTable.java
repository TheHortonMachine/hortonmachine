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

import static eu.hydrologis.edc.utils.Constants.CONTACT;
import static eu.hydrologis.edc.utils.Constants.DATASOURCE;
import static eu.hydrologis.edc.utils.Constants.DATASOURCEACCOUNT_ID;
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.NAME;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * The datasource Table.
 * 
 * <p>
 * The table defines the source of the data. That can be for example
 * a DVD supplied by some municipality but also one real time data source, 
 * in which case one can also exploit the possibility to define some account 
 * data for connection. A warning has to be expressed, since here no 
 * security is given for contained data. This class just represents a mapping.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = DATASOURCE, schema = EDC_SCHEMA)
public class DataSourceTable {

    /**
     * The unique id of the datasource.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The name of the datasource.
     */
    @Column(name = NAME, nullable = false)
    private String name;

    /**
     * Description of the datasource.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * Contact info of the responsible person.
     */
    @Column(name = CONTACT, nullable = true)
    private String contact;

    /**
     * Account data to access the datasource.
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = DATASOURCEACCOUNT_ID, referencedColumnName = ID, nullable = true)
    private DataSourceAccountTable account;

    /*
     * ====================================================
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

    public void setContact( String contact ) {
        this.contact = contact;
    }

    public String getContact() {
        return contact;
    }

    public DataSourceAccountTable getAccount() {
        return account;
    }

    public void setAccount( DataSourceAccountTable account ) {
        this.account = account;
    }

}
