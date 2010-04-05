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
package eu.hydrologis.edc.annotatedclassesdaos;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintStream;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.edc.databases.EdcSessionFactory;

import static eu.hydrologis.edc.utils.Constants.*;

/**
 * The main EDC dao.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class AbstractEdcDao {
    protected DateTimeFormatter formatter = utcDateFormatterYYYYMMDDHHMM;
    protected String formatterPattern = utcDateFormatterYYYYMMDDHHMM_string;
    protected Session session = null;
    protected PrintStream outputStream = System.out;
    protected String insertTable;
    protected EdcSessionFactory edcSessionFactory;

    /**
     * Builds an {@link EdcSessionFactory}, implicitly opening a {@link Session}.
     * 
     * <p>
     * <b>The user has to take care of closing the session calling {@link #closeSession()}.</b>
     * </p>
     * 
     * @param edcSessionFactory the sessionfactory.
     */
    public AbstractEdcDao( EdcSessionFactory edcSessionFactory ) {
        this.edcSessionFactory = edcSessionFactory;
        if (edcSessionFactory != null)
            session = edcSessionFactory.openSession();
    }

    /**
     * Defines an {@link PrintStream output stream} for logging or messaging purposes.
     * 
     * @param outputStream the output stream to use.
     */
    public void setOutputStream( PrintStream outputStream ) {
        this.outputStream = outputStream;
    }

    /**
     * Closes the session. <b>This is mandatory to be called for any EdcDao.</b>
     */
    public void closeSession() {
        if (session != null) {
            session.close();
            session = null;
        }
    }

    /**
     * The record definition, i.e. the line of csv that is needed when inserting.
     * 
     * @return the record definition.
     * @throws Exception 
     */
    public abstract String getRecordDefinition() throws Exception;

    /**
     * Method to populate the table from a CSV file.
     * 
     * @param insertFile the CSV file containing the data.
     * @throws Exception
     */
    public void loadFromCsv( String insertTable, File insertFile ) throws Exception {
        this.insertTable = insertTable;
        Transaction transaction = session.beginTransaction();

        BufferedReader bR = new BufferedReader(new FileReader(insertFile));
        String separatorReplacement = " " + CSV_SEPARATOR + " ";
        String line;
        int index = 0;
        int progressive = 0;
        while( (line = bR.readLine()) != null ) {
            if (line.startsWith(CSV_COMMENT))
                continue;
            line = line.replaceAll(CSV_SEPARATOR, separatorReplacement);
            String[] lineSplit = line.split(CSV_SEPARATOR);

            processLine(lineSplit);
            if (index > 9999) {
                System.out.println("Inserted " + progressive);
                // write to db every now and then
                transaction.commit();
                transaction = session.beginTransaction();
                index = 0;
            }
            index++;
            progressive++;
        }
        bR.close();

        transaction.commit();
    }

    protected abstract void processLine( String[] lineSplit ) throws Exception;

    @SuppressWarnings("unchecked")
    public static String tableAnnotationToString( Class theClass ) throws Exception {
        Table annotation = (Table) theClass.getAnnotation(Table.class);
        String name = annotation.name();
        String schema = annotation.schema();
        StringBuilder sB = new StringBuilder();
        sB.append(schema).append(".").append(name);
        return sB.toString();
    }

    @SuppressWarnings("unchecked")
    public static String columnAnnotationToString( Class theClass, String fieldName ) throws Exception {
        Column annotation = theClass.getDeclaredField(fieldName).getAnnotation(Column.class);
        String name = annotation.name();
        boolean nullable = annotation.nullable();
        StringBuilder sB = new StringBuilder();
        sB.append(name).append(" (").append(nullable).append(")");
        return sB.toString();
    }

    @SuppressWarnings("unchecked")
    public static String joinColumnAnnotationToString( Class theClass, String fieldName )
            throws Exception {
        JoinColumn annotation = theClass.getDeclaredField(fieldName)
                .getAnnotation(JoinColumn.class);
        String name = annotation.name();
        boolean nullable = annotation.nullable();
        StringBuilder sB = new StringBuilder();
        sB.append(name).append(" (").append(nullable).append(")");
        return sB.toString();
    }


}
