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
package eu.hydrologis.edc.utils;

import static eu.hydrologis.edc.utils.Constants.CLOUDINESSTRANSMISSIVITY_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESSTRANSMISSIVITY_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESSTRANSMISSIVITY_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESSTRANSMISSIVITY_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESS_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESS_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESS_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.CLOUDINESS_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.EDCSERIES_SCHEMA;
import static eu.hydrologis.edc.utils.Constants.INCOMINGLONGWAVERADIATION_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.INCOMINGLONGWAVERADIATION_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.INCOMINGLONGWAVERADIATION_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.INCOMINGLONGWAVERADIATION_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.NETSHORTWAVERADIATION_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.NETSHORTWAVERADIATION_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.NETSHORTWAVERADIATION_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.NETSHORTWAVERADIATION_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.PRECIPITATIONS_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.PRECIPITATIONS_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.PRECIPITATIONS_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.PRECIPITATIONS_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.PRESSURE_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.PRESSURE_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.PRESSURE_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.PRESSURE_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.RELATIVEHUMIDITY_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.RELATIVEHUMIDITY_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.RELATIVEHUMIDITY_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.RELATIVEHUMIDITY_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIFFUSE_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIFFUSE_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIFFUSE_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIFFUSE_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIRECT_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIRECT_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIRECT_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONDIRECT_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONGLOBAL_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONGLOBAL_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONGLOBAL_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.SHORTWAVERADIATIONGLOBAL_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.SNOWDEPTH_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.SNOWDEPTH_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.SNOWDEPTH_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.SNOWDEPTH_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.TEMPERATURE_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.TEMPERATURE_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.TEMPERATURE_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.TEMPERATURE_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.WINDDIRECTION_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.WINDDIRECTION_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.WINDDIRECTION_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.WINDDIRECTION_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.WINDSPEED_SERIES_NAME;
import static eu.hydrologis.edc.utils.Constants.WINDSPEED_SERIES_PACKAGE;
import static eu.hydrologis.edc.utils.Constants.WINDSPEED_SERIES_PATH;
import static eu.hydrologis.edc.utils.Constants.WINDSPEED_SERIES_TABLE_PREFIX;
import static eu.hydrologis.edc.utils.Constants.end;
import static eu.hydrologis.edc.utils.Constants.start;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import eu.hydrologis.edc.annotatedclasses.MonitoringPointsTable;
import eu.hydrologis.edc.annotatedclasses.timeseries.SeriesMonitoringPointsTable;
import eu.hydrologis.edc.annotatedclassesdaos.AbstractEdcDao;
import eu.hydrologis.edc.databases.EdcSessionFactory;

