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
package eu.hydrologis.edc;

import static eu.hydrologis.edc.utils.Constants.ANNOTATEDCLASSES;
import static eu.hydrologis.edc.utils.Constants.ANNOTATEDCLASSESDAOS;
import static eu.hydrologis.edc.utils.Constants.DAO;
import static eu.hydrologis.edc.utils.Constants.POIGEOMETRIES;
import static eu.hydrologis.edc.utils.Constants.TABLE;
import static eu.hydrologis.edc.utils.Constants.getAllEdcTables2ClassesMap;

import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.SchemaUpdate;

import eu.hydrologis.edc.annotatedclassesdaos.AbstractEdcDao;
import eu.hydrologis.edc.annotatedclassesdaos.PoiGeometriesDao;
import eu.hydrologis.edc.annotatedclassesdaos.timeseries.SeriesMonitoringPointsDao;
import eu.hydrologis.edc.databases.EdcSessionFactory;
import eu.hydrologis.edc.ramadda.RamaddaManager;
import eu.hydrologis.edc.utils.HibernateManager;

/**
 * The main class of the Environmental Data Center.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class EDC {
    private EdcSessionFactory edcSessionFactory;
    private PrintStream outputStream;
    private SessionFactory sessionFactory;
    private AnnotationConfiguration annotationConfiguration;
    private Properties properties;

    /**
     * Create an EDC on a new connection.
     * 
     * @param properties the {@link Properties} needed to connect
     *              to the database.
     * @param outputStream an {@link PrintStream output stream} for logging 
     *              or messaging purposes.
     * @throws Exception 
     */
    public EDC( Properties properties, PrintStream outputStream ) throws Exception {
        this.properties = properties;
        this.outputStream = outputStream;
    }

    /**
     * Create an EDC on a existing connection.
     * 
     * <p><b>
     * Note: to use the EDC annotated classes, to create the {@link SessionFactory} that
     * is passed here, it is necessary to get the classes through the {@link #getEdcAnnotatedClasses()}
     * method.
     * </b></p>
     * 
     * @param sessionFactory an existing {@link SessionFactory}.
     * @param annotationConfiguration the {@link AnnotationConfiguration} that created the {@link SessionFactory}.
     * @param outputStream a {@link PrintStream} for output.
     * @param properties properties that have contain minimum informations to make a spatial db connection.
     * @throws Exception
     */
    public EDC( SessionFactory sessionFactory, AnnotationConfiguration annotationConfiguration,
            Properties properties, PrintStream outputStream ) throws Exception {
        this.sessionFactory = sessionFactory;
        this.annotationConfiguration = annotationConfiguration;
        this.properties = properties;
        this.outputStream = outputStream;
    }

    /**
     * Checks if a {@link EdcSessionFactory} was ever instantiated.
     * 
     * @return true if a session factory was ever instantiated.
     */
    public boolean hasEdcSessionFactory() {
        return edcSessionFactory != null;
    }

    /**
     * Creates an {@link EdcSessionFactory session factory}.
     * 
     * @return the session factory.
     * @throws Exception 
     */
    public EdcSessionFactory getEdcSessionFactory() throws Exception {
        if (edcSessionFactory == null) {
            if (sessionFactory != null) {
                // create one from existing
                edcSessionFactory = new EdcSessionFactory(sessionFactory, annotationConfiguration, properties);
            } else if (properties != null) {
                // create a new one
                edcSessionFactory = new EdcSessionFactory(properties);
            }
        }
        return edcSessionFactory;
    }

    /**
     * Facility to access a {@link RamaddaManager} from the EDC class directly.
     * 
     * @return the ramadda manager, based on the properties passed to EDC.
     * @throws Exception
     */
    public RamaddaManager createRamaddaManager() throws Exception {
        RamaddaManager ramaddaManager = new RamaddaManager(properties, outputStream);
        return ramaddaManager;
    }

    /**
     * Getter for all the annotated classes made available by EDC.
     * 
     * @return the list of EDC annotated classes. 
     */
    public static List<String> getEdcAnnotatedClasses() {
        return HibernateManager.getAnnotatedClasses();
    }

    /**
     * Creates the database schemas and tables.
     * 
     * @param doUpdate update an existing schema.
     * @param doScript output the creation script.
     * @param createInDatabase create the schema in the connected database.
     * @throws Exception
     */
    public void generateDatabase( boolean doUpdate, boolean doScript, boolean createInDatabase )
            throws Exception {
        if (edcSessionFactory == null) {
            getEdcSessionFactory();
        }
        AnnotationConfiguration annotationConfiguration = edcSessionFactory
                .getAnnotationConfiguration();
        if (doUpdate) {
            SchemaUpdate schemaUpdate = new SchemaUpdate(annotationConfiguration);
            schemaUpdate.execute(doScript, doUpdate);
        } else {
            edcSessionFactory.createSchemas();
            SchemaExport schemaExport = new SchemaExport(annotationConfiguration);
            schemaExport.create(doScript, createInDatabase);
            edcSessionFactory.createSpatialTables();
        }
    }

    /**
     * Populates a table from a csv file.
     * 
     * @param tableName the table into which to put the data. Note that in the case of series,
     *                  the name without the trailing year has to be supplied.
     * @param csvFile the file with the csv data.
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void insertFromCsv( String tableName, File csvFile ) throws Exception {
        Map<String, Class> table2ClassesMap = getAllEdcTables2ClassesMap();
        Class tableClass = table2ClassesMap.get(tableName);

        if (tableClass != null) {
            String tableClassName = tableClass.getCanonicalName();
            /*
             * extraction of the dao class from the supplied table name.
             * A word of caution here to the fact that to work,
             * the table classes need to live in a simmetric package
             * to the annotatedclasses, called annotatedclassesdaos.
             * Also the names of the classes of tables and daos have to be
             * different only by the trailing "Table" or "Dao".
             */
            tableClassName = tableClassName.replaceFirst(ANNOTATEDCLASSES, ANNOTATEDCLASSESDAOS);
            tableClassName = tableClassName.replaceFirst(TABLE, DAO);

            Class< ? > daoClass = Class.forName(tableClassName);

            Class paramatersTypes[] = new Class[1];
            paramatersTypes[0] = EdcSessionFactory.class;
            Constructor constructor = daoClass.getConstructor(paramatersTypes);
            Object arguments[] = new Object[1];
            arguments[0] = getEdcSessionFactory();
            AbstractEdcDao edcDao = (AbstractEdcDao) constructor.newInstance(arguments);
            edcDao.setOutputStream(outputStream);
            if (csvFile != null) {
                try {
                    edcDao.loadFromCsv(null, csvFile);
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new Exception("Expected input was: " + edcDao.getRecordDefinition());
                }
                edcDao.closeSession();
            } else {
                String recordDefinition = edcDao.getRecordDefinition();
                outputStream.println(recordDefinition);
            }
        } else if (tableName.equals(POIGEOMETRIES)) {
            // for manually created tables, the daos must be instantiated manually
            PoiGeometriesDao poiGeometriesDao = new PoiGeometriesDao(edcSessionFactory);
            if (csvFile != null) {
                poiGeometriesDao.setOutputStream(outputStream);
                try {
                    poiGeometriesDao.loadFromCsv(null, csvFile);
                } catch (Exception e) {
                    throw new Exception("Expected input was: "
                            + poiGeometriesDao.getRecordDefinition(), e);
                }
                poiGeometriesDao.closeSession();
            } else {
                String recordDefinition = poiGeometriesDao.getRecordDefinition();
                outputStream.println(recordDefinition);
            }
        } else {
            /*
             * it might be that the table is a series table, in which
             * case the year would be missing, so we have to check that also. 
             */
            Set<String> tableNames = table2ClassesMap.keySet();
            boolean found = false;
            for( String name : tableNames ) {
                if (name.startsWith(tableName)) {
                    found = true;
                }
            }
            if (found) {
                SeriesMonitoringPointsDao seriesDao = new SeriesMonitoringPointsDao(
                        getEdcSessionFactory());
                if (csvFile != null) {
                    seriesDao.setOutputStream(outputStream);
                    try {
                        seriesDao.loadFromCsv(tableName, csvFile);
                    } catch (Exception e) {
                        throw new Exception("Expected input was: "
                                + seriesDao.getRecordDefinition(), e);
                    }
                    seriesDao.closeSession();
                } else {
                    String recordDefinition = seriesDao.getRecordDefinition();
                    outputStream.println(recordDefinition);
                }
            } else {
                throw new IllegalArgumentException("Could not find the provided table: "
                        + tableName);
            }
        }

    }

}
