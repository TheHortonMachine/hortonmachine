/*
 * Library name : dxf
 * (C) 2006 Micha�l Michaud
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * For more information, contact:
 *
 * michael.michaud@free.fr
 *
 */

package org.hortonmachine.gears.io.dxfdwg.libs.dxf;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * A whole dataset contained in a DXF file, and main methods to read from and
 * to write to the file.
 * 
 * The DxfFile class is one of the main class of the dxf library.
 * @author Micha�l Michaud
 * @version 0.5.0
 */
// History
// 2006-11-12 : remove the header writing option
// 2006-10-19 : add optional '_' suffix
// add optional header writer
// DXF layer name is taken from layer attribute if it exists or
// from layer name else if
@SuppressWarnings("nls")
public class DxfFile {
    public final static DxfGroup SECTION = new DxfGroup(0, "SECTION");
    public final static DxfGroup ENDSEC = new DxfGroup(0, "ENDSEC");
    public final static DxfGroup EOF = new DxfGroup(0, "EOF");
    public final static DxfGroup HEADER = new DxfGroup(2, "HEADER");
    // La section CLASSES suivante est posterieure � la version 12 de DXF
    public final static DxfGroup CLASSES = new DxfGroup(2, "CLASSES");
    public final static DxfGroup TABLES = new DxfGroup(2, "TABLES");
    public final static DxfGroup BLOCKS = new DxfGroup(2, "BLOCKS");
    public final static DxfGroup ENTITIES = new DxfGroup(2, "ENTITIES");
    // La section OBJECTS suivante est posterieure � la version 12 de DXF
    public final static DxfGroup OBJECTS = new DxfGroup(2, "OBJECTS");
    // Schema de donn�es g�n�ral pour les ENTITIES
    public static SimpleFeatureType DXF_POINTSCHEMA = null;
    public static SimpleFeatureType DXF_LINESCHEMA = null;
    public static SimpleFeatureType DXF_POLYGONSCHEMA = null;

    public static boolean DXF_SCHEMA_INITIALIZED = false;
    static int iterator = 0;
    private DxfHEADER header = null;
    private DxfCLASSES classes = null;
    private DxfTABLES tables = null;
    private DxfBLOCKS blocks = null;
    private DxfENTITIES entities = null;
    private int coordinatePrecision = 2;
    Map featureSchemas = new HashMap();
    Map datasets = new HashMap();

    SimpleFeatureCollection pointFeatures;
    SimpleFeatureCollection lineFeatures;
    SimpleFeatureCollection polygonFeatures;

    private final CoordinateReferenceSystem crs;
    private static int fid = 0;

    public static synchronized int getNextFid() {
        fid = fid + 1;
        return fid;
    }

    public static synchronized void resetFid() {
        fid = 0;
    }

    public DxfFile( CoordinateReferenceSystem crs ) {
        this.crs = crs;
        resetFid();
        initializeDXF_SCHEMA(crs);
    }

    /**
     * Initialize a JUMP FeatureSchema to load dxf data keeping some graphic
     * attributes.
     */

