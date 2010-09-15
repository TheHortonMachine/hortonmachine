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
package eu.hydrologis.edc.databases.postgres;

import static eu.hydrologis.edc.utils.Constants.DATABASE;
import static eu.hydrologis.edc.utils.Constants.HOST;
import static eu.hydrologis.edc.utils.Constants.PASS;
import static eu.hydrologis.edc.utils.Constants.PORT;
import static eu.hydrologis.edc.utils.Constants.TYPE;
import static eu.hydrologis.edc.utils.Constants.USER;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.classic.Session;

import eu.hydrologis.edc.databases.DatabaseSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;

/**
 * An {@link SessionFactory} delegate for an existing postgres session factory.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ExistingPostgresSessionFactory implements DatabaseSessionFactory {

    private final SessionFactory sessionFactory;
    private final AnnotationConfiguration annotationConfiguration;
    private QueryHandler postgresQueryHandler;
    private DataStore spatialDataStore;

    /**
     * Constructor for the session factory.
     * 
     * @param sessionFactory the existing {@link SessionFactory}.
     * @param annotationConfiguration the {@link AnnotationConfiguration}, 
     *                  if null, the schema creation can't be done. 
     * @throws IOException 
     */
    @SuppressWarnings("unchecked")
    public ExistingPostgresSessionFactory( SessionFactory sessionFactory,
            AnnotationConfiguration annotationConfiguration, Properties properties )
            throws IOException {
        this.sessionFactory = sessionFactory;
        this.annotationConfiguration = annotationConfiguration;

        String type = properties.getProperty(TYPE);
        if (type == null) {
            throw new IllegalArgumentException(
                    "Missing database type definition in properties. (TYPE=...)");
        }

        String database = properties.getProperty(DATABASE);
        if (database == null) {
            throw new IllegalArgumentException(
                    "Missing database definition in properties. (DATABASE=...)");
        }

        String host = properties.getProperty(HOST);
        if (host == null) {
            throw new IllegalArgumentException("Missing host definition in properties. (HOST=...)");
        }

        String user = properties.getProperty(USER);
        if (user == null) {
            throw new IllegalArgumentException("Missing user definition in properties. (USER=...)");
        }

        String pass = properties.getProperty(PASS);
        if (pass == null) {
            throw new IllegalArgumentException(
                    "Missing password definition in properties. (PASS=...)");
        }

        // String showSql = properties.getProperty(SHOW_SQL);
        // String formatSql = properties.getProperty(FORMAT_SQL);

        String port = properties.getProperty(PORT);
        if (port == null) {
            throw new IllegalArgumentException("Missing port definition in properties. (PORT=...)");
        }
        /*
         * create also the spatial part
         */
        Map params = new HashMap();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "postgisng");
        params.put(JDBCDataStoreFactory.SCHEMA.key, "edcgeometries");
        params.put(JDBCDataStoreFactory.DATABASE.key, database);
        params.put(JDBCDataStoreFactory.PORT.key, port);
        params.put(JDBCDataStoreFactory.HOST.key, host);
        params.put(JDBCDataStoreFactory.USER.key, user);
        params.put(JDBCDataStoreFactory.PASSWD.key, pass);

        spatialDataStore = DataStoreFinder.getDataStore(params);
    }

    public AnnotationConfiguration getAnnotationConfiguration() {
        return annotationConfiguration;
    }

    public Session openSession() {
        return sessionFactory.openSession();
    }

    public void closeSessionFactory() {
        sessionFactory.close();
    }

    public void createSchemas() {
        // not needed
    }

    public void createSpatialTables() {
        // not needed
    }

    public QueryHandler getQueryHandler() {
        if (postgresQueryHandler == null)
            postgresQueryHandler = new PostgresQueryHandler(this);
        return postgresQueryHandler;
    }

    public DataStore getSpatialDataStore() {
        return spatialDataStore;
    }

}
