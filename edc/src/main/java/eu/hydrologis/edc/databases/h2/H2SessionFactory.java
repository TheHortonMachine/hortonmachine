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
package eu.hydrologis.edc.databases.h2;

import static eu.hydrologis.edc.utils.Constants.EDCGEOMETRIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.EDC_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.H2_DIALECT;
import static eu.hydrologis.edc.utils.Constants.H2_DRIVER;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDES;
import static eu.hydrologis.edc.utils.Constants.LANDSLIDESGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.OBSTRUCTIONGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.POI;
import static eu.hydrologis.edc.utils.Constants.POIGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.SCHEMAS;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.h2.tools.Server;
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
 * An H2 {@link SessionFactory} delegate.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2SessionFactory implements DatabaseSessionFactory {
    private SessionFactory sessionFactory;
    private AnnotationConfiguration annotationConfiguration;

    private List<String> annotatedClassesList = null;
    private Server tcpServer = null;
    private Server webServer = null;
    private boolean dbIsAlive;
    private final int port;
    // private DataStore spatialDataStore;
    private H2QueryHandler h2QueryHandler;
    private DataStore spatialDataStore;

    @SuppressWarnings("unchecked")
    public H2SessionFactory( String host, int port, String database, String user, String passwd,
            boolean logSql, boolean formatSql ) throws Exception {
        this.port = port;

        String url = "jdbc:h2:tcp://" + host + ":" + port + "/" + database;

        Properties dbProps = new Properties();
        dbProps.put(Environment.DRIVER, H2_DRIVER);
        dbProps.put(Environment.URL, url);
        dbProps.put(Environment.USER, user);
        dbProps.put(Environment.PASS, passwd);
        dbProps.put(Environment.DIALECT, H2_DIALECT);
        dbProps.put(Environment.SHOW_SQL, logSql);
        dbProps.put(Environment.FORMAT_SQL, formatSql);

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

        startWebserver();
        int timeout = 0;
        while( !dbIsAlive ) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (timeout++ > 50) {
                throw new RuntimeException(
                        "An error occurred while starting the embedded database."); //$NON-NLS-1$
            }
        }
        sessionFactory = annotationConfiguration.buildSessionFactory();

        /*
         * create also the spatial part
         */
        Map params = new HashMap();
        params.put(JDBCDataStoreFactory.DBTYPE.key, "h2");
        params.put(JDBCDataStoreFactory.SCHEMA.key, EDCGEOMETRIES_SCHEMA);
        params.put(JDBCDataStoreFactory.DATABASE.key, database);
        // params.put(JDBCDataStoreFactory.NAMESPACE.key, "");
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
        if (spatialDataStore != null)
            spatialDataStore.dispose();
        if (sessionFactory != null)
            sessionFactory.close();
        if (tcpServer != null) {
            tcpServer.stop();
        }
        if (webServer != null) {
            webServer.stop();
        }
    }

    /**
     * start the database instance
     */
    private void startWebserver() {
        Thread h2WebserverThread = new Thread(){
            @SuppressWarnings("nls")
            public void run() {
                try {
                    if (!dbIsAlive) {
                        String[] args = {"-tcp", "-tcpPort", String.valueOf(port), "-tcpAllowOthers",};
                        tcpServer = Server.createTcpServer(args).start();
                        args = new String[]{"-web", "-webPort", String.valueOf(port + 1)};
                        webServer = Server.createWebServer(args).start();
                        dbIsAlive = true;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        };
        h2WebserverThread.start();
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
            // try to create the schemas
            System.out.println("Couldn't create schemas for reason: " + e.getLocalizedMessage());
        }

        /*
         * create functions
         */
        try {
            SQLQuery sqlQuery = session
                    .createSQLQuery("CREATE ALIAS GeoToolsVersion for \"org.geotools.data.h2.JTS.GeoToolsVersion\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS AsWKT for \"org.geotools.data.h2.JTS.AsWKT\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS AsText for \"org.geotools.data.h2.JTS.AsWKT\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS EnvelopeAsText for \"org.geotools.data.h2.JTS.EnvelopeAsText\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS GeomFromText for \"org.geotools.data.h2.JTS.GeomFromText\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS GeomFromWKB for \"org.geotools.data.h2.JTS.GeomFromWKB\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS Envelope for \"org.geotools.data.h2.JTS.Envelope\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS GetSRID FOR \"org.geotools.data.h2.JTS.GetSRID\"");
            sqlQuery.executeUpdate();
            sqlQuery = session
                    .createSQLQuery("CREATE ALIAS GeometryType for \"org.geotools.data.h2.JTS.GeometryType\"");
            sqlQuery.executeUpdate();
        } catch (Exception e) {
            // try to create the schemas
            System.out.println("Couldn't create functions for reason: " + e.getLocalizedMessage());
        }

        transaction.commit();
        session.close();
    }

    public void createSpatialTables() throws IOException {
        Session session = sessionFactory.openSession();
        String edcgeometriesSchema = EDCGEOMETRIES_SCHEMA;
        String edcSchema = EDC_SCHEMA;

        getQueryHandler();
        h2QueryHandler.createPointGeometryTable(session, edcgeometriesSchema, POIGEOMETRIES,
                edcSchema, POI, 2);
        h2QueryHandler.createPolygonGeometryTable(session, edcgeometriesSchema,
                LANDSLIDESGEOMETRIES, edcSchema, LANDSLIDES, 2);
        h2QueryHandler.createLineGeometryTable(session, edcgeometriesSchema, OBSTRUCTIONGEOMETRIES,
                edcSchema, POI, 3);
        session.close();
    }

    public QueryHandler getQueryHandler() {
        if (h2QueryHandler == null)
            h2QueryHandler = new H2QueryHandler(this);
        return h2QueryHandler;
    }

}