    public static synchronized void initializeDXF_SCHEMA( CoordinateReferenceSystem crs ) {
        if (DXF_POINTSCHEMA != null && DXF_POINTSCHEMA.getAttributeCount() != 0)
            return;

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("dxfpointfile");
        b.setCRS(crs);
        b.add("the_geom", Point.class);
        b.add("LAYER", String.class);
        b.add("LTYPE", String.class);
        b.add("ELEVATION", Double.class);
        b.add("THICKNESS", Double.class);
        b.add("COLOR", Integer.class);
        b.add("TEXT", String.class);
        b.add("TEXT_HEIGHT", Double.class);
        b.add("TEXT_STYLE", String.class);
        DXF_POINTSCHEMA = b.buildFeatureType();

        b = new SimpleFeatureTypeBuilder();
        b.setName("dxflinefile");
        b.setCRS(crs);
        b.add("the_geom", LineString.class);
        b.add("LAYER", String.class);
        b.add("LTYPE", String.class);
        b.add("ELEVATION", Double.class);
        b.add("THICKNESS", Double.class);
        b.add("COLOR", Integer.class);
        b.add("TEXT", String.class);
        b.add("TEXT_HEIGHT", Double.class);
        b.add("TEXT_STYLE", String.class);
        DXF_LINESCHEMA = b.buildFeatureType();

        b = new SimpleFeatureTypeBuilder();
        b.setName("dxfpolygonfile");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("LAYER", String.class);
        b.add("LTYPE", String.class);
        b.add("ELEVATION", Double.class);
        b.add("THICKNESS", Double.class);
        b.add("COLOR", Integer.class);
        b.add("TEXT", String.class);
        b.add("TEXT_HEIGHT", Double.class);
        b.add("TEXT_STYLE", String.class);
        DXF_POLYGONSCHEMA = b.buildFeatureType();

    }

    public int getCoordinatePrecision() {
        return coordinatePrecision;
    }
    public void setCoordinatePrecision( int coordinatePrecision ) {
        this.coordinatePrecision = coordinatePrecision;
    }

    public static DxfFile createFromFile( File file, CoordinateReferenceSystem crs ) throws IOException {
        RandomAccessFile raf = new RandomAccessFile(file, "r");
        return createFromFile(raf, crs);
    }

    public static DxfFile createFromFile( RandomAccessFile raf, CoordinateReferenceSystem crs ) throws IOException {
        DxfFile dxfFile = new DxfFile(crs);
        initializeDXF_SCHEMA(crs);

        dxfFile.pointFeatures = new DefaultFeatureCollection();
        dxfFile.lineFeatures = new DefaultFeatureCollection();
        dxfFile.polygonFeatures = new DefaultFeatureCollection();

        DxfGroup group = null;
        while( null != (group = DxfGroup.readGroup(raf)) ) {
            if (group.equals(SECTION)) {
                group = DxfGroup.readGroup(raf);
                System.out.println("SECTION " + group.getValue());
                if (group.equals(HEADER)) {
                    dxfFile.header = DxfHEADER.readHeader(raf);
                } else if (group.equals(CLASSES)) {
                    dxfFile.classes = DxfCLASSES.readClasses(raf);
                } else if (group.equals(TABLES)) {
                    dxfFile.tables = DxfTABLES.readTables(raf);
                } else if (group.equals(BLOCKS)) {
                    dxfFile.blocks = DxfBLOCKS.readEntities(raf);
                    // dxfFile.datasets.put("BLOCKS", dxfFile.blocks.entities);
                    ((DefaultFeatureCollection) dxfFile.pointFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.blocks.pointEntities);
                    ((DefaultFeatureCollection) dxfFile.lineFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.blocks.lineEntities);
                    ((DefaultFeatureCollection) dxfFile.polygonFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.blocks.polygonEntities);
                } else if (group.equals(ENTITIES)) {
                    dxfFile.entities = DxfENTITIES.readEntities(raf);
                    // dxfFile.datasets.put("ENTITIES", dxfFile.entities.entities);
                    ((DefaultFeatureCollection) dxfFile.pointFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.entities.pointEntities);
                    ((DefaultFeatureCollection) dxfFile.lineFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.entities.lineEntities);
                    ((DefaultFeatureCollection) dxfFile.polygonFeatures)
                            .addAll((SimpleFeatureCollection) dxfFile.entities.polygonEntities);
                } else if (group.equals(OBJECTS)) {
                    // objects = DxfOBJECTS.readObjects(br);
                    System.out.println("Jump objects: " + group.getValue());
                } else if (group.getCode() == 999) {
                    System.out.println("Jump 999: " + group.getValue());
                } else {
                    System.out.println("Group " + group.getCode() + " " + group.getValue() + " UNKNOWN");
                }
            } else if (group.getCode() == 999) {
                // System.out.println("Commentaire : " + group.getValue());
            } else if (group.equals(EOF)) {
                break;
            } else {
                // System.out.println("Group " + group.getCode() + " " + group.getValue() +
                // " UNKNOWN");
            }
        }
        raf.close();
        return dxfFile;
    }

