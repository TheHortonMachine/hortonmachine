/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.grass.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.sax.SAXSource;

import org.jgrasstools.grass.dtd64.Gisprompt;
import org.jgrasstools.grass.dtd64.GrassInterface;
import org.jgrasstools.grass.dtd64.Task;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * Grass utilities.
 * 
 * <p>Where possible Sextante stuff has been used. So credits for those go to Victor Olaya.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GrassUtils {

    /**
     * Prefix for parameter variables names. 
     */
    public static String VARIABLE_PARAMETER_PREFIX = "$$";
    public static String VARIABLE_PARAMETER_PREFIX_REGEX = "\\$\\$";

    /**
     * Prefix for flag variables names. 
     */
    public static String VARIABLE_FLAG_PREFIX = "$$";
    public static String VARIABLE_FLAG_PREFIX_REGEX = "\\$\\$";

    /**
     * Suffix for parameter variables names. 
     */
    public static String VARIABLE_PARAMETER_SUFFIX = "PARAMETER";

    /**
     * Suffix for flag variables names. 
     */
    public static String VARIABLE_FLAG_SUFFIX = "FLAG";

    /**
     * Substitution string from dots in parameters names. 
     */
    public static String VARIABLE_DOT_SUBSTITUTION = "__";
    public static String VARIABLE_DOT_SUBSTITUTION_REG = "\\_\\_";

    /**
     * Key used to set and retrieve the grass installation location.
     * 
     * <p>Example on linux: /usr/lib/grass64
     */
    public static String GRASS_ENVIRONMENT_GISBASE_KEY = "jgt-grass.gisbase";

    /**
     * Key used to set and retrieve the grass shell.
     * 
     * <p>Example on linux: /usr/bin/sh
     */
    public static String GRASS_ENVIRONMENT_SHELL_KEY = "jgt-grass.shell";

    /**
     * Category to use for raster processing.
     */
    public static String GRASS_RASTER_CATEGORY = "Grass/Raster Modules";

    /**
     * Category to use for raster 3d processing.
     */
    public static String GRASS_RASTER3D_CATEGORY = "Grass/Raster 3D Modules";

    /**
     * Category to use for vector processing.
     */
    public static String GRASS_VECTOR_CATEGORY = "Grass/Vector Modules";

    /**
     * Category to use for imagery.
     */
    public static String GRASS_IMAGERY_CATEGORY = "Grass/Imagery Modules";

    /**
     * Category to use for imagery.
     */
    public static String GRASS_DISPLAY_CATEGORY = "Grass/Display Modules";

    /**
     * Category to use for generic.
     */
    public static String GRASS_GENERAL_CATEGORY = "Grass/General Modules";

    /**
     * Category to use for database.
     */
    public static String GRASS_DATABASE_CATEGORY = "Grass/Database Modules";

    /**
     * Category to use for otherl modules.
     */
    public static String GRASS_OTHER_CATEGORY = "Grass";

    /**
     * GUI hint: item is an existing file path.
     */
    public static final String FILEIN_UI_HINT = "infile";

    /**
     * GUI hint: item is an existing folder path.
     */
    public static final String FOLDERIN_UI_HINT = "infolder";

    /**
     * GUI hint: item is a new file path.
     */
    public static final String FILEOUT_UI_HINT = "outfile";

    /**
     * GUI hint: item is a new folder path.
     */
    public static final String FOLDEROUT_UI_HINT = "outfolder";

    /**
     * GUI hint: item is a grass file.
     */
    public static final String GRASSFILE_UI_HINT = "grassfile";

    /**
     * Module to ignore during the code generation.
     */
    public static final List<String> grassModulesToIgnore = Arrays.asList("mkftcap.bat");

    /**
     * Modules that can't be launched in non-interactive mode or simply do not make sense.
     */
    public static final List<String> incompatibleGrassModules = Arrays.asList("v.build.polylines", "v.build", "v.category",
            "v.convert", "v.db.connect", "v.digit", "v.in.db", "v.in.sites", "v.kernel", "v.label.sa", "v.label", "v.lrs.create",
            "v.lrs.label", "v.lrs.segment", "v.lrs.where", "v.proj", "v.support", "v.to.rast3", "v.what", "v.what.rast",
            "r.compress", "r.random.surface", "r.region", "r.support", "r.support.stats", "r.timestamp", "r.to.rast3",
            "r.to.rast3elev", "r.what", "r.what.color", "v.net.alloc", "v.net", "v.net.iso", "v.net.path", "v.net.salesman",
            "v.net.steiner", "v.net.visibility", "r.le.setup", "r.le.patch", "r.le.pixel", "r.le.trace", "r.li.cwed",
            "r.li.dominance", "r.li.edgedensity", "r.li.mpa", "r.li.mps", "r.li.padcv", "r.li.padrange", "r.li.padsd",
            "r.li.patchdensity", "r.li.patchnum", "r.li.richness", "r.li.setup", "r.li.shannon", "r.li.shape", "r.li.simpson",
            "r.series", "r.blend", "r.cats", "r.mask", "r.tileset", "v.build.all", "v.centroids", "v.convert.all", "v.db.addcol",
            "v.db.addtable", "v.db.dropcol", "v.db.droptable", "v.db.join", "v.db.reconnect.all", "v.db.renamecol",
            "v.db.univar", "v.db.update", "v.in.e00", "v.in.sites.all", "v.univar.sh", "r.external", "v.external", "v.colors",
            "v.in.garmin", "v.in.gpsbabel", "v.out.gpsbabel", "r.proj", "v.proj", "r.category");

    public static final List<String> reservedWords = Arrays
            .asList("abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const", "continue",
                    "default", "double", "do", "else", "enum", "extends", "false", "final", "finally", "float", "for", "goto",
                    "if", "implements", "import", "instanceof", "int", "interface", "long", "native", "new", "null", "package",
                    "private", "protected", "public", "return", "short", "static", "strictfp", "super", "switch", "synchronized",
                    "this", "throw", "throws", "transient", "true", "try", "void", "volatile", "while");

    // The default prefix for temporary files and folders
    public static final String TMP_PREFIX = "JGT-";

    /**
     * The base package to use to generate the wrapper code in.
     */
    public static final String BASEPACKAGE_PATH = "org/osgeo/grass/";
    public static final String BASEPACKAGE = "org.osgeo.grass.";

    /**
     * Get the jaxb grass {@link Task} for a given xml string.
     * 
     * @param grassXml te xml string to parse.
     * @return the grass task.
     * @throws Exception
     */
    public static Task getTask( String grassXml ) throws Exception {
        String FEATURE_NAMESPACES = "http://xml.org/sax/features/namespaces";
        String FEATURE_NAMESPACE_PREFIXES = "http://xml.org/sax/features/namespace-prefixes";
        JAXBContext jaxbContext = JAXBContext.newInstance(GrassInterface.class);

        XMLReader xmlreader = XMLReaderFactory.createXMLReader();
        xmlreader.setFeature(FEATURE_NAMESPACES, true);
        xmlreader.setFeature(FEATURE_NAMESPACE_PREFIXES, true);
        xmlreader.setEntityResolver(new EntityResolver(){
            public InputSource resolveEntity( String publicId, String systemId ) throws SAXException, IOException {
                InputSource inputSource = new InputSource(GrassInterface.class.getResourceAsStream("/grass-interface.dtd"));
                return inputSource;
            }
        });

        InputSource input = new InputSource(new StringReader(grassXml));
        Source source = new SAXSource(xmlreader, input);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        Task grassInterface = (Task) unmarshaller.unmarshal(source);
        return grassInterface;
    }

    /**
     * Creates a path and folder in the system's default temporary files path. This will be used to store temporary GRASS mapsets
     * for data processing. This will only create one new temporary mapset name per session, which should be all we ever need (the
     * actual temporary GRASS settings files within the mapset are written from scratch each time a GRASS module is run).
     */
    public static String createTemporaryMapsetName() {
        UUID id;
        String tmpPrefix;
        String tmpSuffix;
        String tmpBase;
        String tmpFolder;

        id = UUID.randomUUID();
        tmpPrefix = new String(GrassUtils.TMP_PREFIX);
        tmpSuffix = new String("_" + id);
        tmpBase = new String(System.getProperty("java.io.tmpdir"));
        if (tmpBase.endsWith(File.separator)) {
            tmpFolder = new String(tmpBase + tmpPrefix + tmpSuffix.replace('-', '_') + File.separator + "user");
        } else {
            tmpFolder = new String(tmpBase + File.separator + tmpPrefix + tmpSuffix.replace('-', '_') + File.separator + "user");
        }
        return tmpFolder;
    }

    /**
     * Writes GRASS region ("Windows") settings into a text file.
     * 
     * @param filename name of the text file to write to.
     * @param isLatLong flag to define if lat/long.
     * @throws IOException
     */
    private static void writeGRASSWindow( final String filename, boolean isLatLong ) throws IOException {

        new File(filename).createNewFile();
        try {
            final FileWriter fstream = new FileWriter(filename);
            final BufferedWriter out = new BufferedWriter(fstream);
            if (!isLatLong) {
                /* XY location */
                out.write("proj:       0\n");
                out.write("zone:       0\n");
                out.write("north:      1\n");
                out.write("south:      0\n");
                out.write("east:       1\n");
                out.write("west:       0\n");
                out.write("cols:       1\n");
                out.write("rows:       1\n");
                out.write("e-w resol:  1\n");
                out.write("n-s resol:  1\n");
                out.write("top:        1\n");
                out.write("bottom:     0\n");
                out.write("cols3:      1\n");
                out.write("rows3:      1\n");
                out.write("depths:     1\n");
                out.write("e-w resol3: 1\n");
                out.write("n-s resol3: 1\n");
                out.write("t-b resol:  1\n");
            } else {
                /* lat/lon location */
                out.write("proj:       3\n");
                out.write("zone:       0\n");
                out.write("north:      1N\n");
                out.write("south:      0\n");
                out.write("east:       1E\n");
                out.write("west:       0\n");
                out.write("cols:       1\n");
                out.write("rows:       1\n");
                out.write("e-w resol:  1\n");
                out.write("n-s resol:  1\n");
                out.write("top:        1\n");
                out.write("bottom:     0\n");
                out.write("cols3:      1\n");
                out.write("rows3:      1\n");
                out.write("depths:     1\n");
                out.write("e-w resol3: 1\n");
                out.write("n-s resol3: 1\n");
                out.write("t-b resol:  1\n");
            }
            out.close();
        } catch (final IOException e) {
            throw (e);
        }
    }

    /**
     * Prepare a tmp mapset for run. 
     * 
     * @param isLatLong
     * @return the mapset path.
     * @throws IOException
     */
    public static String prepareMapsetForRun( boolean isLatLong ) throws IOException {
        String tmpMapset = GrassUtils.createTemporaryMapsetName();
        GrassUtils.createTemporaryMapset(tmpMapset, isLatLong);
        return tmpMapset;
    }

    /**
     * Create a gisrc file to run grass commands in a given mapset.
     * 
     * @param mapsetPath the mapset path.
     * @return the created gisrc path.
     * @throws IOException
     */
    public static String createGisRc( String mapsetPath ) throws IOException {
        File tmpMapsetFile = new File(mapsetPath);
        File tmpLocationFile = tmpMapsetFile.getParentFile();
        File tmpGrassdbFile = tmpLocationFile.getParentFile();

        File tmpGisrc = File.createTempFile(GrassUtils.TMP_PREFIX, ".gisrc");
        BufferedWriter gisRcWriter = new BufferedWriter(new FileWriter(tmpGisrc));
        gisRcWriter.write("GISDBASE: " + tmpGrassdbFile.getAbsolutePath() + "\n");
        gisRcWriter.write("LOCATION_NAME: " + tmpLocationFile.getName() + "\n");
        gisRcWriter.write("MAPSET: " + tmpMapsetFile.getName() + "\n");
        gisRcWriter.write("GRASS_GUI: text\n");
        gisRcWriter.close();

        return tmpGisrc.getAbsolutePath();
    }

    /**
     * Creates a temporary location and mapset(s) for GRASS data processing.
     * 
     * <p>A minimal set of folders and files is created in the
     * system's default temporary directory. The settings files are written with sane defaults, so GRASS can do its work. File
     * structure and content will vary slightly depending on whether the user wants to process lat/lon or x/y data.
     *
     * @param mapsetFolder the mapset folder where to create the mapset.
     * @param isLatLon flag to define if lat/long.
     * @throws IOException
     */
    public static void createTemporaryMapset( String mapsetFolder, boolean isLatLon ) throws IOException {

        // FIXME add folder creation checks
        final String tmpFolder = new String(mapsetFolder.substring(0, mapsetFolder.lastIndexOf(File.separator)));
        final boolean b = new File(tmpFolder).mkdir();
        new File(tmpFolder + File.separator + "PERMANENT").mkdir();
        new File(tmpFolder + File.separator + "user").mkdir();
        new File(tmpFolder + File.separator + "PERMANENT" + File.separator + ".tmp").mkdir();
        writeGRASSWindow(tmpFolder + File.separator + "PERMANENT" + File.separator + "DEFAULT_WIND", isLatLon);
        new File(tmpFolder + File.separator + "PERMANENT" + File.separator + "MYNAME").createNewFile();
        try {
            final FileWriter fstream = new FileWriter(tmpFolder + File.separator + "PERMANENT" + File.separator + "MYNAME");
            final BufferedWriter out = new BufferedWriter(fstream);
            if (!isLatLon) {
                /* XY location */
                out.write("SEXTANTE GRASS interface: temporary x/y data processing location.\n");
            } else {
                /* lat/lon location */
                out.write("SEXTANTE GRASS interface: temporary lat/lon data processing location.\n");
            }
            out.close();
        } catch (final IOException e) {
            throw (e);
        }
        if (isLatLon) {
            new File(tmpFolder + File.separator + "PERMANENT" + File.separator + "PROJ_INFO").createNewFile();
            try {
                final FileWriter fstream = new FileWriter(tmpFolder + File.separator + "PERMANENT" + File.separator + "PROJ_INFO");
                final BufferedWriter out = new BufferedWriter(fstream);
                out.write("name: Latitude-Longitude\n");
                out.write("proj: ll\n");
                out.write("ellps: wgs84\n");
                out.close();
            } catch (final IOException e) {
                throw (e);
            }
            new File(tmpFolder + File.separator + "PERMANENT" + File.separator + "PROJ_UNITS").createNewFile();
            try {
                final FileWriter fstream = new FileWriter(tmpFolder + File.separator + "PERMANENT" + File.separator
                        + "PROJ_UNITS");
                final BufferedWriter out = new BufferedWriter(fstream);
                out.write("unit: degree\n");
                out.write("units: degrees\n");
                out.write("meters: 1.0\n");
                out.close();
            } catch (final IOException e) {
                throw (e);
            }
        }
        writeGRASSWindow(tmpFolder + File.separator + "PERMANENT" + File.separator + "WIND", isLatLon);
        new File(tmpFolder + File.separator + "user" + File.separator + "dbf").mkdir();
        new File(tmpFolder + File.separator + "user" + File.separator + ".tmp").mkdir();
        new File(tmpFolder + File.separator + "user" + File.separator + "VAR").createNewFile();
        try {
            final FileWriter fstream = new FileWriter(tmpFolder + File.separator + "user" + File.separator + "VAR");
            final BufferedWriter out = new BufferedWriter(fstream);
            out.write("DB_DRIVER: dbf\n");
            out.write("DB_DATABASE: $GISDBASE/$LOCATION_NAME/$MAPSET/dbf/\n");
            out.close();
        } catch (final IOException e) {
            throw (e);
        }
        writeGRASSWindow(tmpFolder + File.separator + "user" + File.separator + "WIND", isLatLon);
    }

    /*
     * Helper function for deleteTempMapset()
     */
    private static boolean deleteDirectory( final File path ) {
        if (path.exists()) {
            final File[] files = path.listFiles();
            for( int i = 0; i < files.length; i++ ) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * Deletes the location and mapset(s) for GRASS data processing.
     */
    public static void deleteTempMapset( String mapsetFolder ) {
        if ((mapsetFolder != null) && (mapsetFolder.length() > 2)) {
            String tmpFolder = new String(mapsetFolder.substring(0, mapsetFolder.lastIndexOf(File.separator)));
            if (new File(tmpFolder).exists()) {
                deleteDirectory(new File(tmpFolder));
            }
        }
    }

    /**
     * Returns "true" if the current operating system is a variant of Windows.
     * 
     * @return "true", if we are running on Windows, "false" otherwise.
     */
    public static boolean isWindows() {
        final String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("win") >= 0);
    }

    /**
     * Returns "true" if the current operating system is Mac OS X.
     * 
     * @return "true", if we are running on Mac OS X, "false" otherwise.
     */
    public static boolean isMacOSX() {
        final String os = System.getProperty("os.name").toLowerCase();
        return (os.indexOf("mac") >= 0);
    }

    /**
     * Returns "true" if the current operating system is some Unix flavor (including Linux). Does not return true for Mac OS X.
     * 
     * @return "true", if we are running on Unix/Linux, "false" otherwise.
     */
    public static boolean isUnix() {
        final String os = System.getProperty("os.name").toLowerCase();
        return ((os.indexOf("nix") >= 0) || (os.indexOf("nux") >= 0));
    }

    /**
     * Guess the category to use from the module name.
     * 
     * @param name the module's name.
     * @return the guessed category or <code>null</code> if none was guessed.
     */
    public static String name2GrassCategory( String name ) {
        if (name.toLowerCase().startsWith("r.")) {
            return GRASS_RASTER_CATEGORY;
        } else if (name.toLowerCase().startsWith("v.")) {
            return GRASS_VECTOR_CATEGORY;
        } else if (name.toLowerCase().startsWith("r3.")) {
            return GRASS_RASTER3D_CATEGORY;
        } else if (name.toLowerCase().startsWith("i.")) {
            return GRASS_IMAGERY_CATEGORY;
        } else if (name.toLowerCase().startsWith("db.")) {
            return GRASS_DATABASE_CATEGORY;
        } else if (name.toLowerCase().startsWith("g.")) {
            return GRASS_GENERAL_CATEGORY;
        } else if (name.toLowerCase().startsWith("d.")) {
            return GRASS_DISPLAY_CATEGORY;
        } else {

        }
        return null;
    }

    /**
     * Get the hints string from the {@link Gisprompt}.
     * 
     * @param gisprompt the gisprompt to use.
     * @return the guihint to use for gui generation.
     */
    public static String getGuiHintsFromGisprompt( Gisprompt gisprompt ) {
        String age = gisprompt.getAge();
        String element = gisprompt.getElement();
        String prompt = gisprompt.getPrompt();

        if (age.trim().equals("old")) {
            // existing file == open file hint
            return FILEIN_UI_HINT + "," + GRASSFILE_UI_HINT;
        } else if (age.trim().equals("new")) {
            // new file == save file hint
            return FILEOUT_UI_HINT + "," + GRASSFILE_UI_HINT;
        } else if (age.trim().equals("mapset")) {
            // mapset == open folder hint
            return FOLDERIN_UI_HINT;
        }
        // else if (age.trim().equals("any")) {
        // // ???
        // return null;
        // }

        return null;
    }

    /**
     * Get the full qualified file path of the given module.
     * 
     * <p>Note that the module can't have dots in it. It is supposed
     * to be a class safe name with underscores.
     * 
     * @param module the module's name.
     * @return the path to the module to be used in {@link File} construction.
     */
    public static String getModuleQualifiedStructure( String module ) {
        String[] split = module.split("\\_");
        String prefix = split[0];
        String qualifiedName = BASEPACKAGE_PATH + prefix + "/" + module + ".java";
        return qualifiedName;
    }

    /**
     * Get the full qualified name of the given module.
     * 
     * <p>Note that the module can't have dots in it. It is supposed
     * to be a class safe name with underscores.
     * 
     * @param module the module's name.
     * @return the package for the module.
     */
    public static String getModulePackage( String module ) {
        String[] split = module.split("\\_");
        String prefix = split[0];
        String qualifiedName = BASEPACKAGE + prefix;
        return qualifiedName;
    }

    /**
     * Check validity of a var name and returns the proper name.
     * 
     * @param parameterName the name to check.
     * @param prefix the prefix to give in case of invalid names.
     * @return the new or unchanged name.
     */
    public static String checkValidVar( String parameterName, String prefix ) {
        boolean isValid = true;
        if (!parameterName.matches("^[a-zA-Z][a-zA-Z0-9]*?$")) {
            isValid = false;
        }
        if (GrassUtils.reservedWords.contains(parameterName)) {
            isValid = false;
        }
        if (!isValid)
            parameterName = prefix + parameterName;
        return parameterName;
    }

}
