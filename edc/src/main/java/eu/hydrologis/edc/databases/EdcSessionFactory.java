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

import static eu.hydrologis.edc.utils.Constants.DATABASE;
import static eu.hydrologis.edc.utils.Constants.FORMAT_SQL;
import static eu.hydrologis.edc.utils.Constants.GEOLOGYMAP;
import static eu.hydrologis.edc.utils.Constants.H2;
import static eu.hydrologis.edc.utils.Constants.HOST;
import static eu.hydrologis.edc.utils.Constants.LANDCOVERMAP;
import static eu.hydrologis.edc.utils.Constants.METEOMAP;
import static eu.hydrologis.edc.utils.Constants.MORPHOLOGYMAP;
import static eu.hydrologis.edc.utils.Constants.PASS;
import static eu.hydrologis.edc.utils.Constants.PORT;
import static eu.hydrologis.edc.utils.Constants.POSTGRESQL;
import static eu.hydrologis.edc.utils.Constants.RUNS;
import static eu.hydrologis.edc.utils.Constants.SHOW_SQL;
import static eu.hydrologis.edc.utils.Constants.SOILTYPEMAP;
import static eu.hydrologis.edc.utils.Constants.TYPE;
import static eu.hydrologis.edc.utils.Constants.USER;

import java.io.IOException;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import org.geotools.data.DataStore;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.classic.Session;

import eu.hydrologis.edc.annotatedclasses.GeologyMapTable;
import eu.hydrologis.edc.annotatedclasses.LandcoverMapTable;
import eu.hydrologis.edc.annotatedclasses.MeteoMapTable;
import eu.hydrologis.edc.annotatedclasses.MorphologyMapTable;
import eu.hydrologis.edc.annotatedclasses.RunsTable;
import eu.hydrologis.edc.annotatedclasses.SoilTypeMapTable;
import eu.hydrologis.edc.databases.h2.H2SessionFactory;
import eu.hydrologis.edc.databases.postgres.ExistingPostgresSessionFactory;
import eu.hydrologis.edc.databases.postgres.PostgresSessionFactory;
import eu.hydrologis.edc.utils.GeometryLoader;

/**
 * The main {@link SessionFactory} delegate for EDC.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class EdcSessionFactory implements DatabaseSessionFactory {

    private DatabaseSessionFactory databaseSessionFactory;

    /**
     * Constructor for the case of an existing session factory.
     * 
     * @param sessionFactory the existing {@link SessionFactory}.
     * @param annotationConfiguration the {@link AnnotationConfiguration}, 
     *                  if null, the schema creation can't be done. 
     * @param properties 
     * @throws IOException 
     */
    public EdcSessionFactory( SessionFactory sessionFactory,
            AnnotationConfiguration annotationConfiguration, Properties properties )
            throws IOException {
        // databaseSessionFactory = new ExistingDatabaseSessionFactory(sessionFactory,
        // annotationConfiguration);

        String type = properties.getProperty(TYPE);
        if (type == null) {
            throw new IllegalArgumentException(
                    "Missing database type definition in properties. (TYPE=...)");
        }

        if (type.equalsIgnoreCase(H2)) {
            databaseSessionFactory = null;
        } else if (type.equalsIgnoreCase(POSTGRESQL)) {
            databaseSessionFactory = new ExistingPostgresSessionFactory(sessionFactory,
                    annotationConfiguration, properties);
        } else {
            throw new IllegalArgumentException(
                    "The supplied database type is not supported. (TYPE=...)");
        }

    }
    /**
     * Constructor for the case the connection has to be created.
     * 
     * @param properties the {@link Properties} containing the needed connection parameters.
     * @throws Exception
     */
    public EdcSessionFactory( Properties properties ) throws Exception {
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
                    "Missing password definition in properties. (USER=...)");
        }

        String showSql = properties.getProperty(SHOW_SQL);
        String formatSql = properties.getProperty(FORMAT_SQL);

        String port = properties.getProperty(PORT);
        if (port == null) {
            throw new IllegalArgumentException("Missing port definition in properties. (PORT=...)");
        }

        if (type.equalsIgnoreCase(H2)) {
            databaseSessionFactory = new H2SessionFactory(host, Integer.parseInt(port), database,
                    user, pass, Boolean.parseBoolean(showSql), Boolean.parseBoolean(formatSql));
        } else if (type.equalsIgnoreCase(POSTGRESQL)) {
            databaseSessionFactory = new PostgresSessionFactory(host, Integer.parseInt(port),
                    database, user, pass, Boolean.parseBoolean(showSql), Boolean
                            .parseBoolean(formatSql));
        } else {
            throw new IllegalArgumentException(
                    "The supplied database type is not supported. (TYPE=...)");
        }

    }

    public Session openSession() {
        return databaseSessionFactory.openSession();
    }

    public void closeSessionFactory() {
        databaseSessionFactory.closeSessionFactory();
    }

    public AnnotationConfiguration getAnnotationConfiguration() {
        return databaseSessionFactory.getAnnotationConfiguration();
    }

    public Map<String, Class< ? >> getRamaddaMapTables() {
        Map<String, Class< ? >> ramaddaMapNames = new TreeMap<String, Class< ? >>();
        ramaddaMapNames.put(MORPHOLOGYMAP, MorphologyMapTable.class);
        ramaddaMapNames.put(GEOLOGYMAP, GeologyMapTable.class);
        ramaddaMapNames.put(LANDCOVERMAP, LandcoverMapTable.class);
        ramaddaMapNames.put(SOILTYPEMAP, SoilTypeMapTable.class);
        ramaddaMapNames.put(METEOMAP, MeteoMapTable.class);

        return ramaddaMapNames;
    }

    public void createSchemas() {
        databaseSessionFactory.createSchemas();
    }

    public void createSpatialTables() throws IOException {
        databaseSessionFactory.createSpatialTables();
    }

    public QueryHandler getQueryHandler() {
        return databaseSessionFactory.getQueryHandler();
    }

    /**
     * Creates a new {@link GeometryLoader}.
     * 
     * @return the new geometry loader.
     */
    public GeometryLoader getGeometryLoader() {
        GeometryLoader geometryLoader = new GeometryLoader(this);
        return geometryLoader;
    }

    public DataStore getSpatialDataStore() {
        return databaseSessionFactory.getSpatialDataStore();
    }

}
