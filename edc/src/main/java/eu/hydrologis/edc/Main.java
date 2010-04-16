package eu.hydrologis.edc;

import java.io.File;
import java.io.FileInputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

import eu.hydrologis.edc.databases.QueryHandler;
import eu.hydrologis.edc.ramadda.RamaddaEntry;
import eu.hydrologis.edc.ramadda.RamaddaManager;
import eu.hydrologis.edc.utils.Constants;
import eu.hydrologis.edc.utils.GeometryLoader;
import eu.hydrologis.edc.utils.HibernateManager;

public class Main {
    public static void main( String[] args ) {
        PrintStream out = System.out;

        if (args.length > 2) {
            out.println(usage());
            System.exit(0);
        }

        File propertiesFile = null;

        for( int i = 0; i < args.length; i++ ) {
            String arg = args[i];
            // --properties
            if (arg.startsWith("--properties")) {
                String propertiesPath = args[i + 1];
                propertiesFile = new File(propertiesPath);
            }
        }

        if (propertiesFile == null || !propertiesFile.exists()) {
            String userHomePath = System.getProperty("user.home");
            File inHome = new File(userHomePath + File.separator + "edc.properties");
            if (inHome.exists()) {
                out.println("Using configuration file found in the user home folder.");
                propertiesFile = inHome;
            } else {
                out.println(usage());
                System.exit(0);
            }
        }

        EDC edc = null;
        try {
            Properties properties = new Properties();
            properties.load(new FileInputStream(propertiesFile));

            // dump tables definitions
            String dumptablesDefs = properties.getProperty(Constants.DUMPTABLESDEFINITIONS);
            if (dumptablesDefs != null && Boolean.parseBoolean(dumptablesDefs)) {
                HibernateManager.dumpTableDefinitions(out);
                return;
            }

            edc = new EDC(properties, out);

            // generate database
            String generateDb = properties.getProperty(Constants.GENERATEDB);
            if (generateDb != null && Boolean.parseBoolean(generateDb)) {
                edc.generateDatabase(false, true, true);
                return;
            }

            // insert from csv
            String insertFolderPath = properties.getProperty(Constants.INSERT_FOLDER);
            String insertFilePath = properties.getProperty(Constants.INSERT_FILE);
            String insertTable = properties.getProperty(Constants.INSERT_TABLE);
            if (insertFolderPath != null && insertFolderPath.trim().length() > 0) {
                String[] split = insertFolderPath.split(",");
                File insertFolderFile = new File(split[0]);
                if (insertFolderFile.exists() && insertFolderFile.isDirectory()) {
                    for( int j = 1; j < split.length; j++ ) {
                        String fileName = split[j].trim();
                        File file = new File(insertFolderFile, fileName);
                        if (file.exists()) {
                            String tableName = file.getName().replaceFirst(".csv", "");
                            out.println("Insert table: " + tableName);
                            edc.insertFromCsv(tableName, file);
                        } else {
                            out.println("File doesn't exist: " + file.getAbsolutePath());
                        }
                    }
                } else {
                    out.println("The INSERT_FOLDER has to be an existing folder.");
                    return;
                }
            } else if (insertTable != null) {
                File insertFile = null;
                if (insertFilePath != null) {
                    insertFile = new File(insertFilePath);
                }
                edc.insertFromCsv(insertTable, insertFile);
                return;
            }

            // upload a geometry
            String geometryPath = properties.getProperty(Constants.UPLOADGEOMETRYFILE);
            String geometryType = properties.getProperty(Constants.UPLOADGEOMETRYTYPE);
            String geometrySchema = properties.getProperty(Constants.UPLOADGEOMETRYSCHEMA);
            String geometryTable = properties.getProperty(Constants.UPLOADGEOMETRYTABLE);
            if (geometryPath != null && geometryType != null && geometrySchema != null
                    && geometryTable != null) {
                GeometryLoader geometryLoader = edc.getEdcSessionFactory().getGeometryLoader();
                File geometryFile = new File(geometryPath);

                if (geometryType.trim().equals("POINT")) {
                    geometryLoader.fromPointShapefile(geometryFile, geometrySchema, geometryTable);
                } else if (geometryType.trim().equals("LINESTRING")) {
                    geometryLoader.fromLinestringShapefile(geometryFile, geometrySchema,
                            geometryTable);
                } else if (geometryType.trim().equals("POLYGON")) {
                    geometryLoader
                            .fromPolygonShapefile(geometryFile, geometrySchema, geometryTable);
                }
                return;
            }

            // print out a geometry
            String geometryPrintSchema = properties.getProperty(Constants.PRINTGEOMETRYSCHEMA);
            String geometryPrintTable = properties.getProperty(Constants.PRINTGEOMETRYTABLE);
            String geometryPrintId = properties.getProperty(Constants.PRINTGEOMETRYID);
            String geometryPrintEpsg = properties.getProperty(Constants.PRINTGEOMETRYEPSG);
            String geometryPrintHas3d = properties.getProperty(Constants.PRINTGEOMETRYHAS3D);
            Boolean has3D = Boolean.parseBoolean(geometryPrintHas3d);
            if (geometryPrintSchema != null && geometryPrintTable != null) {
                QueryHandler queryHandler = edc.getEdcSessionFactory().getQueryHandler();

                String[] split = geometryPrintId.split(",");
                Long[] ids = new Long[split.length];
                for( int i = 0; i < ids.length; i++ ) {
                    ids[i] = new Long(split[i].trim());
                }

                Map<Long, Geometry> geometryMap = null;
                if (has3D) {
                    geometryMap = queryHandler.getGeometries3D(geometryPrintSchema,
                            geometryPrintTable, geometryPrintEpsg, ids);
                } else {
                    geometryMap = queryHandler.getGeometries(geometryPrintSchema,
                            geometryPrintTable, geometryPrintEpsg, ids);
                }

                out.println(geometryPrintSchema + "." + geometryPrintTable);

                Set<Entry<Long, Geometry>> entrySet = geometryMap.entrySet();
                for( Entry<Long, Geometry> entry : entrySet ) {
                    Long id = entry.getKey();
                    out.println("id = " + id);
                    Geometry geometry = entry.getValue();
                    Coordinate[] coordinates = geometry.getCoordinates();
                    for( Coordinate coordinate : coordinates ) {
                        out.println(coordinate.x + " / " + coordinate.y + " / " + coordinate.z);
                    }
                }
                return;
            }

            /*
             * ramadda
             */
            RamaddaManager ramaddaManager = edc.createRamaddaManager();

            /*
             * dump the content tree of ramadda
             */
            String ramaddaDumptree = properties.getProperty(Constants.RAMADDA_DUMPTREE);
            if (Boolean.parseBoolean(ramaddaDumptree)) {
                ramaddaManager.dumpTree();
                return;
            }

            /*
             * upload to ramadda
             */
            String ramaddaFileUploadPath = properties.getProperty(Constants.RAMADDA_FILEUPLOAD);
            String ramaddaFileUploadParentId = properties
                    .getProperty(Constants.RAMADDA_FILEUPLOADPARENTID);
            if (ramaddaFileUploadPath != null) {
                File file = new File(ramaddaFileUploadPath);
                if (file.exists()) {
                    String name = file.getName();
                    int lastDot = name.lastIndexOf(".");
                    String ext = name.substring(lastDot);

                    // RamaddaEntry ramaddaEntry = ramaddaManager
                    // .getRamaddaEntryById(ramaddaFileUploadParentId);
                    // String entryPath = ramaddaEntry.getEntryPath();
                    String uploadFile = ramaddaManager.uploadFile(name, name, ext,
                            ramaddaFileUploadParentId, ramaddaFileUploadPath);
                    out.println(uploadFile);

                    // String entryXml = ramaddaManager.getRepositoryClient().getEntryXml(
                    // ramaddaFileUploadParentId);
                    // out.println(entryXml);
                } else {
                    out.println("The RAMADDA_FILEUPLOAD has to be an existing file.");
                    return;
                }
            }

            String ramaddaFileDownloadId = properties.getProperty(Constants.RAMADDA_FILEDOWNLOADID);
            String ramaddaFileDownloadPath = properties
                    .getProperty(Constants.RAMADDA_FILEDOWNLOADFOLDER);
            if (ramaddaFileDownloadId != null && ramaddaFileDownloadPath != null) {
                File file = new File(ramaddaFileDownloadPath);
                if (file.getParentFile().canWrite()) {
                    ramaddaManager.downloadFileFromRamadda(ramaddaFileDownloadId, file);
                } else {
                    out
                            .println("The RAMADDA_FILEDOWNLOADFOLDER parent folder has to be a writable folder.");
                    return;
                }
            }

            String id = properties.getProperty(Constants.RAMADDA_FINDBYID);
            if (id != null) {
                RamaddaEntry entry = ramaddaManager.getRamaddaEntryById(id);
                out.println(entry);
            }

            String name = properties.getProperty(Constants.RAMADDA_FINDBYNAME);
            if (name != null) {
                RamaddaEntry entry = ramaddaManager.findRamaddaEntryByName(name);
                out.println(entry);
            }

            id = properties.getProperty(Constants.RAMADDA_OPENDAPBYID);
            if (id != null) {
                RamaddaEntry entry = ramaddaManager.getRamaddaEntryById(id);
                out.println(entry.getOpendapUrl());
            }

            String newGroupName = properties.getProperty(Constants.RAMADDA_NEWGROUP);
            String parentGroupId = properties.getProperty(Constants.RAMADDA_NEWGROUPPARENTID);
            if (newGroupName != null) {
                ramaddaManager.createNewGroup(parentGroupId, newGroupName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (edc != null)
                try {
                    if (edc.hasEdcSessionFactory())
                        edc.getEdcSessionFactory().closeSessionFactory();
                } catch (Exception e) {
                    e.printStackTrace();
                }
        }
    }
    private static String usage() {
        return "USAGE: --properties propertiesFilePath";
    }
}