    public SimpleFeatureCollection getPoints() {
        return pointFeatures;
    }
    public SimpleFeatureCollection getLines() {
        return lineFeatures;
    }
    public SimpleFeatureCollection getPolygons() {
        return polygonFeatures;
    }

    public static Set getLayers( File file ) throws IOException {
        Set datasets = new HashSet();
        datasets.add("BLOCKS");
        datasets.add("ENTITIES");
        return datasets;
    }

    // public static void write( FeatureCollection features, String[] layerNames, FileWriter fw,
    // int precision, boolean suffix ) {
    // FeatureSchema schema = features.getFeatureSchema();
    // Envelope envelope = features.getEnvelope();
    // // System.out.println("layerNames : " + layerNames + " (" + layerNames.length + ")");
    // // System.out.println("header : " + header);
    // // System.out.println("suffix : " + suffix);
    // Date date = new Date(System.currentTimeMillis());
    // try {
    // // if (header) {
    // // COMMENTAIRES DU TRADUCTEUR
    // fw.write(DxfGroup.toString(999, Integer.toString(features.size()) + " features"));
    // fw.write(DxfGroup.toString(999,
    // "TRANSLATION BY fr.michaelm.jump.drivers.dxf.DxfFile (v 0.4)"));
    // fw.write(DxfGroup.toString(999, "DATE : " + date.toString()));
    //
    // // ECRITURE DU HEADER
    // fw.write(DxfGroup.toString(0, "SECTION"));
    // fw.write(DxfGroup.toString(2, "HEADER"));
    // fw.write(DxfGroup.toString(9, "$ACADVER"));
    // fw.write(DxfGroup.toString(1, "AC1009"));
    // fw.write(DxfGroup.toString(9, "$CECOLOR"));
    // fw.write(DxfGroup.toString(62, 256));
    // fw.write(DxfGroup.toString(9, "$CELTYPE"));
    // fw.write(DxfGroup.toString(6, "DUPLAN"));
    // fw.write(DxfGroup.toString(9, "$CLAYER"));
    // fw.write(DxfGroup.toString(8, "0")); // corrected by L. Becker on 2006-11-08
    // fw.write(DxfGroup.toString(9, "$ELEVATION"));
    // fw.write(DxfGroup.toString(40, 0.0, 3));
    // fw.write(DxfGroup.toString(9, "$EXTMAX"));
    // fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
    // fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
    // // fw.write(DxfGroup.toString(30, envelope.getMaxX(), 6));
    // fw.write(DxfGroup.toString(9, "$EXTMIN"));
    // fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
    // fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
    // // fw.write(DxfGroup.toString(30, envelope.getMaxX(), 6));
    // fw.write(DxfGroup.toString(9, "$INSBASE"));
    // fw.write(DxfGroup.toString(10, 0.0, 1));
    // fw.write(DxfGroup.toString(20, 0.0, 1));
    // fw.write(DxfGroup.toString(30, 0.0, 1));
    // fw.write(DxfGroup.toString(9, "$LIMCHECK"));
    // fw.write(DxfGroup.toString(70, 1));
    // fw.write(DxfGroup.toString(9, "$LIMMAX"));
    // fw.write(DxfGroup.toString(10, envelope.getMaxX(), 6));
    // fw.write(DxfGroup.toString(20, envelope.getMaxY(), 6));
    // fw.write(DxfGroup.toString(9, "$LIMMIN"));
    // fw.write(DxfGroup.toString(10, envelope.getMinX(), 6));
    // fw.write(DxfGroup.toString(20, envelope.getMinY(), 6));
    // fw.write(DxfGroup.toString(9, "$LUNITS"));
    // fw.write(DxfGroup.toString(70, 2));
    // fw.write(DxfGroup.toString(9, "$LUPREC"));
    // fw.write(DxfGroup.toString(70, 2));
    // fw.write(DxfGroup.toString(0, "ENDSEC"));
    // // }
    // // ECRITURE DES TABLES
    // fw.write(DxfGroup.toString(0, "SECTION"));
    // fw.write(DxfGroup.toString(2, "TABLES"));
    // fw.write(DxfGroup.toString(0, "TABLE"));
    // fw.write(DxfGroup.toString(2, "STYLE"));
    // fw.write(DxfGroup.toString(70, 1));
    // fw.write(DxfGroup.toString(0, "STYLE")); // added by L. Becker on 2006-11-08
    // DxfTABLE_STYLE_ITEM style = new DxfTABLE_STYLE_ITEM("STANDARD", 0, 0f, 1f, 0f, 0, 1.0f,
    // "xxx.txt", "yyy.txt");
    // fw.write(style.toString());
    // fw.write(DxfGroup.toString(0, "ENDTAB"));
    // fw.write(DxfGroup.toString(0, "TABLE"));
    // fw.write(DxfGroup.toString(2, "LTYPE"));
    // fw.write(DxfGroup.toString(70, 1));
    // fw.write(DxfGroup.toString(0, "LTYPE")); // added by L. Becker on 2006-11-08
    // DxfTABLE_LTYPE_ITEM ltype = new DxfTABLE_LTYPE_ITEM("CONTINUE", 0, "", 65, 0f,
    // new float[0]);
    // fw.write(ltype.toString());
    // fw.write(DxfGroup.toString(0, "ENDTAB"));
    // fw.write(DxfGroup.toString(0, "TABLE"));
    // fw.write(DxfGroup.toString(2, "LAYER"));
    // fw.write(DxfGroup.toString(70, 2));
    // for( int i = 0; i < layerNames.length; i++ ) {
    // DxfTABLE_LAYER_ITEM layer = new DxfTABLE_LAYER_ITEM(layerNames[i], 0, 131,
    // "CONTINUE");
    // fw.write(DxfGroup.toString(0, "LAYER")); // added by L. Becker on 2006-11-08
    // fw.write(layer.toString());
    // if (suffix) {
    // layer = new DxfTABLE_LAYER_ITEM(layerNames[i] + "_", 0, 131, "CONTINUE");
    // fw.write(DxfGroup.toString(0, "LAYER")); // added by L. Becker on 2006-11-08
    // fw.write(layer.toString());
    // }
    // }
    // fw.write(DxfGroup.toString(0, "ENDTAB"));
    // fw.write(DxfGroup.toString(0, "ENDSEC"));
    //
    // // ECRITURE DES FEATURES
    // fw.write(DxfGroup.toString(0, "SECTION"));
    // fw.write(DxfGroup.toString(2, "ENTITIES"));
    // Iterator it = features.iterator();
    // while( it.hasNext() ) {
    // Feature feature = (Feature) it.next();
    // // use the layer attribute for layer name
    // if (feature.getSchema().hasAttribute("LAYER")) {
    // fw.write(DxfENTITY.feature2Dxf(feature, feature.getString("LAYER"), suffix));
    // }
    // // use the JUMP layer name for DXF layer name
    // else if (layerNames.length > 0) {
    // fw.write(DxfENTITY.feature2Dxf(feature, layerNames[0], suffix));
    // } else {
    // fw.write(DxfENTITY.feature2Dxf(feature, "0", false));
    // }
    // }
    // fw.write(DxfGroup.toString(0, "ENDSEC"));
    //
    // // FIN DE FICHIER
    // fw.write(DxfGroup.toString(0, "EOF"));
    // fw.flush();
    // } catch (IOException ioe) {
    // ioe.printStackTrace();
    // }
    // return;
    // }

}
