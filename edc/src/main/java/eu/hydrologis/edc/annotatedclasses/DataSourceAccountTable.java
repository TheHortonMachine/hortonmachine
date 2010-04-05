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

import static eu.hydrologis.edc.utils.Constants.DATASOURCEACCOUNT;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LOGIN;
import static eu.hydrologis.edc.utils.Constants.PASSWORD;
import static eu.hydrologis.edc.utils.Constants.URL;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * The datasource account Table.
 * 
 * <p>
 * The table defines login data for a {@link DataSourceTable}.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = DATASOURCEACCOUNT, schema = EDC_SCHEMA)
public class DataSourceAccountTable {

    /**
     * The unique id of the datasource account.
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
     * The password for the login.
     */
    @Column(name = PASSWORD, nullable = false)
    private String password;

    /**
     * The url to the datasource.
     */
    @Column(name = URL, nullable = false)
    private String url;

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

    public String getPassword() {
        return password;
    }

    public void setPassword( String password ) {
        this.password = password;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl( String url ) {
        this.url = url;
    }

}
