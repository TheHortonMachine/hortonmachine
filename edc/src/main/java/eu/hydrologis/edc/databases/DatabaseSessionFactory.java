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
package eu.hydrologis.edc.databases;

import java.io.IOException;

import org.geotools.data.DataStore;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.classic.Session;

/**
 * Interface for all database types sessionfactories.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface DatabaseSessionFactory {

    /**
     * Opens a {@link Session}.
     * 
     * <p>
     * The closing of the session is responsability of the user.
     * </p>
     * 
     * @return the session as supplied by the current active {@link SessionFactory}.
     */
    public Session openSession();

    /**
     * Closes the current {@link SessionFactory}.
     */
    public void closeSessionFactory();

    /**
     * Getter for the current {@link AnnotationConfiguration}.
     * 
     * <p>This might be needed for example for schame creation.</p>
     * 
     * @return the current annotation configuration.
     */
    public AnnotationConfiguration getAnnotationConfiguration();

    /**
     * Creates database schemas if needed. 
     * 
     * <p>
     * This is needed, since hibernate is not able to create 
     * schemas in the database, so this has to be done through
     * an sql query in the proper way for every database type.
     * </p> 
     */
    public void createSchemas();

    /**
     * Creates the spatial tables.
     * @throws IOException 
     */
    public void createSpatialTables() throws IOException;

    /**
     * Getter for the {@link QueryHandler}.
     * 
     * @return the query handler for the database.
     */
    public QueryHandler getQueryHandler();

    public DataStore getSpatialDataStore();

}
