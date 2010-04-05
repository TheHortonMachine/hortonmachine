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
import static eu.hydrologis.edc.utils.Constants.HOST;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LOGIN;
import static eu.hydrologis.edc.utils.Constants.PORT;
import static eu.hydrologis.edc.utils.Constants.RAMADDA;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The ramadda settings Table.
 * 
 * <p>
 * The table defines host and login setting for the 
 * Ramadda connection to use. Note that the highest 
 * found id is used by edc.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = RAMADDA, schema = EDC_SCHEMA)
public class RamaddaTable {

    /**
     * The unique id of the ramadda account.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;

    /**
     * The user login.
     */
    @Column(name = LOGIN, nullable = false)
    private String login;

    /**
     * The host.
     */
    @Column(name = HOST, nullable = false)
    private String host;

    /**
     * The port.
     */
    @Column(name = PORT, nullable = false)
    private Integer port;

    /**
     * The base of ramadda. 
     */
    @Column(name = BASE, nullable = false)
    private String base;

    /**
     * The parent id of the ramadda root. 
     */
    @Column(name = PARENTID, nullable = false)
    private String parentid;

    /*
     * ====================================================
     */

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin( String login ) {
        this.login = login;
    }

    public String getHost() {
        return host;
    }

    public void setHost( String host ) {
        this.host = host;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort( Integer port ) {
        this.port = port;
    }

    public String getBase() {
        return base;
    }

    public void setBase( String base ) {
        this.base = base;
    }

    public void setParentid( String parentid ) {
        this.parentid = parentid;
    }

    public String getParentid() {
        return parentid;
    }

}
