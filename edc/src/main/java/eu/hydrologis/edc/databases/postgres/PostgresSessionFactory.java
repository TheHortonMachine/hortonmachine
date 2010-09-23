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

import static eu.hydrologis.edc.utils.Constants.EDCGEOMETRIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDES;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDESGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.OBSTRUCTIONGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.POI;
import static eu.hydrologis.edc.utils.Constants.POIGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.POSTGRESQL_DIALECT;
import static eu.hydrologis.edc.utils.Constants.POSTGRESQL_DRIVER;
import static eu.hydrologis.edc.utils.Constants.SCHEMAS;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.hibernate.SQLQuery;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.cfg.Environment;
import org.hibernate.classic.Session;

import eu.hydrologis.edc.databases.DatabaseSessionFactory;
import eu.hydrologis.edc.databases.QueryHandler;
import eu.hydrologis.edc.utils.HibernateManager;

/**
 * An Postgresql {@link SessionFactory} delegate that also handles particular queries.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PostgresSessionFactory implements DatabaseSessionFactory {
    private SessionFactory sessionFactory;
    private AnnotationConfiguration annotationConfiguration;

    private List<String> annotatedClassesList = null;
    // private DataStore spatialDataStore;
    private QueryHandler postgresQueryHandler;
    private DataStore spatialDataStore;

    @SuppressWarnings("unchecked")
    public PostgresSessionFactory( String host, int port, String database, String user,
            String passwd, boolean logSql, boolean formatSql ) throws Exception {

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + database;

        Properties dbProps = new Properties();
        dbProps.put(Environment.DRIVER, POSTGRESQL_DRIVER);
        dbProps.put(Environment.URL, url);
        dbProps.put(Environment.USER, user);
        dbProps.put(Environment.PASS, passwd);
        dbProps.put(Environment.DIALECT, POSTGRESQL_DIALECT);
        dbProps.put(Environment.SHOW_SQL, String.valueOf(logSql));
        dbProps.put(Environment.FORMAT_SQL, String.valueOf(formatSql));

        annotationConfiguration = new AnnotationConfiguration();

        // create a configuration file
        File configFile = HibernateManager.generateConfigFile();
        annotationConfiguration = annotationConfiguration.configure(configFile);
        annotationConfiguration.setProperties(dbProps);

        // gather the annotated classes
        annotatedClassesList = HibernateManager.getAnnotatedClasses();
        for( String annotatedClassString : annotatedClassesList ) {
            annotationConfiguration.addAnnotatedClass(Class.forName(annotatedClassString));
        }

        sessionFactory = annotationConfiguration.buildSessionFactory();

        /*
        * create also the spatial part
        */
        Map params = new HashMap();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "postgisng");
        params.put(JDBCDataStoreFactory.SCHEMA.key, EDCGEOMETRIES_SCHEMA);
        params.put(JDBCDataStoreFactory.DATABASE.key, database);
        params.put(JDBCDataStoreFactory.PORT.key, port);
        params.put(JDBCDataStoreFactory.HOST.key, host);
        params.put(JDBCDataStoreFactory.USER.key, user);
        params.put(JDBCDataStoreFactory.PASSWD.key, passwd);

        spatialDataStore = DataStoreFinder.getDataStore(params);
    }

    public DataStore getSpatialDataStore() {
        return spatialDataStore;
    }

    public Session openSession() {
        return sessionFactory.openSession();
    }

    public AnnotationConfiguration getAnnotationConfiguration() {
        return annotationConfiguration;
    }

    public void closeSessionFactory() {
        if (sessionFactory != null)
            sessionFactory.close();
        if (spatialDataStore != null)
            spatialDataStore.dispose();
    }

    public void createSchemas() {
        String[] schemas = SCHEMAS;
        Session session = sessionFactory.openSession();
        Transaction transaction = session.beginTransaction();

        try {
            for( String schema : schemas ) {
                SQLQuery sqlQuery = session.createSQLQuery("create schema " + schema);
                sqlQuery.executeUpdate();
            }
        } catch (Exception e) {
            System.out.println("Couldn't create schemas for reason: " + e.getLocalizedMessage());
        }

        transaction.commit();
        session.close();
    }

    public void createSpatialTables() throws IOException {
        Session session = sessionFactory.openSession();
        String edcgeometriesSchema = EDCGEOMETRIES_SCHEMA;
        String edcSchema = EDC_SCHEMA;
        getQueryHandler();
        postgresQueryHandler.createPointGeometryTable(session, edcgeometriesSchema, POIGEOMETRIES,
                edcSchema, POI, 2);
        postgresQueryHandler.createPolygonGeometryTable(session, edcgeometriesSchema,
                LANDSLIDESGEOMETRIES, edcSchema, LANDSLIDES, 2);
        postgresQueryHandler.createLineGeometryTable(session, edcgeometriesSchema,
                OBSTRUCTIONGEOMETRIES, edcSchema, POI, 3);
        session.close();
    }

    public QueryHandler getQueryHandler() {
        if (postgresQueryHandler == null)
            postgresQueryHandler = new PostgresQueryHandler(this);
        return postgresQueryHandler;
    }
}
