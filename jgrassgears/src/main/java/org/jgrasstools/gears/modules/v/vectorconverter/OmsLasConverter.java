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
package org.jgrasstools.gears.modules.v.vectorconverter;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doBbox_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doHeader_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_doInfo_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_inFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_inPolygons_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_outFile_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pClasses_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pCode_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pEast_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pImpulses_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pIntensityrange_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pNorth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pSouth_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLASCONVERTER_pWest_DESCRIPTION;
import static org.jgrasstools.gears.utils.math.NumericsUtilities.dEq;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.io.las.core.ALasReader;
import org.jgrasstools.gears.io.las.core.ILasHeader;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.utils.LasStats;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.CrsUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

@Description(OMSLASCONVERTER_DESCRIPTION)
@Documentation(OMSLASCONVERTER_DOCUMENTATION)
@Author(name = OMSLASCONVERTER_AUTHORNAMES, contact = OMSLASCONVERTER_AUTHORCONTACTS)
@Keywords(OMSLASCONVERTER_KEYWORDS)
@Label(OMSLASCONVERTER_LABEL)
@Name(OMSLASCONVERTER_NAME)
@Status(OMSLASCONVERTER_STATUS)
@License(OMSLASCONVERTER_LICENSE)
public class OmsLasConverter extends JGTModel {