/**
 * The hibernate manager.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("unchecked")
public class HibernateManager {

    /**
     * Retrieves the list of annotated classes.
     * 
     * @return the list of annotated classes available.
     * @throws Exception 
     */
    public static List<String> getAnnotatedClasses() {
        List<String> annotatedClassesList = new ArrayList<String>();
        Map<String, Class> allEdcTables2ClassesMap = Constants.getAllEdcTables2ClassesMap();
        Collection<Class> classes = allEdcTables2ClassesMap.values();
        for( Class tmpClass : classes ) {
            annotatedClassesList.add(tmpClass.getCanonicalName());
        }
        return annotatedClassesList;
    }

    /**
     * Generates minimum config file that needs to be on disk.
     * 
     * @throws IOException
     */
    @SuppressWarnings("nls")
    public static File generateConfigFile() throws IOException {
        StringBuilder sB = new StringBuilder();
        sB.append("<?xml version='1.0' encoding='utf-8'?>");
        sB.append("<!DOCTYPE hibernate-configuration PUBLIC");
        sB.append("    \"-//Hibernate/Hibernate Configuration DTD//EN\"");
        sB.append("    \"http://hibernate.sourceforge.net/hibernate-configuration-3.0.dtd\">");
        sB.append("<hibernate-configuration>");
        sB.append(" <session-factory>");
        sB.append("     <!-- Use the C3P0 connection pool provider -->");
        sB.append("     <property name=\"hibernate.c3p0.min_size\">5</property>");
        sB.append("     <property name=\"hibernate.c3p0.max_size\">20</property>");
        sB.append("     <property name=\"hibernate.c3p0.timeout\">300</property>");
        sB.append("     <property name=\"hibernate.c3p0.max_statements\">50</property>");
        sB.append("     <property name=\"hibernate.c3p0.idle_test_period\">3000</property>");
        sB.append(" </session-factory>");
        sB.append("</hibernate-configuration>");

        File tempFile = File.createTempFile("jgrass_hibernate", null);
        BufferedWriter bW = new BufferedWriter(new FileWriter(tempFile));
        bW.write(sB.toString());
        bW.close();

        return tempFile;
    }

    /**
     * Create all the available series classes by naming them with the {@link Constants#start} and {@link Constants#end} year.
     * 
     * @throws Exception
     */
    private static void generateSeriesClasses() throws Exception {
        generateClasses(PRECIPITATIONS_SERIES_PATH, PRECIPITATIONS_SERIES_NAME,
                PRECIPITATIONS_SERIES_PACKAGE, PRECIPITATIONS_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(PRESSURE_SERIES_PATH, PRESSURE_SERIES_NAME, PRESSURE_SERIES_PACKAGE,
                PRESSURE_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(RELATIVEHUMIDITY_SERIES_PATH, RELATIVEHUMIDITY_SERIES_NAME,
                RELATIVEHUMIDITY_SERIES_PACKAGE, RELATIVEHUMIDITY_SERIES_TABLE_PREFIX,
                EDCSERIES_SCHEMA);
        generateClasses(SNOWDEPTH_SERIES_PATH, SNOWDEPTH_SERIES_NAME, SNOWDEPTH_SERIES_PACKAGE,
                SNOWDEPTH_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(TEMPERATURE_SERIES_PATH, TEMPERATURE_SERIES_NAME,
                TEMPERATURE_SERIES_PACKAGE, TEMPERATURE_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(WINDDIRECTION_SERIES_PATH, WINDDIRECTION_SERIES_NAME,
                WINDDIRECTION_SERIES_PACKAGE, WINDDIRECTION_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(WINDSPEED_SERIES_PATH, WINDSPEED_SERIES_NAME, WINDSPEED_SERIES_PACKAGE,
                WINDSPEED_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(SHORTWAVERADIATIONGLOBAL_SERIES_PATH, SHORTWAVERADIATIONGLOBAL_SERIES_NAME,
                SHORTWAVERADIATIONGLOBAL_SERIES_PACKAGE,
                SHORTWAVERADIATIONGLOBAL_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(SHORTWAVERADIATIONDIRECT_SERIES_PATH, SHORTWAVERADIATIONDIRECT_SERIES_NAME,
                SHORTWAVERADIATIONDIRECT_SERIES_PACKAGE,
                SHORTWAVERADIATIONDIRECT_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(SHORTWAVERADIATIONDIFFUSE_SERIES_PATH,
                SHORTWAVERADIATIONDIFFUSE_SERIES_NAME, SHORTWAVERADIATIONDIFFUSE_SERIES_PACKAGE,
                SHORTWAVERADIATIONDIFFUSE_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(NETSHORTWAVERADIATION_SERIES_PATH, NETSHORTWAVERADIATION_SERIES_NAME,
                NETSHORTWAVERADIATION_SERIES_PACKAGE, NETSHORTWAVERADIATION_SERIES_TABLE_PREFIX,
                EDCSERIES_SCHEMA);
        generateClasses(CLOUDINESSTRANSMISSIVITY_SERIES_PATH, CLOUDINESSTRANSMISSIVITY_SERIES_NAME,
                CLOUDINESSTRANSMISSIVITY_SERIES_PACKAGE,
                CLOUDINESSTRANSMISSIVITY_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(CLOUDINESS_SERIES_PATH, CLOUDINESS_SERIES_NAME, CLOUDINESS_SERIES_PACKAGE,
                CLOUDINESS_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
        generateClasses(INCOMINGLONGWAVERADIATION_SERIES_PATH,
                INCOMINGLONGWAVERADIATION_SERIES_NAME, INCOMINGLONGWAVERADIATION_SERIES_PACKAGE,
                INCOMINGLONGWAVERADIATION_SERIES_TABLE_PREFIX, EDCSERIES_SCHEMA);
    }

    /**
     * Generate a type of series, defined by the passed parameters.
     *
     * <p>
     * An example of parameters are:
     * <ul>
     *  <li>PRECIPITATIONS_SERIES_NAME = "SeriesPrecipitations"</li>
     *  <li>PRECIPITATIONS_SERIES_PACKAGE = "eu.hydrologis.edc.annotatedclasses.timeseries.precipitations"</li>
     *  <li>PRECIPITATIONS_SERIES_TABLE_PREFIX = "series_precipitations_"</li>
     *  <li>PRECIPITATIONS_SERIES_PATH = "./src/main/java/eu/hydrologis/edc/annotatedclasses/timeseries/precipitations"</li>
     * </ul>
     * </p>
     * 
     * @param series_path the path were to create the file in.
     * @param series_name the name of the series, to which the year will be added.
     * @param series_package the package the class will be generated in.
     * @param series_table_prefix the table name, to which the year will be added.
     * @param series_schema the name of the database schema the tables will be created in.
     * @throws Exception
     */
    private static void generateClasses( String series_path, String series_name,
            String series_package, String series_table_prefix, String series_schema )
            throws Exception {

        File here = new File(series_path);

        for( int i = start; i <= end; i++ ) {
            String name = series_name + i;
            File classFile = new File(here, name + ".java");
            if (classFile.exists()) {
                continue;
            }

            StringBuilder sB = new StringBuilder();
            sB.append("package " + series_package + ";\n");
            sB.append("\n");
            sB.append("import javax.persistence.Entity;\n");
            sB.append("import javax.persistence.Table;\n");
            sB.append("import static eu.hydrologis.edc.utils.Constants.*;\n");
            sB.append("\n");
            sB.append("import " + SeriesMonitoringPointsTable.class.getCanonicalName() + ";\n");
            sB.append("\n");
            sB.append("@Entity\n");
            sB.append("@Table(name = \"" + series_table_prefix + i + "\", schema = \""
                    + series_schema + "\")\n");
            sB.append("@org.hibernate.annotations.Table(appliesTo = \"" + series_table_prefix + i
                    + "\", \n");
            sB.append("        indexes = @org.hibernate.annotations.Index(\n");
            sB.append("                name = \"IDX_TIMESTAMP_MONPOINT_" + series_table_prefix + i
                    + "\",\n");
            sB.append("                columnNames = {TIMESTAMPUTC, MONITORINGPOINTS_ID}\n");
            sB.append("))\n");
            sB.append("public class " + name + " extends "
                    + SeriesMonitoringPointsTable.class.getSimpleName() + " {\n");
            sB.append("}\n");

            BufferedWriter bW = new BufferedWriter(new FileWriter(classFile));
            bW.write(sB.toString());
            bW.close();
        }

    }

    /**
     * Dumps the columns in proper csv order and their nullability definition for every table.
     * 
     * @param out output {@link PrintStream}.
     * @throws Exception
     */
    public static void dumpTableDefinitions( PrintStream out ) throws Exception {
        List<String> annotatedClasses = getAnnotatedClasses();
        for( String annotatedClass : annotatedClasses ) {
            String annotDaoClass = annotatedClass.replaceFirst("annotatedclasses",
                    "annotatedclassesdaos");
            annotDaoClass = annotDaoClass.replaceFirst("Table", "Dao");
            // Class< ? > c = Class.forName(annotDaoClass);
            // AbstractEdcDao newInstance = (AbstractEdcDao) c.newInstance();
            // String recordDefinition = newInstance.getRecordDefinition();
            // out.println(recordDefinition);
            try {

                Class< ? > daoClass = Class.forName(annotDaoClass);
                Class paramatersTypes[] = new Class[1];
                paramatersTypes[0] = EdcSessionFactory.class;
                Constructor constructor = daoClass.getConstructor(paramatersTypes);
                Object arguments[] = new Object[1];
                arguments[0] = null;
                AbstractEdcDao edcDao = (AbstractEdcDao) constructor.newInstance(arguments);
                out.println(edcDao.getRecordDefinition());
            } catch (Exception e) {
                if (!annotatedClass.matches(".*timeseries.*")) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Run this to generate the {@link MonitoringPointsTable} classes from year {@link Constants#start} to {@link Constants#end}.
     * 
     * @param args
     * @throws Exception
     */
    public static void main( String[] args ) throws Exception {
        // generateSeriesClasses();

        generateClassesDescriptionForJGrassPlugin();

    }

    /**
     * Method needed to generate the xml for the plugins in JGrass
     * needed to register external annotated classes to the database.
     */
    @SuppressWarnings("nls")
    private static void generateClassesDescriptionForJGrassPlugin() {
        List<String> annotatedClasses = getAnnotatedClasses();

        for( String className : annotatedClasses ) {
            System.out.println("<annotatedclass");
            System.out.println("annotatedclass=\"" + className + "\">");
            System.out.println("</annotatedclass>");
        }
    }
}
