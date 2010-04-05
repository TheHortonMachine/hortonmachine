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
import static eu.hydrologis.edc.utils.Constants.DESCRIPTION;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.EMAIL;
import static eu.hydrologis.edc.utils.Constants.FIRSTNAME;
import static eu.hydrologis.edc.utils.Constants.ID;
import static eu.hydrologis.edc.utils.Constants.LASTNAME;
import static eu.hydrologis.edc.utils.Constants.USERS;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

/**
 * The users Table.
 * 
 * <p>
 * The table defines basic informations about users.
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @author Silvia Franceschi (www.hydrologis.com)
 */
@Entity
@Table(name = USERS, schema = EDC_SCHEMA)
public class UsersTable {
    /**
     * The unique id of the user.
     */
    @Id
    @Column(name = ID, nullable = false)
    private Long id;
    
    /**
     * The first name of the user.
     */
    @Column(name = USERNAME, nullable = false)
    private String userName;

    /**
     * The first name of the user.
     */
    @Column(name = FIRSTNAME, nullable = false)
    private String firstName;

    /**
     * The last name of the user.
     */
    @Column(name = LASTNAME, nullable = false)
    private String lastName;

    /**
     * The description of the user.
     */
    @Column(name = DESCRIPTION, nullable = true)
    private String description;

    /**
     * The date of the user creation.
     */
    @Column(name = CREATIONDATE, nullable = false)
    @Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
    private DateTime creationDate;

    /**
     * The email of the user.
     */
    @Column(name = EMAIL, nullable = false)
    private String email;
    
    
    /*
     * =======================================================
     */

    public String toString()
    {
        StringBuilder retValue = new StringBuilder();
        
        retValue.append("UsersTable ( ")
            .append(" id = ").append(this.id)
            .append(" / userName = ").append(this.userName)
            .append(" / firstName = ").append(this.firstName)
            .append(" / lastName = ").append(this.lastName)
            .append(" / description = ").append(this.description)
            .append(" / creationDate = ").append(this.creationDate)
            .append(" / email = ").append(this.email)
            .append(" )");
        
        return retValue.toString();
    }

    public Long getId() {
        return id;
    }

    public void setId( Long id ) {
        this.id = id;
    }

    public void setUserName( String userName ) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName( String firstName ) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName( String lastName ) {
        this.lastName = lastName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription( String description ) {
        this.description = description;
    }

    public DateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate( DateTime creationDate ) {
        this.creationDate = creationDate;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail( String email ) {
        this.email = email;
    }

}