    @Description(OMSLASCONVERTER_inFile_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFile;

    @Description(OMSLASCONVERTER_inPolygons_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPolygons;

    @Description(OMSLASCONVERTER_pIntensityrange_DESCRIPTION)
    @In
    public String pIntensityrange;

    @Description(OMSLASCONVERTER_pImpulses_DESCRIPTION)
    @In
    public String pImpulses;

    @Description(OMSLASCONVERTER_pClasses_DESCRIPTION)
    @In
    public String pClasses;

    // @Description(OMSLASCONVERTER_pIndexrange_DESCRIPTION)
    // @In
    // public String pIndexrange;

    @Description(OMSLASCONVERTER_pNorth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSLASCONVERTER_pSouth_DESCRIPTION)
    @UI(JGTConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSLASCONVERTER_pWest_DESCRIPTION)
    @UI(JGTConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSLASCONVERTER_pEast_DESCRIPTION)
    @UI(JGTConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSLASCONVERTER_pCode_DESCRIPTION)
    @UI(JGTConstants.CRS_UI_HINT)
    @In
    public String pCode;

    @Description(OMSLASCONVERTER_doHeader_DESCRIPTION)
    @In
    public boolean doHeader = false;

    @Description(OMSLASCONVERTER_doInfo_DESCRIPTION)
    @In
    public boolean doInfo = false;

    @Description(OMSLASCONVERTER_doBbox_DESCRIPTION)
    @In
    public boolean doBbox = false;

    @Description(OMSLASCONVERTER_outFile_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFile;

    private double[] intensityRange = null;
    // private long[] indexRange = null;
    private double[] impulses = null;
    private int[] classes = null;

    private ALasReader lasReader;

    private PreparedGeometry filterPolygons;

    private GeometryFactory gf = GeometryUtilities.gf();

    @SuppressWarnings("nls")
    @Execute
    public void process() throws Exception {
        checkNull(inFile);

        if (inPolygons != null) {
            final List<Geometry> polygonsList = FeatureUtilities.featureCollectionToGeometriesList(inPolygons, false, null);
            final GeometryCollection gc = new GeometryCollection(polygonsList.toArray(new Geometry[0]), gf);
            final Geometry polygonUnion = gc.buffer(0.0);
            filterPolygons = PreparedGeometryFactory.prepare(polygonUnion);
        }

        boolean doShapefile = false;
        boolean doCsv = false;
        if (outFile != null) {
            if (outFile.endsWith(".shp")) {
                doShapefile = true;
            } else {
                // any other == csv
                doCsv = true;
            }
        }

        SimpleFeatureCollection outGeodata = new DefaultFeatureCollection();

        CoordinateReferenceSystem crs = null;
        if (pCode != null) {
            crs = CRS.decode(pCode);
        } else {
            // read the prj file
            crs = CrsUtilities.readProjectionFile(inFile, "las");
        }

        final File lasFile = new File(inFile);
        lasReader = ALasReader.getReader(lasFile, crs);
        lasReader.open();

        if (doHeader) {
            final String header = lasReader.getHeader().toString();
            pm.message(header);
            if (!doInfo)
                return;
        }

        if (doBbox && doShapefile) {
            createBboxGeometry(crs, lasFile, outGeodata);
            return;
        }

        if (pIntensityrange != null) {
            final String[] split = pIntensityrange.split(","); //$NON-NLS-1$
            if (split.length != 2) {
                throw new ModelsIllegalargumentException("Intensity range has to be of the form: min,max", this, pm);
            }
            try {
                intensityRange = new double[]{Double.parseDouble(split[0]), Double.parseDouble(split[1])};
            } catch (final Exception e) {
                throw new ModelsIllegalargumentException("Problem in converting min and max to numbers.", this, pm);
            }
        }

        // if (pIndexrange != null) {
        //            final String[] split = pIndexrange.split(","); //$NON-NLS-1$
        // if (split.length != 2) {
        // throw new ModelsIllegalargumentException("Index range has to be of the form: min,max",
        // this);
        // }
        // try {
        // indexRange = new long[]{Long.parseLong(split[0]), Long.parseLong(split[1])};
        // } catch (final Exception e) {
        // throw new
        // ModelsIllegalargumentException("Problem in converting index min and max to numbers.",
        // this);
        // }
        // }

        if (pImpulses != null) {
            final String[] split = pImpulses.split(","); //$NON-NLS-1$
            try {
                impulses = new double[split.length];
                for( int i = 0; i < split.length; i++ ) {
                    impulses[i] = Double.parseDouble(split[i]);
                }
            } catch (final Exception e) {
                throw new ModelsIllegalargumentException("Problem in converting impulses values to numbers.", this, pm);
            }
        }

        if (pClasses != null) {
            final String[] split = pClasses.split(","); //$NON-NLS-1$
            try {
                classes = new int[split.length];
                for( int i = 0; i < split.length; i++ ) {
                    classes[i] = (int) Double.parseDouble(split[i]);
                }
            } catch (final Exception e) {
                throw new ModelsIllegalargumentException("Problem in converting classes to numbers.", this, pm);
            }
        }

        Envelope envelope = null;
        if (pNorth != null && pSouth != null && pEast != null && pWest != null) {
            envelope = new Envelope(pWest, pEast, pSouth, pNorth);
        }

        SimpleFeatureType type = null;
        if (doShapefile) {
            final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("lasdata");
            b.setCRS(crs);
            b.add("the_geom", Point.class);
            b.add("elev", Double.class);
            b.add("intensity", Double.class);
            b.add("classification", Integer.class);
            b.add("impulse", Double.class);
            b.add("numimpulse", Double.class);
            type = b.buildFeatureType();
        }
        BufferedWriter csvWriter = null;
        if (doCsv) {
            csvWriter = new BufferedWriter(new FileWriter(outFile));
        }

        ILasHeader header = lasReader.getHeader();
        final long recordsCount = header.getRecordsCount();
        pm.message("File header info \n" + header);

        // long index = 0;
        long addedFeatures = 0;

        LasStats stats = new LasStats();

        pm.beginTask("Reading las data...", (int) recordsCount);
        while( lasReader.hasNextPoint() ) {
            pm.worked(1);

            // if (indexRange != null) {
            // if (index < indexRange[0]) {
            // // move to the right index
            // final long skip = indexRange[0] - index;
            // lasReader.skipRecords(skip);
            // index = indexRange[0];
            // pm.worked((int) skip);
            // continue;
            // } else if (index > indexRange[1]) {
            // pm.worked((int) (recordsCount - index));
            // // simply stop
            // break;
            // }
            // }
            // index++;
            final LasRecord lasDot = lasReader.getNextPoint();

            final double x = lasDot.x;
            final double y = lasDot.y;
            final double z = lasDot.z;
            final double intensity = lasDot.intensity;
            final int classification = lasDot.classification;
            final double impulse = lasDot.returnNumber;
            final double impulseNumber = lasDot.numberOfReturns;

            final Coordinate tmp = new Coordinate(x, y, z);

            boolean takeIt = true;

            if (envelope != null && envelope.contains(tmp)) {
                takeIt = true;
            }
            if (takeIt && intensityRange != null) {
                takeIt = false;
                if (intensity >= intensityRange[0] && intensity <= intensityRange[1]) {
                    takeIt = true;
                }
            }
            if (takeIt && impulses != null) {
                takeIt = false;
                for( final double imp : impulses ) {
                    if (dEq(impulse, imp)) {
                        takeIt = true;
                        break;
                    }
                }
            }
            if (takeIt && classes != null) {
                takeIt = false;
                for( final double classs : classes ) {
                    if (classification == (int) classs) {
                        takeIt = true;
                        break;
                    }
                }
            }
            if (filterPolygons != null) {
                final Point point = gf.createPoint(tmp);
                if (!filterPolygons.contains(point)) {
                    takeIt = false;
                }
            }

            if (takeIt) {
                if (doInfo) {
                    stats.addClassification(classification);
                    stats.addImpulse((int) impulse);
                    stats.addIntensity(intensity);
                } else if (doShapefile) {
                    final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                    final Point point = gf.createPoint(tmp);
                    final Object[] values = new Object[]{point, z, intensity, classification, impulse, impulseNumber};
                    builder.addAll(values);
                    final SimpleFeature feature = builder.buildFeature(null);
                    ((DefaultFeatureCollection) outGeodata).add(feature);
                    addedFeatures++;
                } else if (doCsv) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(x);
                    sb.append(",");
                    sb.append(y);
                    sb.append(",");
                    sb.append(z);
                    sb.append(",");
                    sb.append(intensity);
                    sb.append(",");
                    sb.append(classification);
                    sb.append(",");
                    sb.append(impulse);
                    sb.append(",");
                    sb.append(impulseNumber);
                    sb.append("\n");
                    csvWriter.write(sb.toString());
                    addedFeatures++;
                }
            }

        }
        pm.done();

        if (doInfo) {
            pm.message(stats.toString());
        }

        if (addedFeatures != 0) {
            pm.message("Points extracted: " + addedFeatures);
        }

        if (doCsv) {
            csvWriter.close();
        }

        if (doShapefile) {
            OmsVectorWriter.writeVector(outFile, outGeodata);
        }

        if (lasReader != null)
            lasReader.close();
    }

    private void createBboxGeometry( CoordinateReferenceSystem crs, File lasFile, SimpleFeatureCollection outGeodata )
            throws IOException {
        final ReferencedEnvelope3D envelope = lasReader.getHeader().getDataEnvelope();
        ReferencedEnvelope env2d = new ReferencedEnvelope(envelope);
        final Polygon polygon = FeatureUtilities.envelopeToPolygon(new Envelope2D(env2d));

        final SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("lasdataenvelope");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("id", String.class);
        final SimpleFeatureType type = b.buildFeatureType();

        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        final Object[] values = new Object[]{polygon, lasFile.getName()};
        builder.addAll(values);
        final SimpleFeature feature = builder.buildFeature(null);
        ((DefaultFeatureCollection) outGeodata).add(feature);
        OmsVectorWriter.writeVector(outFile, outGeodata);
    }

    // public static void main( String[] args ) throws Exception {
    // SimpleFeatureCollection plots =
    // OmsVectorReader.readVector("/media/hydrologis/LESTO/unibz/plot_data/12_plots_32632.shp");
    // List<SimpleFeature> list = FeatureUtilities.featureCollectionToList(plots);
    // DefaultFeatureCollection d = new DefaultFeatureCollection();
    // for( SimpleFeature simpleFeature : list ) {
    // if (simpleFeature.getAttribute("ID").toString().equals("878")) {
    // d.add(simpleFeature);
    // break;
    // }
    // }
    //
    // String path = "/media/hydrologis/LESTO/unibz/LAS_Classificati/uni_bz_63.las";
    // OmsLasConverter c = new OmsLasConverter();
    // c.inFile = path;
    // c.inPolygons = d;
    // c.outFile = "/media/hydrologis/LESTO/unibz/LAS_Classificati/uni_bz_plot878.shp";
    // c.process();
    //
    // }

}
